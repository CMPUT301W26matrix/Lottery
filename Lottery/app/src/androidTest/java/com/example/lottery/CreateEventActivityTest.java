package com.example.lottery;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test for CreateEventActivity.
 * Verifies that the UI components are displayed correctly.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventActivityTest {

    @BeforeClass
    public static void setup() {
        // Manually initialize Firebase for the test process to prevent "FirebaseApp is not initialized" error.
        if (FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(getApplicationContext());
        }
    }

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test
    public void testUIComponentsDisplayed() {
        // Check if the header title is displayed
        onView(withId(R.id.tvHeader)).check(matches(isDisplayed()));
        onView(withText("Create New Event")).check(matches(isDisplayed()));

        // Check if the Event Title input field is displayed
        onView(withId(R.id.etEventTitle)).check(matches(isDisplayed()));

        // Max Capacity input field might need scrolling depending on screen size
        onView(withId(R.id.etMaxCapacity)).perform(scrollTo()).check(matches(isDisplayed()));

        // Check if the Create Event button is displayed (MUST scroll to it because it's in a ScrollView)
        onView(withId(R.id.btnCreateEvent)).perform(scrollTo()).check(matches(isDisplayed()));
    }
}