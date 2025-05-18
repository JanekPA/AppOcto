package com.example.octopus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarForAdminFragment : Fragment() {

    private lateinit var dateText: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteEditText: EditText
    private lateinit var saveNoteButton: Button

    private lateinit var adapter: TrainingEntryAdapter
    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var isSummaryView = true
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar_for_admin, container, false)

        dateText = view.findViewById(R.id.date_text)
        prevButton = view.findViewById(R.id.prev_day_button)
        nextButton = view.findViewById(R.id.next_day_button)
        recyclerView = view.findViewById(R.id.training_list_recycler)
        noteEditText = view.findViewById(R.id.day_note_edit_text)
        saveNoteButton = view.findViewById(R.id.save_note_button)
        val toggleSummaryButton: Button = view.findViewById(R.id.toggle_summary_button)
        adapter = TrainingEntryAdapter()
        adapter.setSummaryMode(true)
        toggleSummaryButton.text = "Edytuj dzień"

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        prevButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            refreshView()
        }

        nextButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            refreshView()
        }

        saveNoteButton.setOnClickListener {
            val note = noteEditText.text.toString()
            val dateStr = dateFormat.format(currentDate.time)
            FirebaseDatabase.getInstance().getReference("ScheduleStatistics")
                .child(dateStr)
                .child("notes")
                .setValue(note)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Notatka zapisana", Toast.LENGTH_SHORT).show()
                }
        }


        toggleSummaryButton.setOnClickListener {
            isSummaryView = !isSummaryView
            adapter.setSummaryMode(isSummaryView)
            toggleSummaryButton.text = if (isSummaryView) "Edytuj dzień" else "Pokaż podsumowanie dnia"
        }
        refreshView()
        return view
    }

    private fun refreshView() {
        val dateStr = dateFormat.format(currentDate.time)
        val dayOfWeek = getDayOfWeek(currentDate)

        dateText.text = "${dayOfWeek.capitalize()} ${dateStr}"

        loadNoteForDate(dateStr)
        loadTrainingsForDate(dayOfWeek, dateStr)
    }

    private fun getDayOfWeek(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Poniedziałek"
            Calendar.TUESDAY -> "Wtorek"
            Calendar.WEDNESDAY -> "Środa"
            Calendar.THURSDAY -> "Czwartek"
            Calendar.FRIDAY -> "Piątek"
            Calendar.SATURDAY -> "Sobota"
            Calendar.SUNDAY -> "Niedziela"
            else -> "Nieznany"
        }
    }

    private fun loadNoteForDate(dateStr: String) {
        val noteRef = FirebaseDatabase.getInstance()
            .getReference("ScheduleStatistics")
            .child(dateStr)
            .child("notes")

        noteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noteEditText.setText(snapshot.getValue(String::class.java) ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania notatki: ${error.message}")
            }
        })
    }

    private fun loadTrainingsForDate(dayOfWeek: String, dateStr: String) {
        val scheduleRef = FirebaseDatabase.getInstance()
            .getReference("schedule")
            .child(dayOfWeek)

        scheduleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(scheduleSnapshot: DataSnapshot) {
                val trainings = mutableListOf<TrainingEntry>()

                for (child in scheduleSnapshot.children) {
                    val classType = child.child("classType").getValue(String::class.java) ?: ""
                    val groupLevel = child.child("groupLevel").getValue(String::class.java) ?: ""
                    val time = child.child("time").getValue(String::class.java) ?: ""
                    val id = time.replace(":", "-")

                    // Domyślne wartości (brak trenera i wypłaty)
                    trainings.add(
                        TrainingEntry(
                            id = id,
                            time = time,
                            classType = classType,
                            groupLevel = groupLevel,
                            trainer = "", // domyślnie pusty
                            paid = false  // domyślnie nieopłacony
                        )
                    )
                }

                val statRef = FirebaseDatabase.getInstance()
                    .getReference("ScheduleStatistics")
                    .child(dateStr)
                    .child("trainings")

                statRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(statSnapshot: DataSnapshot) {
                        // Zastosuj statystyki, jeśli istnieją
                        val updatedTrainings = trainings.map { training ->
                            val statData = statSnapshot.child(training.id)
                            training.copy(
                                trainer = statData.child("trainer").getValue(String::class.java) ?: "",
                                paid = statData.child("paymentReceived").getValue(Boolean::class.java) ?: false,
                                participantsCount = statData.child("participantsCount").getValue(Int::class.java) ?: 0
                            )
                        }

                        adapter.submitList(updatedTrainings, dateStr)
                        saveDayOfWeekInfo(dateStr, dayOfWeek)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Błąd pobierania statystyk: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania treningów: ${error.message}")
            }
        })
    }



    private fun saveDayOfWeekInfo(dateStr: String, dayOfWeek: String) {
        FirebaseDatabase.getInstance().getReference("ScheduleStatistics")
            .child(dateStr)
            .child("dayOfWeek")
            .setValue(dayOfWeek)
    }
}