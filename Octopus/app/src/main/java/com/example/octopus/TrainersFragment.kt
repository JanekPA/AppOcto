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
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TrainersFragment : Fragment() {

    private lateinit var trainingTypeSpinner: Spinner
    private lateinit var groupLevelSpinner: Spinner
    private lateinit var trainersRecyclerView: RecyclerView
    private lateinit var hoursRecyclerView: RecyclerView
    private lateinit var reserveButton: Button
    private lateinit var contactTextView: TextView
    private lateinit var pickDateButton: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var classTypesRef: DatabaseReference
    private lateinit var groupLevelsRef: DatabaseReference
    private lateinit var trainersRef: DatabaseReference
    private lateinit var scheduleRef: DatabaseReference

    private var selectedTrainer: Trainer? = null
    private var selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE) // domyślnie dziś
    private var selectedHour: String? = null

    private lateinit var trainersAdapter: TrainersAdapter
    private lateinit var hoursAdapter: HoursAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_trainers, container, false)

        val addTrainerButton: Button = view.findViewById(R.id.buttonAddTrainer)
        val editTrainerButton: Button = view.findViewById(R.id.editTrainerButton)
        editTrainerButton.visibility = View.GONE

        trainingTypeSpinner = view.findViewById(R.id.spinnerTrainingType)
        groupLevelSpinner = view.findViewById(R.id.spinnerGroupLevel)
        trainersRecyclerView = view.findViewById(R.id.recyclerViewTrainers)
        hoursRecyclerView = view.findViewById(R.id.recyclerViewAvailableHours)
        reserveButton = view.findViewById(R.id.buttonReserve)
        contactTextView = view.findViewById(R.id.textViewContact)
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

        reserveButton.setOnClickListener {
            Toast.makeText(requireContext(), "Rezerwacja w przygotowaniu", Toast.LENGTH_SHORT).show()
        }

        pickDateButton.setOnClickListener {
            showDatePickerDialog()
        }

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
                val key = "$name $surname"

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
        val trainerKey = "${trainer.name} ${trainer.surname}"

        scheduleRef.child(trainerKey).child(date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hours = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                hoursAdapter = HoursAdapter(hours) { hour ->
                    selectedHour = hour
                    reserveButton.isEnabled = true
                }
                hoursRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                hoursRecyclerView.adapter = hoursAdapter
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
        contactTextView.text = "Wybierz trenera, aby uzyskać kontakt!"
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
                    val contact = "📞 $phone\n✉️ $email\n📘 $facebook\n📸 $instagram"

                    val availabilityRef = database.getReference("scheduleTrainers").child(child.key!!)
                    availabilityRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(avSnapshot: DataSnapshot) {
                            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                            val availability = mutableMapOf<String, List<String>>()

                            for (dateSnapshot in avSnapshot.children) {
                                val date = dateSnapshot.key ?: continue
                                val hours = dateSnapshot.children.mapNotNull { it.getValue(String::class.java) }
                                    .distinct()
                                    .sortedBy {
                                        try {
                                            LocalTime.parse(it, formatter)
                                        } catch (e: Exception) {
                                            LocalTime.MIDNIGHT
                                        }
                                    }
                                availability[date] = hours
                            }


                            filteredList.add(Trainer(name, surname, contact, availability, types, levels))
                            loadedCount++
                            if (loadedCount == filteredChildren.size) {
                                setupTrainerRecycler(filteredList)
                                val stillExists = filteredList.any {
                                    it.name == selectedTrainer?.name && it.surname == selectedTrainer?.surname
                                }
                                if (!stillExists) {
                                    selectedTrainer = null
                                    selectedHour = null
                                    contactTextView.text = "Wybierz trenera, aby uzyskać kontakt!"
                                    hoursAdapter = HoursAdapter(emptyList()) { }
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

        val lines = trainer.contact.split("\n")
        phoneInput.setText(lines.getOrNull(0)?.removePrefix("📞 ") ?: "")
        emailInput.setText(lines.getOrNull(1)?.removePrefix("✉️ ") ?: "")
        facebookInput.setText(lines.getOrNull(2)?.removePrefix("📘 ") ?: "")
        instagramInput.setText(lines.getOrNull(3)?.removePrefix("📸 ") ?: "")

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
                val key = "${trainer.name} ${trainer.surname}"
                val newName = nameInput.text.toString()
                val newSurname = surnameInput.text.toString()
                val newKey = "$newName $newSurname"

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

        trainersAdapter = TrainersAdapter(trainers) { trainer ->
            selectedTrainer = trainer
            contactTextView.text = "Kontakt:\n${trainer.contact}"
            updateAvailableHours()

            addHoursButton.apply {
                visibility = View.VISIBLE
                setOnClickListener { showAddHoursDialog(trainer) }
            }

            editTrainerButton.apply {
                visibility = View.VISIBLE
                setOnClickListener { showEditTrainerDialog(trainer) }
            }
        }

        trainersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trainersAdapter
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
                    val trainerKey = "${trainer.name} ${trainer.surname}"
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

        hoursAdapter = HoursAdapter(hours) { selected ->
            selectedHour = selected
            reserveButton.isEnabled = true
        }

        hoursRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        hoursRecyclerView.adapter = hoursAdapter

        if (selectedTrainer == null) {
            contactTextView.text = "Wybierz trenera, aby uzyskać kontakt!"
        }

        if (hours.isEmpty()) {
            reserveButton.isEnabled = false
        }
    }
    data class Trainer(
        val name: String,
        val surname: String,
        val contact: String,
        val availability: Map<String, List<String>>,
        val classTypes: List<String>,
        val groupLevels: List<String>
    )

}
