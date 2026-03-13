package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;

public class WaitingListActivityTest {

    // Use Intent to provide required EXTRA "eventId"
    static Intent intent;
    static {
        intent = new Intent(ApplicationProvider.getApplicationContext(), WaitingListActivity.class);
        intent.putExtra("eventId", UUID.randomUUID().toString()); // Mock event ID
    }

    @Rule
    public ActivityScenarioRule<WaitingListActivity> activityRule =
            new ActivityScenarioRule<>(intent);

    @Test
    public void testAllViewsAreDisplayed() {
        // These views are always visible
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        onView(withId(R.id.waitingListTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyStateIsDisplayed() {
        // Since we use a random UUID, the waiting list will be empty
        // The activity hides the ListView and shows the emptyMessage TextView
        onView(withId(R.id.emptyMessage)).check(matches(isDisplayed()));
    }

    @Test
    public void testHeaderTitleIsCorrect() {
        onView(withId(R.id.waitingListTitle)).check(matches(withText("Event Waiting List")));
    }

    @Test
    public void testNavigationIsDisplayed() {
        onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
    }
}
