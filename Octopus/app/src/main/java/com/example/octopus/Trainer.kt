package com.example.octopus

data class Trainer(
    var name: String ?= null,
    var surname: String ?= null,
    var email: String?= null,
    var facebook: String?= null,
    var phoneNumber: String?= null,
    var instagram: String?= null,
    val availability: Map<String, List<String>> = emptyMap(),
    var classTypes: List<String> = emptyList(),
    val groupLevels: List<String> = emptyList(),
    var description: String ?= null

)
