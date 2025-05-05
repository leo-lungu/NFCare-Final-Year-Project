package com.example.finalyearproject

data class Patient(
    // data class to represent a patient
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val emergencyContact: Map<String, String> = emptyMap(),
    val medicalConditions: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val gender: String = "",
    val bloodType: String = "",
    val doctorId: String = "",
    val medicationScheduleId: String = ""
)

