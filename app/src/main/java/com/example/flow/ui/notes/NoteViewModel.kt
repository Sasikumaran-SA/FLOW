package com.example.flow.ui.notes

import android.app.Application
import androidx.lifecycle.*
import com.example.flow.data.local.AppDatabase
import com.example.flow.data.model.Note
import com.example.flow.data.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow // Keep your Flow import
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64
import kotlin.text.Charsets

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>
    private val auth = FirebaseAuth.getInstance()
    private var hasSynced = false

    init {
        val db = AppDatabase.getDatabase(application)
        val firestore = FirebaseFirestore.getInstance()
        repository = NoteRepository(db.noteDao(), firestore)
        allNotes = repository.getAllNotes().asLiveData()
        syncNotes()
    }

    private fun syncNotes() {
        if (!hasSynced && auth.currentUser != null) {
            viewModelScope.launch {
                repository.syncNotesFromFirebase()
                hasSynced = true
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
    }

    // This is your updated function, which is correct
    fun getNoteById(noteId: String): Flow<Note?> {
        return repository.getNoteById(noteId)
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.getEncoder().encodeToString(digest)
    }

    /**
     * "Encrypts" content. This is a simple XOR obfuscation.
     */
    fun encryptContent(content: String, key: String): String {
        val result = StringBuilder()
        for (i in content.indices) {
            // FIX: Added .toChar() to convert the Int result back to a Char
            val xorChar = (content[i].code xor key[i % key.length].code).toChar()
            result.append(xorChar)
        }
        return Base64.getEncoder().encodeToString(result.toString().toByteArray(Charsets.UTF_8))
    }

    /**
     * "Decrypts" content obfuscated by encryptContent.
     */
    fun decryptContent(encryptedContent: String, key: String): String {
        val decodedBytes = Base64.getDecoder().decode(encryptedContent)
        val decoded = String(decodedBytes, Charsets.UTF_8)

        val result = StringBuilder()
        for (i in decoded.indices) {
            // FIX: Added .toChar() to convert the Int result back to a Char
            val xorChar = (decoded[i].code xor key[i % key.length].code).toChar()
            result.append(xorChar)
        }
        return result.toString()
    }
}