package com.example.finalyearproject

import android.content.Intent
import android.widget.Button
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
class LoginTests {

    private lateinit var activity: LoginActivity

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        activity = Robolectric.buildActivity(LoginActivity::class.java, intent)
            .create().start().resume().get()
    }

    @Test
    fun emptyFieldsFail() {
        val activity = Robolectric.buildActivity(LoginActivity::class.java).setup().get()
        val loginButton = activity.findViewById<Button>(R.id.loginButton)
        loginButton.performClick()
        val toast = ShadowToast.getTextOfLatestToast()
        assertEquals("Please fill in all fields", toast)
    }
}