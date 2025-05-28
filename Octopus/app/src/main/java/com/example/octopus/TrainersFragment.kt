package com.example.octopus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class TrainersFragment : Fragment() {

    private lateinit var trainingTypeSpinner: Spinner
    private lateinit var groupLevelSpinner: Spinner
    private lateinit var trainersRecyclerView: RecyclerView
    private lateinit var hoursRecyclerView: RecyclerView
    private lateinit var reserveButton: Button
    private lateinit var contactTextView: TextView
    private lateinit var contactContainer: LinearLayout // lub inny widok zawierający dane kontaktowe
    private lateinit var pickDateButton: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var classTypesRef: DatabaseReference
    private lateinit var groupLevelsRef: DatabaseReference
    private lateinit var trainersRef: DatabaseReference
    private lateinit var scheduleRef: DatabaseReference
    private lateinit var hoursAdapter: HoursAdapter
    private var selectedTrainer: Trainer? = null
    private var selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE) // domyślnie dziś
    private var selectedHour: String? = null

    private lateinit var trainersAdapter: TrainersAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_trainers, container, false)

        val addTrainerButton: Button = view.findViewById(R.id.buttonAddTrainer)
        val editTrainerButton: Button = view.findViewById(R.id.editTrainerButton)
        val addHoursButton: Button = view.findViewById(R.id.addHoursButton) // ← Dodany przycisk, jeśli masz go w layout

        // Domyślnie ukrywamy przyciski administracyjne
        addTrainerButton.visibility = View.GONE
        editTrainerButton.visibility = View.GONE
        addHoursButton.visibility = View.GONE

        trainingTypeSpinner = view.findViewById(R.id.spinnerTrainingType)
        groupLevelSpinner = view.findViewById(R.id.spinnerGroupLevel)
        trainersRecyclerView = view.findViewById(R.id.recyclerViewTrainers)
        hoursRecyclerView = view.findViewById(R.id.recyclerViewAvailableHours)
        reserveButton = view.findViewById(R.id.buttonReserve)
        contactTextView = view.findViewById(R.id.contactInfoMessage)
        contactContainer = view.findViewById(R.id.contactContainer)
        pickDateButton = view.findViewById(R.id.buttonPickDate)

        database = FirebaseDatabase.getInstance()
        classTypesRef = database.getReference("classTypes")
        groupLevelsRef = database.getReference("groupLevelsTrainers")
        trainersRef = database.getReference("TrainersData")
        scheduleRef = database.getReference("scheduleTrainers")

        reserveButton.isEnabled = false

        setupSpinners()
        loadClassTypes()
        loadGroupLevels()
        loadTrainers()

        addTrainerButton.setOnClickListener { showAddTrainerDialog() }
        editTrainerButton.setOnClickListener {
            selectedTrainer?.let { showEditTrainerDialog(it) }
        }

        // Sprawdzenie roli użytkownika
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = FirebaseDatabase.getInstance().getReference("UsersPersonalization/$uid")

            userRef.child("role").get().addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java)
                if (role == "admin") {
                    addTrainerButton.visibility = View.VISIBLE
                    editTrainerButton.visibility = View.VISIBLE
                    addHoursButton.visibility = View.VISIBLE
                }
            }
        }

        reserveButton.setOnClickListener {
            val userUid = currentUser?.uid ?: return@setOnClickListener
            val database = FirebaseDatabase.getInstance().reference

            database.child("UsersPersonalization").child(userUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val email = snapshot.child("email").getValue(String::class.java)
                        val name = snapshot.child("name").getValue(String::class.java)
                        val surname = snapshot.child("surname").getValue(String::class.java)
                        val phone = snapshot.child("phoneNumber").getValue(String::class.java)

                        if (email.isNullOrEmpty() || name.isNullOrEmpty() || surname.isNullOrEmpty() || phone.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Uzupełnij dane w profilu przed rezerwacją!", Toast.LENGTH_LONG).show()
                            return
                        }

                        val selectedTrainerEmail = selectedTrainer?.email
                        val selectedDate = selectedDate
                        val selectedHour = selectedHour

                        database.child("UsersPersonalization").orderByChild("email").equalTo(selectedTrainerEmail)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(trainerSnap: DataSnapshot) {
                                    val trainerUid = trainerSnap.children.firstOrNull()?.key
                                    if (trainerUid == null) {
                                        Toast.makeText(requireContext(), "Nie znaleziono trenera.", Toast.LENGTH_SHORT).show()
                                        return
                                    }

                                    val reservationId = database.child("ReservedTrainings").child("Pending").push().key ?: UUID.randomUUID().toString()

                                    val reservationData = mapOf(
                                        "firstName" to name,
                                        "lastName" to surname,
                                        "trainerName" to selectedTrainer!!.name,
                                        "trainerSurname" to selectedTrainer!!.surname,
                                        "phoneNumber" to phone,
                                        "userUid" to userUid,
                                        "userEmail" to email,
                                        "trainerUid" to trainerUid,
                                        "trainerEmail" to selectedTrainerEmail,
                                        "date" to selectedDate,
                                        "time" to selectedHour
                                    )

                                    database.child("ReservedTrainings").child("Pending").child(reservationId)
                                        .setValue(reservationData)

                                    val timestamp = System.currentTimeMillis()
                                    val notifId = database.child("Notifications").child(trainerUid).push().key ?: UUID.randomUUID().toString()

                                    val notif = mapOf(
                                        "title" to "Nowa rezerwacja treningu",
                                        "message" to "$email zarezerwował trening na $selectedDate o $selectedHour.",
                                        "reservationId" to reservationId,
                                        "timestamp" to timestamp,
                                        "type" to "training_reservation"
                                    )

                                    database.child("Notifications").child(trainerUid).child(notifId).setValue(notif)

                                    Toast.makeText(requireContext(), "Rezerwacja została złożona", Toast.LENGTH_SHORT).show()
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        pickDateButton.setOnClickListener {
            showDatePickerDialog()
        }
        hoursAdapter = HoursAdapter { selected ->
            selectedHour = selected
            reserveButton.isEnabled = true
        }
        hoursRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        hoursRecyclerView.adapter = hoursAdapter
        return view
    }


    private fun showAddTrainerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_trainer, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextName)
        val surnameInput = dialogView.findViewById<EditText>(R.id.editTextSurname)
        val phoneInput = dialogView.findViewById<EditText>(R.id.editTextPhone)
        val emailInput = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val facebookInput = dialogView.findViewById<EditText>(R.id.editTextFacebook)
        val instagramInput = dialogView.findViewById<EditText>(R.id.editTextInstagram)

        val classTypesText = dialogView.findViewById<TextView>(R.id.textViewClassTypes)
        val groupLevelsText = dialogView.findViewById<TextView>(R.id.textViewGroupLevels)

        val selectedTypes = mutableListOf<String>()
        val selectedLevels = mutableListOf<String>()

        val types = mutableListOf<String>()
        val levels = mutableListOf<String>()

        classTypesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                types.addAll(snapshot.children.mapNotNull { it.getValue(String::class.java) })
                val selectedItemsTypes = BooleanArray(types.size) // ← dodaj to
                classTypesText.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Wybierz typy zajęć")
                        .setMultiChoiceItems(types.toTypedArray(), selectedItemsTypes) { _, which, isChecked ->
                            if (isChecked) selectedTypes.add(types[which]) else selectedTypes.remove(types[which])
                        }
                        .setPositiveButton("OK") { _, _ ->
                            classTypesText.text = if (selectedTypes.isNotEmpty()) selectedTypes.joinToString(", ") else "Wybierz typy zajęć"
                        }
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        groupLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                levels.addAll(snapshot.children.mapNotNull { it.getValue(String::class.java) })
                val selectedItemsLevels = BooleanArray(levels.size) // ← dodaj to
                groupLevelsText.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Wybierz poziomy zaawansowania")
                        .setMultiChoiceItems(levels.toTypedArray(), selectedItemsLevels) { _, which, isChecked ->
                            if (isChecked) selectedLevels.add(levels[which]) else selectedLevels.remove(levels[which])
                        }
                        .setPositiveButton("OK") { _, _ ->
                            groupLevelsText.text = if (selectedLevels.isNotEmpty()) selectedLevels.joinToString(", ") else "Wybierz poziomy grup"
                        }
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj nowego trenera")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = nameInput.text.toString()
                val surname = surnameInput.text.toString()
                val email = emailInput.text.toString().replace(".",",")
                val key = email

                val trainerData = mapOf(
                    "name" to name,
                    "surname" to surname,
                    "phoneNumber" to phoneInput.text.toString(),
                    "email" to emailInput.text.toString(),
                    "facebook" to facebookInput.text.toString(),
                    "instagram" to instagramInput.text.toString(),
                    "classTypes" to selectedTypes,
                    "groupLevels" to selectedLevels
                )
                trainersRef.child(key).setValue(trainerData)
                Toast.makeText(requireContext(), "Trener dodany!", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }




    private fun setupSpinners() {
        val refreshTrainersListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadTrainers()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        trainingTypeSpinner.onItemSelectedListener = refreshTrainersListener
        groupLevelSpinner.onItemSelectedListener = refreshTrainersListener
    }
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->

                val localDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth)
                selectedDate = localDate.format(DateTimeFormatter.ISO_DATE) // pasuje do Firebase
                pickDateButton.text = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) // ładne wyświetlanie
                // Załaduj dostępne godziny na wybraną datę
                selectedTrainer?.let { trainer ->
                    loadAvailableHoursForDate(trainer, selectedDate)
                }
            },
            year, month, day
        )
        datePickerDialog.show()
    }
    private fun loadAvailableHoursForDate(trainer: Trainer, date: String) {
        val trainerKey = trainer.email!!.replace(".", ",")
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        scheduleRef.child(trainerKey).child(date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allHours = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()

                // Funkcja pomocnicza do pobierania z Pending i Confirmed
                fun getReservedHours(statusPath: String, onComplete: (List<String>) -> Unit) {
                    val ref = FirebaseDatabase.getInstance().getReference("ReservedTrainings").child(statusPath)
                    ref.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val reserved = snapshot.children.mapNotNull { res ->
                                val resTrainerEmail = res.child("trainerEmail").getValue(String::class.java)
                                val resDate = res.child("date").getValue(String::class.java)
                                val resTime = res.child("time").getValue(String::class.java)

                                if (resTrainerEmail == trainer.email && resDate == date) resTime else null
                            }
                            onComplete(reserved)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                // Pobierz z obu: Pending i Confirmed
                getReservedHours("Pending") { pendingHours ->
                    getReservedHours("Confirmed") { confirmedHours ->
                        val allReserved = pendingHours + confirmedHours
                        val availableHours = allHours.filter { it !in allReserved }
                        val sortedHours = availableHours.sortedBy {
                            try {
                                LocalTime.parse(it, formatter)
                            } catch (e: Exception) {
                                LocalTime.MIDNIGHT
                            }
                        }
                        hoursAdapter.updateData(sortedHours)
                        reserveButton.isEnabled = sortedHours.isNotEmpty()
                        hoursRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        hoursRecyclerView.adapter = hoursAdapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun loadClassTypes() {
        classTypesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val types = snapshot.children.mapNotNull { it.getValue(String::class.java) } // ZMIANA
                trainingTypeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadGroupLevels() {
        groupLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val levels = snapshot.children.mapNotNull { it.getValue(String::class.java) } // ZMIANA
                groupLevelSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, levels)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadTrainers() {
        // Resetuj wcześniej wybranego trenera i godziny
        selectedTrainer = null
        selectedHour = null
        contactTextView.visibility = View.VISIBLE
        contactContainer.visibility = View.GONE
        hoursRecyclerView.adapter = null
        reserveButton.isEnabled = false

        val selectedType = trainingTypeSpinner.selectedItem?.toString()?.trim()
        val selectedLevel = groupLevelSpinner.selectedItem?.toString()?.trim()

        trainersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allChildren = snapshot.children.toList()
                val filteredChildren = allChildren.filter { child ->
                    val types = child.child("classTypes").children.mapNotNull { it.getValue(String::class.java)?.trim() }
                    val levels = child.child("groupLevels").children.mapNotNull { it.getValue(String::class.java)?.trim() }
                    (selectedType == null || selectedType in types) &&
                            (selectedLevel == null || selectedLevel in levels)
                }

                if (filteredChildren.isEmpty()) {
                    setupTrainerRecycler(emptyList())
                    return
                }

                val filteredList = mutableListOf<Trainer>()
                var loadedCount = 0

                for (child in filteredChildren) {
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val surname = child.child("surname").getValue(String::class.java) ?: continue
                    val phone = child.child("phoneNumber").getValue(String::class.java) ?: ""
                    val email = child.child("email").getValue(String::class.java) ?: ""
                    val facebook = child.child("facebook").getValue(String::class.java) ?: ""
                    val instagram = child.child("instagram").getValue(String::class.java) ?: ""
                    val types = child.child("classTypes").children.mapNotNull { it.getValue(String::class.java)?.trim() }
                    val levels = child.child("groupLevels").children.mapNotNull { it.getValue(String::class.java)?.trim() }

                    val availabilityRef = database.getReference("scheduleTrainers").child(child.key!!)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                    val availability = mutableMapOf<String, List<String>>()

                    val reservedPendingRef = database.getReference("ReservedTrainings/Pending")
                    val reservedConfirmedRef = database.getReference("ReservedTrainings/Confirmed")

                    reservedPendingRef.orderByChild("trainerEmail").equalTo(email)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(pendingSnapshot: DataSnapshot) {
                                val pendingSet = pendingSnapshot.children.mapNotNull {
                                    val date = it.child("date").getValue(String::class.java)
                                    val time = it.child("time").getValue(String::class.java)
                                    if (date != null && time != null) "$date|$time" else null
                                }.toSet()

                                reservedConfirmedRef.orderByChild("trainerEmail").equalTo(email)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(confirmedSnapshot: DataSnapshot) {
                                            val confirmedSet = confirmedSnapshot.children.mapNotNull {
                                                val date = it.child("date").getValue(String::class.java)
                                                val time = it.child("time").getValue(String::class.java)
                                                if (date != null && time != null) "$date|$time" else null
                                            }.toSet()

                                            val reservedSet = pendingSet + confirmedSet

                                            availabilityRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(avSnapshot: DataSnapshot) {
                                                    for (dateSnapshot in avSnapshot.children) {
                                                        val date = dateSnapshot.key ?: continue
                                                        val allHours = dateSnapshot.children.mapNotNull { it.getValue(String::class.java) }

                                                        val availableHours = allHours.filter { hour ->
                                                            "$date|$hour" !in reservedSet
                                                        }.distinct().sortedBy {
                                                            try {
                                                                LocalTime.parse(it, formatter)
                                                            } catch (e: Exception) {
                                                                LocalTime.MIDNIGHT
                                                            }
                                                        }

                                                        if (availableHours.isNotEmpty()) {
                                                            availability[date] = availableHours
                                                        }
                                                    }

                                                    filteredList.add(
                                                        Trainer(name, surname, email, facebook, phone, instagram, availability, types, levels)
                                                    )
                                                    loadedCount++
                                                    if (loadedCount == filteredChildren.size) {
                                                        setupTrainerRecycler(filteredList)
                                                        val stillExists = filteredList.any {
                                                            it.name == selectedTrainer?.name && it.surname == selectedTrainer?.surname
                                                        }
                                                        if (!stillExists) {
                                                            selectedTrainer = null
                                                            selectedHour = null
                                                            contactTextView.visibility = View.VISIBLE
                                                            contactContainer.visibility = View.GONE

                                                            hoursAdapter = HoursAdapter { selected ->
                                                                selectedHour = selected
                                                                reserveButton.isEnabled = true
                                                            }
                                                            hoursAdapter.updateData(emptyList())

                                                            hoursRecyclerView.adapter = hoursAdapter
                                                            reserveButton.isEnabled = false
                                                        }
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    loadedCount++
                                                    if (loadedCount == filteredChildren.size) {
                                                        setupTrainerRecycler(filteredList)
                                                    }
                                                }
                                            })
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            loadedCount++
                                            if (loadedCount == filteredChildren.size) {
                                                setupTrainerRecycler(filteredList)
                                            }
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                loadedCount++
                                if (loadedCount == filteredChildren.size) {
                                    setupTrainerRecycler(filteredList)
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }




    private fun showEditTrainerDialog(trainer: Trainer) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_trainer, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextName)
        val surnameInput = dialogView.findViewById<EditText>(R.id.editTextSurname)
        val phoneInput = dialogView.findViewById<EditText>(R.id.editTextPhone)
        val emailInput = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val facebookInput = dialogView.findViewById<EditText>(R.id.editTextFacebook)
        val instagramInput = dialogView.findViewById<EditText>(R.id.editTextInstagram)
        val classTypesText = dialogView.findViewById<TextView>(R.id.textViewClassTypes)
        val groupLevelsText = dialogView.findViewById<TextView>(R.id.textViewGroupLevels)

        nameInput.setText(trainer.name)
        surnameInput.setText(trainer.surname)

        val linePhoneNumber = trainer.phoneNumber!!.split("\n")
        val lineEmailInput = trainer.email.toString()
        val lineFacebookInput = trainer.facebook.toString()
        val lineInstagramInput = trainer.instagram.toString()
        phoneInput.setText(linePhoneNumber.getOrNull(0)?.removePrefix("📞 ") ?: "")
        emailInput.setText(lineEmailInput)
        facebookInput.setText(lineFacebookInput)
        instagramInput.setText(lineInstagramInput)

        val selectedTypes = trainer.classTypes.toMutableList()
        val selectedLevels = trainer.groupLevels.toMutableList()

        val types = mutableListOf<String>()
        val levels = mutableListOf<String>()

        classTypesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                types.addAll(snapshot.children.mapNotNull { it.getValue(String::class.java) })
                val selectedItemsTypes = types.map { it in selectedTypes }.toBooleanArray()

                classTypesText.text = selectedTypes.joinToString(", ")

                classTypesText.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Wybierz typy zajęć")
                        .setMultiChoiceItems(types.toTypedArray(), selectedItemsTypes) { _, which, isChecked ->
                            if (isChecked) selectedTypes.add(types[which]) else selectedTypes.remove(types[which])
                        }
                        .setPositiveButton("OK") { _, _ ->
                            classTypesText.text = selectedTypes.joinToString(", ")
                        }
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        groupLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                levels.addAll(snapshot.children.mapNotNull { it.getValue(String::class.java) })
                val selectedItemsLevels = levels.map { it in selectedLevels }.toBooleanArray()

                groupLevelsText.text = selectedLevels.joinToString(", ")

                groupLevelsText.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Wybierz poziomy grup")
                        .setMultiChoiceItems(levels.toTypedArray(), selectedItemsLevels) { _, which, isChecked ->
                            if (isChecked) selectedLevels.add(levels[which]) else selectedLevels.remove(levels[which])
                        }
                        .setPositiveButton("OK") { _, _ ->
                            groupLevelsText.text = selectedLevels.joinToString(", ")
                        }
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Edytuj dane trenera")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val key = "${trainer.email}".replace(".",",")
                val newName = nameInput.text.toString()
                val newSurname = surnameInput.text.toString()
                val newKey = "${trainer.email}".replace(".",",")

                val updatedData = mapOf(
                    "name" to newName,
                    "surname" to newSurname,
                    "phoneNumber" to phoneInput.text.toString(),
                    "email" to emailInput.text.toString(),
                    "facebook" to facebookInput.text.toString(),
                    "instagram" to instagramInput.text.toString(),
                    "classTypes" to selectedTypes,
                    "groupLevels" to selectedLevels
                )

                // Usuń starego i zapisz pod nowym kluczem (jeśli klucz się zmienił)
                if (key != newKey) {
                    trainersRef.child(key).removeValue()
                }
                trainersRef.child(newKey).setValue(updatedData)

                Toast.makeText(requireContext(), "Dane zaktualizowane!", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }




    @SuppressLint("SetTextI18n")
    private fun setupTrainerRecycler(trainers: List<Trainer>) {
        val addHoursButton = requireView().findViewById<Button>(R.id.addHoursButton)
        val editTrainerButton = requireView().findViewById<Button>(R.id.editTrainerButton)

        // Domyślnie ukryj przyciski
        addHoursButton.visibility = View.GONE
        editTrainerButton.visibility = View.GONE

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = FirebaseDatabase.getInstance().getReference("UsersPersonalization/$uid")

            userRef.child("role").get().addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.getValue(String::class.java) == "admin"

                trainersAdapter = TrainersAdapter(trainers) { trainer: Trainer ->
                    selectedTrainer = trainer
// Ukryj tekst kontaktowy, pokaż pełny kontener z ikonami itp.
                    contactTextView.visibility = View.GONE
                    contactContainer.visibility = View.VISIBLE

                    requireView().findViewById<TextView>(R.id.contactPhone).text = trainer.phoneNumber
                    requireView().findViewById<TextView>(R.id.contactEmail).text = trainer.email
                    requireView().findViewById<TextView>(R.id.contactFb).text = trainer.facebook
                    requireView().findViewById<TextView>(R.id.contactInsta).text = trainer.instagram

                    updateAvailableHours()

                    if (isAdmin) {
                        addHoursButton.apply {
                            visibility = View.VISIBLE
                            setOnClickListener { showAddHoursDialog(trainer) }
                        }

                        editTrainerButton.apply {
                            visibility = View.VISIBLE
                            setOnClickListener { showEditTrainerDialog(trainer) }
                        }
                    } else {
                        addHoursButton.visibility = View.GONE
                        editTrainerButton.visibility = View.GONE
                    }
                }

                trainersRecyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = trainersAdapter
                }
            }
        } else {
            // Użytkownik niezalogowany – ustaw adapter bez przycisków
            trainersAdapter = TrainersAdapter(trainers) { trainer: Trainer ->
                selectedTrainer = trainer
// Ukryj tekst kontaktowy, pokaż pełny kontener z ikonami itp.
                contactTextView.visibility = View.GONE
                contactContainer.visibility = View.VISIBLE

                requireView().findViewById<TextView>(R.id.contactPhone).text = trainer.phoneNumber
                requireView().findViewById<TextView>(R.id.contactEmail).text = trainer.email
                requireView().findViewById<TextView>(R.id.contactFb).text = trainer.facebook
                requireView().findViewById<TextView>(R.id.contactInsta).text = trainer.instagram

                updateAvailableHours()

                // Przyciski pozostają niewidoczne
                addHoursButton.visibility = View.GONE
                editTrainerButton.visibility = View.GONE
            }

            trainersRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = trainersAdapter
            }
        }
    }


    @SuppressLint("DefaultLocale")
    private fun showAddHoursDialog(trainer: Trainer) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_hours, null)
        val dateButton = dialogView.findViewById<Button>(R.id.buttonPickDate)
        val hourInput = dialogView.findViewById<EditText>(R.id.editTextHour)
        val addHourButton = dialogView.findViewById<Button>(R.id.buttonAddHour)
        val hoursListView = dialogView.findViewById<ListView>(R.id.listViewHours)

        val selectedHours = mutableListOf<String>()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, selectedHours)
        hoursListView.adapter = adapter

        var selectedDate: String? = null

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateButton.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        addHourButton.setOnClickListener {
            val hour = hourInput.text.toString().trim()
            if (hour.matches(Regex("\\d{2}:\\d{2}"))) {
                selectedHours.add(hour)
                adapter.notifyDataSetChanged()
                hourInput.text.clear()
            } else {
                Toast.makeText(requireContext(), "Wprowadź godzinę w formacie HH:MM", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj godziny dostępności")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                if (selectedDate != null && selectedHours.isNotEmpty()) {
                    val trainerKey = "${trainer.email}".replace(".",",")
                    val trainerDateRef = database.getReference("scheduleTrainers").child(trainerKey).child(selectedDate!!)
                    selectedHours.forEachIndexed { index, hour ->
                        trainerDateRef.child(index.toString()).setValue(hour)
                    }
                    Toast.makeText(requireContext(), "Godziny zapisane!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Wybierz datę i dodaj godziny!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateAvailableHours() {
        val hours = selectedTrainer?.availability?.get(selectedDate) ?: emptyList()

        hoursAdapter = HoursAdapter { selected ->
            selectedHour = selected
            reserveButton.isEnabled = true
        }
        hoursAdapter.updateData(hours)


        hoursRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        hoursRecyclerView.adapter = hoursAdapter

        if (selectedTrainer == null) {
            contactTextView.visibility = View.VISIBLE
            contactContainer.visibility = View.GONE
        }


        if (hours.isEmpty()) {
            reserveButton.isEnabled = false
        }
    }


}
