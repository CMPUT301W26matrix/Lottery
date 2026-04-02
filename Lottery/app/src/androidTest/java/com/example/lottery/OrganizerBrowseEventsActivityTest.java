package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.model.Event;
import com.example.lottery.organizer.OrganizerBrowseEventsActivity;
import com.example.lottery.organizer.OrganizerEventDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OrganizerBrowseEventsActivityTest {

    @Rule
    public ActivityScenarioRule<OrganizerBrowseEventsActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), OrganizerBrowseEventsActivity.class)
                    .putExtra("userId", "test_organizer_id"));

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testOrganizerBrowseEventsScreenIsDisplayed() {
        // Use withEffectiveVisibility for the RecyclerView because it may have 0 height if no data is loaded yet,
        // causing isDisplayed() (which checks for a non-empty rectangle) to fail.
        onView(withId(R.id.tvYourEventsTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.rvEvents)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    @Test
    public void testOnEventClickLaunchesOrganizerEventDetailsActivity() {
        activityRule.getScenario().onActivity(activity -> {
            Event event = new Event();
            event.setEventId("organizer_click_event_id");
            activity.onEventClick(event);
        });

        intended(hasComponent(OrganizerEventDetailsActivity.class.getName()));
        intended(hasExtra("eventId", "organizer_click_event_id"));
        intended(hasExtra("userId", "test_organizer_id"));
    }
}
