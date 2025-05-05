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
class EditPatientTests {

    private lateinit var activity: EditPatientActivity

    @Before
    fun setup() {
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), EditPatientActivity::class.java)
            .putExtra("NHS_ID", "7010001234")
        activity = Robolectric.buildActivity(EditPatientActivity::class.java, intent)
            .create().start().resume().get()
    }

    @Test
    fun emptyFieldsFail() {
        activity.findViewById<EditText>(R.id.firstNameEditText).setText("")
        activity.findViewById<EditText>(R.id.lastNameEditText).setText("")
        activity.findViewById<EditText>(R.id.dobEditText).setText("")
        activity.findViewById<EditText>(R.id.genderEditText).setText("")
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeSpinner)
        bloodTypeSpinner.setSelection(0) // assume first item is valid

        activity.findViewById<Button>(R.id.saveButton).performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all required fields", toast)
        assertEquals("First name is required", activity.findViewById<EditText>(R.id.firstNameEditText).error)
        assertEquals("Last name is required", activity.findViewById<EditText>(R.id.lastNameEditText).error)
        assertEquals("Date of birth is required", activity.findViewById<EditText>(R.id.dobEditText).error)
        assertEquals("Gender is required", activity.findViewById<EditText>(R.id.genderEditText).error)

    }

    @Test
    fun completeFieldsPass() {
        activity.findViewById<EditText>(R.id.firstNameEditText).setText("Alice")
        activity.findViewById<EditText>(R.id.lastNameEditText).setText("Smith")
        activity.findViewById<EditText>(R.id.dobEditText).setText("01/01/2000")
        activity.findViewById<EditText>(R.id.genderEditText).setText("Female")
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeSpinner)
        bloodTypeSpinner.setSelection(1) // A+

        activity.findViewById<Button>(R.id.saveButton).performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertNotEquals("Please fill in all required fields", toast)
    }

    @Test
    fun dobEmptyFail() {
        activity.findViewById<EditText>(R.id.firstNameEditText).setText("John")
        activity.findViewById<EditText>(R.id.lastNameEditText).setText("Doe")
        activity.findViewById<EditText>(R.id.dobEditText).setText("")
        activity.findViewById<EditText>(R.id.genderEditText).setText("Male")
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeSpinner)
        bloodTypeSpinner.setSelection(1)

        activity.findViewById<Button>(R.id.saveButton).performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all required fields", toast)
    }

    @Test
    fun dobFutureFail() {
        val firstName = activity.findViewById<EditText>(R.id.firstNameEditText)
        val lastName = activity.findViewById<EditText>(R.id.lastNameEditText)
        val gender = activity.findViewById<EditText>(R.id.genderEditText)
        val dob = activity.findViewById<EditText>(R.id.dobEditText)
        val bloodTypeSpinner = activity.findViewById<Spinner>(R.id.bloodTypeSpinner)
        val saveButton = activity.findViewById<Button>(R.id.saveButton)

        firstName.setText("test")
        lastName.setText("Lasttest")
        gender.setText("Male")
        bloodTypeSpinner.setSelection(1) // e.g., A+

        // Set future DOB
        dob.setText("31/12/2099")

        saveButton.performClick()

        assertEquals("Date of Birth cannot be in the future", dob.error)
    }

}
