package com.example.flow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// Make sure to declare this activity in AndroidManifest.xml
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set the layout

        auth = FirebaseAuth.getInstance()

        // This is the core logic
        // Check if a user is currently logged in
        if (auth.currentUser != null) {
            // User is already logged in, send them to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // No user is logged in, send them to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Call finish() to remove this SplashActivity from the back stack,
        // so the user can't press "back" and return to it.
        finish()
    }
}