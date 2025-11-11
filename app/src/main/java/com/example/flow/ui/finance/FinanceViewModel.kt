package com.example.flow.ui.finance

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.flow.data.local.AppDatabase
import com.example.flow.data.model.Transaction
import com.example.flow.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    val monthlySpending: LiveData<Double>

    private val auth = FirebaseAuth.getInstance()
    private var hasSynced = false

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        val pendingDeletionDao = db.pendingDeletionDao() // --- ADDED ---
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        // --- UPDATED CONSTRUCTOR ---
        repository = TransactionRepository(transactionDao, pendingDeletionDao, firestore, storage)

        allTransactions = repository.getAllTransactions().asLiveData()

        monthlySpending = allTransactions.map { transactions ->
            calculateMonthlySpending(transactions)
        }

        // Sync data and attempt to clear pending deletions on startup
        syncTransactions()
        attemptPendingDeletions()
    }

    private fun syncTransactions() {
        if (!hasSynced && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncTransactionsFromFirebase()
                hasSynced = true
            }
        }
    }

    // --- ADDED ---
    private fun attemptPendingDeletions() {
        if (auth.currentUser != null) {
            viewModelScope.launch {
                repository.attemptPendingDeletions()
            }
        }
    }

    private fun calculateMonthlySpending(transactions: List<Transaction>): Double {
        // ... (no changes to this function)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions.filter {
            val itemCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
            !it.isIncome &&
                    itemCalendar.get(Calendar.MONTH) == currentMonth &&
                    itemCalendar.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun insert(transaction: Transaction, receiptUri: Uri?) = viewModelScope.launch {
        repository.insert(transaction, receiptUri)
    }

    fun update(transaction: Transaction, receiptUri: Uri?) = viewModelScope.launch {
        repository.update(transaction, receiptUri)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
        // Try to delete from Firebase immediately
        attemptPendingDeletions()
    }

    fun getTransactionById(transactionId: String): LiveData<Transaction?> {
        return repository.getTransactionById(transactionId).asLiveData()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}