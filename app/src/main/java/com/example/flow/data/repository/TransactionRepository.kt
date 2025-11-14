package com.example.flow.data.repository

import android.util.Log
import android.widget.Toast
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.local.TransactionDao
import com.example.flow.data.model.PendingDeletion
import com.example.flow.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

//fab.setOnClickListener {
//    Log.d("FirebaseTest", "FAB clicked. Attempting direct write...")
//    Toast.makeText(context, "Running Firebase Test...", Toast.LENGTH_SHORT).show()
//
//    val auth = FirebaseAuth.getInstance()
//    val firestore = FirebaseFirestore.getInstance()
//
//    val userId = auth.currentUser?.uid
//    if (userId == null) {
//        Log.e("FirebaseTest", "Test FAILED: User is null. Not logged in.")
//        Toast.makeText(context, "TEST FAILED: User is null", Toast.LENGTH_SHORT).show()
//        return@setOnClickListener
//    }
//
//    Log.d("FirebaseTest", "User is: $userId. Creating test document...")
//
//    val testDoc = hashMapOf(
//        "testName" to "My Test Transaction",
//        "timestamp" to System.currentTimeMillis()
//    )
//
//    // This writes to /users/{userId}/test_collection/test_doc
//    firestore.collection("users").document(userId)
//        .collection("test_collection").document("test_doc")
//        .set(testDoc)
//        .addOnSuccessListener {
//            Log.d("FirebaseTest", "SUCCESS! Direct write to Firestore worked.")
//            Toast.makeText(context, "TEST SUCCESS: Data written!", Toast.LENGTH_SHORT).show()
//        }
//        .addOnFailureListener { e ->
//            Log.e("FirebaseTest", "FAILURE! Direct write FAILED.", e)
//            Toast.makeText(context, "TEST FAILED: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val firestore: FirebaseFirestore
) {

    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "transactions"
    private var transactionListenerRegistration: ListenerRegistration? = null

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getTransactionsCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME)

    fun getAllTransactions(): Flow<List<Transaction>> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(emptyList())
        } else {
            transactionDao.getAllTransactions(userId)
        }
    }

    fun getTransactionById(transactionId: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun insert(transaction: Transaction) {
        val userId = transaction.userId // Use userId from the object
        transactionDao.insertTransaction(transaction)

        if (userId.isEmpty()) {
            Log.w("TransactionRepo", "Transaction has empty userId. Saved locally.")
            return
        }
        try {
            // Create a document reference to get the ID
            val docRef = getTransactionsCollection(userId).document()
            // Create a new transaction object with the document ID included
            val transactionWithId = transaction.copy(id = docRef.id)
            docRef.set(transactionWithId).await()
            Log.d("TransactionRepo", "SUCCESS! Transaction written to Firestore: ${docRef.id}")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error inserting transaction to Firestore", e)
            // Show user-facing alert on save failure
            // Note: This would require passing context or using a callback
        }
    }

    suspend fun update(transaction: Transaction) {
        val userId = transaction.userId // Use userId from the object
        transactionDao.updateTransaction(transaction)

        if (userId.isEmpty()) {
            Log.w("TransactionRepo", "Transaction has empty userId. Updated locally.")
            return
        }
        try {
            // Ensure the transaction object includes the document ID
            getTransactionsCollection(userId).document(transaction.id).set(transaction).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error updating transaction in Firestore", e)
            // Show user-facing alert on save failure
            // Note: This would require passing context or using a callback
        }
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
        pendingDeletionDao.insert(PendingDeletion(id = transaction.id, collectionName = COLLECTION_NAME))
        Log.d("TransactionRepo", "Transaction deleted locally and queued for remote deletion.")
    }

    suspend fun syncTransactionsFromFirebase() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("TransactionRepo", "User is offline, cannot sync.")
            return
        }
        try {
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)
            val snapshot = getTransactionsCollection(userId).get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)

            for (transaction in transactions) {
                if (transaction.id !in deletedIds) {
                    transactionDao.insertTransaction(transaction)
                }
            }
            Log.d("TransactionRepo", "Successfully synced ${transactions.size} transactions.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error syncing transactions from Firestore", e)
        }
    }

    suspend fun attemptPendingDeletions() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("TransactionRepo", "User is offline, cannot process pending deletions.")
            return
        }
        val pending = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)
        if (pending.isEmpty()) return

        Log.d("TransactionRepo", "Attempting to clear ${pending.size} pending deletions...")
        for (id in pending) {
            try {
                getTransactionsCollection(userId).document(id).delete().await()
                pendingDeletionDao.delete(PendingDeletion(id, COLLECTION_NAME))
                Log.d("TransactionRepo", "Successfully deleted $id from Firebase.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Failed to delete $id from Firebase. Will retry later.", e)
            }
        }
    }

    fun setupRealtimeListener() {
        val userId = getUserId()
        if (userId == null) {
            Log.w("TransactionRepo", "Cannot setup listener, user not logged in.")
            return
        }

        transactionListenerRegistration?.remove()
        val transactionsCollection = getTransactionsCollection(userId)

        transactionListenerRegistration = transactionsCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("TransactionRepo", "Firebase listener failed.", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            CoroutineScope(Dispatchers.IO).launch {
                handleSnapshotChanges(snapshots)
            }
        }
        Log.d("TransactionRepo", "Real-time transaction listener attached.")
    }

    private suspend fun handleSnapshotChanges(snapshots: QuerySnapshot) {
        val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

        for (docChange in snapshots.documentChanges) {
            try {
                val transaction = docChange.document.toObject(Transaction::class.java)
                when (docChange.type) {
                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        if (transaction.id !in deletedIds) {
                            transactionDao.insertTransaction(transaction)
                        }
                    }
                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        transactionDao.deleteTransaction(transaction)
                        pendingDeletionDao.delete(PendingDeletion(transaction.id, COLLECTION_NAME))
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionRepo", "Error processing snapshot change.", e)
            }
        }
    }

    fun removeListener() {
        transactionListenerRegistration?.remove()
        transactionListenerRegistration = null
        Log.d("TransactionRepo", "Transaction listener removed.")
    }
}