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
class SettingsTest {

    private lateinit var activity: SettingsActivity

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), SettingsActivity::class.java)
        activity = Robolectric.buildActivity(SettingsActivity::class.java, intent)
            .create().start().resume().get()
    }

    @Test
    fun noMessageReportFail() {
        val reportEditText = activity.findViewById<EditText>(R.id.issueEditText)
        val reportButton = activity.findViewById<Button>(R.id.reportButton)

        reportEditText.setText("") // no message
        reportButton.performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals("Please enter your issue before submitting", toast)
        assertEquals("Message cannot be empty", reportEditText.error)
    }

    @Test
    fun messageReportPass() {
        val reportEditText = activity.findViewById<EditText>(R.id.issueEditText)
        val reportButton = activity.findViewById<Button>(R.id.reportButton)

        reportEditText.setText("App crashes on login.")
        reportButton.performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals("Issue reported successfully!", toast)
    }
}