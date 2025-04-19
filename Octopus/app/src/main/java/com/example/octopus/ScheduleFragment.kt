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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

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

    private fun openEditDescriptionFragment(item: ScheduleItem, itemId: String) {
        val action = ScheduleFragmentDirections
            .actionScheduleFragmentToEditDescriptionFragment(selectedDay, itemId)
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
                    id = itemId // <-- ważne
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

        val daySpinner: Spinner = view.findViewById(R.id.day_spinner)
        val dayAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.days_of_week, android.R.layout.simple_spinner_dropdown_item
        )
        daySpinner.adapter = dayAdapter
        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDay = parent?.getItemAtPosition(position).toString()
                updateScheduleList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
                    id = newId
                )

                scheduleRef.child(selectedDay).child(newId).setValue(scheduleItem)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showEditDialog(item: ScheduleItem, itemId: String) {
        val options = arrayOf("Edytuj zajęcia", "Edytuj opis zajęć")
        AlertDialog.Builder(requireContext())
            .setTitle("Wybierz akcję")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditClassDialog(item, itemId)
                    1 -> openEditDescriptionFragment(item, itemId)
                }
            }
            .show()
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

    fun formatTimeInput(input: String): String {
        return input.split("-").joinToString("-") { part ->
            val timeParts = part.trim().split(":")
            if (timeParts.size == 2) {
                val hour = timeParts[0].padStart(2, '0')
                val minute = timeParts[1].padStart(2, '0')
                "$hour:$minute"
            } else {
                part.trim()
            }
        }
    }
}
