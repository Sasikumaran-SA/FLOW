package com.example.flow.ui.notes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.flow.R
import com.example.flow.data.model.Note
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NotesFragment : Fragment() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var noteAdapter: NoteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use 'requireActivity()' to scope the ViewModel to the Activity
        // This makes it easier to share the same ViewModel instance
        viewModel = ViewModelProvider(requireActivity()).get(NoteViewModel::class.java)

        setupRecyclerView(view)

        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.submitList(notes)
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_note).setOnClickListener {
            // Navigate to AddEditNoteFragment for a NEW note
            // Pass null for noteId and null for decryptedContent
            val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(null, null)
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView(view: View) {
        noteAdapter = NoteListAdapter { note ->
            // --- THIS IS THE MAIN LOGIC FIX ---
            if (note.locked) {
                // If the note is locked, show password dialog FIRST
                showPasswordDialog(note)
            } else {
                // Note is not locked, navigate directly
                // Pass the note's ID and its (unlocked) content
                val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(note.id, note.content)
                findNavController().navigate(action)
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_notes)
        recyclerView.adapter = noteAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    /**
     * Shows a password prompt dialog to unlock a note.
     * Only navigates to the editor on success.
     */
    private fun showPasswordDialog(note: Note) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.unlock_note_title))

        // Set up the input
        val input = EditText(requireContext())
        input.hint = "Enter password"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton(R.string.confirm) { dialog, _ ->
            val password = input.text.toString()

            if (password.isEmpty()) {
                Toast.makeText(context, R.string.password_empty, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // 1. Hash the entered password to check for a match
            val hashed = viewModel.hashPassword(password)

            if (hashed == note.passwordHash) {
                // 2. Password hash matches, now try to decrypt
                try {
                    val decryptedContent = viewModel.decryptContent(note.content, password)

                    // 3. Decryption success! Navigate to editor with decrypted content
                    val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(note.id, decryptedContent)
                    findNavController().navigate(action)

                } catch (e: Exception) {
                    // This can happen if decryption fails for some reason
                    Toast.makeText(context, "Decryption failed. Incorrect password or corrupted data.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Password hash does not match
                Toast.makeText(context, R.string.incorrect_password, Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}