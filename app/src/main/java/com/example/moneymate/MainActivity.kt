package com.example.moneymate

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.moneymate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.dashboardFragment,
                    R.id.transactionsFragment,
                    R.id.analyticsFragment,
                    R.id.budgetFragment,
                    R.id.settingsFragment
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.bottomNavigation.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                handleDestinationChange(destination)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up navigation", e)
        }
    }

    private fun handleDestinationChange(destination: NavDestination) {
        when (destination.id) {
            R.id.dashboardFragment,
            R.id.transactionsFragment,
            R.id.analyticsFragment,
            R.id.budgetFragment,
            R.id.settingsFragment -> {
                binding.bottomNavigation.visibility = View.VISIBLE
            }
            else -> {
                binding.bottomNavigation.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
