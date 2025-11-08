package com.example.flow.ui.finance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flow.R
// ADD THIS IMPORT
import com.example.flow.ui.finance.FinanceFragmentDirections
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat

class FinanceFragment : Fragment() {

    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var transactionListAdapter: TransactionListAdapter
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var authOverlay: FrameLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var unlockButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_finance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        financeViewModel = ViewModelProvider(this).get(FinanceViewModel::class.java)

        // Find views
        authOverlay = view.findViewById(R.id.auth_overlay)
        contentLayout = view.findViewById(R.id.finance_content_layout)
        unlockButton = view.findViewById(R.id.button_unlock_finance)

        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(context, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    showFinanceData()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        unlockButton.setOnClickListener {
            checkBiometricSupportAndAuthenticate()
        }

        // Setup FAB (should be outside the auth block to be visible, but only enabled after auth)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_transaction)
        fab.setOnClickListener {
            // Navigate to AddEditTransactionFragment
            // FIX: Corrected action ID
            val action = FinanceFragmentDirections.actionFinanceFragmentToAddEditTransactionFragment(null)
            findNavController().navigate(action)
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(context, "No biometric features available on this device.", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(context, "Biometric features are currently unavailable.", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(context, "No biometrics enrolled. Please set up a screen lock or fingerprint.", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(context, "Unknown biometric error.", Toast.LENGTH_SHORT).show()

        }
    }

    private fun showFinanceData() {
        // Hide overlay, show content
        authOverlay.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        // Now, setup the RecyclerView and observers
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view_transactions)
        transactionListAdapter = TransactionListAdapter { transaction ->
            // On item click, navigate to edit
            // FIX: Corrected action ID
            val action = FinanceFragmentDirections.actionFinanceFragmentToAddEditTransactionFragment(transaction.id)
            findNavController().navigate(action)
        }
        recyclerView?.adapter = transactionListAdapter
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        // Observe transaction list
        financeViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionListAdapter.submitList(it)
            }
        }

        // Observe monthly spending (FR-10)
        val spendingTextView = view?.findViewById<TextView>(R.id.text_monthly_spending)
        financeViewModel.monthlySpending.observe(viewLifecycleOwner) { spending ->
            val format = NumberFormat.getCurrencyInstance()
            spendingTextView?.text = format.format(spending)
        }
    }
}