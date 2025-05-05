package com.example.finalyearproject

import android.content.Intent
import android.widget.DatePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PatientAndroidTests {


    @Test
    fun addPatientSaveFirestore() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(
            "com.example.finalyearproject",
            "com.example.finalyearproject.AddPatientActivity"
        )
        ActivityScenario.launch<AddPatientActivity>(intent)

        // test medication data
        val testNHSID = "0123456789"
        val testFirstName = "TestFirstName"
        val testLastName = "TestLastName"
        val testGender = "Male"
        val testBloodType = "A+"


        // fill in the form with fake data
        onView(withId(R.id.NHSIdInput)).perform(typeText(testNHSID), closeSoftKeyboard())
        onView(withId(R.id.firstNameInput)).perform(typeText(testFirstName), closeSoftKeyboard())
        onView(withId(R.id.lastNameInput)).perform(typeText(testLastName), closeSoftKeyboard())

        onView(withId(R.id.dobInput)).perform(click())
        onView(isAssignableFrom(DatePicker::class.java))
            .perform(PickerActions.setDate(2000, 10, 7))
        onView(withText("OK")).perform(click())

        onView(withId(R.id.genderInput)).perform(click())
        onView(withText(testGender)).perform(click())

        onView(withId(R.id.bloodTypeInput)).perform(click())
        onView(withText(testBloodType)).perform(click())



        onView(isRoot()).perform(swipeUp())
        Thread.sleep(1000) // so that it can scroll down and see the button
        onView(withId(R.id.addPatientButton)).perform(click())


        // wait for firestore to save the data
        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection
        FirebaseFirestore.getInstance().collection("patients")
            .document(testNHSID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    documentFound = true
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        // await for firestore
        latch.await(5, TimeUnit.SECONDS)


        assert(documentFound) {
            "Patient was not found."
        }
    }

    @Test
    fun addPatientWrongNHS() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(
            "com.example.finalyearproject",
            "com.example.finalyearproject.AddPatientActivity"
        )
        ActivityScenario.launch<AddPatientActivity>(intent)

        // test medication data
        val testNHSID = "013113" // only 6 digits, not 10
        val testFirstName = "TestFirstName"
        val testLastName = "TestLastName"
        val testDateOfBirth = "01/01/2000"
        val testGender = "Male"
        val testBloodType = "A+"


        // fill in the form with fake data
        onView(withId(R.id.NHSIdInput)).perform(typeText(testNHSID), closeSoftKeyboard())
        onView(withId(R.id.firstNameInput)).perform(typeText(testFirstName), closeSoftKeyboard())
        onView(withId(R.id.lastNameInput)).perform(typeText(testLastName), closeSoftKeyboard())

        onView(withId(R.id.dobInput)).perform(click())
        onView(isAssignableFrom(DatePicker::class.java))
            .perform(PickerActions.setDate(2000, 10, 7))
        onView(withText("OK")).perform(click())

        onView(withId(R.id.genderInput)).perform(click())
        onView(withText(testGender)).perform(click())

        onView(withId(R.id.bloodTypeInput)).perform(click())
        onView(withText(testBloodType)).perform(click())



        onView(isRoot()).perform(swipeUp())
        Thread.sleep(1000) // so that it can scroll down and see the button
        onView(withId(R.id.addPatientButton)).perform(click())

        onView(withId(R.id.NHSIdInput)).check(matches(hasErrorText("NHS ID must be exactly 10 digits")))
        // quick check that error is displayed - next to check if in firestore

        // wait for firestore to save the data
        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection. - it shouldnt as it is invalid
        FirebaseFirestore.getInstance().collection("patients")
            .document(testNHSID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    documentFound = true
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        // await for firestore
        latch.await(5, TimeUnit.SECONDS)

    assert(!documentFound) {
        "Patient was found in Firebase but should not have been added"
    }
    }

    @Test
    fun addDuplicatePatient() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(
            "com.example.finalyearproject",
            "com.example.finalyearproject.AddPatientActivity"
        )
        ActivityScenario.launch<AddPatientActivity>(intent)

        // test medication data
        val testNHSID = "1234123412"
        val testFirstName = "Duplicate"
        val testLastName = "Duplicate"
        val testDateOfBirth = "01/01/2000"
        val testGender = "Male"
        val testBloodType = "A+"

        val db = FirebaseFirestore.getInstance()
        val patientData = hashMapOf(
            "firstName" to testFirstName,
            "lastName" to testLastName,
            "dateOfBirth" to testDateOfBirth,
            "gender" to testGender,
            "bloodType" to testBloodType
        )

        val setupLatch = CountDownLatch(1)
        db.collection("patients").document(testNHSID).set(patientData)
            .addOnSuccessListener { setupLatch.countDown() }
            .addOnFailureListener { setupLatch.countDown() }
        setupLatch.await(3, TimeUnit.SECONDS)


        // fill in the form with fake data
        onView(withId(R.id.NHSIdInput)).perform(typeText(testNHSID), closeSoftKeyboard())
        onView(withId(R.id.firstNameInput)).perform(typeText(testFirstName), closeSoftKeyboard())
        onView(withId(R.id.lastNameInput)).perform(typeText(testLastName), closeSoftKeyboard())

        onView(withId(R.id.dobInput)).perform(click())
        onView(isAssignableFrom(DatePicker::class.java))
            .perform(PickerActions.setDate(2000, 10, 7))
        onView(withText("OK")).perform(click())

        onView(withId(R.id.genderInput)).perform(click())
        onView(withText(testGender)).perform(click())

        onView(withId(R.id.bloodTypeInput)).perform(click())
        onView(withText(testBloodType)).perform(click())



        onView(isRoot()).perform(swipeUp())
        Thread.sleep(1000) // so that it can scroll down and see the button
        onView(withId(R.id.addPatientButton)).perform(click())


        // wait for firestore to save the data
        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection
        FirebaseFirestore.getInstance().collection("patients")
            .document(testNHSID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    documentFound = true
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        // await for firestore
        latch.await(5, TimeUnit.SECONDS)


        onView(withId(R.id.NHSIdInput)).check(matches(hasErrorText("This NHS ID already exists")))
    }

    @Test
    fun addPatientFutureDateOfBirth() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(
            "com.example.finalyearproject",
            "com.example.finalyearproject.AddPatientActivity"
        )
        ActivityScenario.launch<AddPatientActivity>(intent)

        // test medication data

        val testNHSID = "5825881299"
        val testFirstName = "TestFirstName"
        val testLastName = "TestLastName"
        val testDateOfBirth = "01/01/2030"
        val testGender = "Male"
        val testBloodType = "A+"


        // fill in the form with fake data
        onView(withId(R.id.NHSIdInput)).perform(typeText(testNHSID), closeSoftKeyboard())
        onView(withId(R.id.firstNameInput)).perform(typeText(testFirstName), closeSoftKeyboard())
        onView(withId(R.id.lastNameInput)).perform(typeText(testLastName), closeSoftKeyboard())

        onView(withId(R.id.dobInput)).perform(click())
        onView(isAssignableFrom(DatePicker::class.java))
            .perform(PickerActions.setDate(2030, 10, 7))
        onView(withText("OK")).perform(click())

        onView(withId(R.id.genderInput)).perform(click())
        onView(withText(testGender)).perform(click())

        onView(withId(R.id.bloodTypeInput)).perform(click())
        onView(withText(testBloodType)).perform(click())



        onView(isRoot()).perform(swipeUp())
        Thread.sleep(1000) // so that it can scroll down and see the button
        onView(withId(R.id.addPatientButton)).perform(click())
        onView(isRoot()).perform(swipeDown())
        Thread.sleep(1000) // so that it can scroll up and see error text

        onView(withId(R.id.dobInput)).check(matches(hasErrorText("Date of Birth cannot be in the future")))

        // wait for firestore to save the data
        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection. - it shouldnt as it is invalid
        FirebaseFirestore.getInstance().collection("patients")
            .document(testNHSID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    documentFound = true
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        // await for firestore
        latch.await(5, TimeUnit.SECONDS)

        assert(!documentFound) {
            "Patient was found in Firebase but should not have been added"
        }
    }
}

