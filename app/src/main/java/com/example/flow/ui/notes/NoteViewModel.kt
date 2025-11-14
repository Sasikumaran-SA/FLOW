package com.example.flow.ui.notes

import android.app.Application
import androidx.lifecycle.*
import com.example.flow.data.local.AppDatabase
import com.example.flow.data.model.Note
import com.example.flow.data.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64
import kotlin.text.Charsets

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>
    private val auth = FirebaseAuth.getInstance()
    private var hasListenerBeenSet = false

    init {
        val db = AppDatabase.getDatabase(application)
        val noteDao = db.noteDao()
        val pendingDeletionDao = db.pendingDeletionDao() // --- ADDED ---
        val firestore = FirebaseFirestore.getInstance()
        // --- UPDATED CONSTRUCTOR ---
        repository = NoteRepository(noteDao, pendingDeletionDao, firestore)

        allNotes = repository.getAllNotes().asLiveData()

        // Setup sync and realtime listener on startup
        setupSyncAndListener()
        attemptPendingDeletions()
    }

    private fun setupSyncAndListener() {
        if (!hasListenerBeenSet && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncNotesFromFirebase()
            }
            repository.setupRealtimeListener()
            hasListenerBeenSet = true
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

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
        // Try to delete from Firebase immediately
        attemptPendingDeletions()
    }

    fun getNoteById(noteId: String): Flow<Note?> {
        return repository.getNoteById(noteId)
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun onLogout() {
        repository.removeListener()
        hasListenerBeenSet = false
    }

    // ... (Your hashPassword and encrypt/decrypt functions remain unchanged) ...

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.getEncoder().encodeToString(digest)
    }

    fun encryptContent(content: String, key: String): String {
        val result = StringBuilder()
        for (i in content.indices) {
            val xorChar = (content[i].code xor key[i % key.length].code).toChar()
            result.append(xorChar)
        }
        return Base64.getEncoder().encodeToString(result.toString().toByteArray(Charsets.UTF_8))
    }

    fun decryptContent(encryptedContent: String, key: String): String {
        val decodedBytes = Base64.getDecoder().decode(encryptedContent)
        val decoded = String(decodedBytes, Charsets.UTF_8)

        val result = StringBuilder()
        for (i in decoded.indices) {
            val xorChar = (decoded[i].code xor key[i % key.length].code).toChar()
            result.append(xorChar)
        }
        return result.toString()
    }
}