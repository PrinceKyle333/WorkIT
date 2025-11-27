package com.workit.workit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set background color to dark (#312B2B)
        bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.nav_background))

        // Create color state list for icons and text
        val navActiveColor = ContextCompat.getColor(this, R.color.nav_active) // #4CFF0C
        val navInactiveColor = ContextCompat.getColor(this, R.color.nav_inactive) // Gray

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

        // Setup navigation controller
        NavigationUI.setupWithNavController(bottomNav, navController)
    }
}