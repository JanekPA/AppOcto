package com.example.octopus

data class Item(
    val id: String = "",
    val name: String = "",
    val color: String = "",
    val type: String = "",
    val size: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val category: String = ""
)
