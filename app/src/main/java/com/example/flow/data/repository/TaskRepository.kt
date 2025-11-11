package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.local.TaskDao
import com.example.flow.data.model.PendingDeletion
import com.example.flow.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

// --- UPDATED CONSTRUCTOR ---
class TaskRepository(
    private val taskDao: TaskDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val firestore: FirebaseFirestore
) {

    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "tasks"

    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getTasksCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME)

    fun getAllTasks(): Flow<List<Task>> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(emptyList())
        } else {
            // DAO is already filtering out isDeleted=true items
            taskDao.getAllTasks(userId)
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(null)
        } else {
            taskDao.getTaskById(taskId)
        }
    }

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)

        val userId = getUserId()
        if (userId == null) {
            Log.w("TaskRepository", "User offline. Task saved locally.")
            return
        }
        try {
            getTasksCollection(userId).document(task.id).set(task).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Saved locally, but Firestore insert failed.", e)
        }
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)

        val userId = getUserId()
        if (userId == null) {
            Log.w("TaskRepository", "User offline. Task updated locally.")
            return
        }
        try {
            getTasksCollection(userId).document(task.id).set(task).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Updated locally, but Firestore update failed.", e)
        }
    }

    // --- HEAVILY UPDATED DELETE LOGIC ---
    suspend fun delete(task: Task) {
        try {
            // 1. Delete from local Room database FIRST.
            // This makes the UI update instantly.
            taskDao.deleteTask(task)

            // 2. Add a "tombstone" to the pending deletions queue.
            pendingDeletionDao.insert(PendingDeletion(task.id, COLLECTION_NAME))

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Error deleting task locally", e)
        }
    }

    // --- HEAVILY UPDATED SYNC LOGIC ---
    suspend fun syncTasksFromFirebase() {
        val userId = getUserId() ?: return
        try {
            // 1. Get all IDs of items we've deleted locally.
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            val snapshot = getTasksCollection(userId).get().await()
            val tasks = snapshot.toObjects(Task::class.java)

            for (task in tasks) {
                // 2. ONLY add tasks from Firebase if they are NOT in our deletion queue
                if (task.id !in deletedIds) {
                    taskDao.insertTask(task)
                }
            }
            Log.d("TaskRepository", "Successfully synced ${tasks.size} tasks.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Error syncing tasks from Firestore", e)
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
                getTasksCollection(userId).document(pending.id).delete().await()

                // 2. If successful, remove it from the local queue
                pendingDeletionDao.delete(pending)
                Log.i("TaskRepository", "Successfully deleted ${pending.id} from Firebase.")

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TaskRepository", "Failed to delete ${pending.id} from Firebase. Will retry later.", e)
            }
        }
    }
}