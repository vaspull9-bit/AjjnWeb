package com.example.ajjnweb

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var currentHomeFragment: HomeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupBackPressedHandler()
        observeFragmentChanges()
    }

    private fun observeFragmentChanges() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        navHostFragment?.childFragmentManager?.addOnBackStackChangedListener {
            updateCurrentFragment()
        }
        // Инициализируем текущий фрагмент при создании
        updateCurrentFragment()
    }

    private fun updateCurrentFragment() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is HomeFragment) {
            currentHomeFragment = currentFragment
            currentFragment.onNavigationStateChanged = { canGoBack, canGoForward ->
                updateToolbarButtonsState(canGoBack, canGoForward)
            }
            // Обновляем состояние кнопок при смене фрагмента
            currentFragment.updateNavigationState()
        } else {
            currentHomeFragment = null
            updateToolbarButtonsState(false, false)
        }
    }

    private fun updateToolbarButtonsState(canGoBack: Boolean, canGoForward: Boolean) {
        invalidateOptionsMenu()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentHomeFragment?.canGoBack() == true) {
                    currentHomeFragment?.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browser_toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val canGoBack = currentHomeFragment?.canGoBack() ?: false
        val canGoForward = currentHomeFragment?.canGoForward() ?: false

        menu.findItem(R.id.action_back)?.isEnabled = canGoBack
        menu.findItem(R.id.action_forward)?.isEnabled = canGoForward

        menu.findItem(R.id.action_back)?.icon?.alpha = if (canGoBack) 255 else 128
        menu.findItem(R.id.action_forward)?.icon?.alpha = if (canGoForward) 255 else 128

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_back -> {
                currentHomeFragment?.goBack()
                true
            }
            R.id.action_forward -> {
                currentHomeFragment?.goForward()
                true
            }
            R.id.action_refresh -> {
                currentHomeFragment?.reload()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            R.id.about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareCurrentPage() {
        val url = currentHomeFragment?.getCurrentUrl() ?: ""
        val title = currentHomeFragment?.getCurrentTitle() ?: ""

        if (url.isNotEmpty()) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться страницей"))
        } else {
            Toast.makeText(this, "Нет страницы для отправки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        val aboutMessage = "VShargin (C) 2025. vaspull9@gmail.com, AjjnWeb v1.0.1\n\nБраузер для интернета"
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage(aboutMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentFragment()
    }
}