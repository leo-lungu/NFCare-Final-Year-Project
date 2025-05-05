package com.example.finalyearproject

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddPatientActivity : AppCompatActivity() {
    // inputs from the layout file for patient details
    private lateinit var nhsIdInput: EditText
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var bloodTypeSpinner: Spinner
    private lateinit var allergiesInput: EditText
    private lateinit var medicalConditionsInput: EditText
    private lateinit var currentMedicationsInput: EditText
    private lateinit var emergencyContactInput: EditText
    private lateinit var emergencyContactPhoneInput: EditText
    private lateinit var doctorIdInput: EditText
    private lateinit var addPatientButton: Button
    private lateinit var backButton: Button
    private lateinit var allergyInput: AutoCompleteTextView
    private lateinit var addAllergyButton: Button
    private lateinit var allergiesListView: ListView
    private lateinit var medicalConditionInput: AutoCompleteTextView
    private lateinit var addMedicalConditionButton: Button
    private lateinit var medicalConditionsListView: ListView

    // Firebase Firestore instance
    private lateinit var db: FirebaseFirestore

    // lists to store selected medications, allergies, and medical conditions
    private val medicationList = mutableListOf<String>()
    private val selectedMedications = mutableListOf<String>()
    private val selectedAllergies = mutableListOf<String>()
    private val selectedMedicalConditions = mutableListOf<String>()

    // lists to store allergen and medical condition suggestions
    private val allergySuggestions = mutableListOf<String>()
    private val medicalConditionSuggestions = mutableListOf<String>()

    // ArrayAdapters for displaying selected allergies and medical conditions
    private lateinit var allergyAdapter: ArrayAdapter<String>
    private lateinit var medicalConditionAdapter: ArrayAdapter<String>
    private lateinit var allergiesAdapter: ArrayAdapter<String>
    private lateinit var medicalConditionsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_patient)

        if (!isRunningInRobolectric()) {
            // when the app is not running in Robolectric (testing), initalise the Firestore instance
            db = FirebaseFirestore.getInstance()
        }

        // initalise inputs from the layout file
        nhsIdInput = findViewById(R.id.NHSIdInput)
        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        dobInput = findViewById(R.id.dobInput)
        dobInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // format the date to dd/MM/yyyy
                val formattedDate =
                    String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                dobInput.setText(formattedDate)
            }, year, month, day)

            datePicker.datePicker.maxDate = calendar.timeInMillis

            datePicker.show()
        }

        // initalise other inputs
        genderSpinner = findViewById(R.id.genderInput)
        bloodTypeSpinner = findViewById(R.id.bloodTypeInput)
        currentMedicationsInput = findViewById(R.id.currentMedicationsInput)
        emergencyContactInput = findViewById(R.id.emergencyContactInput)
        emergencyContactPhoneInput = findViewById(R.id.emergencyContactPhoneInput)
        doctorIdInput = findViewById(R.id.doctorIdInput)

        // initalise buttons
        addPatientButton = findViewById(R.id.addPatientButton)
        backButton = findViewById(R.id.backButton)

        // initalise allergy inputs and buttons
        allergyInput = findViewById(R.id.allergyInput)
        addAllergyButton = findViewById(R.id.addAllergyButton)
        allergiesListView = findViewById(R.id.allergiesListView)

        // initalise medical condition inputs and buttons
        medicalConditionInput = findViewById(R.id.medicalConditionInput)
        addMedicalConditionButton = findViewById(R.id.addMedicalConditionButton)
        medicalConditionsListView = findViewById(R.id.medicalConditionsListView)

        // gender dropdown
        val genderOptions = arrayOf("Male", "Female", "Other")
        genderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)

        // blood type dropdown
        val bloodTypeOptions =
            arrayOf("Select Blood Type", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
        bloodTypeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypeOptions)

        // date picker for date of birth
        dobInput.setOnClickListener {
            showDatePicker()
        }

        // set up the ArrayAdapter for the allergies and medical conditions list views
        allergiesAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, selectedAllergies)
        allergiesListView.adapter = allergiesAdapter

        // set up the ArrayAdapter for the medical conditions list view
        medicalConditionsAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, selectedMedicalConditions)
        medicalConditionsListView.adapter = medicalConditionsAdapter

        // set up the ArrayAdapter for the allergy and medical condition input fields
        allergyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allergySuggestions)
        allergyInput.setAdapter(allergyAdapter)

        // set up the ArrayAdapter for the medical condition input field
        medicalConditionAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, medicalConditionSuggestions)
        medicalConditionInput.setAdapter(medicalConditionAdapter)

        addPatientButton.setOnClickListener {
            // when the add patient button is clicked, validate the inputs and add the patient to the database
            addPatientToDatabase()
            Log.d("AddPatient", "Add Patient button clicked")
        }

        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric, load the suggestions from Firestore
            loadSuggestions()
        }

        addAllergyButton.setOnClickListener {
            // when the add allergy button is clicked, get the allergy input
            val allergy = allergyInput.text.toString().trim()
            if (allergy.isNotEmpty()) {
                // if the allergy is not empty, add it to the selected allergies list
                if (!selectedAllergies.contains(allergy)) {
                    selectedAllergies.add(allergy)
                    allergiesAdapter.notifyDataSetChanged()

                    if (!allergySuggestions.contains(allergy)) {
                        // if the allergy is not in the suggestions list, add it to Firestore
                        addSuggestionToFirestore("allergens", allergy)
                        allergySuggestions.add(allergy)
                        allergyAdapter.notifyDataSetChanged()
                    }
                }
                allergyInput.text.clear()
            }
        }

        addMedicalConditionButton.setOnClickListener {
            // when the add medical condition button is clicked, get the medical condition input
            val condition = medicalConditionInput.text.toString().trim()
            if (condition.isNotEmpty()) {
                if (!selectedMedicalConditions.contains(condition)) {
                    selectedMedicalConditions.add(condition)
                    medicalConditionsAdapter.notifyDataSetChanged()

                    if (!medicalConditionSuggestions.contains(condition)) {
                        // if the medical condition is not in the suggestions list, add it to Firestore
                        addSuggestionToFirestore("medical_conditions", condition)
                        medicalConditionSuggestions.add(condition)
                        medicalConditionAdapter.notifyDataSetChanged()
                    }
                }
                medicalConditionInput.text.clear()
            }
        }

        backButton.setOnClickListener {
            // when the back button is clicked, finish the activity
            finish()
        }

        val currentMedicationsInput = findViewById<EditText>(R.id.currentMedicationsInput)

        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric, load the medications from Firestore
            db.collection("medications").get().addOnSuccessListener { result ->
                medicationList.clear()
                for (doc in result) {
                    // get the medication name from the document and add it to the medication list
                    doc.getString("name")?.let { medicationList.add(it) }
                }


                currentMedicationsInput.setOnClickListener {
                    // when the current medications input is clicked, show the medication picker dialog
                    showMedicationPicker()
                }
            }
        }
    }

    private fun showDatePicker() {
        // show a date picker dialog to select the date of birth
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // format the selected date to dd/MM/yyyy
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dobInput.setText(dateFormat.format(selectedDate.time))
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun showMedicationPicker() {
        // show a dialog to select medications
        val checkedItems = BooleanArray(medicationList.size) { i ->
            selectedMedications.contains(medicationList[i])
        }

        AlertDialog.Builder(this)
            // set the title and items for the dialog
            .setTitle("Select Medications")
            .setMultiChoiceItems(
                // set the items to be displayed in the dialog
                medicationList.toTypedArray(),
                checkedItems
            ) { _, which, isChecked ->
                val medication = medicationList[which]
                if (isChecked) {
                    if (!selectedMedications.contains(medication)) {
                        selectedMedications.add(medication)
                    }
                } else {
                    selectedMedications.remove(medication)
                }
            }
            .setPositiveButton("Done") { _, _ ->
                // when the done button is clicked, set the selected medications to the input field
                currentMedicationsInput.setText(selectedMedications.joinToString(", "))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadSuggestions() {
        // load allergen and medical condition suggestions from Firestore
        db.collection("lists").document("allergens")
            .get()
            .addOnSuccessListener { document ->
                // get the list of allergens from the document
                val list = document.get("names") as? List<String> ?: emptyList()
                allergySuggestions.clear()
                allergySuggestions.addAll(list)
                allergyAdapter.notifyDataSetChanged() // refresh the adapter
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_ERROR", "Failed to load allergens: ${e.message}")
            }

        db.collection("lists").document("medical_conditions")
            .get()
            .addOnSuccessListener { document ->
                val list = document.get("names") as? List<String> ?: emptyList()
                medicalConditionSuggestions.clear()
                medicalConditionSuggestions.addAll(list)
                medicalConditionAdapter.notifyDataSetChanged() // refresh adapter
            }
            .addOnFailureListener { e ->
                // log the error if loading fails
                Log.e("FIREBASE_ERROR", "Failed to load medical conditions: ${e.message}")
            }
    }


    private fun addSuggestionToFirestore(type: String, value: String) {
        // add a new suggestion to Firestore
        db.collection("lists").document(type)
            // update the document with the new suggestion
            .update("names", FieldValue.arrayUnion(value))
            .addOnSuccessListener {
                Log.d("Suggestion", "$value added to $type list!")
            }
            .addOnFailureListener { e ->
                Log.e("Suggestion", "Failed to add $value to $type: ${e.message}")
            }
    }

    private fun addPatientToDatabase() {
        // validate the inputs and add the patient to the database
        val nhsId = nhsIdInput.text.toString().trim()
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val dobText = dobInput.text.toString().trim()
        val gender = genderSpinner.selectedItem.toString()
        val bloodType = bloodTypeSpinner.selectedItem.toString()
        val allergies = selectedAllergies
        val medicalConditions = selectedMedicalConditions
        val currentMedications = selectedMedications
        val emergencyContact = emergencyContactInput.text.toString().trim()
        val emergencyContactPhone = emergencyContactPhoneInput.text.toString().trim()
        val doctorId = doctorIdInput.text.toString().trim()

        var isValid = true // flag to check if all inputs are valid

        if (!nhsId.matches(Regex("^[0-9]{10}$"))) {
            // if the NHS ID is not exactly 10 digits, show an error
            nhsIdInput.error = "NHS ID must be exactly 10 digits"
            nhsIdInput.requestFocus()
            isValid = false
            Toast.makeText(this, "NHS ID must be exactly 10 digits", Toast.LENGTH_SHORT)
                .show() // toast
        }

        if (nhsId.isEmpty()) {
            // if the NHS ID is empty, show an error
            nhsIdInput.error = "NHS ID is required"
            isValid = false
        }

        if (bloodType == "Select Blood Type") {
            // if the blood type is not selected, show an error
            (bloodTypeSpinner.selectedView as? TextView)?.apply {
                error = "Please select a valid blood type"
                setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
            isValid = false
        }


        if (firstName.isEmpty()) {
            // if the first name is empty, show an error
            firstNameInput.error = "First Name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            // if the last name is empty, show an error
            lastNameInput.error = "Last Name is required"
            isValid = false
        }

        if (dobText.isEmpty()) {
            // if the date of birth is empty, show an error
            dobInput.error = "Date of Birth is required"
            isValid = false
        }


        if (gender.isEmpty() || gender == "Select Gender") {
            // if gender is empty show an error
            (genderSpinner.selectedView as? TextView)?.error = "Gender is required"
            isValid = false
        }

        if (bloodType.isEmpty() || bloodType == "Select Blood Type") {
            // if blood type is empty show an error
            (bloodTypeSpinner.selectedView as? TextView)?.error = "Blood Type is required"
            isValid = false
        }

        if (!isValid) {
            // send user error mesage
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert DOB to Firestore Timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dob = dateFormat.parse(dobText)?.let { Timestamp(it) }

        if (dob != null && dob.toDate().after(Date())) {
            // if the date of birth is in the future, show an error
            dobInput.error = "Date of Birth cannot be in the future"
            return
        }


        val patientData = mapOf(
            // create a map of patient data to be stored in Firestore
            "firstName" to firstName,
            "lastName" to lastName,
            "dob" to dob,
            "gender" to gender,
            "bloodType" to bloodType,
            "allergies" to allergies,
            "medicalConditions" to medicalConditions,
            "currentMedications" to currentMedications,
            "emergencyContact" to emergencyContact,
            "emergencyContactNumber" to emergencyContactPhone,
            "doctorId" to doctorId,
        )

        db.collection("patients").document(nhsId).get()
            // check if the NHS ID already exists in Firestore
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // if the NHS ID already exists, show an error
                    nhsIdInput.error = "This NHS ID already exists"
                    Log.e("FIREBASE_ERROR", "NHS ID already exists")
                } else {
                    db.collection("patients").document(nhsId)
                        // add the patient data to Firestore
                        .set(patientData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Patient Added Successfully!", Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FIREBASE_ERROR", "Failed to add patient: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_ERROR", "Failed to check existing patient: ${e.message}")
            }
    }

    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric (testing) environment
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }


}
