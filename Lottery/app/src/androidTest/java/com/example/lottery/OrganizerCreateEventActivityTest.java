package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for OrganizerCreateEventActivity.
 * Focuses on US 02.03.01: Optionally Limit Waiting List Size and general UI.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerCreateEventActivityTest {

    // OrganizerCreateEventActivity requires a userId to be present in Intent or SharedPreferences,
    // otherwise it calls finish() immediately.
    private static final String TEST_USER_ID = "test_user_123";

    @Rule
    public ActivityScenarioRule<OrganizerCreateEventActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), OrganizerCreateEventActivity.class)
                    .putExtra("userId", TEST_USER_ID));

    @Test
    public void testUIComponentsDisplayed() {
        // Check if the header title is displayed
        onView(withId(R.id.tvHeader)).check(matches(isDisplayed()));
        onView(withId(R.id.tvHeader)).check(matches(withText("Create New Event")));

        // Check if the Event Title input field is displayed
        onView(withId(R.id.etEventTitle)).check(matches(isDisplayed()));

        // Max Capacity input field
        onView(withId(R.id.etMaxCapacity)).perform(scrollTo()).check(matches(isDisplayed()));

        // Launch Event button
        onView(withId(R.id.btnCreateEvent)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Verifies whether Place text input is properly displayed in createEvent page.
     */
    @Test
    public void testPlaceFieldDisplayed() {
        onView(withId(R.id.etPlace)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Verifies US 02.03.01 AC #1: Toggling the waiting list limit switch
     * correctly shows and hides the numeric input field.
     */
    @Test
    public void testWaitingListLimitToggleBehavior() {
        // Initially, the input field (TextInputLayout) should be GONE (not displayed)
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));

        // Click the switch to enable limit
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Now the input field should be VISIBLE
        onView(withId(R.id.tilWaitingListLimit)).perform(scrollTo()).check(matches(isDisplayed()));

        // Click again to disable
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Should be hidden again
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies that the input field correctly clears and hides when the switch is disabled.
     */
    @Test
    public void testSwitchClearsInput() {
        // Toggle ON and type something
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());
        onView(withId(R.id.etWaitingListLimit)).perform(scrollTo(), typeText("50"), closeSoftKeyboard());

        // Toggle OFF
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Toggle ON again - field should be empty
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());
        onView(withId(R.id.etWaitingListLimit)).perform(scrollTo()).check(matches(withText("")));
    }

    /**
     * Verifies that supplying an eventId launches the screen in edit mode.
     */
    @Test
    public void testEditModeIntentUpdatesHeaderAndActionText() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );
        intent.putExtra("eventId", "existing_event_id");
        intent.putExtra("userId", TEST_USER_ID); // Must provide userId to prevent activity from finishing

        try (ActivityScenario<OrganizerCreateEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvHeader)).check(matches(withText("Edit Event")));
            onView(withId(R.id.btnCreateEvent)).perform(scrollTo())
                    .check(matches(withText("Update Event")));
        }
    }
}
