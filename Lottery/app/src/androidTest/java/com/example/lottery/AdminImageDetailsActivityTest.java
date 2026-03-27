package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminImageDetailsActivityTest {

    private ActivityScenario<AdminImageDetailsActivity> launchWithEventId() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminImageDetailsActivity.class
        );
        intent.putExtra("eventId", "test_event");
        return ActivityScenario.launch(intent);
    }

    @Test
    public void testActivityLaunchesSuccessfully() {
        try (ActivityScenario<AdminImageDetailsActivity> scenario = launchWithEventId()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    @Test
    public void testPosterImageViewExists() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDeleteButtonIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.btnDeleteImage)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeleteImage)).check(matches(withText("Delete Image")));
        }
    }

    @Test
    public void testPageHeaderIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.tvPageHeader)).check(matches(isDisplayed()));
            onView(withId(R.id.tvPageHeader)).check(matches(withText(R.string.admin_image_preview_title)));
        }
    }

    @Test
    public void testBottomNavIsDisplayed() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored = launchWithEventId()) {
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profiles)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_images)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_logs)).check(matches(isDisplayed()));
        }
    }

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
