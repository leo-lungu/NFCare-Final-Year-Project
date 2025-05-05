package com.example.finalyearproject

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class PrescriptionActivity : AppCompatActivity() {
    private lateinit var medicationInput: EditText
    private lateinit var dosesInput: EditText
    private lateinit var searchButton: Button
    private lateinit var chewedCheckbox: CheckBox
    private lateinit var foodCheckbox: CheckBox
    private lateinit var assignButton: Button
    private lateinit var medicationResultsListView: ListView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var resultsContainer: LinearLayout
    private lateinit var detailsContainer: LinearLayout

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private var selectedMedication: Map<String, Any>? = null
    private val searchResults = mutableListOf<Map<String, Any>>()
    private lateinit var resultsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription)

        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("NHS_ID") ?: ""

        // initialise UI components
        medicationInput = findViewById(R.id.medicationInput)
        searchButton = findViewById(R.id.searchButton)
        dosesInput = findViewById(R.id.dosesInput)
        chewedCheckbox = findViewById(R.id.chewedCheckbox)
        foodCheckbox = findViewById(R.id.foodCheckbox)
        assignButton = findViewById(R.id.assignButton)
        medicationResultsListView = findViewById(R.id.medicationResultsListView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        resultsContainer = findViewById(R.id.resultsContainer)
        detailsContainer = findViewById(R.id.detailsContainer)

        //  hide the details section
        detailsContainer.visibility = View.GONE


        resultsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        medicationResultsListView.adapter = resultsAdapter // set the adapter for the ListView

        searchButton.setOnClickListener {
            // when the search button is clicked, get the input value
            val query = medicationInput.text.toString().trim()
            if (query.isNotEmpty()) {
                // call the search function with the input value
                searchMedications(query)
            } else {
                // show error message for empty input
                Toast.makeText(this, "Please enter a medication name", Toast.LENGTH_SHORT).show()
            }
        }

        medicationResultsListView.setOnItemClickListener { _, _, position, _ ->
            // when an item in the ListView is clicked, get the selected medication
            selectedMedication = searchResults[position]
            val medicationName = selectedMedication?.get("name") as? String ?: "Unknown Medication"

            findViewById<TextView>(R.id.selectedMedicationName).text = medicationName // set the selected medication name

            resultsContainer.visibility = View.GONE
            detailsContainer.visibility = View.VISIBLE
            // hide the results container
        }

        assignButton.setOnClickListener {
            // when the assign button is clicked, get the input values
            assignPrescription()
        }
    }

    private fun searchMedications(query: String) {
        // search for medications in the Firestore database
        loadingProgressBar.visibility = View.VISIBLE
        searchResults.clear()
        resultsAdapter.clear()

        resultsContainer.visibility = View.VISIBLE

        db.collection("medications")
            // query the medications collection
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                // if the query is successful, clear the previous results and add the new results
                searchResults.clear()
                for (document in documents) {
                    // for each document in the result set, add the medication to the list
                    val medication = document.data
                    searchResults.add(medication.plus("id" to document.id))
                    resultsAdapter.add(medication["name"] as? String ?: "Unknown Medication")
                }

                if (searchResults.isEmpty()) {
                    // if no results are found, show a message
                    resultsAdapter.add("No medications found")
                }

                loadingProgressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                // if the query fails, show an error message
                Toast.makeText(this, "Failed to search for medications", Toast.LENGTH_SHORT).show()
                loadingProgressBar.visibility = View.GONE
            }
    }

    private fun assignPrescription() {
        // assign a prescription to the selected medication
        val dosesPerDay = dosesInput.text.toString().toIntOrNull()

        if (selectedMedication == null) {
            // if no medication is selected, show an error message
            Toast.makeText(this, "Please select a medication from the search results", Toast.LENGTH_SHORT).show()
            return
        }

        if (dosesPerDay == null || dosesPerDay < 1) {
            // if the number of doses per day is invalid, show an error message
            Toast.makeText(this, "Please enter a valid number of doses per day", Toast.LENGTH_SHORT).show()
            return
        }

        val medicationId = selectedMedication?.get("id") as? String ?: return
        val medicationName = selectedMedication?.get("name") as? String ?: return
        val scheduleId = "${userId}_$medicationId"

        val prescription = mapOf(
            // create a new prescription object
            "userId" to userId,
            "medicationId" to medicationId,
            "medicationName" to medicationName,
            "dosesPerDay" to dosesPerDay,
            "startDate" to Timestamp.now(),
            "mustBeChewed" to chewedCheckbox.isChecked,
            "takeWithFood" to foodCheckbox.isChecked,
            "lastAdministered" to emptyList<Timestamp>()
        )

        val updates = mapOf(
            // update the user's medications field
            "currentMedications" to FieldValue.arrayUnion(medicationId),
            "medicationScheduledId" to FieldValue.arrayUnion(scheduleId)
        )

        db.collection("medicationSchedules").document(scheduleId)
            // add the prescription to the Firestore database
            .set(prescription)
            .addOnSuccessListener {
                // show success message and finish the activity
                Toast.makeText(this, "Prescription assigned!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                // show error message
                Toast.makeText(this, "Failed to assign prescription", Toast.LENGTH_SHORT).show()
            }

        db.collection("patients").document(userId)
            // update the user's medications field in the Firestore database
            .update(updates)
            .addOnSuccessListener {
                // show success message
                Log.d("PrescriptionActivity", "User medications updated successfully")
            }
            .addOnFailureListener {
                // show error message
                Log.e("PrescriptionActivity", "Failed to update user medications: ${it.message}")
            }
    }
}