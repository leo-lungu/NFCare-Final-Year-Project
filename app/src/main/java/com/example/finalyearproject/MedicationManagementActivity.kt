package com.example.finalyearproject

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MedicationManagementActivity : AppCompatActivity() {

    private lateinit var medicationNameEditText: EditText
    private lateinit var addMedicationButton: Button
    private lateinit var medicationsRecyclerView: RecyclerView
    private lateinit var medicationSpinner: Spinner
    private lateinit var boxNameEditText: EditText
    private lateinit var expirationDateEditText: EditText
    private lateinit var quantityEditText: EditText
    private lateinit var addBoxButton: Button
    private lateinit var db: FirebaseFirestore

    private val medicationsList = mutableListOf<Medication>()
    private val boxesList = mutableListOf<MedicationSpecific>()
    private lateinit var medicationAdapter: MedicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_management)

        // initialise Firestore
        db = FirebaseFirestore.getInstance()

        // inputs from the layout file for medication name, box name, expiration date, and quantity
        medicationNameEditText = findViewById(R.id.medicationNameEditText)
        addMedicationButton = findViewById(R.id.addMedicationButton)
        medicationsRecyclerView = findViewById(R.id.medicationsRecyclerView)
        medicationSpinner = findViewById(R.id.medicationSpinner)
        boxNameEditText = findViewById(R.id.boxNameEditText)
        expirationDateEditText = findViewById(R.id.expirationDateEditText)
        quantityEditText = findViewById(R.id.quantityEditText)
        addBoxButton = findViewById(R.id.addBoxButton)

        // set up the RecyclerView for displaying medications
        medicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        medicationAdapter = MedicationAdapter(medicationsList, emptyMap())
        medicationsRecyclerView.adapter = medicationAdapter

        // load medications and boxes from Firestore
        loadMedications()
        loadMedicationBoxes()

        addMedicationButton.setOnClickListener {
            // when the add medication button is clicked, get the input values
            val name = medicationNameEditText.text.toString().trim()
            if (name.isEmpty()) {
                // set error message for empty medication name
                Toast.makeText(this, "Please enter a medication name", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val medication = Medication(
                // create a new medication object
                id = name.lowercase(),
                name = name,
            )

            db.collection("medications").document(medication.id)
                // add the medication to Firestore
                .set(medication)
                .addOnSuccessListener {
                    // show success message and clear the input field
                    Toast.makeText(this, "Medication added", Toast.LENGTH_LONG).show()
                    medicationNameEditText.text.clear()
                    loadMedications()
                }
                .addOnFailureListener { e ->
                    // show error message
                    Toast.makeText(this, "Error adding medication: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                    Log.e("FIRESTORE_ERROR", "Error adding medication: ${e.message}")
                }
        }

        addBoxButton.setOnClickListener {
            // when the add box button is clicked, get the input values
            val selectedMedication = medicationSpinner.selectedItem as? Medication
            val boxName = boxNameEditText.text.toString().trim()
            val expirationDate = expirationDateEditText.text.toString().trim()
            val quantityStr = quantityEditText.text.toString().trim()

            if (selectedMedication == null || boxName.isEmpty() || expirationDate.isEmpty() || quantityStr.isEmpty()) {
                // set error message for empty fields
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull()
            // check if quantity is a valid number
            if (quantity == null || quantity <= 0) {
                // set error message for invalid quantity
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val box = MedicationSpecific(
                // create a new medication box object
                medicationId = selectedMedication.id,
                boxName = boxName,
                expirationDate = expirationDate,
                quantity = quantity,
                batchNumber = ""
            )

            db.collection("medicationBoxes").add(box)
                // add the box to Firestore
                .addOnSuccessListener { documentReference ->
                    // show success message and clear the input fields
                    Toast.makeText(this, "Box added", Toast.LENGTH_LONG).show()
                    boxNameEditText.text.clear()
                    expirationDateEditText.text.clear()
                    quantityEditText.text.clear()
                    loadMedicationBoxes()
                }
                .addOnFailureListener { e ->
                    // show error message
                    Toast.makeText(this, "Error adding box: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FIRESTORE_ERROR", "Error adding box: ${e.message}")
                }
        }
    }

    private fun loadMedications() {
        // load medications from Firestore
        db.collection("medications")
            // get all documents from the medications collection
            .get()
            .addOnSuccessListener { result ->
                medicationsList.clear()
                for (document in result) {
                    // create a new medication object for each document
                    val medication =
                        document.toObject(Medication::class.java).copy(id = document.id)
                    medicationsList.add(medication)
                }
                updateSpinner()
                updateRecyclerView()
            }
            .addOnFailureListener { e ->
                // show error message
                Toast.makeText(this, "Error loading medications: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                Log.e("FIRESTORE_ERROR", "Error loading medications: ${e.message}")
            }
    }

    private fun loadMedicationBoxes() {
        // load medication boxes from Firestore
        db.collection("medicationBoxes")
            // get all documents from the medicationBoxes collection
            .get()
            .addOnSuccessListener { result ->
                boxesList.clear()
                for (document in result) {
                    // create a new medication box object for each document
                    val box =
                        // create a new medication box object for each document
                        document.toObject(MedicationSpecific::class.java).copy(id = document.id)
                    boxesList.add(box)
                }
                updateRecyclerView()
            }
            .addOnFailureListener { e ->
                // show error message
                Toast.makeText(this, "Error loading boxes: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("FIRESTORE_ERROR", "Error loading boxes: ${e.message}")
            }
    }

    private fun updateSpinner() {
        // update the spinner with the list of medications
        val adapter = ArrayAdapter(
            // create an ArrayAdapter for the spinner
            this,
            android.R.layout.simple_spinner_item,
            medicationsList // convert to a list of medication names
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        medicationSpinner.adapter = adapter
    }

    private fun updateRecyclerView() {
        // update the RecyclerView with the list of medications and boxes
        val boxesMap = boxesList.groupBy { it.medicationId } // group boxes by medication ID
        medicationAdapter = MedicationAdapter(medicationsList, boxesMap)
        medicationsRecyclerView.adapter = medicationAdapter
    }

    override fun onResume() {
        // when the activity is resumed, reload medications and boxes
        super.onResume()
        loadMedications()
        loadMedicationBoxes()
    }
}