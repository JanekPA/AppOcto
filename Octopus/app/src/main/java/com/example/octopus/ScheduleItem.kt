package com.example.octopus

import android.widget.TextView

data class ScheduleItem(
    val classType: String = "",
    val groupLevel: String? = null,
    val room: String = "",
    val time: String = "",
    var id: String = "" // <- lokalnie w kodzie, do operacji
)