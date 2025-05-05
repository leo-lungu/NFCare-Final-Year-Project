package com.example.finalyearproject

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.containsString


@RunWith(AndroidJUnit4::class)
class MedicationAndroidTests {

    @Test
    fun addMedicationSaveFirestore() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.example.finalyearproject", "com.example.finalyearproject.AddMedicationActivity")
        ActivityScenario.launch<AddMedicationActivity>(intent)

        // test medication data
        val testName = "TestMed123"
        val testDosage = "100mg"
        val testDescription = "This is a test medication."

        // fill in the form with fake data
        onView(withId(R.id.nameInput)).perform(typeText(testName), closeSoftKeyboard())
        onView(withId(R.id.dosageInput)).perform(typeText(testDosage), closeSoftKeyboard())
        onView(withId(R.id.descriptionInput)).perform(typeText(testDescription), closeSoftKeyboard())
        onView(withId(R.id.saveButton)).perform(click())

        // wait for firestore to save the data
        val latch = CountDownLatch(1)
        var documentFound = false

        // within firestore find the data if it exists under the medications collection
        FirebaseFirestore.getInstance().collection("medications")
            .whereEqualTo("name", testName)
            .whereEqualTo("dosage", testDosage)
            .whereEqualTo("description", testDescription)
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
            "Medication with name: $testName, dosage: $testDosage, and description: $testDescription was not found in Firestore."
        }
    }


    @Test
    fun addMedicationNoName() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.example.finalyearproject", "com.example.finalyearproject.AddMedicationActivity")
        ActivityScenario.launch<AddMedicationActivity>(intent)

        // test medication data
        val testName = ""
        val testDosage = "100mg"
        val testDescription = "This is a test medication."

        // fill in the form with fake data
        onView(withId(R.id.nameInput)).perform(typeText(testName), closeSoftKeyboard())
        onView(withId(R.id.dosageInput)).perform(typeText(testDosage), closeSoftKeyboard())
        onView(withId(R.id.descriptionInput)).perform(typeText(testDescription), closeSoftKeyboard())
        onView(withId(R.id.saveButton)).perform(click())


        onView(withId(R.id.nameInput)).check(matches(hasErrorText("Name is required")))

    }

    @Test
    fun searchMedicationList() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.example.finalyearproject", "com.example.finalyearproject.ViewMedicationActivity")
        ActivityScenario.launch<ViewMedicationActivity>(intent)

        // test medication data
        val searchName = "ib"


        // fill in the form with fake data
        onView(withId(R.id.searchInput)).perform(typeText(searchName), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())

        // wait for firestore to search the data
        val latch = CountDownLatch(1)
        latch.await(3, TimeUnit.SECONDS)

        onView(withId(R.id.searchResultsRecyclerView))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))


        onView(withText(containsString("Name: ibhprofen"))).check(matches(isDisplayed()))
    }
}
