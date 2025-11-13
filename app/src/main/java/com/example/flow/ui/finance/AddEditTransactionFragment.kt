package com.example.flow.ui.finance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.flow.R
import com.example.flow.data.model.Transaction
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionFragment : Fragment() {

    private lateinit var financeViewModel: FinanceViewModel
    private val args: AddEditTransactionFragmentArgs by navArgs()

    private var currentTransaction: Transaction? = null
    private var selectedDate: Long = System.currentTimeMillis()
    private val calendar: Calendar = Calendar.getInstance()

    // Views
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var descriptionLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var typeSwitch: SwitchMaterial
    private lateinit var dateText: TextView
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_edit_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        financeViewModel = ViewModelProvider(this).get(FinanceViewModel::class.java)

        descriptionInput = view.findViewById(R.id.edit_text_transaction_description)
        descriptionLayout = view.findViewById(R.id.layout_transaction_description)
        amountInput = view.findViewById(R.id.edit_text_transaction_amount)
        amountLayout = view.findViewById(R.id.layout_transaction_amount)
        typeSwitch = view.findViewById(R.id.switch_transaction_type)
        dateText = view.findViewById(R.id.text_transaction_date_picker)
        saveButton = view.findViewById(R.id.button_save_transaction)
        deleteButton = view.findViewById(R.id.button_delete_transaction)

        updateDateText()

        args.transactionId?.let { transactionId ->
            financeViewModel.getTransactionById(transactionId).observe(viewLifecycleOwner) { transaction ->
                transaction?.let {
                    currentTransaction = it
                    populateFields(it)
                }
            }
            deleteButton.visibility = View.VISIBLE
        }

        dateText.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            saveTransaction()
        }

        deleteButton.setOnClickListener {
            currentTransaction?.let {
                financeViewModel.delete(it)
                findNavController().popBackStack()
            }
        }
    }

    private fun populateFields(transaction: Transaction) {
        descriptionInput.setText(transaction.description)
        amountInput.setText(transaction.amount.toString())
        typeSwitch.isChecked = transaction.isIncome
        selectedDate = transaction.date
        calendar.timeInMillis = selectedDate
        updateDateText()
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = calendar.timeInMillis
            updateDateText()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        dateText.text = sdf.format(Date(selectedDate))
    }

    private fun saveTransaction() {
        val description = descriptionInput.text.toString().trim()
        val amountString = amountInput.text.toString().trim()
        val isIncome = typeSwitch.isChecked

        if (description.isEmpty()) {
            descriptionLayout.error = "Description cannot be empty"
            return
        }
        descriptionLayout.error = null

        if (amountString.isEmpty()) {
            amountLayout.error = "Amount cannot be empty"
            return
        }
        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountLayout.error = "Please enter a valid amount"
            return
        }
        amountLayout.error = null

        val userId = financeViewModel.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentTransaction == null) {
            val newTransaction = Transaction(
                description = description,
                amount = amount,
                isIncome = isIncome,
                date = selectedDate,
                category = "Default",
                userId = userId
            )
            financeViewModel.insert(newTransaction)
        } else {
            val updatedTransaction = currentTransaction!!.copy(
                description = description,
                amount = amount,
                isIncome = isIncome,
                date = selectedDate
            )
            financeViewModel.update(updatedTransaction)
        }

        findNavController().popBackStack()
    }
}