package com.example.finalyearproject

import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.nio.charset.Charset
import java.util.Calendar

class ViewMedicationActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var nfcTextView: TextView
    private lateinit var medicationName: TextView
    private lateinit var medicationDosage: TextView
    private lateinit var medicationDescription: TextView
    private lateinit var medicationBatchNumber: TextView
    private lateinit var medicationExpiryDate: TextView
    private lateinit var backButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: MedicationAdapter
    private var medicationList = mutableListOf<Medication>()
    private var boxesMap = mutableMapOf<String, List<MedicationSpecific>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        try {
            // set the content view to the layout file
            setContentView(R.layout.activity_view_medication)
        } catch (e: Exception) {
            // handle any exceptions that occur during layout inflation
            Log.e("SCAN_MEDICATION", "Error setting content view: ${e.message}", e)
            Toast.makeText(this, "Error loading layout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // initialise Firestore
            db = FirebaseFirestore.getInstance()
            Log.d("SCAN_MEDICATION", "Firestore initialized successfully")
        } catch (e: Exception) {
            // handle any exceptions that occur during Firestore initalisation
            Log.e("SCAN_MEDICATION", "Error initializing Firestore: ${e.message}", e)
            Toast.makeText(this, "Error initializing Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this) // get the default NFC adapter
        if (nfcAdapter == null) {
            // check if NFC is supported on the device
            Log.w("SCAN_MEDICATION", "NFC not supported on this device")
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            // check if NFC is enabled
            Log.w("SCAN_MEDICATION", "NFC is disabled on this device")
            Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_LONG).show()
        }

        try {
            // initailise ui components
            searchInput = findViewById(R.id.searchInput)
            searchButton = findViewById(R.id.searchButton)
            searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
            nfcTextView = findViewById(R.id.nfcTextView)
            medicationName = findViewById(R.id.medicationName)
            medicationDosage = findViewById(R.id.medicationDosage)
            medicationDescription = findViewById(R.id.medicationDescription)
            medicationBatchNumber = findViewById(R.id.medicationBatchNumber)
            medicationExpiryDate = findViewById(R.id.medicationExpiryDate)
            medicationExpiryDate.setOnClickListener {
                // show date picker dialog when the expiry date is clicked
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                    // format the selected date and set it to the EditText
                    val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    medicationExpiryDate.setText(formattedDate)
                }, year, month, day)

                datePicker.datePicker.minDate = calendar.timeInMillis // set minimum date to today

                datePicker.show()
            }

            backButton = findViewById(R.id.backButton)
        } catch (e: Exception) {
            // handle any exceptions that occur during view initialization
            Log.e("SCAN_MEDICATION", "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nfcTextView.text = "Or scan a medication box using NFC" // set the NFC text view

        fetchMedicationSpecificData()

        adapter = MedicationAdapter(medicationList, boxesMap) { selectedMedication ->
            showMedicationPopup(
                name = selectedMedication.name,
                dosage = selectedMedication.dosage,
                description = selectedMedication.description,
                batchNumber = "N/A", // batch not available from search
                expiryDate = "N/A",   // expiry date not available from search
                quantity = "N/A"
            )
        }

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = adapter

        searchButton.setOnClickListener {
            // when the search button is clicked, get the input value
            val queryText = searchInput.text.toString().trim()
            if (queryText.isNotEmpty()) {
                // call the search function with the input value
                searchMedications(queryText)
            } else {
                // show error message for empty input
                Toast.makeText(this, "Enter a medication name", Toast.LENGTH_SHORT).show()
                medicationList.clear()
                adapter.updateList(medicationList)
            }
        }

        backButton.setOnClickListener {
            // when the back button is clicked, finish the activity
            finish()
        }

        if (nfcAdapter == null) {
            // check if NFC is supported on the device
            nfcTextView.text = "NFC not supported on this device"
        }
    }

    private fun fetchMedicationSpecificData() {
        // fetch medication specific data from Firestore
        db.collection("medicationSpecific")
            // get all documents from the medicationSpecific collection
            .get()
            .addOnSuccessListener { documents ->
                // if the query is successful, clear the previous data and add the new data
                val tempMap = mutableMapOf<String, MutableList<MedicationSpecific>>()
                for (doc in documents) {
                    // for each document in the result set, add the medication to the list
                    try {
                        val medicationSpecific = doc.toObject(MedicationSpecific::class.java)
                        val medicationId = medicationSpecific.medicationId
                        if (medicationId.isNotEmpty()) {
                            // check if the medicationId is not empty
                            if (!tempMap.containsKey(medicationId)) {
                                // if the map does not contain the medicationId, add it
                                tempMap[medicationId] = mutableListOf()
                            }
                            tempMap[medicationId]?.add(medicationSpecific)
                        }
                    } catch (e: Exception) {
                        Log.e("FIREBASE_ERROR", "Error parsing medicationSpecific: ${e.message}")
                    }
                }
                boxesMap.clear()
                boxesMap.putAll(tempMap)
                adapter.notifyDataSetChanged() // notify the adapter of data changes
                Log.d("FIREBASE_DEBUG", "Fetched medicationSpecific data: $boxesMap")
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_ERROR", "Error fetching medicationSpecific: ${e.message}")
                Toast.makeText(this, "Error fetching medication boxes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onResume() {
        // when the activity is resumed, set up NFC
        super.onResume()
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            // check if NFC is supported and enabled
            return
        }
        val intent = Intent(this, javaClass).apply {
            // create an intent to handle NFC tag discovery
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            // create a pending intent for the NFC tag discovery
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(
            // create intent filters for different NFC actions
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        // when the activity is paused, disable NFC
        super.onPause()
        if (nfcAdapter != null && nfcAdapter!!.isEnabled) {
            // check if NFC is supported and enabled
            nfcAdapter?.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Log.d("NFC_DEBUG", "NFC Tag Scanned - Intent Action: ${intent.action}")

        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED || intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            // check if the intent action is one of the NFC actions
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            if (tag == null) {
                // check if the tag is null
                Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
                return
            }

            val medicationId = readNfcTag(intent)
            if (medicationId.startsWith("ERROR")) {
                // check if there was an error reading the NFC tag
                Toast.makeText(this, "Error reading NFC: $medicationId", Toast.LENGTH_SHORT).show()
                clearMedicationData()
                return
            }

            Toast.makeText(this, "Scanned Medication ID: $medicationId", Toast.LENGTH_LONG).show()
            Log.d("NFC_PAYLOAD", "Scanned Medication ID: $medicationId")

            fetchMedicationData(medicationId) // fetch medication data using the scanned ID
        } else {
            // handle unsupported NFC actions
            Toast.makeText(this, "Unsupported NFC action: ${intent.action}", Toast.LENGTH_SHORT).show()
            Log.w("NFC_DEBUG", "Unsupported NFC action: ${intent.action}")
        }
    }

    private fun showMedicationPopup(name: String, dosage: String, description: String, batchNumber: String, expiryDate: String, quantity: String) {
        val message = """
        Name: $name
        Dosage: $dosage
        Description: $description
        Batch Number: $batchNumber
        Expiry Date: $expiryDate
        Quantity: $quantity
    """.trimIndent()

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Medication Details")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun readNfcTag(intent: Intent): String {
        // read the NFC tag data
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (!rawMsgs.isNullOrEmpty()) {
            // check if the raw messages are not empty
            val ndefMessage = rawMsgs[0] as NdefMessage
            val ndefRecord = ndefMessage.records.firstOrNull {
                it.tnf == NdefRecord.TNF_WELL_KNOWN &&
                        it.type.contentEquals(NdefRecord.RTD_TEXT)
            }

            if (ndefRecord != null) {
                // check if the NDEF record is not null
                val payload = ndefRecord.payload
                val encoding = if ((payload[0].toInt() and 128) == 0) Charset.forName("UTF-8") else Charset.forName("UTF-16")
                val langCodeLen = payload[0].toInt() and 63
                return String(payload, langCodeLen + 1, payload.size - langCodeLen - 1, encoding)
            }
        }
        return "ERROR: No valid NFC data found"
    }

    private fun searchMedications(query: String) {
        // search for medications in the Firestore database
        if (query.isEmpty()) {
            // check if the query is empty
            medicationList.clear()
            adapter.updateList(medicationList)
            return
        }

        Log.d("SEARCH_DEBUG", "Searching for medication name starting with: $query")



        db.collection("medications")
            // query the medications collection
            .get()
            .addOnSuccessListener { documents ->
                //
                medicationList.clear()
                val lowerQuery = query.lowercase()

                for (doc in documents) {
                    // for each document in the result set, add the medication to the list
                    val name = doc.getString("name") ?: continue

                    if (name.lowercase().contains(lowerQuery)) {
                        // check if the medication name contains the query
                        val medicationId = doc.id
                        val dosage: String = doc.getString("dosage") ?: "Unknown"
                        val description = doc.getString("description") ?: "No Description"

                        val medication = Medication(
                            // create a new medication object
                            id = medicationId,
                            name = name,
                            dosage = dosage,
                            description = description,
                        )
                        medicationList.add(medication)
                        Log.d("SEARCH_DEBUG", "Found medication: $name (ID: $medicationId)")
                    }
                }
                adapter.updateList(medicationList)
                if (medicationList.isEmpty()) {
                    // if no results are found, show a message
                    Log.d("SEARCH_DEBUG", "No medication found matching: $query")
                    Toast.makeText(this, "No medication found matching: $query", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // handle failure
                Log.e("FIREBASE_ERROR", "Error fetching medications: ${e.message}")
                Toast.makeText(this, "Error searching medications", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchMedicationData(medicationId: String) {
        // fetch medication data from Firestore
        db.collection("medicationSpecific").document(medicationId)
            // get the document with the medication ID
            .get()
            .addOnSuccessListener { doc ->
                // if the document exists, get the medication data
                if (doc.exists()) {
                    val medicationId = doc.getString("medicationId") ?: "Unknown"
                    val batchNumber = doc.getString("batchNumber") ?: "Unknown"
                    val expiryTimestamp = doc.getTimestamp("expirationDate")
                    val expiryDate = expiryTimestamp?.toDate()?.toString() ?: "Unknown"
                    val quantityValue = doc.getLong("quantity")?.toInt() ?: -1
                    val quantity = if (quantityValue >= 0) quantityValue.toString() else "Unknown"

                    // now fetch the medication data from the medications collection
                    db.collection("medications").document(medicationId)
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val name = doc.getString("name") ?: "Unknown"
                                val dosage = doc.getString("dosage") ?: "Unknown"
                                val description = doc.getString("description") ?: "No Description"



                                showMedicationPopup(name, dosage, description, batchNumber, expiryDate, quantity)



                            } else {
                                Toast.makeText(this, "Medication not found", Toast.LENGTH_SHORT).show()
                                clearMedicationData()
                            }
                        }
                } else {
                    // if the document does not exist, show a message
                    Toast.makeText(this, "Medication not found", Toast.LENGTH_SHORT).show()
                    clearMedicationData()
                }
            }
            .addOnFailureListener {
                clearMedicationData()
                Log.e("FIREBASE_ERROR", "Error: ${it.message}")
            }
    }

    private fun clearMedicationData() {
        // clear the medication data from the UI
        medicationName.visibility = View.GONE
        medicationDosage.visibility = View.GONE
        medicationDescription.visibility = View.GONE
        medicationBatchNumber.visibility = View.GONE
        medicationExpiryDate.visibility = View.GONE
    }
}