package com.example.octopus

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    override fun onStart() {
        super.onStart()
        updateNavigationUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Menu kliknięcia
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> {
                    navController.navigate(R.id.loginFragment)
                }
                R.id.nav_schedule -> navController.navigate(R.id.scheduleFragment)
                R.id.nav_pricing -> navController.navigate(R.id.pricingFragment)
                R.id.nav_reservation -> navController.navigate(R.id.reservationFragment)
                R.id.nav_trainers -> navController.navigate(R.id.trainersFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_help -> navController.navigate(R.id.helpFragment)
                R.id.for_trainers -> navController.navigate(R.id.forTrainersFragment)
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    drawerLayout.closeDrawer(GravityCompat.END)
                    navController.navigate(R.id.mainFragment)
                    // UI zaktualizuj lekko opóźnieniem, żeby fragment zdążył się załadować
                    drawerLayout.postDelayed({
                        updateNavigationUI()
                    }, 100)
                    true
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // Bottom nav
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> navController.navigate(R.id.mainFragment)
                R.id.nav_schedule -> navController.navigate(R.id.scheduleFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
            }
            true
        }

        updateNavigationUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_toggle -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    drawerLayout.openDrawer(GravityCompat.END)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    public fun updateNavigationUI() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val menu = navView.menu
        val loginItem = menu.findItem(R.id.nav_login)
        val logoutItem = menu.findItem(R.id.nav_logout)
        val forTrainersItem = menu.findItem(R.id.for_trainers)
        val headerView = navView.getHeaderView(0)
        val headerText = headerView.findViewById<TextView>(R.id.nav_header_text)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Użytkownik zalogowany

            val uid = user.uid
            val userRef = FirebaseDatabase.getInstance().getReference("UsersPersonalization").child(uid)
            userRef.get()
                .addOnSuccessListener { snapshot ->
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Użytkowniku"
                    val role = snapshot.child("role").getValue(String::class.java)
                    if (role != "user") {
                        headerText.text = "Witaj, $username! \n Rola: $role"
                        if (role == "trainer")
                        {
                            forTrainersItem.isVisible = true
                        }
                    }
                    else{
                        headerText.text = "Witaj, $username!"
                    }
                }
                .addOnFailureListener {
                    headerText.text = "Witaj!"
                }
            loginItem.isVisible = false
            logoutItem.isVisible = true


        } else {
            // Gość
            headerText.text = "Witaj, gościu!"
            loginItem.isVisible = true
            logoutItem.isVisible = false
        }
    }
}