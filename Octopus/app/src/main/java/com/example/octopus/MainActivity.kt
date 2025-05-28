package com.example.octopus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private var badgeTextView: TextView? = null

    override fun onStart() {
        super.onStart()
        updateNavigationUI()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && badgeTextView != null) {
            startNotificationListener(badgeTextView!!)
        } else {
            badgeTextView?.visibility = View.GONE
        }

    }
    override fun onResume() {
        super.onResume()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ustawienie motywu i layoutu
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> navController.navigate(R.id.loginFragment)
                R.id.nav_profile -> navController.navigate(R.id.userProfileFragment)
                R.id.nav_schedule -> navController.navigate(R.id.scheduleFragment)
                R.id.nav_pricing -> navController.navigate(R.id.pricingFragment)
                R.id.nav_reservation -> navController.navigate(R.id.reservationFragment)
                R.id.nav_trainers -> navController.navigate(R.id.trainersFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_help -> navController.navigate(R.id.helpFragment)
                R.id.for_trainers -> navController.navigate(R.id.forTrainersFragment)
                R.id.nav_admin_panel -> navController.navigate(R.id.forAdminFragment)
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    drawerLayout.closeDrawer(GravityCompat.END)
                    navController.navigate(R.id.mainFragment)
                    drawerLayout.postDelayed({ updateNavigationUI() }, 100)
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuItem = menu.findItem(R.id.action_notifications)
        val actionView = menuItem.actionView
        badgeTextView = actionView?.findViewById(R.id.notification_badge)

        // Nasłuchuj badge tylko jeśli użytkownik jest zalogowany i TextView już istnieje
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null && badgeTextView != null) {
                startNotificationListener(badgeTextView!!)
            } else {
                badgeTextView?.visibility = View.GONE
            }
        }

        actionView?.setOnClickListener {
            onOptionsItemSelected(menuItem)
        }

        return true
    }

    private var notificationListener: ValueEventListener? = null

    private fun startNotificationListener(badgeTextView: TextView) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            badgeTextView.visibility = View.GONE
            return
        }

        val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUser.uid)

        // Usuń poprzedni listener jeśli istnieje
        notificationListener?.let { notifRef.removeEventListener(it) }

        notificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.children.count()
                if (count > 0) {
                    badgeTextView.visibility = View.VISIBLE
                    badgeTextView.text = if (count > 99) "99+" else count.toString()
                } else {
                    badgeTextView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        notifRef.addValueEventListener(notificationListener!!)
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
            R.id.action_notifications -> {
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
                    val type = child.child("type").getValue(String::class.java) ?: "general"

                    notifications.add(NotificationItem(title, message, timestamp, reservationId, type))
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
                        when (notification.type) {
                            "item_reservation" -> showAdminReservationDialog(notification.reservationId)
                            "training_reservation" -> showTrainerTrainingDialog(notification.reservationId)
                            "general" -> showGeneralDialog(notification)
                            "message" -> showMessageDialog(notification)
                            else -> Toast.makeText(this, "Nieznany typ powiadomienia", Toast.LENGTH_SHORT).show()
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

                        ref.child(key).removeValue()
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
                    ref.child("status").setValue("Gotowe do odbioru").addOnSuccessListener {
                        val userUid = data["userId"] as? String ?: return@addOnSuccessListener
                        val itemName = data["itemName"] as? String ?: "przedmiot"

                        val notifRef = FirebaseDatabase.getInstance()
                            .getReference("Notifications")
                            .child(userUid)
                            .push()

                        val notification = mapOf(
                            "title" to "Zmiana statusu zamówienia",
                            "message" to "Status twojego zamówienia na \"$itemName\" został zmieniony!",
                            "timestamp" to System.currentTimeMillis(),
                            "reservationId" to ref.key,
                            "type" to "message"
                        )

                        notifRef.setValue(notification)
                    }
                }
                .setNegativeButton("Odrzuć") { _, _ ->
                    showRejectionReasonDialog(ref, data)

                }
                .setNeutralButton("Anuluj", null)
                .show()
        }
    }
    private fun showRejectionReasonDialog(ref: DatabaseReference, data: Map<String, Any>) {
        val input = EditText(this)
        input.hint = "Wpisz powód odrzucenia"

        AlertDialog.Builder(this)
            .setTitle("Powód odrzucenia")
            .setView(input)
            .setPositiveButton("Potwierdź") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isNotEmpty()) {
                    ref.child("status").setValue("Odrzucono. Powód: $reason")
                        .addOnSuccessListener {
                            val userUid = data["userId"] as? String ?: return@addOnSuccessListener
                            val itemName = data["itemName"] as? String ?: "przedmiot"

                            val notifRef = FirebaseDatabase.getInstance()
                                .getReference("Notifications")
                                .child(userUid)
                                .push()

                            val notification = mapOf(
                                "title" to "Zamówienie odrzucone",
                                "message" to "Twoje zamówienie na \"$itemName\" zostało odrzucone. Powód: $reason",
                                "timestamp" to System.currentTimeMillis(),
                                "reservationId" to ref.key,
                                "type" to "item_reservation"
                            )

                            notifRef.setValue(notification)
                        }

                    Toast.makeText(this, "Rezerwacja odrzucona", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Musisz wpisać powód", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    private fun showTrainerTrainingDialog(reservationId: String) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("ReservedTrainings")
            .child("Pending")
            .child(reservationId)

        ref.get().addOnSuccessListener { snapshot ->
            val data = snapshot.value as? Map<String, Any> ?: return@addOnSuccessListener

            // Odczytaj email trenera z rezerwacji i porównaj z aktualnie zalogowanym
            val trainerEmail = data["trainerEmail"] as? String ?: return@addOnSuccessListener
            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return@addOnSuccessListener
            if (trainerEmail != currentUserEmail) return@addOnSuccessListener // Nie dla tego trenera

            val message = """
            Rezerwujący: ${data["firstName"]} ${data["lastName"]}
            Email: ${data["userEmail"]}
            Telefon: ${data["phoneNumber"]}
            Data: ${data["date"]}
            Godzina: ${data["time"]}
        """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Rezerwacja treningu")
                .setMessage(message)
                .setPositiveButton("Zaakceptuj") { _, _ ->
                    confirmReservation(ref, reservationId, data)
                }
                .setNegativeButton("Odrzuć") { _, _ ->
                    showTrainingRejectionDialog(ref)
                }
                .setNeutralButton("Anuluj", null)
                .show()
        }
    }
    private fun confirmReservation(ref: DatabaseReference, reservationId: String, data: Map<String, Any>) {
        val confirmedRef = FirebaseDatabase.getInstance()
            .getReference("ReservedTrainings")
            .child("Confirmed")
            .child(reservationId)

        confirmedRef.setValue(data).addOnSuccessListener {
            ref.removeValue() // usuń z "Pending"

            // Usuń zajętą godzinę z ScheduleTrainers
            val trainerId = (data["trainerEmail"] as? String)?.replace(".", ",") ?: return@addOnSuccessListener
            val date = data["date"] as? String ?: return@addOnSuccessListener
            val hour = data["hour"] as? String ?: return@addOnSuccessListener

            val scheduleRef = FirebaseDatabase.getInstance()
                .getReference("ScheduleTrainers")
                .child(trainerId)
                .child(date)

            scheduleRef.get().addOnSuccessListener { dateSnapshot ->
                for (timeSnap in dateSnapshot.children) {
                    if (timeSnap.getValue(String::class.java) == hour) {
                        timeSnap.ref.removeValue() // usuń zajętą godzinę
                        break
                    }
                }
            }

            Toast.makeText(this, "Rezerwacja zaakceptowana", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showTrainingRejectionDialog(reservationRef: DatabaseReference) {
        val input = EditText(this)
        input.hint = "Wpisz powód odrzucenia"

        AlertDialog.Builder(this)
            .setTitle("Powód odrzucenia")
            .setView(input)
            .setPositiveButton("Potwierdź") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Musisz wpisać powód", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                reservationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return

                        val dataMap = snapshot.value as? Map<String, Any?> ?: return
                        val trainerEmail = dataMap["trainerEmail"] as? String ?: return
                        val trainerName = dataMap["trainerName"] as? String ?: return
                        val trainerSurname = dataMap["trainerSurname"] as? String ?: return
                        val date = dataMap["date"] as? String ?: return
                        val time = dataMap["time"] as? String ?: return
                        val userUid = dataMap["userUid"] as? String ?: return

                        // Przywróć godzinę w ScheduleTrainers (bez duplikatów)
                        val formattedEmail = trainerEmail.replace(".", ",")
                        val scheduleDayRef = FirebaseDatabase.getInstance()
                            .getReference("scheduleTrainers")
                            .child(formattedEmail)
                            .child(date)

                        scheduleDayRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(daySnapshot: DataSnapshot) {
                                val alreadyExists = daySnapshot.children.any {
                                    it.getValue(String::class.java) == time
                                }
                                if (!alreadyExists) {
                                    scheduleDayRef.push().setValue(time)
                                }

                                // Przenieś dane do ReservedTrainings/Rejected
                                val rejectedRef = FirebaseDatabase.getInstance()
                                    .getReference("ReservedTrainings/Rejected")
                                    .push()

                                val rejectedData = dataMap.toMutableMap().apply {
                                    this["status"] = "rejected"
                                    this["rejectionReason"] = reason
                                }

                                rejectedRef.setValue(rejectedData)

                                // Usuń z ReservedTrainings/Pending
                                reservationRef.removeValue()

                                // Powiadom użytkownika
                                val notifRef = FirebaseDatabase.getInstance()
                                    .getReference("Notifications")
                                    .child(userUid)
                                    .push()

                                val notification = mapOf(
                                    "title" to "Rezerwacja odrzucona",
                                    "message" to "Twoja rezerwacja treningu na $date o $time u trenera $trainerName $trainerSurname została odrzucona. Powód: $reason",
                                    "timestamp" to System.currentTimeMillis(),
                                    "type" to "message"
                                )
                                notifRef.setValue(notification)

                                Toast.makeText(this@MainActivity, "Rezerwacja odrzucona", Toast.LENGTH_SHORT).show()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@MainActivity, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
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
        val forAdminItem = menu.findItem(R.id.nav_admin_panel)
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
                        else if (role == "admin")
                        {
                            forAdminItem.isVisible = true
                            forTrainersItem.isVisible = false
                        }
                    }
                    else{
                        headerText.text = "Witaj, $username!"
                        userProfileItem.isVisible = true
                        forTrainersItem.isVisible = false
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
            forAdminItem.isVisible = false
        }
    }
    private fun showGeneralDialog(notification: NotificationItem) {
        AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMessageDialog(notification: NotificationItem) {
        // Możesz dodać tu np. możliwość odpowiedzi
        AlertDialog.Builder(this)
            .setTitle("Wiadomość")
            .setMessage(notification.message)
            .setPositiveButton("Zamknij", null)
            .show()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Usuń listener do powiadomień, jeśli istnieje
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(it.uid)
            notificationListener?.let { listener -> notifRef.removeEventListener(listener) }
        }
    }
    override fun onStop() {
        super.onStop()
        // Usuń nasłuchiwacz, jeśli niepotrzebny
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(it.uid)
            notificationListener?.let { listener ->
                notifRef.removeEventListener(listener)
            }
        }
    }

}