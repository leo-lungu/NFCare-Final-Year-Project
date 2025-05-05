package com.example.finalyearproject

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WriteMedicationTests {

    private lateinit var activity: WriteMedicationActivity

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), WriteMedicationActivity::class.java)
        activity = Robolectric.buildActivity(WriteMedicationActivity::class.java, intent)
            .create().start().resume().get()
    }

    @Test
    fun emptySearchFail() {
        val searchInput = activity.findViewById<EditText>(R.id.searchInput)
        val searchButton = activity.findViewById<Button>(R.id.searchButton)

        searchInput.setText("")
        searchButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Enter a medication name", toastText)
        assertEquals("Enter a medication name", searchInput.error)

    }


    @Test
    fun emptyFieldsFail() {
        // Simulate a medication being selected (mock selection)
        val batchNumberInput = activity.findViewById<EditText>(R.id.batchNumberInput).setText("")
        val expirationDateInputField =     activity.findViewById<EditText>(R.id.expirationDateInputField).setText("")
        val quantityInput =     activity.findViewById<EditText>(R.id.quantityInput).setText("")
        val saveBoxButton =     activity.findViewById<Button>(R.id.saveBoxButton).performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("All fields must be filled", toastText)
    }

    @Test
    fun invalidQuantityFail() {
        val batchNumberInput = activity.findViewById<EditText>(R.id.batchNumberInput)
        val expirationDateInputField = activity.findViewById<EditText>(R.id.expirationDateInputField)
        val quantityInput = activity.findViewById<EditText>(R.id.quantityInput)
        quantityInput.setText("t")
        val saveBoxbutton = activity.findViewById<Button>(R.id.saveBoxButton)

        batchNumberInput.setText("123456")
        expirationDateInputField.setText("2025-12-31")
        saveBoxbutton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Quantity must be a number", toastText)
        assertEquals("Quantity must be a number", quantityInput.error)
    }

    @Test
    fun writeNfcNoSelection() {
        val writeButton = activity.findViewById<Button>(R.id.writeNfcButton)
        writeButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("No medication selected!", toastText)
    }

    @Test
    fun expiredAlreadyFail() {
        val batchNumberInput = activity.findViewById<EditText>(R.id.batchNumberInput)
        val expirationDateInputField = activity.findViewById<EditText>(R.id.expirationDateInputField)
        val quantityInput = activity.findViewById<EditText>(R.id.quantityInput)
        val saveBoxButton = activity.findViewById<Button>(R.id.saveBoxButton)

        batchNumberInput.setText("BN456")
        expirationDateInputField.setText("2020-01-01") // expired date
        quantityInput.setText("10")

        saveBoxButton.performClick()

        val toastText = ShadowToast.getTextOfLatestToast()
        assertEquals("Expiration date cannot be in the past", toastText)
        assertEquals("Expiration date cannot be in the past", expirationDateInputField.error)
    }
}