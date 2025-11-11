package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.NoteDao
import com.example.flow.data.local.PendingDeletionDao
import com.example.flow.data.model.Note
import com.example.flow.data.model.PendingDeletion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

// --- UPDATED CONSTRUCTOR ---
class NoteRepository(
    private val noteDao: NoteDao,
    private val pendingDeletionDao: PendingDeletionDao,
    private val firestore: FirebaseFirestore
) {
    private val auth = FirebaseAuth.getInstance()
    private val COLLECTION_NAME = "notes"

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getNotesCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection(COLLECTION_NAME)

    fun getAllNotes(): Flow<List<Note>> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(emptyList())
        } else {
            noteDao.getAllNotes(userId)
        }
    }

    fun getNoteById(noteId: String): Flow<Note?> {
        val userId = getUserId()
        return if (userId == null) {
            flowOf(null)
        } else {
            noteDao.getNoteById(noteId)
        }
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)

        val userId = getUserId()
        if (userId == null) {
            Log.w("NoteRepository", "User offline. Note saved locally.")
            return
        }
        try {
            getNotesCollection(userId).document(note.id).set(note).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Saved locally, but Firestore insert failed.", e)
        }
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)

        val userId = getUserId()
        if (userId == null) {
            Log.w("NoteRepository", "User offline. Note updated locally.")
            return
        }
        try {
            getNotesCollection(userId).document(note.id).set(note).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Updated locally, but Firestore update failed.", e)
        }
    }

    // --- HEAVILY UPDATED DELETE LOGIC ---
    suspend fun delete(note: Note) {
        try {
            // 1. Delete from local Room database FIRST.
            noteDao.deleteNote(note)

            // 2. Add a "tombstone" to the pending deletions queue.
            pendingDeletionDao.insert(PendingDeletion(note.id, COLLECTION_NAME))

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NoteRepository", "Error deleting note locally", e)
        }
    }

    // --- HEAVILY UPDATED SYNC LOGIC ---
    suspend fun syncNotesFromFirebase() {
        val userId = getUserId() ?: return
        try {
            // 1. Get all IDs of items we've deleted locally.
            val deletedIds = pendingDeletionDao.getPendingDeletionIdsByCollection(COLLECTION_NAME)

            val snapshot = getNotesCollection(userId).get().await()
            val notes = snapshot.toObjects(Note::class.java)

            for (note in notes) {
                // 2. ONLY add notes from Firebase if they are NOT in our deletion queue
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
                getNotesCollection(userId).document(pending.id).delete().await()

                // 2. If successful, remove it from the local queue
                pendingDeletionDao.delete(pending)
                Log.i("NoteRepository", "Successfully deleted ${pending.id} from Firebase.")

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("NoteRepository", "Failed to delete ${pending.id} from Firebase. Will retry later.", e)
            }
        }
    }
}