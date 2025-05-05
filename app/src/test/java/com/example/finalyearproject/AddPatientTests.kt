package com.example.finalyearproject

import android.widget.*
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@Config(sdk = [30])
@RunWith(RobolectricTestRunner::class)
class AddPatientTests {

    private lateinit var activity: AddPatientActivity

    @Before
    fun setup() {
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), AddPatientActivity::class.java)
        activity = Robolectric.buildActivity(AddPatientActivity::class.java, intent).create().start().resume().get()
    }

    @Test
    fun emptyFieldsFail() {
        val nhsIdInput = activity.findViewById<EditText>(R.id.NHSIdInput)
        val firstNameInput = activity.findViewById<EditText>(R.id.firstNameInput)
        val lastNameInput = activity.findViewById<EditText>(R.id.lastNameInput)
        val dobInput = activity.findViewById<EditText>(R.id.dobInput)
        val genderSpinner = activity.findViewById<Spinner>(R.id.genderInput)
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeInput)
        val addButton = activity.findViewById<Button>(R.id.addPatientButton)

        nhsIdInput.setText("") // invalid
        firstNameInput.setText("") // invalid
        lastNameInput.setText("") // invalid
        dobInput.setText("") // invalid
        genderSpinner.setSelection(0) // "Male", so valid
        bloodTypeSpinner.setSelection(0) // "select Blood Type" → Invalid

        addButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all required fields", toastText)
        assertEquals("NHS ID is required", nhsIdInput.error)
        assertEquals("First Name is required", firstNameInput.error)
        assertEquals("Last Name is required", lastNameInput.error)
        assertEquals("Date of Birth is required", dobInput.error)
        assertEquals("Select Blood Type", bloodTypeSpinner.selectedItem.toString())
    }

    @Test
    fun invalidIdFail() {
        val nhsIdInput = activity.findViewById<EditText>(R.id.NHSIdInput)
        nhsIdInput.setText("12345") // invalid NHS ID

        val addButton = activity.findViewById<Button>(R.id.addPatientButton)
        addButton.performClick()

        assertEquals("NHS ID must be exactly 10 digits", nhsIdInput.error)
    }

    @Test
    fun dobFutureFail() {
        val nhsIdInput = activity.findViewById<EditText>(R.id.NHSIdInput)
        val firstNameInput = activity.findViewById<EditText>(R.id.firstNameInput)
        val lastNameInput = activity.findViewById<EditText>(R.id.lastNameInput)
        val dobInput = activity.findViewById<EditText>(R.id.dobInput)
        val genderSpinner = activity.findViewById<Spinner>(R.id.genderInput)
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeInput)
        val addButton = activity.findViewById<Button>(R.id.addPatientButton)

        nhsIdInput.setText("1234567890")
        firstNameInput.setText("Alice")
        lastNameInput.setText("Smith")
        dobInput.setText("31/12/2099") // Future date
        genderSpinner.setSelection(0) // Male
        bloodTypeSpinner.setSelection(1) // A+

        addButton.performClick()

        assertEquals("Date of Birth cannot be in the future", dobInput.error)
    }
}
