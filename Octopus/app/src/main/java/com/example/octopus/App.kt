package com.example.octopus

import android.app.Application
import android.content.Context
import java.util.Locale

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "pl") ?: "pl"
        setAppLocale(languageCode)
    }

    private fun setAppLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
