package com.example.octopus

data class FaqItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
