package com.example.octopus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {

    private val database = FirebaseDatabase.getInstance()
    private val scheduleRef = database.getReference("schedule")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        val welcomeText: TextView = view.findViewById(R.id.tv_welcome)
        welcomeText.text = "Witaj w aplikacji Octopus, Użytkowniku!"

        val recyclerView: RecyclerView = view.findViewById(R.id.upcoming_sessions_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<Button>(R.id.btn_reservation).setOnClickListener {
            findNavController().navigate(R.id.reservationFragment)
        }
        view.findViewById<Button>(R.id.btn_trainers).setOnClickListener {
            findNavController().navigate(R.id.trainersFragment)
        }
        view.findViewById<Button>(R.id.btn_pricing).setOnClickListener {
            findNavController().navigate(R.id.pricingFragment)
        }

        loadUpcomingSessions(recyclerView)

        return view
    }

    private fun loadUpcomingSessions(recyclerView: RecyclerView) {
        val dayOrder = listOf(
            "Poniedziałek", "Wtorek", "Środa",
            "Czwartek", "Piątek", "Sobota", "Niedziela"
        )

        val now = Calendar.getInstance()
        val currentDayName = SimpleDateFormat("EEEE", Locale("pl")).format(now.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val currentDayIndex = dayOrder.indexOf(currentDayName)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val upcomingSessions = mutableListOf<DisplaySession>()

        scheduleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for ((dayIndex, dayName) in dayOrder.withIndex()) {
                    val daySnapshot = snapshot.child(dayName)
                    for (sessionSnapshot in daySnapshot.children) {
                        try {
                            val item = sessionSnapshot.getValue(ScheduleItem::class.java)
                            if (item != null && item.time.isNotBlank()) {
                                val startMinutes = parseStartMinutes(item.time)
                                val shouldInclude =
                                    (dayIndex > currentDayIndex) ||
                                            (dayIndex == currentDayIndex && startMinutes > currentMinutes)

                                if (shouldInclude) {
                                    upcomingSessions.add(
                                        DisplaySession(
                                            day = dayName,
                                            time = item.time,
                                            classType = item.classType,
                                            groupLevel = item.groupLevel,
                                            room = item.room,
                                            id = sessionSnapshot.key ?: "",
                                            dayIndex = dayIndex,
                                            startMinutes = startMinutes
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("MainFragment", "Nieprawidłowy wpis w bazie: ${sessionSnapshot.value}")
                        }
                    }

                }

                val sorted = upcomingSessions
                    .sortedWith(compareBy({ it.dayIndex }, { it.startMinutes }))
                    .take(10)

                recyclerView.adapter = UpcomingSessionsAdapter(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainFragment", "Błąd Firebase: ${error.message}")
                Toast.makeText(requireContext(), "Błąd: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun parseStartMinutes(time: String): Int {
        return try {
            val parts = time.split("-").first().split(":")
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            hour * 60 + minute
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }

    data class DisplaySession(
        val day: String,
        val time: String,
        val classType: String,
        val groupLevel: String?,
        val room: String,
        val id: String,
        val dayIndex: Int,
        val startMinutes: Int
    )
}
