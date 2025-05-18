package com.example.octopus

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart

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

        setupSpinner(roomSpinner, listOf("Mała sala", "Duża sala", "Obie sale"))

        val trainings = resources.getStringArray(R.array.class_types_array).toList()
        val levels = listOf("Początkujący", "Średni", "Zaawansowany", "Wszystkie")

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
                    // TODO: Wywołaj loadParticipantStats(dateInput.text, roomSpinner.selectedItem, ...)
                }
                "Użytkownicy aplikacji" -> {
                    // TODO: Wywołaj loadUserStats(dateInput.text)
                }
                "Rezerwacje" -> {
                    // TODO: Wywołaj loadOrderStats(dateInput.text, itemNameInput.text)
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
}

