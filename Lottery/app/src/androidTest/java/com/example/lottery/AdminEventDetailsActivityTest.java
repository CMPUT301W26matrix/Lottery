package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link AdminEventDetailsActivity}.
 * Covers US 03.01.01: As an administrator, I want to be able to remove events.
 * Covers US 03.10.01: As an administrator, I want to remove event comments
 *     that violate app policy.
 */
@RunWith(AndroidJUnit4.class)
public class AdminEventDetailsActivityTest {

    // US 03.01.01: Admin should see event details with poster, details header, and delete button
    @Test
    public void testAdminEventDetailsScreenIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvPageHeader)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.cvPoster)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.tvDetailsHeader)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeleteEvent)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }

    // US 03.01.01: Deleting an event should show confirmation dialog
    @Test
    public void testDeleteButtonShowsConfirmationDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(scrollTo(), click());
            onView(withText("Confirm Deletion")).check(matches(isDisplayed()));
            onView(withText("Do you want to delete this event?")).check(matches(isDisplayed()));
            onView(withText("Delete")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));
        }
    }

    // US 03.01.01: Cancelling event deletion should dismiss dialog
    @Test
    public void testDeleteConfirmationCancelDismissesDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(scrollTo(), click());
            onView(withText("Cancel")).perform(click());

            onView(withText("Confirm Deletion")).check(doesNotExist());
            onView(withText("Do you want to delete this event?")).check(doesNotExist());
            onView(withId(R.id.btnDeleteEvent)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    // US 03.10.01: Admin should have access to comments for moderation
    @Test
    public void testCommentsButtonIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnComments)).check(matches(isDisplayed()));
        }
    }

    // US 03.01.01: Admin view should not expose organizer edit controls
    @Test
    public void testAdminScreenDoesNotExposeOrganizerEditButton() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }
}
