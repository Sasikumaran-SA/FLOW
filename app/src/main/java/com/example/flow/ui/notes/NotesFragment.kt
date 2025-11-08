package com.example.flow.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        setupRecyclerView(view)

        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.submitList(notes)
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_note).setOnClickListener {
            // Navigate to AddEditNoteFragment, passing null for noteId
            val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(null, null)
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView(view: View) {
        noteAdapter = NoteListAdapter { note ->
            if (note.isLocked) {
                // Show password prompt if the note is locked
                showPasswordPrompt(note, false)
            } else {
                // Navigate directly to edit screen
                val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(note.id, null)
                findNavController().navigate(action)
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_notes)
        recyclerView.adapter = noteAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun showPasswordPrompt(note: Note, isEditingLockedNote: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_password_prompt, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.edit_text_password)
        val title = if (isEditingLockedNote) getString(R.string.confirm_password_title) else getString(R.string.unlock_note_title)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val password = passwordInput.text.toString()
                if (password.isEmpty()) {
                    Toast.makeText(context, R.string.password_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val hashed = viewModel.hashPassword(password)
                    // FIX: All 'note' properties are now resolved
                    if (hashed == note.passwordHash) {
                        // Correct password
                        val decryptedContent = viewModel.decryptContent(note.content, password)
                        val action = NotesFragmentDirections.actionNavNotesToAddEditNoteFragment(note.id, decryptedContent)
                        findNavController().navigate(action)
                    } else {
                        // Incorrect password
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
}