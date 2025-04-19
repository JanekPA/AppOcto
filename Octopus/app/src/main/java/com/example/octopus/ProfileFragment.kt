/*package com.example.k

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.k.databinding.ProfileBinding
import com.example.k.models.ListItem
import com.example.octopus.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class Profile : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var imageViewProfile: ImageView
    private lateinit var uid: String
    private val REQUEST_EXTERNAL_STORAGE = 1
    private lateinit var infoTextView: TextView
    private lateinit var binding: ProfileBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var profileImageView: ImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
        imageViewProfile = findViewById(R.id.View_Image)
        val uploadButton: Button = findViewById(R.id.upload_image)
        infoTextView = findViewById(R.id.info_text_view)
        setupViews()
        uploadButton.setOnClickListener {
            openImageChooser()
        }
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        loadImageFromFirebase()

        //val b1 = findViewById<ImageButton>(R.id.B1)
        val b2 = findViewById<ImageButton>(R.id.B2)
        val b3 = findViewById<ImageButton>(R.id.B3)

        //b1.setOnClickListener {
        //    infoTextView.text = "Information for B1"
        //}

        b2.setOnClickListener {
            infoTextView.text = "Information for B2"
        }

        b3.setOnClickListener {
            infoTextView.text = "Information for B3"
        }

        drawerLayout = binding.myDrawerLayout
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        loadNickname()

        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        val headerView = navigationView.getHeaderView(0)
        profileImageView = headerView.findViewById(R.id.View_Image2)

        navigationView.setNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.nav_preferences -> {
                    retrievingDataToPrefChanging()
                    finish()
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, Profile::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, Settings::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.Logout_button -> logout()
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        loadProfilePicture()

        showUserPreferences()
    }

    private fun showUserPreferences() {

        val holidaysRef = firebaseDatabase.getReference("UsersPersonalization")

        val firebaseUser = firebaseAuth.currentUser
        firebaseUser?.let { user ->
            val uid = user.uid

            holidaysRef.child(uid).get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {

                        val country = dataSnapshot.child("Country").children.map { activity ->
                            activity.key!!
                        }.toList()
                        val activities = dataSnapshot.child("Activities").children.map { activity ->
                            activity.key!!
                        }.toList()

                        val hobbies = dataSnapshot.child("Hobbies").children.map { hobby ->
                            hobby.key!!
                        }.toList()

                        val holidayInfo = StringBuilder()
                        val countryString = country.joinToString(", ")
                        holidayInfo.append("Country: $countryString\n")

                        holidayInfo.append("\nActivities:\n")
                        activities.forEach { activity ->
                            holidayInfo.append("- $activity\n")
                        }

                        holidayInfo.append("\nHobbies:\n")
                        hobbies.forEach { hobby ->
                            holidayInfo.append("- $hobby\n")
                        }

                        val mainWindowTextView = findViewById<TextView>(R.id.info_text_view)
                        mainWindowTextView.text = holidayInfo.toString()

                        val b1 = findViewById<ImageButton>(R.id.B1)

                        b1.setOnClickListener {
                            mainWindowTextView.text = holidayInfo.toString()
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error getting data", exception)
                }
        }
    }

    private fun openImageChooser() {
        if (!checkExternalStoragePermission()) {
            requestExternalStoragePermission()
        } else {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            resultLauncher.launch(intent)
        }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                selectedImageUri?.let {
                    uploadImageToFirebase(it)
                }
            }
        }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val profilePicturesRef = storageReference.child("ProfilePictures/$uid/")
        val filename = "profile.jpg"
        val imageRef = profilePicturesRef.child(filename)
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                loadImageFromFirebase()
                println("Image uploaded successfully")
            }
            .addOnFailureListener { exception ->
                println("Error uploading image: $exception")
            }
    }

    private fun loadImageFromFirebase() {
        val profilePicturesRef = storageReference.child("ProfilePictures/$uid/")
        val filename = "profile.jpg"
        val imageRef = profilePicturesRef.child(filename)
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            Picasso.get().load(uri).into(imageViewProfile)
        }.addOnFailureListener { exception ->
            println("Error loading image from Firebase Storage: $exception")
        }
    }

    private fun checkExternalStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestExternalStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Permission denied. Unable to access external storage.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupViews() {
        binding.AccountButton?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.CalendarVProfile.setOnClickListener {
            val intent = Intent(this, CalendarView::class.java)
            startActivity(intent)
            finish()
        }
        binding.AddButtonProfile?.setOnClickListener {
            val intent = Intent(this, AddHolidayActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.notificationProfile?.setOnClickListener {
            val intent = Intent(this, Community::class.java)
            startActivity(intent)
            finish()
        }
        binding.button4Profile?.setOnClickListener {
            val intent = Intent(this, AddNotes::class.java)
            startActivity(intent)
            finish()
        }
        binding.homeProfile?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        drawerLayout = binding.myDrawerLayout
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    private fun loadProfilePicture() {
        // How is this working? idk trust me bro
        // Code bellow made especially for nav_drawer
        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        currentUser?.let {
            val uid = it.uid
            val profilePicturesRef = FirebaseStorage.getInstance().reference.child("ProfilePictures/$uid/")
            val filename = "profile.jpg"
            val imageRef = profilePicturesRef.child(filename)

            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this@Profile)
                    .load(uri)
                    .into(profileImageView)
            }.addOnFailureListener { exception ->
                // errors
                Log.e("MainActivity", "Error loading profile picture from Firebase Storage: $exception")
            }
        }
    }
    private fun logout() {
        firebaseAuth.signOut()
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("remember", "false")
        editor.apply()
        finish()
        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
    }
    private fun retrievingDataToPrefChanging()
    {
        val firebaseAuthRD = FirebaseAuth.getInstance()
        val firebaseUserRD = firebaseAuthRD.currentUser
        val firebaseDatabaseRD = FirebaseDatabase.getInstance()
        val userPersonalizationRD = firebaseDatabaseRD.getReference("UsersPersonalization")
        firebaseUserRD?.let { user ->
            val uid = user.uid

            userPersonalizationRD.child(uid).get().addOnSuccessListener { persSnapshot ->
                if(persSnapshot.exists())
                {
                    val countryName = persSnapshot.child("Country").children.first().key
                    val hobbies = persSnapshot.child("Hobbies")
                    val activities = persSnapshot.child("Activities")
                    ///AKTYWNOSC + HOBBY - POBRANIE
                    val hobbyList = mutableListOf<ListItem>()
                    for(hobbySnapshot in hobbies.children)
                    {
                        val hobbyName = hobbySnapshot.key
                        hobbyName?.let {hobbyList.add(ListItem(it))}
                    }

                    ///AKTYWNOSC + HOBBY - POBRANIE
                    val activityList = mutableListOf<ListItem>()
                    for(activitySnapshot in activities.children)
                    {
                        val activityName = activitySnapshot.key
                        activityName?.let {activityList.add(ListItem(it))}
                    }
                    val intent = Intent(this, PrefChanging::class.java)
                    intent.putExtra("countryName", countryName)
                    if(hobbyList.isNotEmpty() && activityList.isNotEmpty()) {
                        intent.putExtra("hobbies", hobbyList.toTypedArray())
                        for(hobby in hobbyList)
                        {
                            Log.e("HOBBY",hobby.name) ///working properly
                        }
                        intent.putExtra("activities", activityList.toTypedArray())
                        for(activity in activityList)
                        {
                            Log.e("ACTIVITY", activity.name) ///working properly
                        }
                    }
                    intent.putParcelableArrayListExtra("hobbies", ArrayList(hobbyList))
                    intent.putParcelableArrayListExtra("activities",ArrayList(activityList))

                    startActivity(intent)
                    finish()
                }
            }

        }
    }
    private fun loadNickname()
    {
        val nickname = firebaseDatabase.getReference("UsersPersonalization").child(uid).child("nickname")
        nickname.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nick = snapshot.value.toString()
                val navigationView: NavigationView = findViewById(R.id.navigation_view)
                val headerView = navigationView.getHeaderView(0)
                profileImageView = headerView.findViewById(R.id.View_Image2)
                val usernameMAIN: TextView = findViewById(R.id.username_text)
                usernameMAIN.text = nick
                val username: TextView = headerView.findViewById(R.id.textView_username)
                username.text = nick
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error fetching nickname")
                Toast.makeText(applicationContext, "Error fetching nickname", Toast.LENGTH_SHORT).show()
            }
        })
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}*/