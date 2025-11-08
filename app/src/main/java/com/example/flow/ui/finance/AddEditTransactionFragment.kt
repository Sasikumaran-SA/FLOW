package com.example.flow.ui.finance

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.flow.R
import com.example.flow.data.model.Transaction
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent // Make sure this import is present

class AddEditTransactionFragment : Fragment() {

    private lateinit var financeViewModel: FinanceViewModel
    private val args: AddEditTransactionFragmentArgs by navArgs()

    private var currentTransaction: Transaction? = null
    private var selectedDate: Long = System.currentTimeMillis()
    private val calendar: Calendar = Calendar.getInstance()
    private var selectedReceiptUri: Uri? = null

    // Views
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var descriptionLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var typeSwitch: SwitchMaterial
    private lateinit var dateText: TextView
    private lateinit var receiptPreview: ImageView
    private lateinit var attachReceiptButton: Button
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    // Activity Result Launcher for picking an image
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedReceiptUri = it
            receiptPreview.setImageURI(it)
            receiptPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_edit_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        financeViewModel = ViewModelProvider(this).get(FinanceViewModel::class.java)

        // Find all views
        descriptionInput = view.findViewById(R.id.edit_text_transaction_description)
        descriptionLayout = view.findViewById(R.id.layout_transaction_description)
        amountInput = view.findViewById(R.id.edit_text_transaction_amount)
        amountLayout = view.findViewById(R.id.layout_transaction_amount)
        typeSwitch = view.findViewById(R.id.switch_transaction_type)
        dateText = view.findViewById(R.id.text_transaction_date_picker)
        receiptPreview = view.findViewById(R.id.image_receipt_preview)
        attachReceiptButton = view.findViewById(R.id.button_attach_receipt)
        saveButton = view.findViewById(R.id.button_save_transaction)
        deleteButton = view.findViewById(R.id.button_delete_transaction)

        // Set initial date
        updateDateText()

        // Check if we are editing an existing transaction
        args.transactionId?.let { transactionId ->
            financeViewModel.getTransactionById(transactionId).observe(viewLifecycleOwner) { transaction ->
                transaction?.let {
                    currentTransaction = it
                    populateFields(it)
                }
            }
            deleteButton.visibility = View.VISIBLE
        }

        // Set listeners
        dateText.setOnClickListener {
            showDatePicker()
        }

        attachReceiptButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
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

        // Load receipt image if it exists
        transaction.receiptImageUrl?.let { url ->
            receiptPreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(url)
                .into(receiptPreview)
        }
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

        // Validation
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
            // Creating a new transaction
            val newTransaction = Transaction(
                description = description,
                amount = amount,
                isIncome = isIncome,
                date = selectedDate,
                category = "Default", // You can expand this later
                userId = userId
            )
            // The ViewModel will handle the image upload
            financeViewModel.insert(newTransaction, selectedReceiptUri)
        } else {
            // Updating an existing transaction
            val updatedTransaction = currentTransaction!!.copy(
                description = description,
                amount = amount,
                isIncome = isIncome,
                date = selectedDate
            )
            // The ViewModel will handle replacing the image if a new one was selected
            financeViewModel.update(updatedTransaction, selectedReceiptUri)
        }

        findNavController().popBackStack() // Go back to the finance list
    }
}