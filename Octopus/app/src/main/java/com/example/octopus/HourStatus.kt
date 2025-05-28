package com.example.octopus
data class HourStatus(
    val time: String,
    val status: Status,
    val reservationId: String? = null // tylko dla zarezerwowanych lub oczekujących
) {
    enum class Status {
        FREE,
        CONFIRMED,
        PENDING
    }
}