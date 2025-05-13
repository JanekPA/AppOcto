package com.example.octopus

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    override fun onStart() {
        super.onStart()
        updateNavigationUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NavController
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
        setContentView(R.layout.activity_main)
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
                R.id.nav_profile -> navController.navigate(R.id.userProfileFragment)
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
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", "pl") ?: "pl"
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
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
            R.id.menu_notifications -> {
                showNotificationsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    private fun showNotificationsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_notification, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.notifications_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val ref = FirebaseDatabase.getInstance().getReference("Notifications").child(uid)

            ref.get().addOnSuccessListener { snapshot ->
                val notifications = mutableListOf<NotificationItem>()
                val notificationKeys = mutableListOf<String>()

                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val message = child.child("message").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val reservationId = child.child("reservationId").getValue(String::class.java) ?: ""

                    notifications.add(NotificationItem(title, message, timestamp, reservationId))
                    notificationKeys.add(child.key ?: "")
                }

                notifications.reverse()
                notificationKeys.reverse()

                val adapter = NotificationAdapter(
                    notifications,
                    notificationKeys,
                    onDelete = { key, position ->
                        ref.child(key).removeValue()
                    },
                    onClick = { notification ->
                        if (notification.reservationId.isNotEmpty()) {
                            showAdminReservationDialog(notification.reservationId)
                        }
                    }
                )

                recyclerView.adapter = adapter

                val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val position = viewHolder.adapterPosition
                        val key = adapter.getItemKey(position)

                        // Usuń z Firebase
                        ref.child(key).removeValue()

                        // Usuń lokalnie
                        adapter.removeItem(position)
                    }
                })

                itemTouchHelper.attachToRecyclerView(recyclerView)
            }
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                (resources.displayMetrics.heightPixels * 0.6).toInt()
            )
        }

        dialog.show()
    }



    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
    private fun showAdminReservationDialog(reservationId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("ReservedItems").child(reservationId)
        ref.get().addOnSuccessListener { snapshot ->
            val data = snapshot.value as? Map<String, Any> ?: return@addOnSuccessListener

            val message = """
            Przedmiot: ${data["itemName"]}
            Ilość: ${data["quantity"]}
            Płatność: ${data["payment"]}
            Rezerwujący: ${data["firstName"]} ${data["lastName"]}
            Telefon: ${data["phone"]}
            Data odbioru: ${data["pickupDate"]}
            Status: ${data["status"]}
        """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Szczegóły rezerwacji")
                .setMessage(message)
                .setPositiveButton("Zaakceptuj") { _, _ ->
                    ref.child("status").setValue("Gotowe do odbioru")
                    Toast.makeText(this, "Rezerwacja zaakceptowana", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Odrzuć") { _, _ ->
                    showRejectionReasonDialog(ref)
                }
                .setNeutralButton("Anuluj", null)
                .show()
        }
    }
    private fun showRejectionReasonDialog(ref: DatabaseReference) {
        val input = EditText(this)
        input.hint = "Wpisz powód odrzucenia"

        AlertDialog.Builder(this)
            .setTitle("Powód odrzucenia")
            .setView(input)
            .setPositiveButton("Potwierdź") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isNotEmpty()) {
                    ref.child("status").setValue("Odrzucono. Powód: $reason")
                    Toast.makeText(this, "Rezerwacja odrzucona", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Musisz wpisać powód", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    public fun updateNavigationUI() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val menu = navView.menu
        val loginItem = menu.findItem(R.id.nav_login)
        val logoutItem = menu.findItem(R.id.nav_logout)
        val forTrainersItem = menu.findItem(R.id.for_trainers)
        val userProfileItem = menu.findItem(R.id.nav_profile)
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
                        userProfileItem.isVisible = true
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
            userProfileItem.isVisible = false
        }
    }
}