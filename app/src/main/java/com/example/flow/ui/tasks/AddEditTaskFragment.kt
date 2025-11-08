package com.example.flow.ui.tasks

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.flow.R
import com.example.flow.data.model.Task
import com.example.flow.databinding.FragmentAddEditTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddEditTaskFragment : Fragment() {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel, scoped to the navigation graph
    private lateinit var viewModel: TaskViewModel

    // Safe-Args to get the taskId from the navigation action
    private val args: AddEditTaskFragmentArgs by navArgs()

    private var currentTask: Task? = null
    private var selectedDeadline: Long? = null
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the shared ViewModel
        viewModel = ViewModelProvider(findNavController().getViewModelStoreOwner(R.id.main_nav_graph))
            .get(TaskViewModel::class.java)

        setupSpinner()
        setupDatePicker()

        // Check if we are in "Edit Mode" or "Add Mode"
        args.taskId?.let { id ->
            // --- EDIT MODE ---
            loadTaskData(id)
            binding.buttonDeleteTask.visibility = View.VISIBLE
        } ?: run {
            // --- ADD MODE ---
            // Set default priority to Low
            binding.spinnerTaskPriority.setSelection(0)
            binding.buttonDeleteTask.visibility = View.GONE
        }

        // Set click listeners
        binding.buttonSaveTask.setOnClickListener {
            saveTask()
        }

        binding.buttonDeleteTask.setOnClickListener {
            deleteTask()
        }
    }

    private fun loadTaskData(taskId: String) {
        viewModel.getTaskById(taskId).observe(viewLifecycleOwner) { task ->
            task?.let {
                currentTask = it
                binding.editTextTaskTitle.setText(it.title)
                // Set spinner priority (0=Low, 1=Med, 2=High). Our data is 1, 2, 3.
                binding.spinnerTaskPriority.setSelection(it.priority - 1)
                it.deadline?.let { deadline ->
                    selectedDeadline = deadline
                    calendar.timeInMillis = deadline
                    updateDeadlineText()
                }
            }
        }
    }

    private fun setupSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.priority_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerTaskPriority.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Set time to avoid timezone issues, e.g., end of day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            selectedDeadline = calendar.timeInMillis
            updateDeadlineText()
        }

        binding.textTaskDeadlinePicker.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDeadlineText() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.textTaskDeadlinePicker.text = "Due: ${sdf.format(calendar.time)}"
    }

    private fun saveTask() {
        val title = binding.editTextTaskTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Get priority (0=Low, 1=Med, 2=High) -> Add 1 to store as (1, 2, 3)
        val priority = binding.spinnerTaskPriority.selectedItemPosition + 1
        val userId = viewModel.getCurrentUserId()

        if (userId == null) {
            Toast.makeText(requireContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentTask == null) {
            // --- Create New Task ---
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                title = title,
                priority = priority,
                deadline = selectedDeadline,
                isCompleted = false,
                userId = userId
                // description and listName use default values
            )
            viewModel.insert(newTask)
            Toast.makeText(requireContext(), "Task Saved", Toast.LENGTH_SHORT).show()
        } else {
            // --- Update Existing Task ---
            val updatedTask = currentTask!!.copy(
                title = title,
                priority = priority,
                deadline = selectedDeadline
            )
            viewModel.update(updatedTask)
            Toast.makeText(requireContext(), "Task Updated", Toast.LENGTH_SHORT).show()
        }

        // Go back to the task list
        findNavController().popBackStack()
    }

    private fun deleteTask() {
        currentTask?.let {
            viewModel.delete(it)
            Toast.makeText(requireContext(), "Task Deleted", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}