package com.example.octopus

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyReservationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReservationAdapter
    private lateinit var database: DatabaseReference

    private val reservations = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_reservations, container, false)
        recyclerView = view.findViewById(R.id.myReservationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReservationAdapter(reservations) { reservation ->
            showReservationDetailsDialog(reservation)
        }
        recyclerView.adapter = adapter

        loadUserReservations()

        return view
    }

    private fun loadUserReservations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentEmail = currentUser?.email ?: return

        database = FirebaseDatabase.getInstance().reference.child("ReservedItems")
        database.addValueEventListener(object : ValueEventListener {
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
}
