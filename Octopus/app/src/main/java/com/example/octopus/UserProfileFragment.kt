package com.example.octopus

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class UserProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var editImie: EditText
    private lateinit var editNazwisko: EditText
    private lateinit var editUsername: EditText
    private lateinit var editPhone: EditText
    private lateinit var textEmail: TextView
    private lateinit var saveButton: Button
    private lateinit var reservationList: RecyclerView
    private lateinit var imieWarning: TextView
    private lateinit var nazwiskoWarning: TextView
    private lateinit var phoneNumberWarning: TextView
    private lateinit var usernameWarning: TextView
    private val reservations = mutableListOf<Map<String, Any>>()
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private lateinit var adapter: ReservationAdapter
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        editImie = view.findViewById(R.id.edit_imie)
        editNazwisko = view.findViewById(R.id.edit_nazwisko)
        editUsername = view.findViewById(R.id.edit_username)
        editPhone = view.findViewById(R.id.edit_phone)
        textEmail = view.findViewById(R.id.text_email)
        saveButton = view.findViewById(R.id.save_button)
        reservationList = view.findViewById(R.id.reservation_list)
        reservationList.layoutManager = LinearLayoutManager(requireContext())
        imieWarning = view.findViewById(R.id.imie_warning)
        nazwiskoWarning = view.findViewById(R.id.nazwisko_warning)
        usernameWarning = view.findViewById(R.id.username_warning)
        phoneNumberWarning = view.findViewById(R.id.phoneNumber_warning)
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        adapter = ReservationAdapter(reservations) { reservation ->
            showReservationDetailsDialog(reservation)
        }
        reservationList.adapter = adapter
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            saveUserData()
        }

        loadUserData()
        loadUserReservations()

        return view
    }

    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val ref = database.child("UsersPersonalization").child(userId)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    editImie.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                    editNazwisko.setText(snapshot.child("surname").getValue(String::class.java) ?: "")
                    editUsername.setText(snapshot.child("username").getValue(String::class.java) ?: "")
                    editPhone.setText(snapshot.child("phoneNumber").getValue(String::class.java) ?: "")
                    textEmail.text = currentUser.email ?: ""

                    imieWarning.visibility = if (editImie.text.isEmpty()) View.VISIBLE else View.GONE
                    nazwiskoWarning.visibility = if (editNazwisko.text.isEmpty()) View.VISIBLE else View.GONE
                    usernameWarning.visibility = if (editUsername.text.isEmpty()) View.VISIBLE else View.GONE
                    phoneNumberWarning.visibility = if (editPhone.text.isEmpty()) View.VISIBLE else View.GONE
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@UserProfileFragment).load(imageUrl).circleCrop().into(profileImage)

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    private fun saveUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val ref = database.child("UsersPersonalization").child(userId)

        val updatedData = mapOf(
            "name" to editImie.text.toString(),
            "surname" to editNazwisko.text.toString(),
            "username" to editUsername.text.toString(),
            "phoneNumber" to editPhone.text.toString(),
            "email" to (currentUser.email ?: "") // dla pewności aktualizuj email
        )

        ref.updateChildren(updatedData)
            .addOnSuccessListener {
                if (imageUri != null) {
                    val storageRef = storage.reference.child("profile_images/$userId.jpg")
                    storageRef.putFile(imageUri!!).addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            ref.child("profileImageUrl").setValue(uri.toString())
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Zapisano dane i zdjęcie", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Dane zapisane, ale nie udało się zapisać zdjęcia", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Błąd zapisu zdjęcia", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Dane zapisane", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Błąd zapisu danych", Toast.LENGTH_SHORT).show()
            }
    }




    private fun loadUserReservations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentEmail = currentUser?.email ?: return

        val refdatabase = database.child("ReservedItems")
        refdatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reservations.clear()
                for (firstLevel in snapshot.children) {
                    val data = firstLevel.value as? Map<String, Any>
                    if (data != null && data["userEmail"] == currentEmail) {
                        reservations.add(data)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Błąd ładowania rezerwacji", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showReservationDetailsDialog(reservation: Map<String, Any>) {
        val message = """
        Przedmiot: ${reservation["itemName"]}
        Ilość: ${reservation["quantity"]}
        Płatność: ${reservation["payment"]}
        Rezerwujący: ${reservation["firstName"]} ${reservation["lastName"]}
        Telefon: ${reservation["phone"]}
        Data odbioru: ${reservation["pickupDate"]}
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Szczegóły rezerwacji")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            profileImage.setImageURI(imageUri)
        }
    }
}
