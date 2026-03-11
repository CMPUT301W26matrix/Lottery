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

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for US 02.03.01 - Optionally Limit Waiting List Size.
 * Verifies that the organizer can toggle the limit and that validation is performed.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventRegistrationUITest {

    @Rule
    public ActivityScenarioRule<OrganizerCreateEventActivity> activityRule =
            new ActivityScenarioRule<>(OrganizerCreateEventActivity.class);

    /**
     * Verifies that checking the "Limit Waiting List Size" switch toggles the visibility
     * of the numeric input field (Acceptance Criterion #1).
     */
    @Test
    public void testLimitToggleVisibility() {
        // Initially, the limit input should be hidden
        onView(withId(R.id.tilWaitingListLimit))
                .check(matches(not(isDisplayed())));

        // Toggle the switch ON
        onView(withId(R.id.swLimitWaitingList))
                .perform(scrollTo(), click());

        // Now the limit input should be visible
        onView(withId(R.id.tilWaitingListLimit))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Toggle the switch OFF
        onView(withId(R.id.swLimitWaitingList))
                .perform(scrollTo(), click());

        // It should be hidden again
        onView(withId(R.id.tilWaitingListLimit))
                .check(matches(not(isDisplayed())));
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
}
