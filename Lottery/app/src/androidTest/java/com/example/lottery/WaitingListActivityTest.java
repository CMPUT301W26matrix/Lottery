package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class WaitingListActivityTest {

    // Use an Intent to provide the required "eventId" extra.
    static Intent intent;
    static {
        intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitingListActivity.class
        );
        intent.putExtra("eventId", UUID.randomUUID().toString()); // Mock event ID
    }

    @Rule
    public ActivityScenarioRule<WaitingListActivity> activityRule =
            new ActivityScenarioRule<>(intent);

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAllViewsAreDisplayed() {
        // These views should always be visible.
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        onView(withId(R.id.waitingListTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyStateIsDisplayed() {
        // Since a random UUID is used, the waiting list should be empty.
        // Wait briefly in case the activity loads data asynchronously.
        waitFor(3000);
        onView(withId(R.id.emptyMessage))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    @Test
    public void testHeaderTitleIsCorrect() {
        onView(withId(R.id.waitingListTitle))
                .check(matches(withText("Event Waiting List")));
    }

    @Test
    public void testNavigationIsDisplayed() {
        onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
    }
}
