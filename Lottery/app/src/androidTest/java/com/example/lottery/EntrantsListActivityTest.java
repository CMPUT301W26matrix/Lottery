package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EntrantsListActivity.
 * Verifies that the organizer can switch between entrant list tabs and interact with functional components.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsListActivityTest {
    
    @Rule
    public ActivityScenarioRule<EntrantsListActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), EntrantsListActivity.class)
                    .putExtra("eventId", "test_event_id")
                    .putExtra("userId", "test_user_id"));

    /**
     * Verifies that all navigation buttons are displayed upon initialization.
     */
    @Test
    public void testInitializedPageVisibility() {
        onView(withId(R.id.entrants_list_waited_list_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_invited_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_cancelled_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_signed_up_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_send_notification_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_view_location_btn)).check(matches(isDisplayed()));
    }

    /**
     * Verifies switching to the Signed Up (Accepted) entrants list.
     */
    @Test
    public void testSwitchToSignedUpList() {
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(click());
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(isDisplayed()));

        // Verify other layouts are hidden
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies switching to the Waited List entrants list.
     */
    @Test
    public void testSwitchToWaitedListedList() {
        // First switch to another tab to ensure we are testing a real transition
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(click());
        
        onView(withId(R.id.entrants_list_waited_list_btn)).perform(click());
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(isDisplayed()));

        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies switching to the Map view (View Location).
     */
    @Test
    public void testSwitchToViewLocation() {
        onView(withId(R.id.entrants_list_view_location_btn)).perform(click());
        onView(withId(R.id.view_location_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.mapView)).check(matches(isDisplayed()));

        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies that the Sample Winners dialog appears.
     */
    @Test
    public void testClickSampleFragmentVisibility() {
        onView(withId(R.id.entrants_list_sample_btn)).perform(click());
        // Check for dialog elements (SampleFragment uses AlertDialog)
        onView(withText("Sample Winners")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that the Send Notification dialog appears.
     */
    @Test
    public void testClickNotificationFragmentVisibility() {
        onView(withId(R.id.entrants_list_send_notification_btn)).perform(click());
        // Check for dialog elements (NotificationFragment uses AlertDialog)
        onView(withText("Compose Notification")).check(matches(isDisplayed()));
    }
}
