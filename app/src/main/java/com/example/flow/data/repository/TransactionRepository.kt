package com.example.flow.data.repository

import android.net.Uri
import android.util.Log
import com.example.flow.data.local.TransactionDao
import com.example.flow.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val auth = FirebaseAuth.getInstance()
    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getTransactionsCollection() = firestore
        .collection("users")
        .document(getUserId()!!)
        .collection("transactions")

    private fun getReceiptsStorageRef() = storage.reference
        .child("receipts")
        .child(getUserId()!!)

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(getUserId()!!)
    }

    fun getTransactionById(transactionId: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun insert(transaction: Transaction, receiptUri: Uri?) {
        var finalTransaction = transaction
        // 1. If user attached a receipt, upload it
        if (receiptUri != null) {
            val imageUrl = uploadReceipt(receiptUri, transaction.id)
            imageUrl?.let {
                finalTransaction = transaction.copy(receiptImageUrl = it)
            }
        }

        // 2. Save final transaction (with or without URL) to Room
        transactionDao.insertTransaction(finalTransaction)

        // 3. Save to Firestore
        try {
            getTransactionsCollection().document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error inserting transaction to Firestore", e)
        }
    }

    suspend fun update(transaction: Transaction, newReceiptUri: Uri?) {
        var finalTransaction = transaction
        // 1. If user attached a *new* receipt, upload it
        if (newReceiptUri != null) {
            // Note: This doesn't delete the old receipt, but could be added
            val imageUrl = uploadReceipt(newReceiptUri, transaction.id)
            imageUrl?.let {
                finalTransaction = transaction.copy(receiptImageUrl = it)
            }
        }

        // 2. Update in Room
        transactionDao.updateTransaction(finalTransaction)

        // 3. Update in Firestore
        try {
            getTransactionsCollection().document(finalTransaction.id).set(finalTransaction).await()
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error updating transaction in Firestore", e)
        }
    }

    // --- UPDATED DELETE FUNCTION ---
    suspend fun delete(transaction: Transaction) {
        try {
            // 1. Delete from Firestore FIRST
            getTransactionsCollection().document(transaction.id).delete().await()

            // 2. ONLY if remote delete succeeds, delete from local Room
            transactionDao.deleteTransaction(transaction)

            // 3. (Optional) Delete receipt from Storage if it exists
            transaction.receiptImageUrl?.let {
                try {
                    storage.getReferenceFromUrl(it).delete().await()
                } catch (e: Exception) {
                    Log.e("TransactionRepo", "Error deleting receipt from Storage", e)
                }
            }
        } catch (e: Exception) {
            // If remote delete fails, log the error and DO NOT delete from local.
            Log.e("TransactionRepo", "Error deleting transaction from Firestore", e)
        }
    }

    private suspend fun uploadReceipt(uri: Uri, transactionId: String): String? {
        return try {
            val ref = getReceiptsStorageRef().child("${transactionId}_${UUID.randomUUID()}.jpg")
            val uploadTask = ref.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error uploading receipt", e)
            null
        }
    }

    suspend fun syncTransactionsFromFirebase() {
        if (getUserId() == null) return
        try {
            val snapshot = getTransactionsCollection().get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)
            for (transaction in transactions) {
                transactionDao.insertTransaction(transaction)
            }
            Log.d("TransactionRepo", "Successfully synced ${transactions.size} transactions.")
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error syncing transactions from Firestore", e)
        }
    }
}