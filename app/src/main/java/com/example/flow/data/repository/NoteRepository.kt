package com.example.flow.data.repository

import android.util.Log
import com.example.flow.data.local.NoteDao
import com.example.flow.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class NoteRepository(
    private val noteDao: NoteDao,
    private val firestore: FirebaseFirestore
) {
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getNotesCollection() = firestore
        .collection("users")
        .document(getUserId()!!)
        .collection("notes")

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes(getUserId()!!)
    }

    fun getNoteById(noteId: String): Flow<Note?> {
        return noteDao.getNoteById(noteId)
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
        try {
            getNotesCollection().document(note.id).set(note).await()
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error inserting note to Firestore", e)
        }
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
        try {
            getNotesCollection().document(note.id).set(note).await()
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error updating note in Firestore", e)
        }
    }

    // --- UPDATED DELETE FUNCTION ---
    suspend fun delete(note: Note) {
        try {
            // 1. Delete from remote Firestore database FIRST
            getNotesCollection().document(note.id).delete().await()

            // 2. ONLY if remote delete succeeds, delete from local Room
            noteDao.deleteNote(note)

        } catch (e: Exception) {
            // If remote delete fails, log the error and DO NOT delete from local.
            Log.e("NoteRepository", "Error deleting note from Firestore", e)
        }
    }

    suspend fun syncNotesFromFirebase() {
        if (getUserId() == null) return
        try {
            val snapshot = getNotesCollection().get().await()
            val notes = snapshot.toObjects(Note::class.java)
            for (note in notes) {
                noteDao.insertNote(note)
            }
            Log.d("NoteRepository", "Successfully synced ${notes.size} notes.")
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error syncing notes from Firestore", e)
        }
    }
}