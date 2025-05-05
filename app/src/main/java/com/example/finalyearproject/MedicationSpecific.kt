package com.example.finalyearproject

data class MedicationSpecific(
    // data class to represent a specific medication box
    val id: String = "",
    val medicationId: String = "",
    val boxName: String = "",
    val expirationDate: String = "",
    val quantity: Int = 0,
    val batchNumber: String = ""
)