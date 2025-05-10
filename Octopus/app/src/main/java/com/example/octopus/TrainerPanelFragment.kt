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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TrainerPanelFragment : Fragment() {
    private var dataTrainer: Trainer? = null
    private lateinit var profileImage: ImageView
    private lateinit var personalHoursList: RecyclerView
    private lateinit var addHourButton: Button
    private lateinit var nameText: TextView
    private lateinit var contactText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var editDataButton: Button
    private lateinit var editDescriptionButton: Button
    private lateinit var classTypesRef: DatabaseReference
    private lateinit var groupLevelsRef: DatabaseReference
    private lateinit var pickDateButton: Button
    private lateinit var scheduleRef: DatabaseReference
    private lateinit var trainersRef: DatabaseReference
    private var selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE) // domyślnie dziś
    private var selectedHour: String? = null
    private lateinit var hoursAdapter: HoursAdapter
    private lateinit var trainer: Trainer
    private lateinit var database: FirebaseDatabase
    private val auth = FirebaseAuth.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trainer_panel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance()
        profileImage = view.findViewById(R.id.trainer_profile_image)
        personalHoursList = view.findViewById(R.id.trainer_hours_list)
        addHourButton = view.findViewById(R.id.button_add_hour)
        nameText = view.findViewById(R.id.text_name)
        classTypesRef = database.getReference("classTypes")
        groupLevelsRef = database.getReference("groupLevelsTrainers")
        contactText = view.findViewById(R.id.text_contact)
        descriptionText = view.findViewById(R.id.text_description)
        editDataButton = view.findViewById(R.id.editDataButton)
        editDescriptionButton = view.findViewById(R.id.editDescriptionButton)
        pickDateButton = view.findViewById(R.id.buttonPickDate)
        scheduleRef = database.getReference("scheduleTrainers")
        personalHoursList.layoutManager = LinearLayoutManager(context)
        val userEmail = auth.currentUser?.email ?: return
        trainersRef = database.getReference("TrainersData")
        trainersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (trainerSnapshot in snapshot.children) {
                    val trainer = trainerSnapshot.getValue(Trainer::class.java)
                    if (trainer?.email == userEmail) {

                        this@TrainerPanelFragment.trainer = trainer
                        this@TrainerPanelFragment.dataTrainer = trainer
                        updateTrainerUI()
                        loadAvailableHoursForDate(trainer, selectedDate)
                        return
                    }
                }
                Toast.makeText(context, "Nie znaleziono trenera dla adresu: $userEmail", Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Błąd bazy danych", Toast.LENGTH_SHORT).show()
            }
        })

        pickDateButton.setOnClickListener {
            showDatePickerDialog()
        }
        editDataButton.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentEmail = currentUser?.email ?: return@setOnClickListener

            val trainersRef = FirebaseDatabase.getInstance().getReference("TrainersData")

            trainersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var existingTrainer: Trainer? = null
                    var existingKey: String? = null
                    for (trainerSnap in snapshot.children) {
                        val trainer = trainerSnap.getValue(Trainer::class.java)
                        if (trainer?.email == currentEmail) {
                            existingTrainer = trainer
                            existingKey = trainerSnap.key
                            break
                        }
                    }

                    if (existingTrainer != null && existingKey != null) {
                        showEditTrainerDialog(existingTrainer)
                        dataTrainer = existingTrainer
                    } else {
                        val baseName = "Nowy Trener"
                        var newKey = baseName
                        var counter = 2
                        while (snapshot.hasChild(newKey)) {
                            newKey = "$baseName$counter"
                            counter++
                        }

                        val newTrainer = Trainer(
                            name = "Nowy",
                            surname = "Trener",
                            email = currentEmail,
                            phoneNumber = "",
                            facebook = "",
                            instagram = "",
                            availability = mapOf(),
                            classTypes = listOf(),
                            groupLevels = listOf(),
                            description = ""
                        )
                        dataTrainer = newTrainer
                        trainersRef.child(newKey).setValue(newTrainer).addOnSuccessListener {
                            showEditTrainerDialog(newTrainer)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Błąd bazy danych", Toast.LENGTH_SHORT).show()
                }
            })
        }



        editDescriptionButton.setOnClickListener {
            if (::trainer.isInitialized) {
                showEditDialog("Opis", descriptionText.text.toString()) { newDesc ->
                    trainer.description = newDesc
                    descriptionText.text = newDesc
                    trainersRef.child(trainer.name + " " + trainer.surname).child("description").setValue(newDesc)
                }
            } else {
                Toast.makeText(context, "Dane trenera nie zostały jeszcze załadowane", Toast.LENGTH_SHORT).show()
            }
        }

        addHourButton.setOnClickListener {
                dataTrainer?.let { it1 -> showAddHoursDialog(it1) }
        }
    }
    private fun updateTrainerUI() {
        nameText.text = "${trainer.name} ${trainer.surname}"
        contactText.text = "📞 ${trainer.phoneNumber}\n✉️ ${trainer.email}\n📘 ${trainer.facebook}\n📸 ${trainer.instagram}"
        descriptionText.text = trainer.description ?: "Brak opisu"
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
                dataTrainer?.let { trainer ->
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
                }
                personalHoursList.layoutManager = LinearLayoutManager(requireContext())
                personalHoursList.adapter = hoursAdapter
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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



    private fun showEditDialog(title: String, current: String, onSave: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(current)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("Zapisz") { _, _ -> onSave(editText.text.toString()) }
            .setNegativeButton("Anuluj", null)
            .show()
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
        phoneInput.setText(trainer.phoneNumber)
        emailInput.setText(trainer.email)
        facebookInput.setText(trainer.facebook)
        instagramInput.setText(trainer.instagram)
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
                    "description" to trainer.description,
                    "classTypes" to selectedTypes,
                    "groupLevels" to selectedLevels
                )

                // Usuń starego i zapisz pod nowym kluczem (jeśli klucz się zmienił)
                if (key != newKey) {
                    trainersRef.child(key).removeValue()
                }
                trainersRef.child(newKey).setValue(updatedData)

                Toast.makeText(requireContext(), "Dane zaktualizowane!", Toast.LENGTH_SHORT).show()
                updateTrainerUI()
            }
            .setNegativeButton("Anuluj", null)
            .show()

    }

    private fun getTodayDate(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(calendar.time)
    }
}

