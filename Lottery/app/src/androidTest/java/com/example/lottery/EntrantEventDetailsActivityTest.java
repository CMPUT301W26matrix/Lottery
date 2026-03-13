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

public class EntrantEventDetailsActivityTest {

    // Using a factory method for the Intent to ensure it's fresh and has all required extras.
    private static Intent createIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "test_event_id_" + UUID.randomUUID().toString());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "test_user_id");
        return intent;
    }

    @Rule
    public ActivityScenarioRule<EntrantEventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    @Test
    public void testInitialUIState() {
        // We check for views that should be present immediately upon launch.
        // Note: The activity might finish if the event is not found in Firestore (async).
        // We try to verify the basic structure before it potentially finishes.
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(withText("Event Details")));
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    @Test
    public void testBottomNavigationIsDisplayed() {
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
    }
}
