package com.embedded2025.notificationsa15

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

/**
 * Activity principale dell'applicazione.
 */
class MainActivity: AppCompatActivity() {
    /**
     * Launcher per la richiesta di permesso di notifiche.
     */
    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) Log.d("PERMISSION", "Permesso notifiche concesso")
            else Log.d("PERMISSION", "Permesso notifiche negato")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNavigation()
        setupNotifications()
    }

    /**
     * Imposta la navigazione tra i fragment utilizzando Navigation Component.
     * Si occupa di gestire la navigazione tra i fragment nel loro host, la visualizzazione
     * del titolo della destinazione e la corretta visualizzazione del drawer.
     */
    private fun setupNavigation() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val toolbarTitle = findViewById<TextView>(R.id.toolbar_title)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)

        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.simpleNotificationFragment,
            R.id.expandableNotificationFragment,
            R.id.actionsNotificationFragment,
            R.id.chatNotificationFragment,
            R.id.progressNotificationFragment,
            R.id.mediaPlayerNotificationFragment,
            R.id.callNotificationFragment,
            R.id.emailNotificationFragment,
            R.id.finalFragment
        )

        val appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations)
            .setOpenableLayout(drawer)
            .build()

        toolbar.setupWithNavController(navController, appBarConfiguration)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        navView.setNavigationItemSelectedListener { menuItem ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .build()

            try {
                navController.navigate(menuItem.itemId, null, navOptions)
            } catch (e: IllegalArgumentException) {
                Log.e("DrawerClick", "Impossibile navigare a ${menuItem.title}", e)
            }

            drawer.closeDrawers()
            return@setNavigationItemSelectedListener true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarTitle.text = destination.label ?: getString(R.string.app_name)
            supportActionBar?.title = ""

            val menu = navView.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                item.isChecked = item.itemId == destination.id
            }

            val iconRes = when (destination.id) {
                R.id.homeFragment -> R.drawable.ic_home
                R.id.simpleNotificationFragment -> R.drawable.ic_simple
                R.id.expandableNotificationFragment -> R.drawable.ic_expandable
                R.id.actionsNotificationFragment -> R.drawable.ic_action
                R.id.chatNotificationFragment -> R.drawable.ic_chat
                R.id.progressNotificationFragment -> R.drawable.ic_progress
                R.id.mediaPlayerNotificationFragment -> R.drawable.ic_media
                R.id.callNotificationFragment -> R.drawable.ic_call
                R.id.emailNotificationFragment -> R.drawable.ic_email
                R.id.finalFragment -> R.drawable.ic_done
                else -> 0
            }
            val menuItem = toolbar.menu.findItem(R.id.action_current_fragment_icon)
            if (iconRes != 0) {
                menuItem?.setIcon(iconRes)
                menuItem?.isVisible = true
            } else {
                menuItem?.isVisible = false
            }
        }
    }

    /**
     * Richiede il permesso di notifiche all'utente se non è già stato concesso.
     */
    private fun setupNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Crea il menu della toolbar.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Gestisce le azioni dei menu della toolbar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController)
                || super.onOptionsItemSelected(item)
    }
}