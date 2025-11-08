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
    private var hasSynced = false // --- ADDED ---

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        val firestore = FirebaseFirestore.getInstance()
        repository = TaskRepository(taskDao, firestore)
        allTasks = repository.getAllTasks().asLiveData()

        syncTasks() // --- ADDED ---
    }

    // --- ADDED ---
    private fun syncTasks() {
        // Only sync if we haven't already and a user is logged in
        if (!hasSynced && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncTasksFromFirebase()
                hasSynced = true
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
    }

    fun getTaskById(taskId: String): LiveData<Task?> {
        // .asLiveData() converts the Flow to LiveData
        return repository.getTaskById(taskId).asLiveData()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}