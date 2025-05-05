package com.example.finalyearproject

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore // Firestore instance
    private lateinit var allergenInput: AutoCompleteTextView // AutoCompleteTextView for allergen input
    private lateinit var addAllergenButton: Button // Button to add allergen
    private lateinit var allergensListView: ListView // ListView to display selected allergens

    private val selectedAllergens = mutableListOf<String>() // list to store selected allergens
    private val allergenSuggestions = mutableListOf<String>() // list to store allergen suggestions

    private lateinit var allergensAdapter: ArrayAdapter<String> // ArrayAdapter for allergens ListView
    private lateinit var allergenAdapter: ArrayAdapter<String> // ArrayAdapter for allergen suggestions


    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication) // Set the content view to the layout file

        if (!isRunningInRobolectric()) {
            // when the app is not running in Robolectric (testing), initialize the Firestore instance
            db = FirebaseFirestore.getInstance()
        }

        // inputs from the layout file for medication name, dosage, and description
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val dosageInput = findViewById<EditText>(R.id.dosageInput)
        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)

        // buttons for saving the medication and going back
        val saveButton = findViewById<Button>(R.id.saveButton)
        val backButton = findViewById<Button>(R.id.backButton)

        saveButton.setOnClickListener {
            // when the save button is clicked, get the input values
            val name = nameInput.text.toString().trim()
            val dosage = dosageInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val allergens = selectedAllergens.toList()

            if (name.isEmpty() || dosage.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                // set error messages for empty fields
                if (name.isEmpty()) {
                    nameInput.error = "Name is required"
                }
                if (dosage.isEmpty()) {
                    dosageInput.error = "Dosage is required"
                }
                if (description.isEmpty()) {
                    descriptionInput.error = "Description is required"
                }
                return@setOnClickListener
            }

            if (!isRunningInRobolectric()) {
                // if the app is not running in Robolectric, add the medication to Firestore
                val medication = hashMapOf(
                    "name" to name,
                    "dosage" to dosage,
                    "description" to description,
                    "allergens" to selectedAllergens
                )

                db.collection("medications")
                    // add the medication to the Firestore collection
                    .add(medication)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(
                            this,
                            "Medication '$name' added with ID: ${documentReference.id}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error adding medication: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        backButton.setOnClickListener {
            // when the back button is clicked, finish the activity
            finish()
        }

        // initialise the views for allergen input, add allergen button, and allergens list view
        allergenInput = findViewById(R.id.allergenInput)
        addAllergenButton = findViewById(R.id.addAllergenButton)
        allergensListView = findViewById(R.id.allergensListView)

        // set up the ArrayAdapter for the allergens list view and allergen suggestions
        allergensAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, selectedAllergens)
        allergensListView.adapter = allergensAdapter

        // set up the ArrayAdapter for the allergen input
        allergenAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, allergenSuggestions)
        allergenInput.setAdapter(allergenAdapter)

        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric, load the allergen suggestions from Firestore
            loadAllergenSuggestions()
        }

        addAllergenButton.setOnClickListener {
            // when the add allergen button is clicked, get the allergen input
            val allergen = allergenInput.text.toString().trim()
            if (allergen.isNotEmpty()) {
                // if the allergen is not empty, add it to the selected allergens list
                if (!selectedAllergens.contains(allergen)) {
                    // check if the allergen is already in the selected allergens list
                    selectedAllergens.add(allergen)
                    allergensAdapter.notifyDataSetChanged()

                    if (!allergenSuggestions.contains(allergen)) {
                        // if the allergen is not in the suggestions list, add it to Firestore
                        addSuggestionToFirestore("allergens", allergen)
                        allergenSuggestions.add(allergen)
                        allergenAdapter.notifyDataSetChanged()
                    }
                }
                allergenInput.text.clear()
            }
        }
    }

    private fun loadAllergenSuggestions() {
        // load the allergen suggestions from Firestore
        db.collection("lists").document("allergens")
            // get the document with the allergen suggestions
            .get()
            .addOnSuccessListener { document ->
                val list = document.get("names") as? List<String> ?: emptyList()
                allergenSuggestions.clear()
                allergenSuggestions.addAll(list)
                allergenAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_ERROR", "Failed to load allergens: ${e.message}")
            }
    }

    private fun addSuggestionToFirestore(type: String, value: String) {
        // add a suggestion to Firestore
        db.collection("lists").document(type)
            .update("names", FieldValue.arrayUnion(value)) // add the value to the list
            .addOnSuccessListener {
                Log.d("Suggestion", "$value added to $type list!")
            }
            .addOnFailureListener { e ->
                Log.e("Suggestion", "Failed to add $value to $type: ${e.message}")
            }
    }

    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }
}