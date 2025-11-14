package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.NoteDao
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.model.Note
import com.example.flow.data.model.PendingDeletion
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

class NoteRepository(
    private val noteDao: NoteDao,
    private val pendingDeletionDao: PendingDeletionDao, // --- ADDED ---
    private val firestore: FirebaseFirestore
) {
    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "notes" // --- ADDED ---
    private var noteListenerRegistration: ListenerRegistration? = null

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getNotesCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME) // --- UPDATED ---

    fun getAllNotes(): Flow<List<Note>> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(emptyList())
        } else {
            noteDao.getAllNotes(userId)
        }
    }

    fun getNoteById(noteId: String): Flow<Note?> {
        return noteDao.getNoteById(noteId)
    }

    suspend fun insert(note: Note) {
        // 1. Insert into local Room database
        noteDao.insertNote(note)

        // 2. Insert into remote Firestore database (only if online)
        val userId = getUserId()
        if (userId == null) {
            Log.w("NoteRepository", "User is offline. Note saved locally.")
            return
        }

        try {
            getNotesCollection(userId).document(note.id).set(note).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Error inserting note to Firestore", e)
        }
    }

    suspend fun update(note: Note) {
        // 1. Update in local Room database
        noteDao.updateNote(note)

        // 2. Update in remote Firestore database (only if online)
        val userId = getUserId()
        if (userId == null) {
            Log.w("NoteRepository", "User is offline. Note updated locally.")
            return
        }

        try {
            getNotesCollection(userId).document(note.id).set(note).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Error updating note in Firestore", e)
        }
    }

    // --- UPDATED DELETE LOGIC ---
    suspend fun delete(note: Note) {
        // 1. Delete from local Room database FIRST
        noteDao.deleteNote(note)
        // 2. Add to pending deletions queue
        pendingDeletionDao.insert(PendingDeletion(id = note.id, collectionName = COLLECTION_NAME))
        Log.d("NoteRepository", "Note deleted locally and queued for remote deletion.")
    }

    // --- UPDATED SYNC LOGIC ---
    suspend fun syncNotesFromFirebase() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("NoteRepository", "User is offline, cannot sync.")
            return
        }

        try {
            // 1. Get IDs of all locally deleted items
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            // 2. Get all items from Firebase
            val snapshot = getNotesCollection(userId).get().await()
            val notes = snapshot.toObjects(Note::class.java)

            // 3. Sync to Room
            for (note in notes) {
                // ONLY insert if the note is NOT in our pending deletion queue
                if (note.id !in deletedIds) {
                    noteDao.insertNote(note)
                }
            }
            Log.d("NoteRepository", "Successfully synced ${notes.size} notes.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Error syncing notes from Firestore", e)
        }
    }

    // --- ADDED FUNCTION ---
    suspend fun attemptPendingDeletions() {
        val userId = getUserId()
        if (userId == null) {
            Log.d("NoteRepository", "User is offline, cannot process pending deletions.")
            return
        }

        val pending = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)
        if (pending.isEmpty()) return

        Log.d("NoteRepository", "Attempting to clear ${pending.size} pending deletions...")
        for (id in pending) {
            try {
                // 1. Try to delete from Firebase
                getNotesCollection(userId).document(id).delete().await()
                // 2. If successful, remove from local queue
                pendingDeletionDao.delete(PendingDeletion(id, COLLECTION_NAME))
                Log.d("NoteRepository", "Successfully deleted $id from Firebase.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("NoteRepository", "Failed to delete $id from Firebase. Will retry later.", e)
            }
        }
    }

    fun setupRealtimeListener() {
        val userId = getUserId()
        if (userId == null) {
            Log.w("NoteRepository", "Cannot setup listener, user not logged in.")
            return
        }

        noteListenerRegistration?.remove()
        val notesCollection = getNotesCollection(userId)

        noteListenerRegistration = notesCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("NoteRepository", "Firebase listener failed.", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            CoroutineScope(Dispatchers.IO).launch {
                handleSnapshotChanges(snapshots)
            }
        }
        Log.d("NoteRepository", "Real-time note listener attached.")
    }

    private suspend fun handleSnapshotChanges(snapshots: QuerySnapshot) {
        val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

        for (docChange in snapshots.documentChanges) {
            try {
                val note = docChange.document.toObject(Note::class.java)
                when (docChange.type) {
                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        if (note.id !in deletedIds) {
                            noteDao.insertNote(note)
                        }
                    }

                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        noteDao.deleteNote(note)
                        pendingDeletionDao.delete(PendingDeletion(note.id, COLLECTION_NAME))
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error processing snapshot change.", e)
            }
        }
    }

    fun removeListener() {
        noteListenerRegistration?.remove()
        noteListenerRegistration = null
        Log.d("NoteRepository", "Note listener removed.")
    }
}