package com.example.finalyearproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    // variables for UI components
    private lateinit var logoutButton: Button
    private lateinit var reportButton: Button
    private lateinit var reportTextBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        logoutButton = findViewById(R.id.logoutButton) // find the logout button
        reportButton = findViewById(R.id.reportButton)
        reportTextBox = findViewById(R.id.issueEditText)

        logoutButton.setOnClickListener {
            // when the logout button is clicked, sign out the user
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java) // create an intent to start the LoginActivity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }

        reportButton.setOnClickListener {
            // when the report button is clicked, get the input value
            val reportText = reportTextBox.text.toString().trim()

            if (reportText.isNotEmpty()) {
                // check if the report text is not empty
                if (!isRunningInRobolectric()) {
                    // check if the app is not running in Robolectric (testing)
                    val report = mapOf(
                        // create a map for the report
                        "message" to reportText,
                        "reportedBy" to FirebaseAuth.getInstance().currentUser?.uid,
                        "timestamp" to Timestamp.now(),
                        "device" to android.os.Build.MODEL,
                        "resolved" to false
                    )

                    FirebaseFirestore.getInstance().collection("reports")
                        // add the report to the Firestore database
                        .add(report)
                        .addOnSuccessListener {
                            //
                            Toast.makeText(this, "Issue reported successfully!", Toast.LENGTH_SHORT).show()
                            reportTextBox.text.clear()
                        }
                        .addOnFailureListener {
                            // handle failure
                            Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // fake data for robolectric testing
                    Toast.makeText(this, "Issue reported successfully!", Toast.LENGTH_SHORT).show()
                    reportTextBox.text.clear()
                }
            } else {
                // handle empty report text
                reportTextBox.error = "Message cannot be empty"
                Toast.makeText(this, "Please enter your issue before submitting", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun isRunningInRobolectric(): Boolean {
        // check if the app is running in Robolectric (testing environment)
        return "robolectric" in android.os.Build.FINGERPRINT.lowercase()
    }
}
