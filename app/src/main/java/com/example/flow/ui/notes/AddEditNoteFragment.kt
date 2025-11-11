package com.example.flow.ui.notes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.flow.R
import com.example.flow.data.model.Note
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch // Import for launch
import java.util.*

class AddEditNoteFragment : Fragment() {

    private lateinit var viewModel: NoteViewModel
    private val args: AddEditNoteFragmentArgs by navArgs()

    // Views
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var speechButton: ImageButton
    private lateinit var lockButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var saveButton: Button

    private var currentNote: Note? = null
    private var isNoteLocked: Boolean = false
    private var currentPasswordHash: String? = null

    // Launcher for Speech-to-Text
    private val speechToTextLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                val currentContent = contentEditText.text.toString()
                contentEditText.setText("$currentContent ${spokenText[0]}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_edit_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use 'requireActivity()' to get the same Activity-scoped ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(NoteViewModel::class.java)

        // Find all views
        titleEditText = view.findViewById(R.id.edit_text_note_title)
        contentEditText = view.findViewById(R.id.edit_text_note_content)
        backButton = view.findViewById(R.id.button_back_note)
        speechButton = view.findViewById(R.id.button_speech_to_text)
        lockButton = view.findViewById(R.id.button_lock_note)
        deleteButton = view.findViewById(R.id.button_delete_note)
        saveButton = view.findViewById(R.id.button_save_note)

        // Load data if editing
        args.noteId?.let { noteId ->
            // --- EDIT MODE ---
            loadNoteData(noteId)
        } ?: run {
            // --- ADD NEW NOTE MODE ---
            isNoteLocked = false
            currentNote = null
            deleteButton.visibility = View.GONE // Can't delete a new note
        }

        updateLockIcon()

        // --- Set Click Listeners ---
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        saveButton.setOnClickListener {
            saveNote()
        }

        deleteButton.setOnClickListener {
            deleteNote()
        }

        lockButton.setOnClickListener {
            toggleLock()
        }

        speechButton.setOnClickListener {
            startSpeechToText()
        }

        // Handle system back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Simply pop back, no special logic needed
                findNavController().popBackStack()
            }
        })
    }

    private fun loadNoteData(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getNoteById(id).collect { note ->
                note?.let {
                    currentNote = it
                    titleEditText.setText(it.title)
                    isNoteLocked = it.isLocked
                    currentPasswordHash = it.passwordHash

                    // --- SIMPLIFIED LOGIC ---
                    // If decryptedContent was passed from NotesFragment, use it
                    if (args.decryptedContent != null) {
                        contentEditText.setText(args.decryptedContent)
                    } else {
                        // Otherwise, this is an unlocked note, show its content
                        contentEditText.setText(it.content)
                    }
                    updateLockIcon()
                }
            }
        }
    }

    // --- REMOVED 'showPasswordPromptForViewing' ---
    // This logic is no longer needed here.

    private fun updateLockIcon() {
        if (isNoteLocked) {
            lockButton.setImageResource(android.R.drawable.ic_lock_lock)
            lockButton.contentDescription = getString(R.string.unlock_note)
        } else {
            lockButton.setImageResource(R.drawable.ic_lock_open)
            lockButton.contentDescription = getString(R.string.lock_note)
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add text...")
        }
        try {
            speechToTextLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech-to-text not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLock() {
        if (isNoteLocked) {
            // Note is currently locked, user wants to unlock it
            showPasswordPrompt(isSettingPassword = false, isUnlocking = true)
        } else {
            // Note is unlocked, user wants to lock it
            showPasswordPrompt(isSettingPassword = true, isUnlocking = false)
        }
    }

    private fun showPasswordPrompt(isSettingPassword: Boolean, isUnlocking: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_password_prompt, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.edit_text_password)
        val confirmPasswordInput = dialogView.findViewById<EditText>(R.id.edit_text_confirm_password)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.layout_confirm_password)

        val title: String
        if (isSettingPassword) {
            title = getString(R.string.set_password_title)
            confirmPasswordLayout.visibility = View.VISIBLE
        } else if (isUnlocking) {
            title = getString(R.string.unlock_note_title)
            confirmPasswordLayout.visibility = View.GONE
        } else {
            // This case is for 'saveNote' on an already-locked note
            title = getString(R.string.confirm_password_title)
            confirmPasswordLayout.visibility = View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val password = passwordInput.text.toString()
                if (password.isEmpty()) {
                    Toast.makeText(context, R.string.password_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isSettingPassword) {
                    val confirmPassword = confirmPasswordInput.text.toString()
                    if (password != confirmPassword) {
                        Toast.makeText(context, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
                    } else {
                        // Passwords match, set lock
                        isNoteLocked = true
                        currentPasswordHash = viewModel.hashPassword(password)
                        updateLockIcon()
                        Toast.makeText(context, R.string.password_set, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Unlocking or Confirming for save
                    val hashed = viewModel.hashPassword(password)
                    if (hashed == currentPasswordHash) {
                        if (isUnlocking) {
                            // User chose to explicitly unlock the note
                            isNoteLocked = false
                            currentPasswordHash = null
                            updateLockIcon()
                            Toast.makeText(context, R.string.password_removed, Toast.LENGTH_SHORT).show()
                        } else {
                            // User is saving an already-locked note
                            // This 'else' branch is triggered by saveNote()
                            saveLockedNote(password)
                        }
                    } else {
                        Toast.makeText(context, R.string.incorrect_password, Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun saveNote() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(context, "Cannot save empty note", Toast.LENGTH_SHORT).show()
            return
        }

        if (isNoteLocked) {
            // If note is locked, we need to confirm password to re-encrypt and save
            // This will call saveLockedNote(password) on success
            showPasswordPrompt(isSettingPassword = false, isUnlocking = false)
        } else {
            // Note is not locked, save directly
            val userId = viewModel.getCurrentUserId()
            if (userId == null) {
                Toast.makeText(context, "Error: Not logged in", Toast.LENGTH_SHORT).show()
                return
            }

            val noteToSave = currentNote?.copy(
                title = title,
                content = content, // Save plain text
                isLocked = false,
                passwordHash = null,
                lastModified = System.currentTimeMillis()
            ) ?: Note(
                title = title,
                content = content,
                isLocked = false,
                passwordHash = null,
                userId = userId,
                lastModified = System.currentTimeMillis()
            )

            if (currentNote == null) {
                viewModel.insert(noteToSave)
            } else {
                viewModel.update(noteToSave)
            }
            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    // This is called from the showPasswordPrompt() logic
    private fun saveLockedNote(password: String) {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        val userId = viewModel.getCurrentUserId()!!

        // Re-encrypt the content with the confirmed password
        val encryptedContent = viewModel.encryptContent(content, password)

        val noteToSave = currentNote?.copy(
            title = title,
            content = encryptedContent, // Save encrypted text
            isLocked = true,
            passwordHash = currentPasswordHash, // Hash remains the same
            lastModified = System.currentTimeMillis()
        ) ?: Note(
            // This case handles a NEW note that is locked before first save
            title = title,
            content = encryptedContent,
            isLocked = true,
            passwordHash = currentPasswordHash,
            userId = userId,
            lastModified = System.currentTimeMillis()
        )

        if (currentNote == null) {
            viewModel.insert(noteToSave)
        } else {
            viewModel.update(noteToSave)
        }
        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }


    private fun deleteNote() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_note)
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton(R.string.delete_note) { _, _ ->
                currentNote?.let {
                    viewModel.delete(it)
                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                }
                findNavController().popBackStack()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}