package com.example.finalyearproject

import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@Config(sdk = [30])
@RunWith(RobolectricTestRunner::class)
class AddMedicationTests {

    private lateinit var activity: AddMedicationActivity

    @Before
    fun setup() {
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), AddMedicationActivity::class.java)
        activity = Robolectric.buildActivity(AddMedicationActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()
    }

    @Test
    fun emptyFieldsFail() { //
        val nameInput = activity.findViewById<EditText>(R.id.nameInput)
        val dosageInput = activity.findViewById<EditText>(R.id.dosageInput)
        val descriptionInput = activity.findViewById<EditText>(R.id.descriptionInput)
        val saveButton = activity.findViewById<Button>(R.id.saveButton)

        // leave all fields empty
        nameInput.setText("")
        dosageInput.setText("")
        descriptionInput.setText("")

        saveButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all fields", toastText)
        assertEquals("Name is required", nameInput.error)
        assertEquals("Dosage is required", dosageInput.error)
        assertEquals("Description is required", descriptionInput.error)
    }

    @Test
    fun emptyDosageFail() {
        val nameInput = activity.findViewById<EditText>(R.id.nameInput)
        val dosageInput = activity.findViewById<EditText>(R.id.dosageInput)
        val descriptionInput = activity.findViewById<EditText>(R.id.descriptionInput)
        val saveButton = activity.findViewById<Button>(R.id.saveButton)

        // leave dosage field empty
        nameInput.setText("medication")
        dosageInput.setText("")
        descriptionInput.setText("this is good for colds")

        saveButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all fields", toastText)
        assertEquals("Dosage is required", dosageInput.error)
    }

    @Test
    fun completeFieldsPass() {
        val nameInput = activity.findViewById<EditText>(R.id.nameInput)
        val dosageInput = activity.findViewById<EditText>(R.id.dosageInput)
        val descriptionInput = activity.findViewById<EditText>(R.id.descriptionInput)
        val saveButton = activity.findViewById<Button>(R.id.saveButton)

        // leave dosage field empty
        nameInput.setText("medication")
        dosageInput.setText("390mg")
        descriptionInput.setText("this is good for covid")

        saveButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertNotEquals("Please fill in all fields", toastText)
        assertEquals(null, nameInput.error)
        assertEquals(null, dosageInput.error)
        assertEquals(null, descriptionInput.error)
    }

    @Test
    fun testBackButtonFinishesActivity() {
        val backButton = activity.findViewById<Button>(R.id.backButton)
        backButton.performClick()
        assertTrue(activity.isFinishing)
    }

}
