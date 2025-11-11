package com.example.flow.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.flow.LoginActivity
import com.example.flow.ThemeManager
import com.example.flow.ThemeManager.ThemeMode
import com.example.flow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val userEmailText = view.findViewById<TextView>(R.id.text_user_email)
		val themeButton = view.findViewById<Button>(R.id.button_theme)
        val logoutButton = view.findViewById<Button>(R.id.button_logout)
        val deleteAccountButton = view.findViewById<Button>(R.id.button_delete_account)

        // Display current user email
        auth.currentUser?.email?.let {
            userEmailText.text = "Logged in as: $it"
        }

        // Logout logic
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

		// Theme chooser
		updateThemeButtonLabel(themeButton)
		themeButton.setOnClickListener {
			showThemeChooser(themeButton)
		}

        // Delete Account logic
        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                auth.signOut()
                // Navigate to LoginActivity and clear the back stack
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid

        // 1. (Optional but good practice) Delete user's data from Firestore/Storage
        // This is complex, as it requires deleting all sub-collections.
        // For this project, we'll just delete the user auth.
        // A full app would use a Cloud Function for this.
        // We will just delete the user document.

        firestore.collection("users").document(userId).delete()
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete user data: ${it.message}", Toast.LENGTH_SHORT).show()
            }

        // 2. Delete the user from Firebase Auth
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, R.string.account_deleted_toast, Toast.LENGTH_SHORT).show()
                    // Navigate to LoginActivity
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "Failed to delete account. Try logging in again. ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showThemeChooser(anchor: View) {
        val modes = arrayOf("System", "Light", "Dark")
        val current = when (ThemeManager.getSavedTheme(requireContext())) {
            ThemeMode.SYSTEM -> 0
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK -> 2
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Choose theme")
            .setSingleChoiceItems(modes, current) { dialog, which ->
                val mode = when (which) {
                    0 -> ThemeMode.SYSTEM
                    1 -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }
                ThemeManager.saveAndApplyTheme(requireContext(), mode)
                if (anchor is Button) updateThemeButtonLabel(anchor)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateThemeButtonLabel(button: Button) {
        val label = when (ThemeManager.getSavedTheme(requireContext())) {
            ThemeMode.SYSTEM -> "Theme: System"
            ThemeMode.LIGHT -> "Theme: Light"
            ThemeMode.DARK -> "Theme: Dark"
        }
        button.text = label
    }
}