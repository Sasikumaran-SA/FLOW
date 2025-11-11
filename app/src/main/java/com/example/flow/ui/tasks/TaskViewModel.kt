package com.example.flow.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.flow.data.local.AppDatabase
import com.example.flow.data.model.Task
import com.example.flow.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val allTasks: LiveData<List<Task>>

    private val auth = FirebaseAuth.getInstance()
    private var hasSynced = false

    init {
        val db = AppDatabase.getDatabase(application)
        val taskDao = db.taskDao()
        val pendingDeletionDao = db.pendingDeletionDao() // --- ADDED ---
        val firestore = FirebaseFirestore.getInstance()
        // --- UPDATED CONSTRUCTOR ---
        repository = TaskRepository(taskDao, pendingDeletionDao, firestore)

        allTasks = repository.getAllTasks().asLiveData()

        // Sync data and attempt to clear pending deletions on startup
        syncTasks()
        attemptPendingDeletions()
    }

    private fun syncTasks() {
        if (!hasSynced && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncTasksFromFirebase()
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

    fun insert(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun update(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
        // Try to delete from Firebase immediately
        attemptPendingDeletions()
    }

    fun getTaskById(taskId: String): LiveData<Task?> {
        return repository.getTaskById(taskId).asLiveData()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}