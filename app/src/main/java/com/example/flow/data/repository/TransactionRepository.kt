package com.example.flow.data.repository

import android.net.Uri
import android.util.Log
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.local.TransactionDao
import com.example.flow.data.model.PendingDeletion
import com.example.flow.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingDeletionDao: PendingDeletionDao, // --- ADDED ---
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "transactions" // --- ADDED ---

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getTransactionsCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME) // --- UPDATED ---

    private fun getReceiptsStorageRef(userId: String) = storage.reference
        .child("receipts")
        .child(userId)

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

    suspend fun insert(transaction: Transaction, receiptUri: Uri?) {
        val userId = getUserId()
        var finalTransaction = transaction

        // 1. Handle receipt upload (only if online)
        if (receiptUri != null && userId != null) {
            try {
                val imageUrl = uploadReceipt(userId, receiptUri, transaction.id)
                imageUrl?.let {
                    finalTransaction = transaction.copy(receiptImageUrl = it)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Error uploading receipt", e)
            }
        }

        // 2. Save final transaction to Room
        transactionDao.insertTransaction(finalTransaction)

        // 3. Save to Firestore (only if online)
        if (userId == null) {
            Log.w("TransactionRepo", "User is offline. Transaction saved locally.")
            return
        }

        try {
            getTransactionsCollection(userId).document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error inserting transaction to Firestore", e)
        }
    }

    suspend fun update(transaction: Transaction, newReceiptUri: Uri?) {
        val userId = getUserId()
        var finalTransaction = transaction

        // 1. Handle new receipt upload (only if online)
        if (newReceiptUri != null && userId != null) {
            try {
                val imageUrl = uploadReceipt(userId, newReceiptUri, transaction.id)
                imageUrl?.let {
                    finalTransaction = transaction.copy(receiptImageUrl = it)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Error uploading new receipt", e)
            }
        }

        // 2. Update in Room
        transactionDao.updateTransaction(finalTransaction)

        // 3. Update in Firestore (only if online)
        if (userId == null) {
            Log.w("TransactionRepo", "User is offline. Transaction updated locally.")
            return
        }

        try {
            getTransactionsCollection(userId).document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error updating transaction in Firestore", e)
        }
    }

    // --- UPDATED DELETE LOGIC ---
    suspend fun delete(transaction: Transaction) {
        // 1. Delete from Room FIRST
        transactionDao.deleteTransaction(transaction)
        // 2. Add to pending deletions queue
        pendingDeletionDao.insert(PendingDeletion(id = transaction.id, collectionName = COLLECTION_NAME))
        Log.d("TransactionRepo", "Transaction deleted locally and queued for remote deletion.")

        // 3. (Optional) Immediately try to delete receipt from Storage if online
        val userId = getUserId()
        if (userId != null && transaction.receiptImageUrl != null) {
            try {
                storage.getReferenceFromUrl(transaction.receiptImageUrl).delete().await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Error deleting receipt from Storage. Will not retry.", e)
            }
        }
    }

    private suspend fun uploadReceipt(userId: String, uri: Uri, transactionId: String): String? {
        return try {
            val ref = getReceiptsStorageRef(userId).child("${transactionId}_${UUID.randomUUID()}.jpg")
            val uploadTask = ref.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error uploading receipt", e)
            null
        }
    }

    // --- UPDATED SYNC LOGIC ---
    suspend fun syncTransactionsFromFirebase() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("TransactionRepo", "User is offline, cannot sync.")
            return
        }

        try {
            // 1. Get IDs of all locally deleted items
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            // 2. Get all items from Firebase
            val snapshot = getTransactionsCollection(userId).get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)

            // 3. Sync to Room
            for (transaction in transactions) {
                // ONLY insert if the transaction is NOT in our pending deletion queue
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

    // --- ADDED FUNCTION ---
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
                // 1. Try to delete from Firebase
                getTransactionsCollection(userId).document(id).delete().await()
                // 2. If successful, remove from local queue
                pendingDeletionDao.delete(PendingDeletion(id, COLLECTION_NAME))
                Log.d("TransactionRepo", "Successfully deleted $id from Firebase.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Failed to delete $id from Firebase. Will retry later.", e)
            }
        }
    }
}