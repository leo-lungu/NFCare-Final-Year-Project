package com.example.finalyearproject

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.nfc.*
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.SecretKey
import java.security.KeyStore
import android.util.Base64


class MainActivity : AppCompatActivity() {

    // initalise the variables
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcTextView: TextView
    private lateinit var patientName: TextView
    private lateinit var patientDob: TextView
    private lateinit var patientGender: TextView
    private lateinit var patientEmergencyContact: TextView
    private lateinit var patientMedicalConditions: TextView
    private lateinit var patientBloodType: TextView
    private lateinit var patientMedications: TextView
    private lateinit var patientAllergies: TextView
    private lateinit var patientDoctorId: TextView
    private lateinit var patientActionButtons: LinearLayout
    private lateinit var editPatientButton: Button
    private lateinit var administerPatientButton: Button
    private lateinit var db: FirebaseFirestore
    private var isRemovingNfc = false
    private var isAdministeringMedication = false
    private var reason: String = ""
    private lateinit var prescriptionButton: Button
    private var currentPatientId: String? = null
    private lateinit var callEmergencyContactButton: Button
    private var currentEmergencyContactNumber: String? = null
    private lateinit var removeNfcButton: Button
    private lateinit var exportPdfButton: Button

    private var patientAllergiesList = emptyList<String>() // list to store patient allergies


    private val sessionTimeout: Long = 1 * 60 * 1000L // 1 minute timeout for security
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeoutRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // initialise the variables
        nfcTextView = findViewById(R.id.nfcTextView)
        patientName = findViewById(R.id.patientName)
        patientDob = findViewById(R.id.patientDob)
        patientGender = findViewById(R.id.patientGender)
        patientEmergencyContact = findViewById(R.id.patientEmergencyContact)
        patientMedicalConditions = findViewById(R.id.patientMedicalConditions)
        patientBloodType = findViewById(R.id.patientBloodType)
        patientAllergies = findViewById(R.id.allergies)
        patientMedications = findViewById(R.id.patientMedications)
        patientDoctorId = findViewById(R.id.patientDoctorId)
        patientActionButtons = findViewById(R.id.patientActionButtons)
        editPatientButton = findViewById(R.id.editPatientButton)
        administerPatientButton = findViewById(R.id.administerPatientButton)
        prescriptionButton = findViewById(R.id.prescriptionButton)
        callEmergencyContactButton = findViewById(R.id.callEmergencyContactButton)
        removeNfcButton = findViewById(R.id.removeNfcButton)
        exportPdfButton = findViewById(R.id.exportPdfButton)



        db = FirebaseFirestore.getInstance() // initialise the Firestore instance
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            // if NFC is not supported, show a message and finish the activity
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
        }

        timeoutRunnable = Runnable {
            // if the session times out, show a message and log out the user
            Toast.makeText(this, "Session timed out!", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupUserActivityListener()

        editPatientButton.setOnClickListener {
            // edit patient button listener
            resetSessionTimer()
            val rawText = nfcTextView.text.toString() // get text from nfc

            val nhsId = if (rawText.startsWith("Scanned NHS ID: ")) { // extract nhs id
                rawText.replace("Scanned NHS ID: ", "")
            } else {
                // no nhs id
                Toast.makeText(this, "Please scan patient NFC card first", Toast.LENGTH_SHORT)
                    .show()

                return@setOnClickListener
            }

            if (nhsId.isBlank()) {
                // if no NHS id
                Toast.makeText(this, "Invalid NHS ID, please scan again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, EditPatientActivity::class.java)
            intent.putExtra("NHS_ID", nhsId)
            startActivity(intent)
        }

        administerPatientButton.setOnClickListener {
            // administer patient button listener
            resetSessionTimer()
            Toast.makeText(this, "Administer Patient clicked", Toast.LENGTH_SHORT).show()
            isAdministeringMedication = true
            Toast.makeText(this, "Waiting to scan medication box NFC...", Toast.LENGTH_SHORT).show()
            nfcTextView.text = "Waiting for medication box scan..."

        }

        prescriptionButton.setOnClickListener {
            // prescription button listener
            resetSessionTimer() // reset session timer
            val rawText = nfcTextView.text.toString() // get text from nfc

            val nhsId: String = if (rawText.startsWith("Scanned NHS ID: ")) {
                // extract nhs id

                rawText.replace("Scanned NHS ID: ", "")
            } else {
                // no nhs id
                Toast.makeText(this, "Please scan patient NFC Wristband first", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (nhsId.isBlank()) {
                // if no NHS id
                Toast.makeText(this, "Invalid NHS ID, please scan again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(
                this,
                PrescriptionActivity::class.java
            ) // create intent for prescription activity
            intent.putExtra("NHS_ID", nhsId)
            startActivity(intent)
        }

        callEmergencyContactButton.setOnClickListener {
            // call emergency contact button listener
            resetSessionTimer() // reset session timer
            val number = currentEmergencyContactNumber
            if (!number.isNullOrBlank()) {
                // if number is not null or blank
                val intent = Intent(Intent.ACTION_DIAL).apply { // create intent for dialer
                    data = Uri.parse("tel:$number") // set data to number
                }
                startActivity(intent)
            } else {
                // if no number
                Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        removeNfcButton.setOnClickListener {
            // remove nfc button listener
            resetSessionTimer()
            if (currentPatientId == null) {
                // if no patient id
                Toast.makeText(this, "No patient currently loaded.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isRemovingNfc = true // set removing nfc to true
            nfcTextView.text = "Please scan the patient's NFC wristband again to confirm removal."
            Toast.makeText(this, "Scan the wristband again to confirm removal.", Toast.LENGTH_LONG)
                .show()
        }

        exportPdfButton.setOnClickListener {
            // export pdf button listener
            resetSessionTimer()
            exportPatientToPdf()
        }


        val bottomNavigationView =
            findViewById<BottomNavigationView>(R.id.bottomNavigationView) // find bottom navigation view
        bottomNavigationView.setOnItemSelectedListener { item ->
            // set item selected listener
            when (item.itemId) {
                R.id.nav_home -> {
                    // navigate to home activity
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    clearPatientData() // clear patient data when navigating to home
                    nfcTextView.text = "Scan an NFC tag"
                    true
                }

                R.id.nav_medication -> {
                    // navigate to medication activity
                    Toast.makeText(this, "Medication clicked", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MedicationActivity::class.java))
                    true
                }

                R.id.nav_write -> {
                    // navigate to write nfc activity
                    startActivity(Intent(this, WriteNfcActivity::class.java))
                    true
                }

                R.id.nav_add -> {
                    // navigate to add patient activity
                    startActivity(Intent(this, AddPatientActivity::class.java))
                    true
                }

                R.id.nav_settings -> {
                    // navigate to settings activity
                    startActivity(Intent(this, SettingsActivity::class.java))
                    Log.d("NAVIGATION", "Settings clicked")
                    true
                }

                else -> false
            }
        }

        Log.d("FIREBASE_INIT", "Firestore initialized successfully")

    }

    override fun onResume() {
        // when the activity is resumed, set the content view to the layout file
        super.onResume()
        Log.d("SESSION_TIMER", "onResume called, resetting session timer")
        resetSessionTimer()

        val intent = Intent(this, javaClass).apply {
            //
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            // create a pending intent for the activity
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val filters = arrayOf(
            // create intent filters for NFC
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )

        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            filters,
            null
        ) // enable foreground dispatch for NFC
    }

    override fun onPause() {
        // when the activity is paused, set the content view to the layout file
        super.onPause()
        handler.removeCallbacks(timeoutRunnable)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun resetSessionTimer() {
        // reset the session timer - this is called when the user interacts with the app
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, sessionTimeout)
    }

    override fun onNewIntent(intent: Intent) {
        // when a new intent is received, set the content view to the layout file
        super.onNewIntent(intent)
        setIntent(intent)

        Log.d("NFC_DEBUG", "NFC Tag Scanned - Intent Action: ${intent.action}")
        Log.d("NFC_STATE", "isAdministeringMedication: $isAdministeringMedication")

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        Log.d("NFC_DEBUG", "Tag: $tag")
        if (tag == null) {
            // if no tag is found
            Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
            return
        }

        val scannedId = readNfcTag(intent) // read the NFC tag

        if (scannedId.startsWith("ERROR")) {
            // if there is an error reading the NFC tag
            Toast.makeText(this, "Error reading NFC", Toast.LENGTH_SHORT).show()
            nfcTextView.text = "Error reading NFC tag"
            return
        }

        if (isAdministeringMedication) {
            // if administering medication
            Log.d("NFC_FLOW", "isAdministeringMedication = true. Scanned ID: $scannedId")
            isAdministeringMedication = false
            Log.d("NFC_FLOW", "Starting medication box scan for ID: $scannedId")
            handleMedicationBoxScan(scannedId)
            Log.d("NFC_FLOW", "Finished handleMedicationBoxScan call")
        } else if (isRemovingNfc) {
            // if removing NFC
            isRemovingNfc = false
            if (scannedId == currentPatientId) {
                // if the scanned ID matches the current patient ID
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                if (tag != null) {
                    clearNfcTag(tag)
                }
                currentPatientId = null
                nfcTextView.text = "Scan an NFC tag"
                clearPatientData()
                Toast.makeText(this, "NFC tag cleared and patient unlinked.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    this,
                    "Wristband does not match the current patient.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // if not administering medication or removing NFC
            currentPatientId = scannedId
            nfcTextView.text = "Scanned NHS ID: $scannedId"
            Toast.makeText(this, "Scanned NHS ID: $scannedId", Toast.LENGTH_SHORT).show()
            fetchPatientData(scannedId)
        }
    }

    private fun setupUserActivityListener() {
        // set up user activity listener
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, _ ->
            // when the user touches the screen, reset the session timer
            resetSessionTimer()
            false
        }
    }

    private fun readNfcTag(intent: Intent): String {
        // read the NFC tag and return the NHS ID
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        Log.d("NFC_TAG", "Raw messages = $rawMsgs")
        if (!rawMsgs.isNullOrEmpty()) {
            // if there are messages in the NFC tag
            val ndefMessage = rawMsgs[0] as NdefMessage
            val ndefRecord = ndefMessage.records.firstOrNull {
                // find the NDEF record with the well-known type and text type
                it.tnf == NdefRecord.TNF_WELL_KNOWN &&
                        it.type.contentEquals(NdefRecord.RTD_TEXT)
            }
            Log.d("NFC_TAG", "NDEF record = $ndefRecord")

            if (ndefRecord != null) {
                // if the NDEF record is not null
                val payload = ndefRecord.payload
                val encoding =
                    if ((payload[0].toInt() and 128) == 0) Charset.forName("UTF-8") else Charset.forName(
                        "UTF-16"
                    )
                val langCodeLen = payload[0].toInt() and 63
                val textLength = payload.size - langCodeLen - 1

                return if (textLength > 0) {
                    // if the text length is greater than 0
                    val rawText = String(payload, langCodeLen + 1, textLength, encoding)
                    Log.d("NFC_TAG", "Encrypted text: $rawText")
                    return try {
                        // only decrypt if scanning wristband (not administering medication)
                        if (isAdministeringMedication) {
                            rawText // dont decrypt if administering medication
                        } else {
                            val decrypted = decrypt(rawText)
                            Log.d("NFC_TAG", "Decrypted text: $decrypted")
                            decrypted
                        }
                    } catch (e: Exception) {
                        Log.e("NFC_TAG", "Decryption failed: ${e.message}")
                        "ERROR: Failed to decrypt NFC data"
                    }
                } else {
                    "ERROR: Empty NFC tag"
                }
            }
        }
        return "ERROR: No valid NFC data found"
    }

    private fun fetchPatientData(nhsId: String) {
        // fetch patient data from Firestore
        db.collection("patients").document(nhsId)
            // get the document with the NHS ID
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // if the document exists
                    val firstName = doc.getString("firstName") ?: "Unknown"
                    val lastName = doc.getString("lastName") ?: "Unknown"

                    val dobValue = doc.get("dob")
                    val dateOfBirth = when (dobValue) {
                        // check the type of dobValue and format accordingly
                        is String -> dobValue
                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                            dobValue.toDate()
                        )

                        else -> "Unknown"
                    }

                    // get other patient data
                    val gender = doc.getString("gender") ?: "Unknown"
                    val bloodType = doc.getString("bloodType") ?: "Unknown"
                    val doctorId = doc.getString("doctorId") ?: "Unknown"
                    val contactName = doc.getString("emergencyContact") ?: "None"
                    val contactNumber = doc.getString("emergencyContactNumber") ?: ""

                    currentEmergencyContactNumber = contactNumber // store the contact number

                    // get medical conditions and current medications
                    val medicalConditions =
                        doc.get("medicalConditions") as? List<String> ?: emptyList()
                    val currentMedications =
                        doc.get("currentMedications") as? List<String> ?: emptyList()

                    if (currentMedications.isNotEmpty()) {
                        // if there are current medications
                        val medicationNames = mutableListOf<String>()
                        var fetchedCount = 0

                        for (medId in currentMedications) {
                            // for each medication ID, fetch the medication name
                            db.collection("medications").document(medId).get()
                                // get the document with the medication ID
                                .addOnSuccessListener { medDoc ->
                                    fetchedCount++ // increment the fetched count
                                    val name = medDoc.getString("name")
                                        ?: medId // fallback to ID if name not found
                                    medicationNames.add(name)

                                    // Once all medications are fetched
                                    if (fetchedCount == currentMedications.size) { // if all medications are fetched
                                        patientMedications.text =
                                            "Current Medications: ${medicationNames.joinToString(", ")}"
                                        patientMedications.visibility = View.VISIBLE
                                    }
                                }
                                .addOnFailureListener {
                                    fetchedCount++ // increment the fetched count
                                    Log.e(
                                        "FETCH_MEDICATION",
                                        "Failed to fetch medication $medId: ${it.message}"
                                    )

                                    if (fetchedCount == currentMedications.size) { // if all medications are fetched
                                        patientMedications.text =
                                            "Current Medications: ${medicationNames.joinToString(", ")}"
                                        patientMedications.visibility = View.VISIBLE
                                    }
                                }
                        }
                    } else {
                        // if there are no current medications
                        patientMedications.text = "Current Medications: None"
                        patientMedications.visibility = View.VISIBLE
                    }
                    // set the patient data to the text views
                    currentEmergencyContactNumber = contactNumber
                    patientName.text = "Name: $firstName $lastName"
                    patientName.visibility = View.VISIBLE
                    patientDob.text = "Date of Birth: $dateOfBirth"
                    patientDob.visibility = View.VISIBLE
                    patientGender.text = "Gender: $gender"
                    patientGender.visibility = View.VISIBLE
                    patientEmergencyContact.text =
                        "Emergency Contact:\n$contactName: $contactNumber"
                    patientEmergencyContact.visibility = View.VISIBLE
                    patientMedicalConditions.text = "Medical Conditions: ${
                        // if there are medical conditions
                        if (medicalConditions.isNotEmpty()) medicalConditions.joinToString(", ") else "None"
                    }"
                    patientMedicalConditions.visibility = View.VISIBLE
                    patientBloodType.text = "Blood Type: $bloodType"
                    patientBloodType.visibility = View.VISIBLE
                    patientMedications.text = "Current Medications: ${
                        // if there are current medications
                        if (currentMedications.isNotEmpty()) currentMedications.joinToString(", ") else "None"
                    }"
                    patientMedications.visibility = View.VISIBLE
                    patientDoctorId.text = "Doctor ID: $doctorId"
                    patientDoctorId.visibility = View.VISIBLE

                    findViewById<LinearLayout>(R.id.patientInfoSection).visibility = View.VISIBLE

                    // show action buttons
                    patientActionButtons.visibility = View.VISIBLE
                    prescriptionButton.visibility = View.VISIBLE
                    callEmergencyContactButton.visibility = View.VISIBLE

                    db.collection("medicationSchedules")
                        // check if the patient has any prescriptions
                        .whereEqualTo("userId", nhsId)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                // if there are no prescriptions
                                administerPatientButton.isEnabled = false
                                administerPatientButton.alpha = 0.5f // disable the button
                            } else {
                                // if there are prescriptions
                                administerPatientButton.isEnabled = true
                                administerPatientButton.alpha = 1.0f
                            }
                        }
                        .addOnFailureListener {
                            // if failed to check prescriptions
                            Log.e("FIREBASE_ERROR", "Failed to check prescriptions: ${it.message}")
                            administerPatientButton.isEnabled = false
                            administerPatientButton.alpha = 0.5f
                        }

                    patientAllergiesList =
                        doc.get("allergies") as? List<String> ?: emptyList() // get
                    patientAllergies.text = "Allergies: ${
                        // if there are allergies
                        if (patientAllergiesList.isNotEmpty()) patientAllergiesList.joinToString(", ") else "None"
                    }"
                } else {
                    // if the document does not exist
                    nfcTextView.text = "No record found for NHS ID: $nhsId"
                    clearPatientData()
                }
            }
            .addOnFailureListener {
                nfcTextView.text = "Failed to fetch patient data."
                clearPatientData()
                Log.e("FIREBASE_ERROR", "Error: ${it.message}")
            }

    }

    private fun handleMedicationBoxScan(boxId: String) {
        // handle the medication box scan
        Log.d("MED_SCAN", "Scanning box with ID: $boxId")

        db.collection("medicationSpecific").document(boxId)
            // get the document with the box ID
            .get()
            .addOnSuccessListener { doc ->
                Log.d("MED_SCAN", "Fetched medication box document: exists=${doc.exists()}")
                if (doc.exists()) {
                    // if the document exists

                    val name = doc.getString("name") ?: "Unknown"
                    val batch = doc.getString("batchNumber") ?: "Unknown"
                    val quantity = doc.getLong("quantity")?.toInt() ?: 0
                    val expiration =
                        doc.getTimestamp("expirationDate")?.toDate()?.toString() ?: "Unknown"
                    val medId = doc.getString("medicationId") ?: "Unknown"

                    val medicationBox = MedicationSpecific(
                        // create a MedicationSpecific object
                        id = boxId,
                        medicationId = medId,
                        boxName = name,
                        expirationDate = expiration,
                        quantity = quantity,
                        batchNumber = batch
                    )

                    val nhsId = currentPatientId ?: ""
                    Log.d("MED_SCAN", "Current patient ID: $nhsId")
                    val scheduleId = "${nhsId}_${medId}" // create schedule ID

                    db.collection("medications").document(medId)
                        // get the document with the medication ID
                        .get()
                        .addOnSuccessListener { medicationDoc ->
                            // if the document exists
                            if (medicationDoc.exists()) {
                                // if the document exists

                                val medAllergens = medicationDoc.get("allergens") as? List<String>
                                    ?: emptyList() // get allergens

                                Log.d("DEBUG_ALLERGENS", "Patient allergies: $patientAllergiesList")
                                Log.d("DEBUG_ALLERGENS", "Medication allergens: $medAllergens")

                                val patientSet = patientAllergiesList.map { it.trim().lowercase() }
                                    .toSet() // allergies set
                                val medSet = medAllergens.map { it.trim().lowercase() }.toSet()

                                val commonAllergens = patientSet.intersect(medSet)

                                val continueToScheduleCheck = {
                                    // continue to schedule check
                                    db.collection("medicationSchedules").document(scheduleId)
                                        // get the document with the schedule ID
                                        .get()
                                        .addOnSuccessListener { scheduleDoc ->
                                            if (scheduleDoc.exists()) {
                                                // if the document exists

                                                // get schedule details, doses, and warnings
                                                val dosesPerDay =
                                                    scheduleDoc.getLong("dosesPerDay")?.toInt() ?: 1
                                                val lastAdmin = try {
                                                    scheduleDoc.getTimestamp("lastAdministered")?.toDate()
                                                } catch (e: Exception) {
                                                    Log.e("DATA_TYPE_ERROR", "Invalid type for 'lastAdministered': ${e.message}")
                                                    null
                                                }
                                                val dosesTakenToday =
                                                    scheduleDoc.getLong("dosesTakenToday")?.toInt()
                                                        ?: 0
                                                val mustBeChewed =
                                                    scheduleDoc.getBoolean("mustBeChewed") == true
                                                val takeWithFood =
                                                    scheduleDoc.getBoolean("takeWithFood") == true

                                                val now = Date()
                                                val calendarNow = Calendar.getInstance()
                                                val calendarLast = Calendar.getInstance()
                                                calendarLast.time = lastAdmin ?: Date(0)

                                                val sameDay =
                                                    // check if the last admin date is the same as today
                                                    calendarNow.get(Calendar.YEAR) == calendarLast.get(
                                                        Calendar.YEAR
                                                    ) &&
                                                            calendarNow.get(Calendar.DAY_OF_YEAR) == calendarLast.get(
                                                        Calendar.DAY_OF_YEAR
                                                    )

                                                val effectiveDosesTaken =
                                                    if (sameDay) dosesTakenToday else 0
                                                val maxDosesReached =
                                                    effectiveDosesTaken >= dosesPerDay

                                                // calculate the minimum spacing between doses
                                                val minSpacingMs =
                                                    ((16f / dosesPerDay) * 60 * 60 * 1000).toLong()
                                                val enoughTimePassed =
                                                    lastAdmin == null || (now.time - lastAdmin.time) >= minSpacingMs

                                                val warnings = mutableListOf<String>() // list to store warnings

                                                if (maxDosesReached) {
                                                    // if the maximum doses have been reached
                                                    warnings.add("All scheduled doses have already been taken today.")
                                                }


                                                if (!enoughTimePassed) {
                                                    // check if enough time has passed since the last dose
                                                    val waitMinutes =
                                                        ((minSpacingMs - (now.time - (lastAdmin?.time
                                                            ?: 0))) / 60000).toInt()
                                                    warnings.add("Too soon for next dose. Wait at least $waitMinutes more minutes.")
                                                }


                                                if (mustBeChewed) {
                                                    // check if the medication must be chewed or taken with food
                                                    warnings.add("This medication must be chewed before swallowing.")

                                                }

                                                if (takeWithFood) {
                                                    // check if the medication should be taken with food
                                                    warnings.add("This medication should be taken with food.")

                                                }

                                                if (warnings.isNotEmpty()) {
                                                    // if there are warnings
                                                    val message = warnings.joinToString("\n\n")
                                                    runOnUiThread {
                                                        // show the warnings in an alert dialog
                                                        AlertDialog.Builder(this)
                                                            .setTitle("Medication Advisory")
                                                            .setMessage(message)
                                                            .setPositiveButton("Proceed") { dialog, _ ->
                                                                // if the user clicks proceed
                                                                dialog.dismiss()
                                                                showMedicationDialog(
                                                                    // show the medication dialog
                                                                    medicationBox,
                                                                    scheduleId
                                                                )
                                                            }
                                                            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                                                            .show()
                                                    }
                                                } else {
                                                    // if there are no warnings. show the medication dialog
                                                    showMedicationDialog(medicationBox, scheduleId)
                                                }
                                            } else {
                                                Toast.makeText(
                                                    // if no prescription found
                                                    this,
                                                    "No prescription found for this patient and medication.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                AlertDialog.Builder(this)
                                                    .setTitle("Prescription Not Found")
                                                    .setMessage("This patient does not have a valid prescription for this medication. Administration cannot proceed.")
                                                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                                    .show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                this,
                                                "Failed to fetch medication schedule: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        }
                                }

                                if (commonAllergens.isNotEmpty()) {
                                    // if there are common allergens

                                    // show an alert dialog with the allergens
                                    val allergenWarningMessage = "Patient allergic to: ${commonAllergens.joinToString(", ")}.\n\nProceed anyway?"
                                    AlertDialog.Builder(this)
                                        .setTitle("Allergy Warning")
                                        .setMessage(allergenWarningMessage)
                                        .setPositiveButton("Proceed") { dialog, _ ->
                                            dialog.dismiss()
                                            continueToScheduleCheck()
                                        }
                                        .setNegativeButton("Cancel") { dialog, _ ->
                                            dialog.dismiss()
                                            Toast.makeText(
                                                this,
                                                "Medication administration cancelled due to allergy risk.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        .show()
                                } else {
                                    // if there are no common allergens
                                    continueToScheduleCheck()
                                }
                            } else {
                                // if no medication info found
                                Toast.makeText(
                                    this,
                                    "Medication info not found.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                // if failed to fetch medication info
                                this,
                                "Failed to fetch medication info: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // if no box found
                    Toast.makeText(this, "No box found with ID: $boxId", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                // if failed to fetch box
                Toast.makeText(
                    this,
                    "Failed to fetch medication box: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showMedicationDialog(box: MedicationSpecific, scheduleId: String) {
        // show the medication dialog
        val message = """
    Name: ${box.boxName}
    Batch: ${box.batchNumber}
    Expiry: ${box.expirationDate}
    Quantity: ${box.quantity}
    """.trimIndent()

        AlertDialog.Builder(this) // create an alert dialog
            .setTitle("Administer Medication")
            .setMessage(message)
            .setPositiveButton("Administer") { _, _ ->
                // Pass the scheduleId to the logMedicationAdministration function
                logMedicationAdministration(box, scheduleId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logMedicationAdministration(box: MedicationSpecific, scheduleId: String) {
        // log the medication administration
        val scheduleRef = db.collection("medicationSchedules").document(scheduleId)

        db.runTransaction { transaction ->
            // run a transaction to update the medication schedule
            val snapshot = transaction.get(scheduleRef)

            val calendarNow = Calendar.getInstance()
            val now = calendarNow.time

            val lastAdmin: Date? = try {
                snapshot.getTimestamp("lastAdministered")?.toDate()
            } catch (e: Exception) {
                Log.e("DATA_TYPE_ERROR", "Invalid 'lastAdministered' type: ${e.message}")
                null
            }
            val dosesTakenToday = snapshot.getLong("dosesTakenToday") ?: 0

            val calendarLast = Calendar.getInstance()
            calendarLast.time = lastAdmin ?: Date(0)

            val sameDay = calendarNow.get(Calendar.YEAR) == calendarLast.get(Calendar.YEAR) &&
                    calendarNow.get(Calendar.DAY_OF_YEAR) == calendarLast.get(Calendar.DAY_OF_YEAR)

            val updatedDosesToday = if (sameDay) dosesTakenToday + 1 else 1

            transaction.update(
                // update the medication schedule
                scheduleRef, mapOf(
                    "lastAdministered" to Timestamp(now),
                    "dosesTakenToday" to updatedDosesToday
                )
            )
        }.addOnSuccessListener {
            // if the transaction is successful
            Log.d("FIREBASE_DEBUG", "Medication schedule updated successfully.")

            //  update the medication box quantity
            updateMedicationBoxQuantity(box)
        }.addOnFailureListener { e ->
            Log.e("FIREBASE_ERROR", "Error updating medication schedule: ${e.message}")
            Toast.makeText(
                this,
                "Failed to update medication schedule: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateMedicationBoxQuantity(box: MedicationSpecific) {
        // update the medication box quantity
        val boxRef = db.collection("medicationSpecific").document(box.id)

        db.runTransaction { transaction ->
            // run a transaction to update the medication box
            val snapshot = transaction.get(boxRef)
            val currentQuantity = snapshot.getLong("quantity") ?: 0 // get current quantity

            val expirationDate = snapshot.getTimestamp("expirationDate")?.toDate() // get expiration date
            val currentDate = Date()
            if (expirationDate != null && expirationDate.before(currentDate)) {
                // if the medication has expired
                throw FirebaseFirestoreException(
                    "MEDICATION HAS EXPIRED",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            if (currentQuantity > 0) {
                // if the current quantity is greater than 0
                transaction.update(boxRef, "quantity", currentQuantity - 1)

                Log.d(
                    "TRANSACTION",
                    "Preparing to update quantity. CurrentQuantity=$currentQuantity"
                )
                Log.d("TRANSACTION", "Returning map with remainingQuantity=${currentQuantity - 1}")
                return@runTransaction mapOf(
                    // return a map with the remaining quantity
                    "remainingQuantity" to (currentQuantity - 1),
                    "boxId" to box.id,
                    "boxName" to box.boxName
                )
            } else {
                // if the current quantity is 0
                throw FirebaseFirestoreException(
                    "Medication quantity is already zero",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }
        }.addOnSuccessListener { result ->
            // if the transaction is successful
            try {
                Log.d("TRANSACTION", "Transaction completed. Result=$result")
                val resultMap = result as? Map<String, Any>
                Log.d("TRANSACTION", "ResultMap = $resultMap")
                if (resultMap == null) {
                    Toast.makeText(
                        // if the result is not a map
                        this,
                        "Unexpected error occurred. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("TRANSACTION_RESULT", "Result was not a map: $result")
                    return@addOnSuccessListener
                }

                val logData = mapOf(
                    // create a log data map
                    "boxId" to resultMap["boxId"],
                    "boxName" to resultMap["boxName"],
                    "timestamp" to Timestamp.now(),
                    "administeredBy" to FirebaseAuth.getInstance().currentUser?.uid,
                    "remainingQuantity" to resultMap["remainingQuantity"],
                    "patientId" to currentPatientId,
                )

                val documentId = "${currentPatientId}_${System.currentTimeMillis()}"

                db.collection("administrationLogs")
                    // log the medication administration
                    .document(documentId)
                    .set(logData)
                    .addOnSuccessListener {
                        // if the log is successful
                        Toast.makeText(
                            this,
                            "Medication administered and quantity updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        // if failed to log the medication administration
                        Toast.makeText(
                            this,
                            "Logged quantity change, but failed to write log.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: Exception) {
                // if there is an error processing the result
                Toast.makeText(this, "Crash in result processing: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                Log.e("LOGGING_CRASH", "Exception: ", e)
            }
        }.addOnFailureListener { e ->
            Log.e("TRANSACTION", "Transaction failed: ${e.message}", e)

            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            // have an alert dialog to show the error message and confirm
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Expired Medication")
                    .setMessage("Medication has expired. Please check the box, and dispose of it.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
                vibrate() // vibrate as error happens to alert user
            }

            val logData = mapOf(
                // create a log data map
                "boxId" to box.id,
                "boxName" to box.boxName,
                "timestamp" to Timestamp.now(),
                "administeredBy" to FirebaseAuth.getInstance().currentUser?.uid,
                "eventType" to "FAILURE",
                "patientId" to currentPatientId,
                "failureReason" to reason
            )

            val documentId = "${currentPatientId}_${System.currentTimeMillis()}"

            db.collection("administrationLogs")
                // log the medication administration failure
                .document(documentId)
                .set(logData)
                .addOnSuccessListener {
                    Log.d(
                        "FIREBASE_DEBUG",
                        "Medication administration failure logged within database."
                    )
                }
                .addOnFailureListener {
                    // if failed to log the medication administration failure
                    Log.d(
                        "FIREBASE_DEBUG",
                        "Unable to log medication administration failure: ${it.message}"
                    )
                }
        }
    }

    private fun clearNfcTag(tag: Tag) {
        // clear the NFC tag
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                //
                ndef.connect()
                val emptyMessage = NdefMessage(
                    arrayOf(
                        NdefRecord.createTextRecord("en", "")
                    )
                )
                ndef.writeNdefMessage(emptyMessage)
                ndef.close()
                Toast.makeText(this, "NFC tag cleared successfully.", Toast.LENGTH_SHORT).show()
                Log.d("NFC_REMOVE", "NFC tag cleared")
            } else {
                // if NDEF is not supported
                Toast.makeText(this, "NDEF is not supported on this tag.", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            // if there is an error clearing the NFC tag
            Toast.makeText(this, "Failed to clear NFC tag: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("NFC_REMOVE", "Error clearing NFC tag", e)
        }
    }


    private fun clearPatientData() {
        // clear the patient data
        patientName.visibility = View.GONE
        patientDob.visibility = View.GONE
        patientGender.visibility = View.GONE
        patientEmergencyContact.visibility = View.GONE
        patientMedicalConditions.visibility = View.GONE
        patientAllergies.visibility = View.GONE
        patientBloodType.visibility = View.GONE
        patientMedications.visibility = View.GONE
        patientDoctorId.visibility = View.GONE
        patientActionButtons.visibility = View.GONE // hide the action buttons
    }

    private fun vibrate() {
        // vibrate the device
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(500)
    }

    private fun exportPatientToPdf() {
        // export the patient data to a PDF
        if (currentPatientId == null) {
            Toast.makeText(this, "No patient loaded to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            // set up the paint for drawing text
            textSize = 14f
        }

        var y = 50

        fun writeLine(text: String) {
            // write a line of text to the PDF
            canvas.drawText(text, 40f, y.toFloat(), paint)
            y += 25
        }

        // write the patient data to the PDF
        writeLine("Patient Summary - NHS ID: $currentPatientId")
        writeLine(patientName.text.toString())
        writeLine(patientDob.text.toString())
        writeLine(patientGender.text.toString())
        writeLine(patientBloodType.text.toString())
        writeLine(patientAllergies.text.toString())
        writeLine(patientEmergencyContact.text.toString())
        writeLine(patientMedicalConditions.text.toString())
        writeLine(patientMedications.text.toString())
        writeLine(patientDoctorId.text.toString())

        pdfDocument.finishPage(page)

        // save the PDF to the downloads folder
        val filename = "Patient_$currentPatientId.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)

        try {
            // write the PDF to the file
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF saved to Downloads!", Toast.LENGTH_LONG).show()
            Log.d("PDF_EXPORT", "PDF saved to: ${file.absolutePath}")

            // open the PDF using FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            // set up the intent to open the PDF
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(openIntent)

        } catch (e: Exception) {
            // if there is an error exporting the PDF
            Toast.makeText(this, "Error exporting PDF: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("PDF_EXPORT", "Error: ", e)
        } finally {
            pdfDocument.close()
        }
    }

    private fun getSecretKey(): SecretKey {
        // get the secret key from the Android KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey("nfcKeyAlias", null) as SecretKey
    }

    private fun decrypt(encryptedData: String): String {
        // decrypt the encrypted data
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)

        // extract the IV and encrypted bytes
        val iv = decoded.copyOfRange(0, 12)
        val encryptedBytes = decoded.copyOfRange(12, decoded.size)

        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val originalBytes = cipher.doFinal(encryptedBytes)

        return String(originalBytes, Charsets.UTF_8) // convert to string
    }


}