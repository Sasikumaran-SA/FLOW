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
import com.example.flow.SessionState // You need to create this object
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat

// Create this file (e.g., in your app's root package)
object SessionState {
    var financeAuthenticated = false
}

class FinanceFragment : Fragment() {

    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var transactionListAdapter: TransactionListAdapter
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var authOverlay: FrameLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var unlockButton: Button
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_finance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        financeViewModel = ViewModelProvider(this).get(FinanceViewModel::class.java)

        authOverlay = view.findViewById(R.id.auth_overlay)
        contentLayout = view.findViewById(R.id.finance_content_layout)
        unlockButton = view.findViewById(R.id.button_unlock_finance)
        fab = view.findViewById(R.id.fab_add_transaction)

        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SessionState.financeAuthenticated = true
                    showFinanceData()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    }
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

        fab.setOnClickListener {
            val action = FinanceFragmentDirections.actionFinanceFragmentToAddEditTransactionFragment(null)
            findNavController().navigate(action)
        }

        // If authenticated earlier in this app session, skip prompting
        if (SessionState.financeAuthenticated) {
            showFinanceData()
        } else {
            // Check auth on startup
            checkBiometricSupportAndAuthenticate()
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                biometricPrompt.authenticate(promptInfo)
            // Handle errors
            else ->
                Toast.makeText(context, "Biometric features not available or not enrolled.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showFinanceData() {
        authOverlay.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        fab.show() // Show the FAB only after auth

        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view_transactions)
        transactionListAdapter = TransactionListAdapter { transaction ->
            val action = FinanceFragmentDirections.actionFinanceFragmentToAddEditTransactionFragment(transaction.id)
            findNavController().navigate(action)
        }
        recyclerView?.adapter = transactionListAdapter
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        financeViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionListAdapter.submitList(it)
            }
        }

        val incomeTextView = view?.findViewById<TextView>(R.id.text_total_income)
        val expenseTextView = view?.findViewById<TextView>(R.id.text_total_expense)
        val format = NumberFormat.getCurrencyInstance()
        
        financeViewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            incomeTextView?.text = format.format(income)
        }
        
        financeViewModel.totalExpense.observe(viewLifecycleOwner) { expense ->
            expenseTextView?.text = format.format(expense)
        }
    }
}