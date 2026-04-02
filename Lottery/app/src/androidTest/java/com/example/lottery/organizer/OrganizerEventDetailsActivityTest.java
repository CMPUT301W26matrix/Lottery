package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for OrganizerEventDetailsActivity.
 * Verifies that the activity correctly handles intents and displays event data.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * US 02.01.04: Opening an organizer's event details page with an eventId
     * shows the main event details surface for that event.
     */
    @Test
    public void testActivityLaunchWithIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_event_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Check if the main components are displayed
            onView(ViewMatchers.withId(R.id.tvEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.04 / US 02.04.01: The organizer event-details page renders the
     * core schedule and waiting-list controls needed to manage the event.
     */
    @Test
    public void testUIDisplay() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "dummy_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvDetailsHeader))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            onView(withId(R.id.tvScheduledDate))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            onView(withId(R.id.btnViewWaitingList))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    /**
     * US 02.04.01: The organizer event-details page reserves space for the event
     * poster so visual information is available to entrants.
     */
    @Test
    public void testPosterImageViewVisibility() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_poster_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the poster container and image are displayed
            onView(withId(R.id.cvPoster)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.04.02: Editing an event poster starts from the organizer details page
     * and forwards the selected eventId into the edit flow.
     */
    @Test
    public void testEditEventButtonLaunchesEditScreen() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "edit_target_event");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnEditEvent)).check(matches(isDisplayed()));
            onView(withId(R.id.btnEditEvent)).perform(click());

            intended(allOf(
                    hasComponent(OrganizerCreateEventActivity.class.getName()),
                    hasExtra("eventId", "edit_target_event")
            ));
        }
    }

    /**
     * US 03.01.01: Organizer event details do not expose the administrator-only
     * event removal control reserved for admin moderation.
     */
    @Test
    public void testOrganizerScreenDoesNotExposeAdminDeleteButton() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "organizer_event_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).check(doesNotExist());
        }
    }

    /**
     * US 02.02.01 / US 02.06.01: Clicking "View Waiting List" navigates to
     * EntrantsListActivity with the correct eventId, so the organizer can
     * view entrant lists and see where they joined from on a map.
     */
    @Test
    public void testViewWaitingList_navigatesToEntrantsList() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "nav_test_event");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnViewWaitingList)).perform(scrollTo(), click());

            intended(allOf(
                    hasComponent(EntrantsListActivity.class.getName()),
                    hasExtra("eventId", "nav_test_event")
            ));
        }
    }
}
