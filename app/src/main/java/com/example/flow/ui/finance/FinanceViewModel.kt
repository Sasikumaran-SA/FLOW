package com.example.flow.ui.finance

import android.app.Application
import androidx.lifecycle.*
import com.example.flow.data.local.AppDatabase
import com.example.flow.data.model.Transaction
import com.example.flow.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    val monthlySpending: LiveData<Double>

    private val auth = FirebaseAuth.getInstance()
    private var hasListenerBeenSet = false

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        val pendingDeletionDao = db.pendingDeletionDao()
        val firestore = FirebaseFirestore.getInstance()

        repository = TransactionRepository(transactionDao, pendingDeletionDao, firestore)

        allTransactions = repository.getAllTransactions().asLiveData()

        monthlySpending = allTransactions.map { transactions ->
            calculateMonthlySpending(transactions)
        }

        setupSyncAndListener()
        attemptPendingDeletions()
    }

    private fun setupSyncAndListener() {
        if (!hasListenerBeenSet && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncTransactionsFromFirebase()
            }
            repository.setupRealtimeListener()
            hasListenerBeenSet = true
        }
    }

    private fun attemptPendingDeletions() {
        if (auth.currentUser != null) {
            viewModelScope.launch {
                repository.attemptPendingDeletions()
            }
        }
    }

    fun onLogout() {
        repository.removeListener()
        hasListenerBeenSet = false
    }

    private fun calculateMonthlySpending(transactions: List<Transaction>): Double {
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

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
        attemptPendingDeletions()
    }

    fun getTransactionById(transactionId: String): LiveData<Transaction?> {
        return repository.getTransactionById(transactionId).asLiveData()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}