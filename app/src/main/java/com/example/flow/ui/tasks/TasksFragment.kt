package com.example.flow.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flow.R
// FIX: ADDED THIS IMPORT
import com.example.flow.ui.tasks.TasksFragmentDirections
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TasksFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskListAdapter: TaskListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskViewModel = ViewModelProvider(findNavController().getViewModelStoreOwner(R.id.main_nav_graph))
            .get(TaskViewModel::class.java)

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_tasks)
        taskListAdapter = TaskListAdapter(
            onTaskClicked = { task ->
                val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(task.id)
                findNavController().navigate(action)
            },
            onTaskChecked = { task, isChecked ->
                // Create a copy of the task with the new 'completed' status
                val updatedTask = task.copy(completed = isChecked)
                // Update the task in the database
                taskViewModel.update(updatedTask)
            }
        )

        recyclerView.adapter = taskListAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe LiveData
        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            tasks?.let {
                taskListAdapter.submitList(it)
            }
        }

        // Setup FAB
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_task)
        fab.setOnClickListener {
            val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(null)
            findNavController().navigate(action)
        }
    }
}