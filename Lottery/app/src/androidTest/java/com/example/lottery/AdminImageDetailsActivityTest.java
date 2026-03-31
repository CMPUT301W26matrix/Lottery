package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.model.Event;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link AdminImageDetailsActivity}.
 * Covers US 03.03.01: As an administrator, I want to be able to remove images.
 * Covers US 03.06.01: As an administrator, I want to be able to browse images
 *     that are uploaded so I can remove them if necessary.
 */
@RunWith(AndroidJUnit4.class)
public class AdminImageDetailsActivityTest {

    @Before
    public void setUp() {
        Event event = new Event();
        event.setTitle("Test Event Title");
        event.setDetails("Test event description for admin review.");
        event.setPosterBase64("data:image/jpeg;base64,/9j/4AAQSkZJRg==");
        AdminImageDetailsActivity.testEvent = event;
    }

    @After
    public void tearDown() {
        AdminImageDetailsActivity.testEvent = null;
    }

    private ActivityScenario<AdminImageDetailsActivity> launchWithEventId() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminImageDetailsActivity.class
        );
        intent.putExtra("eventId", "test_event");
        return ActivityScenario.launch(intent);
    }

    // US 03.06.01: Image details screen should launch successfully with event ID
    @Test
    public void testActivityLaunchesSuccessfully() {
        try (ActivityScenario<AdminImageDetailsActivity> scenario = launchWithEventId()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    // US 03.06.01: Admin should see the poster image
    @Test
    public void testPosterImageViewExists() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    // US 03.06.01: Event title should be rendered from event data
    @Test
    public void testEventTitleIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.tvEventTitle)).check(matches(withText("Test Event Title")));
        }
    }

    // US 03.06.01: Event details should be rendered from event data
    @Test
    public void testEventDetailsAreDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.tvEventDetails))
                    .check(matches(withText("Test event description for admin review.")));
        }
    }

    // US 03.03.01: Delete button should be enabled when a poster exists
    @Test
    public void testDeleteButtonIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.btnDeleteImage)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeleteImage)).check(matches(withText("Delete Image")));
            onView(withId(R.id.btnDeleteImage)).check(matches(isEnabled()));
        }
    }

    // US 03.03.01: Delete button should be disabled when no poster exists
    @Test
    public void testDeleteButtonDisabledWithoutPoster() {
        AdminImageDetailsActivity.testEvent.setPosterBase64(null);
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.btnDeleteImage)).check(matches(not(isEnabled())));
        }
    }

    // US 03.06.01: Image preview page header should be displayed
    @Test
    public void testPageHeaderIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.tvPageHeader)).check(matches(isDisplayed()));
            onView(withId(R.id.tvPageHeader)).check(matches(withText(R.string.admin_image_preview_title)));
        }
    }

    // US 03.06.01: Admin bottom navigation should be visible on image details
    @Test
    public void testBottomNavIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profiles)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_images)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_logs)).check(matches(isDisplayed()));
        }
    }

    // US 03.03.01: Deleting an image should show confirmation dialog
    @Test
    public void testDeleteButtonShowsConfirmationDialog() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.btnDeleteImage)).perform(click());
            onView(withText("Confirm Deletion")).check(matches(isDisplayed()));
            onView(withText("Do you want to delete this poster image?")).check(matches(isDisplayed()));
            onView(withText("Delete")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));
        }
    }

    // US 03.03.01: Cancelling image deletion should dismiss dialog
    @Test
    public void testDeleteConfirmationCancelDismissesDialog() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.btnDeleteImage)).perform(click());
            onView(withText("Cancel")).perform(click());

            onView(withText("Confirm Deletion")).check(doesNotExist());
            onView(withText("Do you want to delete this poster image?")).check(doesNotExist());
            onView(withId(R.id.btnDeleteImage)).check(matches(isDisplayed()));
        }
    }

    // US 03.03.01: Missing event ID should finish the activity gracefully
    @Test
    public void testMissingEventIdFinishesActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminImageDetailsActivity.class
        );
        // No eventId extra
        try (ActivityScenario<AdminImageDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}
