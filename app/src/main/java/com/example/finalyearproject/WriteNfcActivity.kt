package com.example.finalyearproject

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64


class WriteNfcActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PatientAdapter
    private lateinit var writeNfcButton: Button
    private var patientList = mutableListOf<Patient>()
    private lateinit var db: FirebaseFirestore
    private lateinit var backButton: Button
    private var selectedPatientId: String? = null

    private val secretKey = "1234567890abcdef" // secret key for AES encryption

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        generateKeyIfNeeded()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_write_nfc)
        findViewById<View>(R.id.expirationDateInput)?.visibility = View.GONE

        db = FirebaseFirestore.getInstance()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // input fields and buttons
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        backButton = findViewById(R.id.backButton)
        writeNfcButton = findViewById(R.id.writeNfcButton)

        writeNfcButton.visibility = View.GONE

        adapter = PatientAdapter(patientList) { selectedPatient ->
            // handle patient selection
            selectedPatientId = selectedPatient.id
            Toast.makeText(
                this,
                "Selected: ${selectedPatient.firstName} ${selectedPatient.lastName}",
                Toast.LENGTH_SHORT
            ).show()

            writeNfcButton.visibility = View.VISIBLE
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchButton.setOnClickListener {
            // when the search button is clicked, get the input value
            val queryText = searchInput.text.toString().trim()
            if (queryText.isNotEmpty()) {
                // call the search function with the input value
                searchPatients(queryText)
            } else {
                // show error message for empty input
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show()
            }
        }

        writeNfcButton.setOnClickListener {
            // when the write NFC button is clicked, check if a patient is selected
            if (selectedPatientId != null) {
                // start NFC writing process
                Toast.makeText(
                    this,
                    "Tap an NFC tag to write NHS ID: $selectedPatientId",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("DEBUG_NFC", "Ready to Write: $selectedPatientId")

            } else {
                // show error message for no patient selected
                Toast.makeText(this, "No patient selected!", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            // when the back button is clicked, go back to the previous activity
            finish()
        }
    }

    override fun onResume() {
        //
        super.onResume()
        val intent = Intent(this, javaClass).apply {
            // set the intent to the current activity
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            // create a pending intent for NFC tag discovery
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        // when the activity is paused, disable NFC foreground dispatch
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        // when a new intent is received, check if it's an NFC tag discovery
        super.onNewIntent(intent)
        setIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // if the intent action is NFC tag discovery
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)

            if (tag != null && selectedPatientId != null) {
                // if a tag is detected and a patient is selected
                writeToNfc(tag, selectedPatientId!!)
            } else {
                // if no tag or patient is selected
                Toast.makeText(
                    this,
                    "Select a patient and press 'Write NFC' before scanning!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun writeToNfc(tag: Tag, patientId: String) {
        // write the patient ID to the NFC tag
        try {
            // check if the tag is already formatted
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                // if the tag is formatted, write the patient ID
                ndef.connect()
                val encryptedPatientId = encrypt(patientId)
                val textRecord = NdefRecord.createTextRecord("en", encryptedPatientId)
                val ndefMessage = NdefMessage(arrayOf(textRecord))
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                Toast.makeText(this, "Patient ID written to NFC!", Toast.LENGTH_SHORT).show()
            } else {
                // if the tag is not formatted, format it and write the patient ID
                formatTag(tag, patientId)
            }
        } catch (e: Exception) {
            // handle any errors during NFC writing
            Log.e("NFC_TAG", "Error writing to NFC: ${e.message}")
            Toast.makeText(this, "Failed to write to NFC", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTag(tag: Tag, data: String) {
        // format the NFC tag and write data to it
        try {
            // check if the tag can be formatted
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                // if the tag can be formatted, format it and write data
                ndefFormatable.connect()
                val textRecord = NdefRecord.createTextRecord("en", data)
                val ndefMessage = NdefMessage(arrayOf(textRecord))
                ndefFormatable.format(ndefMessage)
                ndefFormatable.close()
                Toast.makeText(this, "Tag formatted & written!", Toast.LENGTH_SHORT).show()
            } else {
                // if the tag cannot be formatted, show an error message
                Toast.makeText(this, "This tag cannot be formatted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // handle any errors during formatting
            Log.e("NFC_TAG", "Error formatting tag: ${e.message}")
            Toast.makeText(this, "Failed to format tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchPatients(query: String) {
        // search for patients in the Firestore database
        if (query.isEmpty()) {
            // if the query is empty, clear the list
            patientList.clear()
            adapter.updateList(patientList)
            return
        }

        Log.d("SEARCH_DEBUG", "Searching for: $query")

        db.collection("patients")
            // query the patients collection
            .get()
            .addOnSuccessListener { documents ->
                // if the query is successful, clear the previous results and add the new results
                patientList.clear()
                for (doc in documents) {
                    // for each document in the result set, add the patient to the list
                    try {
                        val patientId = doc.id
                        val firstName = doc.getString("firstName") ?: "Unknown"
                        val lastName = doc.getString("lastName") ?: "Unknown"

                        val dobValue = doc.get("dob") // get the date of birth value
                        val dateOfBirth: String = when (dobValue) {
                            // check the type of the date of birth value
                            is String -> dobValue
                            is Timestamp -> SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            ).format(dobValue.toDate())

                            else -> "Unknown"
                        }

                        val emergencyContact =
                            doc.get("emergencyContact") as? Map<String, String> ?: emptyMap()
                        val medicalConditions =
                            doc.get("medicalConditions") as? List<String> ?: emptyList()
                        val currentMedications =
                            doc.get("currentMedications") as? List<String> ?: emptyList()

                        val patient = Patient(
                            // create a new patient object
                            id = patientId,
                            firstName = firstName,
                            lastName = lastName,
                            dateOfBirth = dateOfBirth,
                            emergencyContact = emergencyContact,
                            medicalConditions = medicalConditions,
                            currentMedications = currentMedications
                        )

                        if (firstName.contains(query, ignoreCase = true) ||
                            lastName.contains(query, ignoreCase = true) ||
                            dateOfBirth.contains(query, ignoreCase = true)
                        ) { // check if the patient matches the search query
                            patientList.add(patient)
                        }
                    } catch (e: Exception) {
                        // handle any errors during parsing
                        Log.e("FIREBASE_ERROR", "Error parsing document: ${e.message}")
                    }
                }
                Log.d("SEARCH_DEBUG", "Found ${patientList.size} matching patients")
                adapter.updateList(patientList)
            }
            .addOnFailureListener { e ->
                // if the query fails, show an error message
                Log.e("FIREBASE_ERROR", "Error fetching patients: ${e.message}")
                Toast.makeText(this, "Error searching patients", Toast.LENGTH_SHORT).show()
            }
    }


    private fun generateKeyIfNeeded() {
        // check if the key already exists
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias("nfcKeyAlias")) {
            // if not, generate a new key
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                // set the key size and other parameters
                KeyGenParameterSpec.Builder(
                    // set the key alias and purposes
                    "nfcKeyAlias",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }


    private fun getSecretKey(): SecretKey {
        // get the secret key from the Android KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey("nfcKeyAlias", null) as SecretKey
    }

    private fun encrypt(text: String): String {
        // encrypt the text using AES encryption
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv // get the initialisation vector
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val combined = iv + encryptedBytes
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

}
