package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.local.TaskDao
import com.example.flow.data.model.PendingDeletion
import com.example.flow.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class TaskRepository(
    private val taskDao: TaskDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val firestore: FirebaseFirestore
) {

    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "tasks"
    private var taskListenerRegistration: ListenerRegistration? = null

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
            taskDao.getAllTasks(userId)
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return taskDao.getTaskById(taskId)
    }

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)

        val userId = getUserId()
        if (userId == null) {
            Log.w("TaskRepository", "User is offline. Task saved locally.")
            return
        }

        try {
            getTasksCollection(userId).document(task.id).set(task).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Error inserting task to Firestore", e)
        }
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)

        val userId = getUserId()
        if (userId == null) {
            Log.w("TaskRepository", "User is offline. Task updated locally.")
            return
        }

        try {
            getTasksCollection(userId).document(task.id).set(task).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskRepository", "Error updating task in Firestore", e)
        }
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
        pendingDeletionDao.insert(PendingDeletion(id = task.id, collectionName = COLLECTION_NAME))
        Log.d("TaskRepository", "Task deleted locally and queued for remote deletion.")
    }

    suspend fun syncTasksFromFirebase() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("TaskRepository", "User is offline, cannot sync.")
            return
        }

        try {
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            val snapshot = getTasksCollection(userId).get().await()
            val tasks = snapshot.toObjects(Task::class.java)

            for (task in tasks) {
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

    suspend fun attemptPendingDeletions() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("TaskRepository", "User is offline, cannot process pending deletions.")
            return
        }

        val pending = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)
        if (pending.isEmpty()) return

        Log.d("TaskRepository", "Attempting to clear ${pending.size} pending deletions...")
        for (id in pending) {
            try {
                getTasksCollection(userId).document(id).delete().await()
                pendingDeletionDao.delete(PendingDeletion(id, COLLECTION_NAME))
                Log.d("TaskRepository", "Successfully deleted $id from Firebase.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TaskRepository", "Failed to delete $id from Firebase. Will retry later.", e)
            }
        }
    }

    fun setupRealtimeListener() {
        val userId = getUserId()
        if (userId == null) {
            Log.w("TaskRepository", "Cannot setup listener, user not logged in.")
            return
        }

        taskListenerRegistration?.remove()
        val tasksCollection = getTasksCollection(userId)

        taskListenerRegistration = tasksCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("TaskRepository", "Firebase listener failed.", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            CoroutineScope(Dispatchers.IO).launch {
                handleSnapshotChanges(snapshots)
            }
        }
        Log.d("TaskRepository", "Real-time task listener attached.")
    }

    private suspend fun handleSnapshotChanges(snapshots: QuerySnapshot) {
        val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

        for (docChange in snapshots.documentChanges) {
            try {
                val task = docChange.document.toObject(Task::class.java)
                when (docChange.type) {
                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        if (task.id !in deletedIds) {
                            taskDao.insertTask(task)
                        }
                    }
                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        taskDao.deleteTask(task)
                        pendingDeletionDao.delete(PendingDeletion(task.id, COLLECTION_NAME))
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskRepository", "Error processing snapshot change.", e)
            }
        }
    }

    fun removeListener() {
        taskListenerRegistration?.remove()
        taskListenerRegistration = null
        Log.d("TaskRepository", "Task listener removed.")
    }
}