package com.example.octopus

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class ScheduleFragment : Fragment() {

    private lateinit var smallRoomAdapter: ScheduleAdapter
    private lateinit var largeRoomAdapter: ScheduleAdapter
    private var selectedDay: String = "Poniedziałek"

    private val database = FirebaseDatabase.getInstance()
    private val scheduleRef = database.getReference("schedule")
    private val classTypesRef = database.getReference("classTypes")
    private val groupLevelsRef = database.getReference("groupLevels")

    private var classTypes: List<String> = emptyList()
    private var groupLevels: List<String> = emptyList()
    private lateinit var dayViewPager: ViewPager2
    private lateinit var daysOfWeek: List<String>

    private fun openEditDescriptionFragment(item: ScheduleItem) {
        val classType = item.classType
        val groupLevel = item.groupLevel ?: ""

        val action = ScheduleFragmentDirections
            .actionScheduleFragmentToEditDescriptionFragment(classType, groupLevel)
        findNavController().navigate(action)
    }

    private fun showEditClassDialog(item: ScheduleItem, itemId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_schedule, null)
        val timeInput: EditText = dialogView.findViewById(R.id.class_time_input)
        val roomSpinner: Spinner = dialogView.findViewById(R.id.room_spinner)
        val classTypeSpinner: Spinner = dialogView.findViewById(R.id.class_type_spinner)
        val groupLevelSpinner: Spinner = dialogView.findViewById(R.id.group_level_spinner)

        timeInput.setText(item.time)

        roomSpinner.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.rooms_array, android.R.layout.simple_spinner_dropdown_item
        )

        classTypeSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, classTypes
        )
        classTypeSpinner.setSelection(classTypes.indexOf(item.classType))

        groupLevelSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, groupLevels
        )
        item.groupLevel?.let {
            groupLevelSpinner.setSelection(groupLevels.indexOf(it))
        }

        val roomOptions = resources.getStringArray(R.array.rooms_array)
        val oldRoomIndex = roomOptions.indexOfFirst { roomName -> roomName == item.room }
        roomSpinner.setSelection(oldRoomIndex)

        AlertDialog.Builder(requireContext())
            .setTitle("Edytuj zajęcia")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val newTime = timeInput.text.toString().trim()
                val room = roomSpinner.selectedItem.toString()
                val itemId = item.id // 🔥 teraz działa
                val newClassType = classTypeSpinner.selectedItem.toString()
                val newGroupLevel = if (newClassType == "Wolna Mata") null else groupLevelSpinner.selectedItem.toString()

                val updatedItem = ScheduleItem(
                    classType = newClassType,
                    groupLevel = newGroupLevel,
                    room = room,
                    time = newTime,
                    id = itemId, // <-- ważne
                    day = selectedDay
                )
                scheduleRef.child(selectedDay).child(itemId).setValue(updatedItem)
            }
            .setNegativeButton("Usuń") { _, _ ->
                scheduleRef.child(selectedDay).child(itemId).removeValue()

            }
            .setNeutralButton("Anuluj", null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        val smallRoomRecyclerView: RecyclerView = view.findViewById(R.id.small_room_recycler_view)
        val largeRoomRecyclerView: RecyclerView = view.findViewById(R.id.large_room_recycler_view)

        smallRoomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        largeRoomRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        smallRoomAdapter = ScheduleAdapter(emptyMap()) { itemId, item -> showEditDialog(item, itemId) }
        largeRoomAdapter = ScheduleAdapter(emptyMap()) { itemId, item -> showEditDialog(item, itemId) }

        smallRoomRecyclerView.adapter = smallRoomAdapter
        largeRoomRecyclerView.adapter = largeRoomAdapter

        val addButton: FloatingActionButton = view.findViewById(R.id.add_schedule_button)
        addButton.setOnClickListener { showAddDialog() }
        daysOfWeek = resources.getStringArray(R.array.days_of_week).toList()
        dayViewPager = view.findViewById(R.id.day_view_pager)

        val dayAdapter = DayPagerAdapter(daysOfWeek) { day ->
            // callback np. przy kliknięciu w dzień w adapterze (jeśli jest potrzebny)
            selectedDay = day
            updateScheduleList()
        }

        dayViewPager.adapter = dayAdapter

// Ustawienie początkowego dnia tygodnia na aktualny
        val calendar = Calendar.getInstance()
        val todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Poniedziałek = 0
        dayViewPager.setCurrentItem(todayIndex, false)
        selectedDay = daysOfWeek[todayIndex]
        updateScheduleList()  // od razu ładujemy zajęcia dla początkowego dnia

// Dodajemy listener dla ViewPager2
        dayViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectedDay = daysOfWeek[position]
                updateScheduleList()
            }
        })

        loadClassTypesAndGroupLevels()

        return view
    }

    private fun loadClassTypesAndGroupLevels() {
        classTypesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classTypes = snapshot.children.mapNotNull { it.getValue(String::class.java) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        groupLevelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupLevels = snapshot.children.mapNotNull { it.getValue(String::class.java) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_schedule, null)
        val timeInput: EditText = dialogView.findViewById(R.id.class_time_input)
        val roomSpinner: Spinner = dialogView.findViewById(R.id.room_spinner)
        val classTypeSpinner: Spinner = dialogView.findViewById(R.id.class_type_spinner)
        val groupLevelSpinner: Spinner = dialogView.findViewById(R.id.group_level_spinner)

        roomSpinner.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.rooms_array, android.R.layout.simple_spinner_dropdown_item
        )

        classTypeSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, classTypes
        )

        groupLevelSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, groupLevels
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj zajęcia")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val time = timeInput.text.toString().trim()
                val room = roomSpinner.selectedItem.toString()
                val classType = classTypeSpinner.selectedItem.toString()
                val groupLevel = if (classType == "Wolna Mata") null else groupLevelSpinner.selectedItem.toString()

                if (time.isEmpty()) {
                    Toast.makeText(requireContext(), "Wprowadź godziny zajęć!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newId = scheduleRef.child(selectedDay).push().key!!
                val scheduleItem = ScheduleItem(
                    classType = classType,
                    groupLevel = groupLevel,
                    room = room,
                    time = time,
                    id = newId,
                    day = selectedDay
                )

                scheduleRef.child(selectedDay).child(newId).setValue(scheduleItem)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showEditDialog(item: ScheduleItem, itemId: String) {
        val options = arrayOf("Edytuj zajęcia", "Edytuj opis zajęć")
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        val userRef =
            uid?.let { FirebaseDatabase.getInstance().getReference("UsersPersonalization").child(it) }
        if (userRef != null) {
            userRef.get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.child("role").getValue(String::class.java)
                    if(role == "admin") {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Wybierz akcję")
                            .setItems(options) { _, which ->
                                when (which) {
                                    0 -> showEditClassDialog(item, itemId)
                                    1 -> openEditDescriptionFragment(item)
                                }
                            }
                            .show()
                    }
                    else
                    {
                        openEditDescriptionFragment(item)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Błąd scheduleFragment!", Toast.LENGTH_SHORT).show()
                }
        }
        else{
            openEditDescriptionFragment(item)
        }


    }

    private fun updateScheduleList() {
        scheduleRef.child(selectedDay).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val smallRoomList = mutableListOf<Pair<String, ScheduleItem>>()
                val largeRoomList = mutableListOf<Pair<String, ScheduleItem>>()

                for (itemSnapshot in snapshot.children) {
                    val item = try {
                        itemSnapshot.getValue(ScheduleItem::class.java)
                    } catch (e: Exception) {
                        Log.e("FIREBASE_PARSE", "Błąd parsowania: ${itemSnapshot.key} → ${itemSnapshot.value}", e)
                        null
                    }

                    val itemId = itemSnapshot.key ?: continue
                    if (item != null) {
                        item.id = itemId
                        when (item.room) {
                            "Mała Sala" -> smallRoomList.add(item.id to item.copy())
                            "Duża Sala" -> largeRoomList.add(item.id to item.copy())
                        }
                    }
                }


                val sortedSmallRoomMap = smallRoomList
                    .sortedBy { parseStartMinutes(it.second.time ?: "") }
                    .toMap(LinkedHashMap())

                val sortedLargeRoomMap = largeRoomList
                    .sortedBy { parseStartMinutes(it.second.time ?: "") }
                    .toMap(LinkedHashMap())

                smallRoomAdapter.updateList(sortedSmallRoomMap)
                largeRoomAdapter.updateList(sortedLargeRoomMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun parseStartMinutes(timeRange: String): Int {
        return try {
            val start = timeRange.split("-")[0].trim()
            val parts = start.split(":")
            if (parts.size != 2) return Int.MAX_VALUE
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour == null || minute == null) return Int.MAX_VALUE
            hour * 60 + minute
        } catch (e: Exception) {
            Log.e("PARSE_EXCEPTION", "Błąd parsowania czasu: $timeRange", e)
            Int.MAX_VALUE
        }
    }


}
