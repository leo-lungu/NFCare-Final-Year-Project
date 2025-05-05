package com.example.finalyearproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MedicationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // when the activity is created, set the content view to the layout file
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication)

        // inputs from the layout file for medication name, dosage, and description
        val addMedicationButton = findViewById<Button>(R.id.addMedicationButton)
        val writeMedicationButton = findViewById<Button>(R.id.writeMedicationButton)
        val backButton = findViewById<Button>(R.id.backButton)
        val viewMedicationButton = findViewById<Button>(R.id.viewMedicationButton)

        addMedicationButton.setOnClickListener {
            // when the add medication button is clicked, start the AddMedicationActivity
            startActivity(Intent(this, AddMedicationActivity::class.java))
        }

        writeMedicationButton.setOnClickListener {
            // when the write medication button is clicked, start the WriteMedicationActivity
            startActivity(Intent(this, WriteMedicationActivity::class.java))
        }

        viewMedicationButton.setOnClickListener {
            // when the view medication button is clicked, start the ViewMedicationActivity
            startActivity(Intent(this, ViewMedicationActivity::class.java))
        }

        backButton.setOnClickListener {
            // when the back button is clicked, finish the activity
            finish()
        }
    }
}