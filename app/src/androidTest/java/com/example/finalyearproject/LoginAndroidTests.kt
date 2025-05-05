package com.example.finalyearproject

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    val scenario = ActivityScenario.launch(LoginActivity::class.java)

    @Test
    fun loginValidFields() {
        // before running this test:
        // 1. make sure user exists
        // 2. make sure the app is in the foreground
        // 3. comment finish() in goToMainActivity() in LoginActivity.kt

        // Fill in and submit form
        onView(withId(R.id.usernameInput)).perform(typeText("1234"), closeSoftKeyboard())
        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("testtest"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(3000)

        onView(withId(R.id.nfcTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun loginInvalidUsernameValidEmailPassword() {
        // before running this test:
        // 1. make sure user exists
        // 2. make sure the app is in the foreground
        // 3. comment finish() in goToMainActivity() in LoginActivity.kt

        // Fill in and submit form
        onView(withId(R.id.usernameInput)).perform(typeText("12234"), closeSoftKeyboard())
        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("testtest"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(3000)

        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun loginValidUsernameEmailInvalidPassword() {
        // before running this test:
        // 1. make sure user exists
        // 2. make sure the app is in the foreground
        // 3. comment finish() in goToMainActivity() in LoginActivity.kt

        // Fill in and submit form
        onView(withId(R.id.usernameInput)).perform(typeText("1234"), closeSoftKeyboard())
        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("testest"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(3000)

        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun loginLogout() {
        // before running this test:
        // 1. make sure user exists
        // 2. make sure the app is in the foreground
        // 3. comment finish() in goToMainActivity() in LoginActivity.kt

        // Fill in and submit form
        onView(withId(R.id.usernameInput)).perform(typeText("1234"), closeSoftKeyboard())
        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("testtest"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(3000)


        // Logout
        onView(withId(R.id.nav_settings)).perform(click())
        Thread.sleep(3000)
        onView(withId(R.id.logoutButton)).perform(click())
        Thread.sleep(3000)

        onView(withId(R.id.usernameInput)).check(matches(isDisplayed()))
    }

    @Test
    fun reportMessage() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.example.finalyearproject", "com.example.finalyearproject.SettingsActivity")
        ActivityScenario.launch<SettingsActivity>(intent)
        val testReportMessage = "Test report message"

        // Fill in and submit form
        onView(withId(R.id.issueEditText)).perform(typeText(testReportMessage), closeSoftKeyboard())
        onView(withId(R.id.reportButton)).perform(click())

        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection. - it shouldnt as it is invalid
        FirebaseFirestore.getInstance().collection("reports")
            .whereEqualTo("message", testReportMessage)
            .get()
            .addOnSuccessListener { documents ->
                documentFound = !documents.isEmpty
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }


        // await for firestore
        latch.await(5, TimeUnit.SECONDS)

        assert(documentFound) {
            "Report was not added in Firebase but should have been added"
        }
    }

    @Test
    fun reportNoMessageFail() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.example.finalyearproject", "com.example.finalyearproject.SettingsActivity")
        ActivityScenario.launch<SettingsActivity>(intent)
        val testReportMessage = ""

        // fill in and submit form
        onView(withId(R.id.issueEditText)).perform(typeText(testReportMessage), closeSoftKeyboard())
        onView(withId(R.id.reportButton)).perform(click())

        onView(withId(R.id.issueEditText)).check(matches(hasErrorText("Message cannot be empty")))

        // check if the report was added to firestore

        val latch = CountDownLatch(1)
        var documentFound = false

        FirebaseFirestore.getInstance().collection("reports")
            .whereEqualTo("message", "")
            .get()
            .addOnSuccessListener { documents ->
                documentFound = !documents.isEmpty
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        latch.await(5, TimeUnit.SECONDS)

        assert(!documentFound) {
            "Report with empty message was incorrectly added to Firebase"
        }
    }
}
