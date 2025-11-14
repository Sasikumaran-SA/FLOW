package com.example.flow.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flow.R
import com.example.flow.data.model.Note

class NoteListAdapter(
    private val onNoteClicked: (Note) -> Unit
) : ListAdapter<Note, NoteListAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_note_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.text_note_content_preview)
        private val lockIcon: ImageView = itemView.findViewById(R.id.icon_note_locked)

        fun bind(note: Note) {
            titleTextView.text = note.title

            if (note.locked) {
                contentTextView.text = "This note is locked."
                lockIcon.visibility = View.VISIBLE
            } else {
                contentTextView.text = note.content
                lockIcon.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onNoteClicked(note)
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}