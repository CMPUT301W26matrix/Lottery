package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
 * UI tests for US 02.03.01 - Optionally Limit Waiting List Size.
 * Verifies that the organizer can toggle the limit and that validation is performed.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsListActivityTest {
    /**
     * initialize an intent for test
     */
    @Rule
    public ActivityScenarioRule<EntrantsListActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(),EntrantsListActivity.class).putExtra("eventId", "test_event_id"));

    /**
     * test the layout visibility when the page is initialized
     * <p>
     *     <ul>
     *         check visibility of all buttons
     *     </ul>
     * </p>
     */
    @Test
    public void testInitializedPageVisibility() {
        // Initially, the waited listed button must be visible
        onView(withId(R.id.entrants_list_waited_list_btn))
                .check(matches(isDisplayed()));

        // Initially, the invited button must be visible
        onView(withId(R.id.entrants_list_invited_btn))
                .check(matches(isDisplayed()));

        // Initially, the cancelled button must be visible
        onView(withId(R.id.entrants_list_cancelled_btn))
                .check(matches(isDisplayed()));

        //Initially, the signed up button must be visible
        onView(withId(R.id.entrants_list_signed_up_btn))
                .check(matches(isDisplayed()));

        //Initially, the send notification button must be visible
        onView(withId(R.id.entrants_list_send_notification_btn))
                .check(matches(isDisplayed()));

        //Initially, the view location button must be visible
        onView(withId(R.id.entrants_list_view_location_btn))
                .check(matches(isDisplayed()));
    }

    /**
     * test the layout visibility when user switch to signed up list
     * <p>
     *     <ul>
     *         click signed up button, verify view signed up layout visibility (should be VISIBLE)
     *     </ul>
     *     <ul>
     *         other 4 linear layout must be invisible
     *     </ul>
     * </p>
     */
    @Test
    public void testSwitchToSignedUpList() {
        // click signed up button, verify signed up entrants list visibility
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(),click());
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(isDisplayed()));

        // other 4 linear layout must be invisible
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.view_location_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * test the layout visibility when user switch to waited listed layout
     * <p>
     *     <ul>
     *         click waited listed button, verify view waited listed layout visibility (should be VISIBLE)
     *     </ul>
     *     <ul>
     *         other 4 linear layout must be invisible
     *     </ul>
     * </p>
     */
    @Test
    public void testSwitchToWaitedListedList() {
        // click waited listed button, verify waited listed entrants list visibility
        onView(withId(R.id.entrants_list_waited_list_btn)).perform(click());
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(isDisplayed()));

        // other 4 linear layout must be invisible
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.view_location_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * test the layout visibility when user switch to invited list
     * <p>
     *     <ul>
     *         click invited list button, verify invited list layout visibility (should be VISIBLE)
     *     </ul>
     *     <ul>
     *         other 4 linear layout must be invisible
     *     </ul>
     * </p>
     */
    @Test
    public void testSwitchToInvitedList() {
        // click invited button, verify invited entrants list visibility
        onView(withId(R.id.entrants_list_invited_btn)).perform(scrollTo(),click());
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(isDisplayed()));

        // other 4 linear layout must be invisible
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.view_location_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * test the layout visibility when user switch to cancelled list
     * <p>
     *     <ul>
     *         click cancelled list button, verify view cancelled list layout visibility (should be VISIBLE)
     *     </ul>
     *     <ul>
     *         other 4 linear layout must be invisible
     *     </ul>
     * </p>
     */
    @Test
    public void testSwitchToCancelledList() {
        // click cancelled button, verify signed up entrants list visibility
        onView(withId(R.id.entrants_list_cancelled_btn)).perform(click());
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(isDisplayed()));

        // other 4 linear layout must be invisible
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.view_location_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * test the layout visibility when user switch to view location
     * <p>
     *     <ul>
     *         click view location button, verify view location component(a map) visibility (should be VISIBLE)
     *     </ul>
     *     <ul>
     *         other 4 linear layout must be invisible
     *     </ul>
     * </p>
     */
    @Test
    public void testSwitchToViewLocation() {
        // click view location button, verify view location component(a map) visibility
        onView(withId(R.id.entrants_list_view_location_btn)).perform(click());
        onView(withId(R.id.view_location_layout)).check(matches(isDisplayed()));

        // other 4 linear layout must be invisible
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * test whether sample winners button works correctly
     * <p>
     *     <ul>
     *         click sample winners button, verify sample fragment visibility (should be GONE -> VISIBLE)
     *     </ul>
     * </p>
     */
    @Test
    public void testClickSampleFragmentVisibility() {
        onView(withId(R.id.sample_fragment)).check(doesNotExist());
        onView(withId(R.id.entrants_list_sample_btn)).perform(click());
        onView(withId(R.id.sample_fragment)).check(matches(isDisplayed()));
    }

    /**
     * test whether send notification button works correctly
     * <p>
     *     <ul>
     *         click send notification button, verify notification fragment visibility (should be GONE -> VISIBLE)
     *     </ul>
     * </p>
     */
    @Test
    public void testClickNotificationFragmentVisibility() {
        onView(withId(R.id.notification_fragment)).check(doesNotExist());
        onView(withId(R.id.entrants_list_send_notification_btn)).perform(click());
        onView(withId(R.id.notification_fragment)).check(matches(isDisplayed()));
    }
}

