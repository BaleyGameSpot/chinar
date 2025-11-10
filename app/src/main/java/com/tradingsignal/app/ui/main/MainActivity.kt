package com.tradingsignal.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.tradingsignal.app.R
import com.tradingsignal.app.TradingSignalApplication
import com.tradingsignal.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var unreadBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeUnreadSignals()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Setup badge for unread signals
        unreadBadge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_home)
        unreadBadge?.isVisible = false
    }

    private fun observeUnreadSignals() {
        val app = application as TradingSignalApplication
        val repository = app.signalRepository

        repository.getUnreadCount().observe(this) { count ->
            if (count > 0) {
                unreadBadge?.isVisible = true
                unreadBadge?.number = count
            } else {
                unreadBadge?.isVisible = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}