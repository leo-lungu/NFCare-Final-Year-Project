package com.example.finalyearproject

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EditPatientActivity : AppCompatActivity() {

    // initialise the views and variables
    private lateinit var nhsIdTextView: TextView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var dobEditText: EditText
    private lateinit var genderEditText: EditText
    private lateinit var bloodTypeSpinner: Spinner
    private lateinit var doctorIdEditText: EditText
    private lateinit var emergencyContactEditText: EditText
    private lateinit var emergencyContactNumberEditText: EditText
    private lateinit var medicalConditionsEditText: EditText
    private lateinit var currentMedicationsEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var db: FirebaseFirestore
    private var nhsId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_patient)

        // initialise the views
        nhsIdTextView = findViewById(R.id.nhsIdTextView)
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        dobEditText = findViewById(R.id.dobEditText)
        dobEditText = findViewById(R.id.dobEditText)
        dobEditText.setOnClickListener {
            // when the dobEditText is clicked, show a DatePickerDialog
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // when a date is selected, format it and set it to the dobEditText
                val formattedDate =
                    String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                dobEditText.setText(formattedDate)
            }, year, month, day)

            datePicker.datePicker.maxDate = calendar.timeInMillis // set max date to today

            datePicker.show()
        }
        genderEditText = findViewById(R.id.genderEditText)
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner)
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bloodTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bloodTypeSpinner.adapter = adapter
        doctorIdEditText = findViewById(R.id.doctorIdEditText)
        emergencyContactEditText = findViewById(R.id.emergencyContactEditText)
        emergencyContactNumberEditText = findViewById(R.id.emergencyContactPhoneEditText)
        medicalConditionsEditText = findViewById(R.id.medicalConditionsEditText)
        currentMedicationsEditText = findViewById(R.id.currentMedicationsEditText)
        saveButton = findViewById(R.id.saveButton)


        nhsId = intent.getStringExtra("NHS_ID")
        if (nhsId == null) {
            // if the NHS ID is null, show a message and finish the activity
            Toast.makeText(this, "No NHS ID provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nhsIdTextView.text = nhsId // set the NHS ID to the TextView

        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric, initialise the Firestore instance
            db = FirebaseFirestore.getInstance()
            fetchPatientData(nhsId!!)
        }

        saveButton.setOnClickListener {
            // when the save button is clicked, save the patient data
            savePatientData()
        }
    }

    private fun fetchPatientData(nhsId: String) {
        // fetch the patient data from Firestore
        db.collection("patients").document(nhsId)
            // get the document with the NHS ID
            .get()
            .addOnSuccessListener { doc ->
                // check if the activity is not finishing
                if (isFinishing) return@addOnSuccessListener

                if (doc.exists()) {
                    // log the raw data for debugging
                    Log.d("FIRESTORE_DATA", "Raw patient data: ${doc.data}")

                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""

                    val dobValue = doc.get("dob")
                    val dob = when (dobValue) {
                        // handle different types of date formats
                        is String -> dobValue
                        is com.google.firebase.Timestamp -> {
                            // convert timestamp to string
                            val sdf = java.text.SimpleDateFormat(
                                "dd/MM/yyyy",
                                java.util.Locale.getDefault()
                            )
                            sdf.format(dobValue.toDate())
                        }

                        else -> ""
                    }


                    val gender = doc.getString("gender") ?: ""
                    val bloodType = doc.getString("bloodType") ?: ""
                    val doctorId = doc.getString("doctorId") ?: ""

                    // emergencyContact as a string
                    val emergencyContactName = doc.getString("emergencyContact") ?: ""
                    val emergencyContactNumber = doc.getString("emergencyContactNumber") ?: ""
                    val contactStr =
                        if (emergencyContactName.isNotEmpty() && emergencyContactNumber.isNotEmpty()) {
                            // format the emergency contact string
                            "$emergencyContactName: $emergencyContactNumber"
                        } else {
                            // if no contact is provided, set a default value
                            emergencyContactName.ifEmpty { "Not provided" }
                        }

                    val medicalConditions = try {
                        // handle medicalConditions as a string
                        doc.get("medicalConditions") as? List<*>
                    } catch (e: Exception) {
                        Log.e("FIREBASE_PARSE", "Could not read medicalConditions: ${e.message}")
                        null
                    }?.filterIsInstance<String>() ?: emptyList()

                    val currentMedications = try {
                        // handle currentMedications as a string
                        doc.get("currentMedications") as? List<*>
                    } catch (e: Exception) {
                        Log.e("FIREBASE_PARSE", "Could not read currentMedications: ${e.message}")
                        null
                    }?.filterIsInstance<String>() ?: emptyList()

                    // set the fetched data to the views
                    firstNameEditText.setText(firstName)
                    lastNameEditText.setText(lastName)
                    dobEditText.setText(dob)
                    genderEditText.setText(gender)
                    bloodTypeSpinner.setSelection(
                        (bloodTypeSpinner.adapter as ArrayAdapter<String>).getPosition(
                            bloodType
                        )
                    )
                    doctorIdEditText.setText(doctorId)
                    emergencyContactEditText.setText(contactStr)
                    emergencyContactNumberEditText.setText(emergencyContactNumber)
                    medicalConditionsEditText.setText(medicalConditions.joinToString(", "))
                    currentMedicationsEditText.setText(currentMedications.joinToString(", "))
                } else {
                    // patient was not found
                    Toast.makeText(this, "Patient not found", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                if (isFinishing) return@addOnFailureListener

                Toast.makeText(this, "Error fetching patient data: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                Log.e("FIREBASE_ERROR", "Error fetching patient: ${e.message}")
                finish()
            }
    }

    private fun savePatientData() {
        // get the input data from the views
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val dob = dobEditText.text.toString().trim()
        val gender = genderEditText.text.toString().trim()
        val bloodType = bloodTypeSpinner.selectedItem.toString().trim()
        val doctorId = doctorIdEditText.text.toString().trim()
        val emergencyContact = emergencyContactEditText.text.toString().trim()
        val emergencyContactNumber = emergencyContactNumberEditText.text.toString().trim()
        val medicalConditionsStr = medicalConditionsEditText.text.toString().trim()
        val currentMedicationsStr = currentMedicationsEditText.text.toString().trim()

        // different validation checks to ensure the data is valid
        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() || gender.isEmpty() || bloodType.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_LONG).show()
            if (firstName.isEmpty()) {
                firstNameEditText.error = "First name is required"
            }
            if (lastName.isEmpty()) {
                lastNameEditText.error = "Last name is required"
            }
            if (dob.isEmpty()) {
                dobEditText.error = "Date of birth is required"
            }
            if (gender.isEmpty()) {
                genderEditText.error = "Gender is required"
            }
            if (bloodType.isEmpty()) {
                (bloodTypeSpinner.selectedView as TextView).error = "Blood type is required"
            }
            return
        }

        // check if the date of birth is in the correct format
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val enteredDate = try {
            dateFormat.parse(dob)
        } catch (e: Exception) {
            null
        }

        if (enteredDate != null && enteredDate.after(java.util.Date())) {
            // check if the date of birth is in the future
            dobEditText.error = "Date of Birth cannot be in the future"
            return
        }


        val medicalConditions = if (medicalConditionsStr.isNotEmpty()) {
            // handle medicalConditions as a string
            medicalConditionsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()


        val currentMedications = if (currentMedicationsStr.isNotEmpty()) {
            // handle currentMedications as a string
            currentMedicationsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()


        val updatedPatient = hashMapOf(
            // create a map of the updated patient data
            "firstName" to firstName,
            "lastName" to lastName,
            "dob" to dob,
            "gender" to gender,
            "bloodType" to bloodType,
            "doctorId" to doctorId,
            "emergencyContact" to emergencyContact,
            "emergencyContactNumber" to emergencyContactNumber,
            "medicalConditions" to medicalConditions,
            "currentMedications" to currentMedications
        )

        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric, update the patient data in Firestore
            nhsId?.let { id ->
                // check if the NHS ID is not null
                db.collection("patients").document(id)
                    // update the document with the NHS ID
                    .set(updatedPatient)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Patient updated successfully", Toast.LENGTH_LONG)
                            .show()
                        finish() // Return to MainActivity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error updating patient: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("FIREBASE_ERROR", "Error updating patient: ${e.message}")
                    }
            }
        }
    }

    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }

}