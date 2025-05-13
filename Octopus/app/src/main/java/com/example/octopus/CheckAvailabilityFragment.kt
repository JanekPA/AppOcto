package com.example.octopus

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class CheckAvailabilityFragment : Fragment() {

    private lateinit var itemTypeSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemButton: Button

    private lateinit var database: DatabaseReference
    private lateinit var adapter: ItemsAdapter

    private var currentType: String = "Sprzęt"
    private val items = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_check_availability, container, false)

        itemTypeSpinner = view.findViewById(R.id.itemTypeSpinner)
        recyclerView = view.findViewById(R.id.itemsRecyclerView)
        addItemButton = view.findViewById(R.id.addItemButton)

        adapter = ItemsAdapter(items) { selectedItem ->
            showReservationDialog(selectedItem)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference

        itemTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentType = parent?.getItemAtPosition(position).toString()
                loadItems()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        addItemButton.setOnClickListener {
            showAddItemDialog()
        }

        return view
    }

    private fun loadItems() {
        val typeRef = database.child("Storage").child(currentType)
        typeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
                for (itemSnap in snapshot.children) {
                    val item = itemSnap.getValue(Item::class.java)
                    if(item?.quantity == 0)
                    {
                        typeRef.child(item.id).removeValue()
                    }
                    else {
                        item?.let { items.add(it) }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.itemNameEditText)
        val colorEditText = dialogView.findViewById<EditText>(R.id.itemColorEditText)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.itemTypeSpinner)
        val sizeSpinner = dialogView.findViewById<Spinner>(R.id.itemSizeSpinner)
        val customSizeEditText = dialogView.findViewById<EditText>(R.id.itemCustomSizeEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.itemQuantityEditText)
        val priceEditText = dialogView.findViewById<EditText>(R.id.itemPriceEditText)


        // Ukryj/odkryj elementy na podstawie typu
        if (currentType == "Ubranie") {
            typeSpinner.visibility = View.VISIBLE
            sizeSpinner.visibility = View.VISIBLE
            customSizeEditText.visibility = View.GONE
        } else if (currentType == "Sprzęt") {
            typeSpinner.visibility = View.GONE
            sizeSpinner.visibility = View.GONE
            customSizeEditText.visibility = View.VISIBLE
        } else if (currentType == "Akcesoria") {
            typeSpinner.visibility = View.GONE
            sizeSpinner.visibility = View.GONE
            customSizeEditText.visibility = View.GONE
            colorEditText.visibility = View.VISIBLE
        }

        // Adaptery do spinnerów (dla ubrań)
        if (currentType == "Ubranie") {
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.item_type_options,
                android.R.layout.simple_spinner_item
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                typeSpinner.adapter = it
            }

            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.item_size_options,
                android.R.layout.simple_spinner_item
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                sizeSpinner.adapter = it
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj nową rzecz")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { dialog, _ ->
                val name = nameEditText.text.toString()
                val color = colorEditText.text.toString()
                val type = if (currentType == "Ubranie") typeSpinner.selectedItem.toString() else "Uniwersalny"
                val size = when (currentType) {
                    "Ubranie" -> sizeSpinner.selectedItem.toString()
                    "Sprzęt" -> customSizeEditText.text.toString().ifEmpty { "Uniwersalny" }
                    "Akcesoria" -> sizeSpinner.selectedItem.toString()
                    else -> ""
                }
                val quantity = quantityEditText.text.toString().toIntOrNull() ?: 1
                val price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
                val itemId = database.child("Storage").child(currentType).push().key ?: UUID.randomUUID().toString()
                val newItem = Item(itemId, name, color, type, size, quantity, price, currentType)

                database.child("Storage").child(currentType).child(itemId).setValue(newItem)
                loadItems()
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }


    private fun showReservationDialog(item: Item) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Zaloguj się, aby zarezerwować przedmiot", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_reservation_form, null)

        val firstNameEditText = dialogView.findViewById<EditText>(R.id.firstNameEditText)
        val lastNameEditText = dialogView.findViewById<EditText>(R.id.lastNameEditText)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.phoneEditText)
        val paymentSpinner = dialogView.findViewById<Spinner>(R.id.paymentSpinner)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)  // dodaj to pole w XML

        val paymentOptions = listOf("Gotówka")
        val paymentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentOptions)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentSpinner.adapter = paymentAdapter
        val pickupDateEditText = dialogView.findViewById<EditText>(R.id.pickupDateEditText)
        pickupDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selectedDate = String.format("%02d-%02d-%d", day, month + 1, year)
                    pickupDateEditText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Zarezerwuj: ${item.name}")
            .setView(dialogView)
            .setPositiveButton("Zarezerwuj") { dialog, _ ->
                val firstName = firstNameEditText.text.toString()
                val lastName = lastNameEditText.text.toString()
                val phone = phoneEditText.text.toString()
                val payment = paymentSpinner.selectedItem?.toString() ?: "Nie wybrano"
                val quantity = quantityEditText.text.toString().toIntOrNull() ?: 1

                if (quantity > item.quantity) {
                    Toast.makeText(requireContext(), "Nie ma tylu sztuk dostępnych", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val reservation = mapOf(
                    "itemName" to item.name,
                    "userId" to user.uid,
                    "userEmail" to user.email,  // <-- dodaj to
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "phone" to phone,
                    "payment" to payment,
                    "quantity" to quantity,
                    "pickupDate" to pickupDateEditText.text.toString(),
                    "price" to item.price,
                    "status" to "W trakcie realizacji"
                )


                val reservationId = database.child("ReservedItems").push().key ?: UUID.randomUUID().toString()
                database.child("ReservedItems").child(reservationId).setValue(reservation)

                // Aktualizacja ilości w magazynie
                val newQuantity = item.quantity - quantity
                if (newQuantity == 0) {
                    database.child("Storage").child(currentType).child(item.id).removeValue()
                }
                database.child("Storage").child(currentType).child(item.id).child("quantity").setValue(newQuantity)
                    .addOnCompleteListener {
                        Toast.makeText(requireContext(), "Zarezerwowano!", Toast.LENGTH_SHORT).show()
                        loadItems()
                        dialog.dismiss()
                    }
                val userRoleRef = database.child("UsersPersonalization")
                database.child("UsersPersonalization").orderByKey().equalTo(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (userSnap in snapshot.children) {
                            val role = userSnap.child("role").getValue(String::class.java)
                            if (role == "user" || role == "trainer") {
                                // Powiadom wszystkich adminów
                                database.child("UsersPersonalization").addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(adminSnapshot: DataSnapshot) {
                                        for (adminSnap in adminSnapshot.children) {
                                            val adminRole = adminSnap.child("role").getValue(String::class.java)
                                            if (adminRole == "admin") {
                                                val adminUid = adminSnap.key ?: continue
                                                val notifId = database.child("Notifications").child(adminUid).push().key ?: UUID.randomUUID().toString()

                                                val notification = mapOf(
                                                    "title" to "Nowa rezerwacja",
                                                    "message" to "Użytkownik ${user.email} ($firstName $lastName) złożył zamówienie.",
                                                    "reservationId" to reservationId,
                                                    "timestamp" to System.currentTimeMillis()
                                                )

                                                database.child("Notifications").child(adminUid).child(notifId).setValue(notification)
                                            }
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            }
            .setNegativeButton("Anuluj", null)
            .show()
    }


}
