package com.example.octopus

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {

    private lateinit var statTypeSpinner: Spinner
    private lateinit var dateInput: EditText
    private lateinit var roomSpinner: Spinner
    private lateinit var trainingMulti: MultiAutoCompleteTextView
    private lateinit var levelMulti: MultiAutoCompleteTextView
    private lateinit var itemNameInput: EditText
    private lateinit var participantsFilters: LinearLayout
    private lateinit var reservationsFilters: LinearLayout
    private lateinit var loadStatsButton: Button
    private lateinit var barChart: BarChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statTypeSpinner = view.findViewById(R.id.stat_type_spinner)
        dateInput = view.findViewById(R.id.date_input)
        roomSpinner = view.findViewById(R.id.room_spinner)
        trainingMulti = view.findViewById(R.id.training_multi)
        levelMulti = view.findViewById(R.id.level_multi)
        itemNameInput = view.findViewById(R.id.item_name_input)
        participantsFilters = view.findViewById(R.id.participants_filters)
        reservationsFilters = view.findViewById(R.id.reservations_filters)
        loadStatsButton = view.findViewById(R.id.load_stats_button)
        barChart = view.findViewById(R.id.bar_chart)

        setupSpinner(roomSpinner, resources.getStringArray(R.array.rooms_array).toList())

        val trainings = resources.getStringArray(R.array.class_types_array).toList() + "Wszystkie"
        val levels = resources.getStringArray(R.array.group_levels_array).toList() + "Wszystkie"

        setupMultiAutoComplete(trainingMulti, trainings)
        setupMultiAutoComplete(levelMulti, levels)

        statTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateFiltersVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadStatsButton.setOnClickListener {
            when (statTypeSpinner.selectedItem.toString()) {
                "Uczestnicy" -> {
                    val dateRange = parseDateRange(dateInput.text.toString())
                    if (dateRange.isEmpty()) {
                        Toast.makeText(requireContext(), "Nieprawidłowy format daty", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val localDates = dateRange.map { date ->
                        LocalDate.of(
                            date.year + 1900,
                            date.month + 1,
                            date.date
                        )
                    }

                    val startDate = localDates.first()
                    val endDate = localDates.last()

                    val selectedRooms = listOf(roomSpinner.selectedItem.toString())
                    val selectedTrainings = trainingMulti.text.toString()
                        .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val selectedLevels = levelMulti.text.toString()
                        .split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    loadParticipantStats(
                        startDate,
                        endDate,
                        selectedRooms,
                        selectedTrainings,
                        selectedLevels
                    ) { data ->
                        println("STATISTICS: $data") // <- sprawdzisz w logach czy coś zwraca
                        updateBarChart(data)
                    }

                }

                "Użytkownicy aplikacji" -> {
                    // TODO: implementacja
                }

                "Rezerwacje" -> {
                    // TODO: implementacja
                }
            }
        }

    }

    private fun updateFiltersVisibility() {
        val selected = statTypeSpinner.selectedItem.toString()
        participantsFilters.visibility = if (selected == "Uczestnicy") View.VISIBLE else View.GONE
        reservationsFilters.visibility = if (selected == "Rezerwacje") View.VISIBLE else View.GONE
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupMultiAutoComplete(view: MultiAutoCompleteTextView, items: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)
        view.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
    }
    fun loadParticipantStats(
        startDate: LocalDate,
        endDate: LocalDate,
        selectedRooms: List<String>,
        selectedTrainings: List<String>,
        selectedLevels: List<String>,
        callback: (Map<String, Map<String, Int>>) -> Unit
    ) {
        val stats = mutableMapOf<String, MutableMap<String, Int>>()
        val db = FirebaseDatabase.getInstance().reference

        val dateRange =  getLocalDateRange(startDate, endDate)
        var processedDates = 0

        for (date in dateRange) {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val statRef = db.child("ScheduleStatistics").child(dateStr)

            statRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dayOfWeek = snapshot.child("dayOfWeek").getValue(String::class.java) ?: return
                    val trainingsSnapshot = snapshot.child("trainings")

                    val scheduleRef = db.child("schedule").child(dayOfWeek)
                    scheduleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(scheduleSnap: DataSnapshot) {
                            for (timeSnap in trainingsSnapshot.children) {
                                val time = timeSnap.key ?: continue
                                val classType = timeSnap.child("classType").getValue(String::class.java) ?: continue
                                val groupLevel = timeSnap.child("groupLevel").getValue(String::class.java) ?: continue
                                val count = timeSnap.child("participantsCount").getValue(Int::class.java) ?: 0

                                val trainingMatch = scheduleSnap.children.firstOrNull {
                                    it.child("classType").getValue(String::class.java) == classType &&
                                            it.child("groupLevel").getValue(String::class.java) == groupLevel &&
                                            it.child("time").getValue(String::class.java)?.replace(":", "-") == time

                                }

                                val room = trainingMatch?.child("room")?.getValue(String::class.java) ?: "Nieznana sala"

                                // Filtry
                                if ((selectedRooms.contains("Wszystkie") || selectedRooms.contains(room)) &&
                                    (selectedTrainings.contains("Wszystkie") || selectedTrainings.contains(classType)) &&
                                    (selectedLevels.contains("Wszystkie") || selectedLevels.contains(groupLevel))
                                ) {
                                    stats.getOrPut(dateStr) { mutableMapOf() }.merge("$classType\n$groupLevel", count, Int::plus)
                                }
                            }

                            processedDates++
                            if (processedDates == dateRange.size) {
                                callback(stats)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
    private fun getLocalDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    private fun fetchRoomForTraining(
        dayOfWeek: String,
        time: String,
        classType: String,
        groupLevel: String,
        callback: (String?) -> Unit
    ) {
        val scheduleRef = FirebaseDatabase.getInstance().reference.child("schedule").child(dayOfWeek)

        scheduleRef.get().addOnSuccessListener { daySnapshot ->
            for (trainingIdSnapshot in daySnapshot.children) {
                val trainingSnapshot = trainingIdSnapshot.child(time)
                val gLevel = trainingSnapshot.child("groupLevel").getValue(String::class.java)
                val cType = trainingSnapshot.child("classType").getValue(String::class.java)
                if (gLevel == groupLevel && cType == classType) {
                    val room = trainingSnapshot.child("room").getValue(String::class.java)
                    callback(room)
                    return@addOnSuccessListener
                }
            }
            callback(null)
        }
    }
    private fun parseDateRange(input: String): List<Date> {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return try {
            if (input.contains("-")) {
                val parts = input.split("-").map { it.trim() }
                val start = formatter.parse(parts[0])
                val end = formatter.parse(parts[1])
                if (start != null && end != null) {
                    val dates = mutableListOf<Date>()
                    var current = Calendar.getInstance().apply { time = start }
                    val endCal = Calendar.getInstance().apply { time = end }
                    while (!current.after(endCal)) {
                        dates.add(current.time)
                        current.add(Calendar.DATE, 1)
                    }
                    dates
                } else emptyList()
            } else {
                listOfNotNull(formatter.parse(input))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    private fun updateBarChart(data: Map<String, Map<String, Int>>) {
        val allTypes = data.values.flatMap { it.keys }.toSet().sorted()
        val dates = data.keys.sorted()
        val entriesMap = mutableMapOf<String, MutableList<BarEntry>>()

        dates.forEachIndexed { dateIndex, date ->
            val typeCounts = data[date] ?: return@forEachIndexed
            typeCounts.forEach { (typeKey, count) ->
                // Tylko jeśli count > 0, tworzymy wpisy
                if (count > 0) {
                    entriesMap.getOrPut(typeKey) { MutableList(dates.size) { BarEntry(it.toFloat(), 0f) } }
                    entriesMap[typeKey]!![dateIndex] = BarEntry(dateIndex.toFloat(), count.toFloat())
                }
            }
        }

        // Tworzenie DataSetów
        val filteredEntriesMap = entriesMap.filterValues { list -> list.any { it.y > 0f } }

// Tworzenie datasetów tylko dla typów z danymi
        val dataSets = filteredEntriesMap.map { (typeKey, entries) ->
            BarDataSet(entries, typeKey).apply {
                color = getColorForType(typeKey)
                valueTextColor = Color.WHITE
                valueTextSize = 10f
                setDrawValues(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?): String {
                        return barEntry?.y?.toInt().takeIf { it != null && it > 0 }?.toString() ?: ""
                    }
                }
            }
        }

        val barData = BarData(dataSets)

        // Parametry szerokości słupków i przestrzeni
        val isGrouped = dataSets.size > 1
        val groupSpace = if (isGrouped) 0.2f else 0f
        val barSpace = if (isGrouped) 0.02f else 0f
        val barWidth = if (isGrouped) (1f - groupSpace) / dataSets.size - barSpace else 0.3f
        barData.barWidth = barWidth

        barChart.data = barData
        barChart.setBackgroundColor(Color.BLACK)
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.description.isEnabled = false
        barChart.setFitBars(!isGrouped)
        barChart.setExtraOffsets(10f, 10f, 10f, 20f)

        // X Axis
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.LTGRAY
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 0f
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = dates.size.toFloat()
        xAxis.labelCount = dates.size
        xAxis.axisLineColor = Color.LTGRAY

        // Y Axis
        val yAxisLeft = barChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.textColor = Color.LTGRAY
        yAxisLeft.gridColor = Color.DKGRAY
        yAxisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false

        // Legenda
        barChart.legend.textColor = Color.WHITE
        barChart.legend.isWordWrapEnabled = true

        // Grupowanie tylko jeśli więcej niż jeden typ
        if (isGrouped) {
            barChart.groupBars(0f, groupSpace, barSpace)
        }

        barChart.invalidate()
    }


    private fun getColorForType(type: String): Int {
        return when {
            type.contains("Boks", ignoreCase = true) -> Color.parseColor("#e74c3c")     // czerwony
            type.contains("BJJ", ignoreCase = true) -> Color.parseColor("#3498db")     // niebieski
            type.contains("Kickboxing", ignoreCase = true) -> Color.parseColor("#2ecc71") // zielony
            type.contains("MMA", ignoreCase = true) -> Color.parseColor("#9b59b6")     // fiolet
            type.contains("No-Gi", ignoreCase = true) -> Color.parseColor("#f1c40f")    // żółty
            else -> Color.GRAY
        }
    }




}

