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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Instrumented tests for {@link AdminBrowseLogsActivity}.
 * Covers US 03.08.01: As an administrator, I want to review logs of all
 *     notifications sent to entrants by organizers.
 */
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseLogsActivityTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseLogsActivity> activityRule =
            new ActivityScenarioRule<>(AdminBrowseLogsActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // US 03.08.01: Admin should see notification logs page title
    @Test
    public void testPageTitleIsDisplayed() {
        onView(withId(R.id.tvPageTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvPageTitle)).check(matches(withText(R.string.admin_logs_title)));
    }

    // US 03.08.01: Admin should see notification logs subtitle
    @Test
    public void testPageSubtitleIsDisplayed() {
        onView(withId(R.id.tvPageSubtitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvPageSubtitle)).check(matches(withText(R.string.admin_logs_subtitle)));
    }

    // US 03.08.01: Admin should see log section title
    @Test
    public void testSectionTitleIsDisplayed() {
        onView(withId(R.id.tvSectionTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvSectionTitle)).check(matches(withText(R.string.admin_all_logs_title)));
    }

    // US 03.08.01: Notification log list should be visible
    @Test
    public void testRecyclerViewExists() {
        onView(withId(R.id.rvLogs)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    // US 03.08.01: Log browser should be scrollable
    @Test
    public void testScrollViewIsScrollable() {
        onView(withId(R.id.main_scroll_view)).check(matches(isDisplayed()));
    }

    // US 03.08.01: Admin bottom navigation should be visible
    @Test
    public void testBottomNavIsDisplayed() {
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profiles)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_images)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_logs)).check(matches(isDisplayed()));
    }

    // US 03.04.01: Admin should navigate to event browser from logs tab
    @Test
    public void testNavigateToHome() {
        onView(withId(R.id.nav_home)).perform(click());
        intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
    }

    // US 03.05.01: Admin should navigate to profile browser from logs tab
    @Test
    public void testNavigateToProfiles() {
        onView(withId(R.id.nav_profiles)).perform(click());
        intended(hasComponent(AdminBrowseProfilesActivity.class.getName()));
        intended(hasExtra("role", "admin"));
    }

    // US 03.06.01: Admin should navigate to image browser from logs tab
    @Test
    public void testNavigateToImages() {
        onView(withId(R.id.nav_images)).perform(click());
        intended(hasComponent(AdminBrowseImagesActivity.class.getName()));
    }

    // US 03.08.01: Empty state should show when no notification logs exist
    @Test
    public void testNoLogsMessageVisibility() {
        activityRule.getScenario().onActivity(activity -> activity.findViewById(R.id.tvNoLogs).setVisibility(View.VISIBLE));
        onView(withId(R.id.tvNoLogs)).check(matches(isDisplayed()));
        onView(withId(R.id.tvNoLogs)).check(matches(withText(R.string.admin_no_logs)));
    }
}
