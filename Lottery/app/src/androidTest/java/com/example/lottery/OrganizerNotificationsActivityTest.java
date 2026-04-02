package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.adapter.OrganizerNotificationEventAdapter;
import com.example.lottery.model.Event;
import com.example.lottery.organizer.OrganizerNotificationsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationsActivityTest {

    private static final String TEST_USER_ID = "test_organizer_456";

    private Intent createLaunchIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerNotificationsActivity.class);
        intent.putExtra("userId", TEST_USER_ID);
        return intent;
    }

    private void injectMockEvent(ActivityScenario<OrganizerNotificationsActivity> scenario) {
        scenario.onActivity(activity -> {
            RecyclerView rv = activity.findViewById(R.id.rvOrganizerEvents);
            OrganizerNotificationEventAdapter adapter = (OrganizerNotificationEventAdapter) rv.getAdapter();

            List<Event> mockList = new ArrayList<>();
            Event event = new Event();
            event.setEventId("event_123");
            event.setTitle("Mock Test Event");
            mockList.add(event);

            // Replace the list in the adapter
            try {
                java.lang.reflect.Field field = adapter.getClass().getDeclaredField("eventList");
                field.setAccessible(true);
                field.set(adapter, mockList);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void testActivityLaunch() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            onView(withId(R.id.tvTitle)).check(matches(withText("Notifications")));
            onView(withId(R.id.rvOrganizerEvents)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testOpenNotificationDialogForWaitlist() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            // Click Notify Waiting List
            onView(withId(R.id.btnNotifyWaiting)).perform(click());

            // Verify Dialog title
            onView(withText("Notify Waiting List")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText("Send")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testOpenNotificationDialogForSelected() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifySelected)).perform(click());

            onView(withText("Notify Invited Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testOpenNotificationDialogForCancelled() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyCancelled)).perform(click());

            onView(withText("Notify Cancelled Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testSendNotificationFlow() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyWaiting)).perform(click());

            // Type message
            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Hello waiting list!"), closeSoftKeyboard());

            // Click Send
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            // Verify dialog is dismissed
            onView(withText("Notify Waiting List")).check(doesNotExist());
        }
    }
}
