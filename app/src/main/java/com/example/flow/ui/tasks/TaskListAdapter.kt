package com.example.flow.ui.tasks

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flow.R
import com.example.flow.data.model.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskListAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit
) : ListAdapter<Task, TaskListAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_task_title)
        private val deadlineTextView: TextView = itemView.findViewById(R.id.text_task_deadline)
        private val priorityTextView: TextView = itemView.findViewById(R.id.text_task_priority)
        private val completedCheckBox: CheckBox = itemView.findViewById(R.id.checkbox_task_completed)
        private val context: Context = itemView.context

        fun bind(task: Task) {
            titleTextView.text = task.title
            completedCheckBox.isChecked = task.completed

            // Set strike-through for completed tasks
            if (task.completed) {
                titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Set deadline text
            if (task.deadline != null) {
                deadlineTextView.text = "Due: ${formatDate(task.deadline)}"
                deadlineTextView.visibility = View.VISIBLE
            } else {
                deadlineTextView.visibility = View.GONE
            }

            // Set priority
            when (task.priority) {
                1 -> { // High
                    priorityTextView.text = "High"
                    priorityTextView.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)
                }
                2 -> { // Medium
                    priorityTextView.text = "Medium"
                    priorityTextView.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_light)
                }
                else -> { // Low (3)
                    priorityTextView.text = "Low"
                    priorityTextView.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_light)
                }
            }

            // Set click listeners
            itemView.setOnClickListener {
                onTaskClicked(task)
            }

            // --- THIS IS THE FIXED LOGIC ---
            // 1. Set listener to null to prevent firing while we set the initial state
            completedCheckBox.setOnCheckedChangeListener(null)
            // 2. Set the checked state based on the task data
            completedCheckBox.isChecked = task.completed
            // 3. Now, set the *real* listener
            completedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }
            // --- END OF FIX ---
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}