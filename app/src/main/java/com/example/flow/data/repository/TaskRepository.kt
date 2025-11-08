package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.TaskDao
import com.example.flow.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await // Make sure this import is here

class TaskRepository(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore
) {

    private val auth = FirebaseAuth.getInstance()

    // Helper to get the current user's ID
    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    // Helper to get the user-specific tasks collection
    private fun getTasksCollection() = firestore
        .collection("users")
        .document(getUserId()!!) // Assume user is logged in for repository actions
        .collection("tasks")

    // --- Local Database (Room) Operations ---

    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks(getUserId() ?: "")
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return taskDao.getTaskById(taskId=getUserId() ?: "")
    }

    // --- Combined (Local + Remote) Operations ---

    suspend fun insert(task: Task) {
        // 1. Insert into local Room database
        taskDao.insertTask(task)
        // 2. Insert into remote Firestore database
        try {
            getTasksCollection().document(task.id).set(task).await()
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error inserting task to Firestore", e)
        }
    }

    suspend fun update(task: Task) {
        // 1. Update in local Room database
        taskDao.updateTask(task)
        // 2. Update in remote Firestore database
        try {
            getTasksCollection().document(task.id).set(task).await() // `set` also works for update
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error updating task in Firestore", e)
        }
    }

    suspend fun delete(task: Task) {
        // 1. Delete from local Room database
        taskDao.deleteTask(task)
        // 2. Delete from remote Firestore database
        try {
            getTasksCollection().document(task.id).delete().await()
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error deleting task from Firestore", e)
        }
    }

    // This version is INTENTIONALLY MISSING the 'syncTasksFromFirebase' function.
}