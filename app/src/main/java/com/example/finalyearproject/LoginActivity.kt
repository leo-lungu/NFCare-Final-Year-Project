package com.example.finalyearproject

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.nio.charset.StandardCharsets

class LoginActivity : AppCompatActivity() {
    // initalise the variables
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        Log.d("INTENT_DEBUG", "Intent Action: ${intent.action}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this) // get the NFC adapter
        if (!isRunningInRobolectric()) {
            // if the app is not running in Robolectric (testing), check if NFC is supported
            if (nfcAdapter == null) {
                // if NFC is not supported, show a message
                Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        loginButton.setOnClickListener {
            // when the login button is clicked, get the input values
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                // if any field is empty, show a message
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isRunningInRobolectric()) {
                // if the app is running in Robolectric (testing), show a message
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance().collection("usernameEmail")
                // get the username and email from the Firestore database
                .document(username)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val storedEmail = document.getString("email")
                        // check if the email is correct
                        if (storedEmail == email) {
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                // sign in with the email and password
                                .addOnSuccessListener {
                                    // if the sign in is successful
                                    Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()

                                    goToMainActivity() // go to the main activity
                                }
                                .addOnFailureListener {
                                    // if the sign in fails
                                    Toast.makeText(
                                        this,
                                        "One of the fields is incorrect",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            // if the email is incorrect
                            Toast.makeText(
                                this,
                                "One of the fields is incorrect",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // if the username does not exist
                        Toast.makeText(this, "One of the fields is incorrect", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener {
                    // if the Firestore query fails

                    Toast.makeText(this, "One of the fields is incorrect", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    override fun onResume() {
        // when the activity is resumed, set the intent to the current intent
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            // create a pending intent to handle NFC intents
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        // create an intent filter for NDEF_DISCOVERED
        val filters = arrayOf(
            // create an intent filter for NDEF_DISCOVERED
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                // add a category to the intent filter
                try {
                    addDataType("*/*")
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                }
            }
        )
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            filters,
            null
        ) // enable foreground dispatch
    }

    override fun onPause() {
        // when the activity is paused, disable foreground dispatch
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        // when a new intent is received, set the intent to the current intent
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("INTENT_DEBUG", "onNewIntent called with: ${intent.action}")

        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) {
            Log.d("INTENT_DEBUG", "Ignoring non-NDEF intent: ${intent.action}")
            return
        }

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        // check if the tag is null
        if (tag == null) {
            // if the tag is null, show a message
            Log.d("INTENT_DEBUG", "No tag found in intent")
            Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
            return
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            // if the tag is NDEF formatted, read the NDEF message
            try {
                ndef.connect() // connect to the tag
                val message = ndef.ndefMessage
                if (message != null && message.records.isNotEmpty()) {
                    // if the message is not null and has records, read the first record
                    val record = message.records[0]
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(
                            NdefRecord.RTD_TEXT
                        )
                    ) {
                        // if the record is a text record, read the payload
                        val payload = record.payload
                        val textEncoding =
                            if (payload[0].toInt() and 128 == 0) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
                        val languageCodeLength = payload[0].toInt() and 63
                        val username = String(
                            // create a string from the payload
                            payload,
                            languageCodeLength + 1,
                            payload.size - languageCodeLength - 1,
                            textEncoding
                        )
                        usernameInput.setText(username)
                        Toast.makeText(this, "Username scanned: $username", Toast.LENGTH_SHORT)
                            .show() // show a message
                    } else {
                        Log.d("INTENT_DEBUG", "NDEF record is not text")
                        Toast.makeText(this, "NFC tag contains non-text data", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.d("INTENT_DEBUG", "No NDEF message found")
                    Toast.makeText(this, "Empty NFC tag", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("INTENT_DEBUG", "Error reading NDEF: ${e.message}")
                Toast.makeText(this, "Error reading NFC tag", Toast.LENGTH_SHORT).show()
            } finally {
                // close the NDEF connection
                try {
                    ndef.close()
                } catch (_: Exception) {
                }
            }
        } else {
            Log.d("INTENT_DEBUG", "Tag is not NDEF formatted")
            Toast.makeText(this, "Please use an NDEF-formatted tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToMainActivity() {
        // when the login is successful, go to the main activity
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent) // start the main activity
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch main screen: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e("LOGIN_REDIRECT", "Redirect Failed: ${e.stackTraceToString()}")
        }
    }

    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric (testing)
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }


}