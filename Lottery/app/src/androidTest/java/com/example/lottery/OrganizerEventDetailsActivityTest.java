package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for OrganizerEventDetailsActivity.
 * Verifies that the activity correctly handles intents and displays event data.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsActivityTest {

    /**
     * Test Case 1: Verifies that the activity launches correctly when provided
     * with a valid eventId via Intent.
     */
    @Test
    public void testActivityLaunchWithIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_event_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Check if the main components are displayed
            onView(withId(R.id.tvEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test Case 2: Verifies that UI elements are present and initialized
     * (Placeholder check). Note: Real Firestore data loading is asynchronous.
     */
    @Test
    public void testUIDisplay() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "dummy_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvDetailsHeader))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            onView(withId(R.id.tvScheduledDate))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    /**
     * Test Case 3: Verifies that the poster ImageView is visible.
     * In the prototype, this will show the placeholder if the URI is invalid.
     */
    @Test
    public void testPosterImageViewVisibility() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_poster_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the poster container and image are displayed
            onView(withId(R.id.cvPoster)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }
}
