package com.example.flow

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavOptions // ADD THIS IMPORT
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
// We will replace setupWithNavController with a manual listener
// import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Set up NavController
        navController = navHostFragment.navController

        // Set up the Toolbar
        setSupportActionBar(toolbar)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_tasks, R.id.nav_finance, R.id.nav_notes)
        )

        // Link NavController to Toolbar
        setupActionBarWithNavController(navController, appBarConfiguration)


        bottomNavView.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .build()

            when (item.itemId) {
                R.id.nav_tasks -> {
                    navController.navigate(R.id.nav_tasks, null, navOptions)
                    true
                }
                R.id.nav_finance -> {
                    navController.navigate(R.id.nav_finance, null, navOptions)
                    true
                }
                R.id.nav_notes -> {
                    navController.navigate(R.id.nav_notes, null, navOptions)
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_tasks -> bottomNavView.menu.findItem(R.id.nav_tasks).isChecked = true
                R.id.nav_finance -> bottomNavView.menu.findItem(R.id.nav_finance).isChecked = true
                R.id.nav_notes -> bottomNavView.menu.findItem(R.id.nav_notes).isChecked = true
            }
        }
    }

    // This enables the "Up" button (back arrow) in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Inflate the (main_options_menu.xml)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_options_menu, menu)
        return true
    }

    // Handle clicks on the (Settings) icon
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                if (navController.currentDestination?.id != R.id.settingsFragment) {
                    navController.navigate(R.id.action_global_to_settingsFragment)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}