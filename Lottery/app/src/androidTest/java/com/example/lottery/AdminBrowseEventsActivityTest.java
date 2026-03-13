package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseEventsActivityTest {
    @Rule
    public ActivityScenarioRule<AdminBrowseEventsActivity> activityRule =
            new ActivityScenarioRule<>(AdminBrowseEventsActivity.class);
    @Test
    public void testAdminBrowseEventsScreenIsDisplayed() {
        onView(withId(R.id.tvAppTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvAppTitle)).check(matches(withText(R.string.admin_event_browser_title)));

        onView(withId(R.id.tvOrganizerLabel)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvOrganizerLabel))
                .check(matches(withText(R.string.admin_event_browser_subtitle)));

        onView(withId(R.id.tvYourEventsTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvYourEventsTitle))
                .check(matches(withText(R.string.admin_all_events_title)));

        // RecyclerView starts empty, only check it's VISIBLE or not
        onView(withId(R.id.rvEvents)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }
}
