package com.example.octopus

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import java.util.Locale
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    private lateinit var themeSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private val languageCodes = arrayOf("pl", "en", "uk")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        themeSpinner = view.findViewById(R.id.spinner_theme)
        // Ustaw adapter
        val themeNames = resources.getStringArray(R.array.themes_array)
        val themeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, themeNames)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter
        languageSpinner = view.findViewById(R.id.spinner_language)
        val languageNames = arrayOf(
            getString(R.string.polish),
            getString(R.string.english),
            getString(R.string.ukrainian)
        )
        val langAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, languageNames)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = langAdapter

        val prefslang = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val selectedLangCode = prefslang.getString("language", "pl") ?: "pl"
        languageSpinner.setSelection(languageCodes.indexOf(selectedLangCode))
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val newLang = languageCodes[position]
                if (newLang != prefslang.getString("language", "pl")) {
                    prefslang.edit { putString("language", newLang) }
                    setLocale(newLang)
                    requireActivity().recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // Wczytaj zapisany wybór
        val prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val selectedTheme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        themeSpinner.setSelection(
            when (selectedTheme) {
                AppCompatDelegate.MODE_NIGHT_NO -> 1
                AppCompatDelegate.MODE_NIGHT_YES -> 2
                else -> 0
            }
        )

        // Obsługa zmiany
        themeSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val newMode = when (position) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                if (newMode != selectedTheme) {
                    AppCompatDelegate.setDefaultNightMode(newMode)
                    prefs.edit().putInt("theme", newMode).apply()
                    requireActivity().recreate()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        return view
    }
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = requireContext().resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
