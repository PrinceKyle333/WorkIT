package com.workit.workit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.res.ColorStateList
import androidx.lifecycle.lifecycleScope
import com.workit.workit.auth.RoleManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            try {
                val userRole = RoleManager.getUserRole()

                when (userRole) {
                    RoleManager.UserRole.STUDENT -> setupStudentNavigation()
                    RoleManager.UserRole.EMPLOYER -> setupEmployerNavigation()
                    else -> {
                        // Unknown role, logout and redirect
                        auth.signOut()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                auth.signOut()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun setupStudentNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            return
        }

        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set student navigation graph
        navController.setGraph(R.navigation.nav_graph_student)

        setupBottomNavColors(bottomNav)
        NavigationUI.setupWithNavController(bottomNav, navController)
    }

    private fun setupEmployerNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            return
        }

        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Clear existing menu and set employer menu
        bottomNav.menu.clear()
        menuInflater.inflate(R.menu.bottom_nav_employer, bottomNav.menu)

        // Set employer navigation graph
        navController.setGraph(R.navigation.nav_graph_employer)

        setupBottomNavColors(bottomNav)
        NavigationUI.setupWithNavController(bottomNav, navController)
    }

    private fun setupBottomNavColors(bottomNav: BottomNavigationView) {
        // Set background color to dark (#312B2B)
        bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.nav_background))

        // Create color state list for icons and text
        val navActiveColor = ContextCompat.getColor(this, R.color.nav_active) // #4CFF0C
        val navInactiveColor = ContextCompat.getColor(this, R.color.nav_inactive) // White

        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(navActiveColor, navInactiveColor)
        )

        // Apply colors to icons
        bottomNav.itemIconTintList = colorStateList
        // Apply colors to text labels
        bottomNav.itemTextColor = colorStateList
    }
}