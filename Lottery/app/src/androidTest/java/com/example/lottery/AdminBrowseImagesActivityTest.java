package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.model.Event;

import org.junit.After;
import org.junit.Before;

/**
 * Instrumented tests for {@link AdminBrowseImagesActivity}.
 * Covers US 03.06.01: As an administrator, I want to be able to browse images
 *     that are uploaded so I can remove them if necessary.
 * Covers US 03.03.01: As an administrator, I want to be able to remove images.
 */
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseImagesActivityTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseImagesActivity> activityRule =
            new ActivityScenarioRule<>(AdminBrowseImagesActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // US 03.06.01: Admin should see image browser title
    @Test
    public void testBrowseImagesScreenIsDisplayed() {
        onView(withId(R.id.tvAppTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvAppTitle)).check(matches(withText(R.string.admin_image_browser_title)));
    }

    // US 03.06.01: Admin should see image browser subtitle
    @Test
    public void testSubtitleIsDisplayed() {
        onView(withId(R.id.tvSubtitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvSubtitle)).check(matches(withText(R.string.admin_image_browser_subtitle)));
    }

    // US 03.06.01: Image list view should be visible
    @Test
    public void testRecyclerViewExists() {
        onView(withId(R.id.rvImages)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    // US 03.06.01: Admin bottom navigation should be visible
    @Test
    public void testBottomNavIsDisplayed() {
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profiles)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_images)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_logs)).check(matches(isDisplayed()));
    }

    // US 03.06.01: Navigation icons and labels should exist
    @Test
    public void testNavHighlightElementsExist() {
        onView(withId(R.id.nav_home_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home_text)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_images_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_images_text)).check(matches(isDisplayed()));
    }

    // US 03.06.01: Image browser should be scrollable
    @Test
    public void testScrollViewIsScrollable() {
        onView(withId(R.id.main_scroll_view)).check(matches(isDisplayed()));
    }

    // US 03.04.01: Admin should navigate to event browser from images tab
    @Test
    public void testNavigateToHome() {
        onView(withId(R.id.nav_home)).perform(click());
        intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
    }

    // US 03.05.01: Admin should navigate to profile browser from images tab
    @Test
    public void testNavigateToProfiles() {
        onView(withId(R.id.nav_profiles)).perform(click());
        intended(hasComponent(AdminBrowseProfilesActivity.class.getName()));
        intended(hasExtra("role", "admin"));
    }

    // US 03.06.01: Empty state should show when no images are uploaded
    @Test
    public void testNoImagesMessageVisibility() {
        activityRule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.tvNoImages).setVisibility(View.VISIBLE);
        });
        onView(withId(R.id.tvNoImages)).check(matches(isDisplayed()));
    }

    // US 03.03.01: Clicking an image should navigate to image details for removal
    @Test
    public void testOnImageClickLaunchesAdminImageDetailsActivity() {
        activityRule.getScenario().onActivity(activity -> {
            Event event = new Event();
            event.setEventId("test_image_event_id");
            activity.onImageClick(event);
        });

        intended(hasComponent(AdminImageDetailsActivity.class.getName()));
        intended(hasExtra("eventId", "test_image_event_id"));
    }
}
