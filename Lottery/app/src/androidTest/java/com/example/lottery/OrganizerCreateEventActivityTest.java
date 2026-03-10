package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for OrganizerCreateEventActivity.
 * Focuses on US 02.03.01: Optionally Limit Waiting List Size.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerCreateEventActivityTest {

    @Rule
    public ActivityScenarioRule<OrganizerCreateEventActivity> activityRule =
            new ActivityScenarioRule<>(OrganizerCreateEventActivity.class);

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
     * Verifies US 02.03.01 AC #1: Toggling the waiting list limit switch
     * correctly shows and hides the numeric input field.
     */
    @Test
    public void testWaitingListLimitToggleBehavior() {
        // Scroll to the switch
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo());

        // Initially, the input field (TextInputLayout) should be GONE (not displayed)
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));

        // Click the switch to enable limit
        onView(withId(R.id.swLimitWaitingList)).perform(click());

        // Now the input field should be VISIBLE
        onView(withId(R.id.tilWaitingListLimit)).perform(scrollTo()).check(matches(isDisplayed()));

        // Click again to disable
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Should be hidden again
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));
    }
}
