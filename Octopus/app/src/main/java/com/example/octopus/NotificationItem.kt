package com.example.octopus

data class NotificationItem(
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val reservationId: String = "",  // Dodaj to pole
    val type: String = "general"
)
