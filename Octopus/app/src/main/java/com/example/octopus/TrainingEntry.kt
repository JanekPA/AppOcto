package com.example.octopus

data class TrainingEntry(
    val id: String,
    val time: String,
    val classType: String,
    val groupLevel: String,
    var trainer: String = "",
    var paid: Boolean = false,
    var isSaved: Boolean = false
)