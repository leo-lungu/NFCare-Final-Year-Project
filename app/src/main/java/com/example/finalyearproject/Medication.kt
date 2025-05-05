package com.example.finalyearproject

data class Medication(
    // data class to represent a medication
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val description: String = "",
    val allergens: List<String> = emptyList(),
)