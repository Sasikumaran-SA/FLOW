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

    // LiveData for monthly spending summary (FR-10)
    val monthlySpending: LiveData<Double>

    private val auth = FirebaseAuth.getInstance()
    private var hasSynced = false // --- ADDED ---

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        repository = TransactionRepository(transactionDao, firestore, storage)
        allTransactions = repository.getAllTransactions().asLiveData()

        // This line is correct. It uses the .map KTX extension function.
        monthlySpending = allTransactions.map { transactions ->
            calculateMonthlySpending(transactions)
        }

        syncTransactions() // --- ADDED ---
    }

    // --- ADDED ---
    private fun syncTransactions() {
        if (!hasSynced && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncTransactionsFromFirebase()
                hasSynced = true
            }
        }
    }

    private fun calculateMonthlySpending(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions.filter {
            // Filter for expenses in the current month and year
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
    }

    fun getTransactionById(transactionId: String): LiveData<Transaction?> {
        return repository.getTransactionById(transactionId).asLiveData()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}