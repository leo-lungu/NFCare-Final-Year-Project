package com.example.finalyearproject

import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WriteMedicationActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicationAdapter
    private lateinit var writeNfcButton: Button
    private var medicationList = mutableListOf<Medication>()
    private var boxesMap = mutableMapOf<String, List<MedicationSpecific>>()
    private lateinit var db: FirebaseFirestore
    private lateinit var backButton: Button
    private var selectedMedicationId: String? =
        null // stores selected medication ID for NFC writing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_nfc)

        if (!isRunningInRobolectric()) {
            // if not running in Robolectric (testing), initialise Firebase
            db = FirebaseFirestore.getInstance()
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this) // get NFC adapter

        // input fields and buttons from the layout file
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        backButton = findViewById(R.id.backButton)
        writeNfcButton = findViewById(R.id.writeNfcButton)
        val batchNumberInput = findViewById<EditText>(R.id.batchNumberInput)
        val expirationDateInput = findViewById<EditText>(R.id.expirationDateInputField)
        val quantityInput = findViewById<EditText>(R.id.quantityInput)
        val saveBoxButton = findViewById<Button>(R.id.saveBoxButton)

        writeNfcButton.visibility = View.GONE // hide NFC write button initially

        adapter = MedicationAdapter(medicationList, boxesMap) { selectedMedication ->
            // handle medication selection
            selectedMedicationId = selectedMedication.id
            Toast.makeText(this, "Selected: ${selectedMedication.name}", Toast.LENGTH_SHORT).show()

            batchNumberInput.visibility = View.VISIBLE
            expirationDateInput.visibility = View.VISIBLE
            quantityInput.visibility = View.VISIBLE
            saveBoxButton.visibility = View.VISIBLE
            writeNfcButton.visibility = View.GONE


            expirationDateInput.setOnClickListener {
                // show date picker dialog for expiration date
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePicker =
                    DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                        // set selected date to the input field
                        val formattedDate = String.format(
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                        )
                        expirationDateInput.setText(formattedDate)
                    }, year, month, day)

                datePicker.datePicker.minDate = calendar.timeInMillis
                datePicker.show()
            }
        }


        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchButton.setOnClickListener {
            // get the input value from the search field
            val queryText = searchInput.text.toString().trim()
            if (queryText.isNotEmpty()) {
                // call the search function with the input value
                searchMedications(queryText)
            } else {
                // show error message for empty input
                Toast.makeText(this, "Enter a medication name", Toast.LENGTH_SHORT).show()
                searchInput.error = "Enter a medication name"
            }
        }

        writeNfcButton.setOnClickListener {
            // handle NFC write button click
            if (selectedMedicationId != null) { // check if a medication is selected
                Toast.makeText(
                    this,
                    "Tap an NFC tag to write Medication ID: $selectedMedicationId",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("DEBUG_NFC", "Ready to Write: $selectedMedicationId")

            } else {
                Toast.makeText(this, "No medication selected!", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            // handle back button click
            finish()
        }

        saveBoxButton.setOnClickListener {
            // handle save box button click
            val batchNumber = batchNumberInput.text.toString().trim()
            val expirationRaw = expirationDateInput.text.toString().trim()
            val quantityStr = quantityInput.text.toString().trim()

            if (batchNumber.isEmpty() || expirationRaw.isEmpty() || quantityStr.isEmpty()) {
                // check if any field is empty - show error message for each empty field
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()

                if (batchNumber.isEmpty()) {
                    batchNumberInput.error = "Batch number is required"
                }
                if (expirationRaw.isEmpty()) {
                    expirationDateInput.error = "Expiration date is required"

                }
                if (quantityStr.isEmpty()) {
                    quantityInput.error = "Quantity is required"
                }
                return@setOnClickListener
            }


            val quantity = try {
                // try to parse quantity input to integer
                quantityStr.toInt()
            } catch (e: Exception) {
                // handle invalid quantity input
                Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show()
                quantityInput.error = "Quantity must be a number"
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // date format
            val expirationDate = try {
                // try to parse expiration date input
                val parsedDate = dateFormat.parse(expirationRaw)!!
                if (parsedDate.before(java.util.Date())) {
                    // check if expiration date is in the past
                    expirationDateInput.error = "Expiration date cannot be in the past"
                    Toast.makeText(
                        this,
                        "Expiration date cannot be in the past",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                Timestamp(parsedDate)
            } catch (e: Exception) {
                // handle invalid date format
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medication = medicationList.find { it.id == selectedMedicationId }
            // find the selected medication from the list
            val medicationBox = hashMapOf(
                // create a map for the medication box
                "batchNumber" to batchNumber,
                "expirationDate" to expirationDate,
                "medicationId" to selectedMedicationId,
                "name" to medication?.name,
                "quantity" to quantity
            )

            db.collection("medicationSpecific")
                // add the medication box to Firestore
                .add(medicationBox)
                .addOnSuccessListener { documentRef ->
                    // show success message and clear input fields
                    Toast.makeText(this, "Medication box saved!", Toast.LENGTH_SHORT).show()

                    val generatedBoxId = documentRef.id
                    Log.d("DEBUG_NFC_WRITE", "Generated doc ID: $generatedBoxId")

                    // clear input fields
                    batchNumberInput.text.clear()
                    expirationDateInput.text.clear()
                    quantityInput.text.clear()

                    batchNumberInput.visibility = View.GONE
                    expirationDateInput.visibility = View.GONE
                    quantityInput.visibility = View.GONE
                    saveBoxButton.visibility = View.GONE

                    selectedMedicationId = generatedBoxId


                    Toast.makeText(
                        this,
                        "Tap NFC tag to write Box ID: $generatedBoxId",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { e ->
                    // handle failure
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }


    }

    override fun onResume() {
        // enable NFC foreground dispatch when the activity is resumed
        super.onResume()
        val intent = Intent(this, javaClass).apply {
            // create an intent for the current activity
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            // create a pending intent for the NFC tag
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        // disable NFC foreground dispatch when the activity is paused
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        // handle new NFC intent
        super.onNewIntent(intent)
        setIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // check if the intent action is NFC tag discovered
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)

            if (tag != null && selectedMedicationId != null) {
                // check if a tag is detected and a medication is selected
                writeToNfc(tag, selectedMedicationId!!)
            } else {
                Toast.makeText(
                    this,
                    "Select a medication and press 'Write NFC' before scanning!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun writeToNfc(tag: Tag, medicationId: String) {
        // write medication ID to NFC tag
        try {
            // check if the tag is writable
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                // check if the tag is already formatted
                ndef.connect()
                val textRecord = NdefRecord.createTextRecord("en", medicationId)
                val ndefMessage = NdefMessage(arrayOf(textRecord))
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                Toast.makeText(this, "Medication ID written to NFC!", Toast.LENGTH_SHORT).show()
            } else {
                // if the tag is not formatted, format it
                formatTag(tag, medicationId)
            }
        } catch (e: Exception) {
            // handle error
            Log.e("NFC_TAG", "Error writing to NFC: ${e.message}")
            Toast.makeText(this, "Failed to write to NFC", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTag(tag: Tag, data: String) {
        // format the NFC tag
        try {
            // check if the tag is formatable
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                // format the tag
                ndefFormatable.connect()
                val textRecord = NdefRecord.createTextRecord("en", data)
                val ndefMessage = NdefMessage(arrayOf(textRecord))
                ndefFormatable.format(ndefMessage)
                ndefFormatable.close()
                Toast.makeText(this, "Tag formatted & written!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "This tag cannot be formatted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // handle error
            Log.e("NFC_TAG", "Error formatting tag: ${e.message}")
            Toast.makeText(this, "Failed to format tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchMedications(query: String) {
        // search for medications in the Firestore database
        db.collection("medications")
            // query the medications collection
            .get()
            .addOnSuccessListener { documents ->
                // if the query is successful, clear the previous results and add the new results
                medicationList.clear()
                val lowerQuery = query.lowercase()

                for (doc in documents) {
                    val name = doc.getString("name") ?: continue

                    if (name.lowercase().contains(lowerQuery)) {
                        val medicationId = doc.id
                        val dosage = doc.getString("dosage") ?: "No Dosage Specified"
                        val allergens = doc.get("allergens") as? List<String> ?: emptyList()
                        val description = doc.getString("description") ?: "No description"

                        val medication = Medication(
                            // create a new medication object for each document
                            id = medicationId,
                            name = name,
                            dosage = dosage,
                            description = description,
                            allergens = allergens
                        )
                        medicationList.add(medication)
                        Log.d("SEARCH_DEBUG", "Found medication: $name (ID: $medicationId)")
                    }
                }

                if (medicationList.isEmpty()) {
                    // if no results are found, show a message
                    Log.d("SEARCH_DEBUG", "No medication found matching: $query")
                    Toast.makeText(this, "No medication found matching: $query", Toast.LENGTH_SHORT)
                        .show()
                }

                adapter.updateList(medicationList)
            }
            .addOnFailureListener { e ->
                // handle failure
                Log.e("FIREBASE_ERROR", "Error searching medications: ${e.message}")
                Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric (testing environment)
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }
}