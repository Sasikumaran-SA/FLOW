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

// --- UPDATED CONSTRUCTOR ---
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "transactions"

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getTransactionsCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME)

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
        val userId = getUserId()
        return if (userId == null) {
            flowOf(null)
        } else {
            transactionDao.getTransactionById(transactionId)
        }
    }

    suspend fun insert(transaction: Transaction, receiptUri: Uri?) {
        // ... (no changes to insert logic)
        var finalTransaction = transaction
        val userId = getUserId()

        if (receiptUri != null) {
            if (userId != null) {
                val imageUrl = uploadReceipt(userId, receiptUri, transaction.id)
                imageUrl?.let {
                    finalTransaction = transaction.copy(receiptImageUrl = it)
                }
            } else {
                Log.w("TransactionRepo", "Offline: Receipt will be uploaded on next update.")
            }
        }

        transactionDao.insertTransaction(finalTransaction)

        if (userId == null) {
            Log.w("TransactionRepo", "User offline. Transaction saved locally.")
            return
        }
        try {
            getTransactionsCollection(userId).document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Saved locally, but Firestore insert failed.", e)
        }
    }

    suspend fun update(transaction: Transaction, newReceiptUri: Uri?) {
        // ... (no changes to update logic)
        var finalTransaction = transaction
        val userId = getUserId()

        if (newReceiptUri != null) {
            if (userId != null) {
                val imageUrl = uploadReceipt(userId, newReceiptUri, transaction.id)
                imageUrl?.let {
                    finalTransaction = transaction.copy(receiptImageUrl = it)
                }
            } else {
                Log.w("TransactionRepo", "Offline: Receipt will be uploaded on next update.")
            }
        }

        transactionDao.updateTransaction(finalTransaction)

        if (userId == null) {
            Log.w("TransactionRepo", "User offline. Transaction updated locally.")
            return
        }
        try {
            getTransactionsCollection(userId).document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Updated locally, but Firestore update failed.", e)
        }
    }

    // --- HEAVILY UPDATED DELETE LOGIC ---
    suspend fun delete(transaction: Transaction) {
        try {
            // 1. Delete receipt from storage *if we are online*
            // We do this first, because it's okay if it fails and the receipt is orphaned.
            val userId = getUserId()
            if (userId != null && transaction.receiptImageUrl != null) {
                try {
                    storage.getReferenceFromUrl(transaction.receiptImageUrl).delete().await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("TransactionRepo", "Failed to delete receipt from Storage, but will proceed.", e)
                }
            }

            // 2. Delete from local Room database FIRST.
            transactionDao.deleteTransaction(transaction)

            // 3. Add a "tombstone" to the pending deletions queue.
            pendingDeletionDao.insert(PendingDeletion(transaction.id, COLLECTION_NAME))

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TransactionRepo", "Error deleting transaction locally", e)
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

    // --- HEAVILY UPDATED SYNC LOGIC ---
    suspend fun syncTransactionsFromFirebase() {
        val userId = getUserId() ?: return
        try {
            // 1. Get all IDs of items we've deleted locally.
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            val snapshot = getTransactionsCollection(userId).get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)

            for (transaction in transactions) {
                // 2. ONLY add transactions from Firebase if they are NOT in our deletion queue
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

    // --- NEW FUNCTION ---
    /**
     * Tries to clear the pending deletion queue by deleting items from Firebase.
     */
    suspend fun attemptPendingDeletions() {
        val userId = getUserId() ?: return // Not online

        val pendingDeletions = pendingDeletionDao.getAllPendingDeletions()
            .filter { it.collectionName == COLLECTION_NAME }

        for (pending in pendingDeletions) {
            try {
                // 1. Try to delete from Firebase
                getTransactionsCollection(userId).document(pending.id).delete().await()

                // 2. If successful, remove it from the local queue
                pendingDeletionDao.delete(pending)
                Log.i("TransactionRepo", "Successfully deleted ${pending.id} from Firebase.")

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TransactionRepo", "Failed to delete ${pending.id} from Firebase. Will retry later.", e)
            }
        }
    }
}