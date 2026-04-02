package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Intent;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.adapter.NotificationAdapter;
import com.example.lottery.entrant.EntrantEventDetailsActivity;
import com.example.lottery.entrant.NotificationsActivity;
import com.example.lottery.model.NotificationItem;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Instrumented tests for NotificationsActivity.
 * Firestore network is disabled so onCreate's loadNotifications() resolves
 * from empty cache immediately; test data is injected via reflection.
 *
 * US 01.04.01: Entrant receives and views notifications.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

    private static final String TEST_USER_ID = "test-user-123";

    @Before
    public void setUp() throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().disableNetwork());
        Intents.init();
    }

    @After
    public void tearDown() throws Exception {
        Intents.release();
        Tasks.await(FirebaseFirestore.getInstance().enableNetwork());
    }

    private static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    private ActivityScenario<NotificationsActivity> launchActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationsActivity.class
        );
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, TEST_USER_ID);
        return ActivityScenario.launch(intent);
    }

    private NotificationItem createNotification(
            String id,
            String title,
            String message,
            String type,
            String eventId,
            boolean isRead
    ) {
        NotificationItem item = new NotificationItem();
        item.setNotificationId(id);
        item.setTitle(title);
        item.setMessage(message);
        item.setType(type);
        item.setEventId(eventId);
        item.setRead(isRead);
        return item;
    }

    @SuppressWarnings("unchecked")
    private void injectNotifications(
            ActivityScenario<NotificationsActivity> scenario,
            List<NotificationItem> items
    ) {
        scenario.onActivity(activity -> {
            try {
                Field listField = NotificationsActivity.class.getDeclaredField("notificationList");
                listField.setAccessible(true);
                List<NotificationItem> notificationList =
                        (List<NotificationItem>) listField.get(activity);

                if (notificationList != null) {
                    notificationList.clear();
                    notificationList.addAll(items);
                }

                Field adapterField = NotificationsActivity.class.getDeclaredField("adapter");
                adapterField.setAccessible(true);
                NotificationAdapter adapter =
                        (NotificationAdapter) adapterField.get(activity);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                Method updateEmptyStateMethod =
                        NotificationsActivity.class.getDeclaredMethod("updateEmptyState");
                updateEmptyStateMethod.setAccessible(true);
                updateEmptyStateMethod.invoke(activity);

            } catch (Exception e) {
                throw new RuntimeException("Failed to inject test notifications", e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    // US 01.04.01: Activity launches and shows the notification list
    @Test
    public void testActivityLaunch() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            List<NotificationItem> items = new ArrayList<>();
            items.add(createNotification(
                    "notif-launch", "Launch Test", "msg", "general", null, false));
            injectNotifications(scenario, items);
            onView(isRoot()).perform(waitFor(300));

            onView(withId(R.id.rvNotifications))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnBack))
                    .check(matches(isDisplayed()));
        }
    }

    // US 01.04.01: Empty inbox shows a placeholder message
    @Test
    public void testEmptyStateShowsNoNotificationsMessage() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            injectNotifications(scenario, new ArrayList<>());
            onView(isRoot()).perform(waitFor(300));

            onView(withId(R.id.tvNoNotifications))
                    .check(matches(isDisplayed()));
        }
    }

    // US 01.04.01: Injected notification title is visible in the list
    @Test
    public void testNotificationDisplay() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            List<NotificationItem> items = new ArrayList<>();
            items.add(createNotification(
                    "notif-1",
                    "Test Notification Title",
                    "This is a test notification message.",
                    "general",
                    null,
                    false
            ));

            injectNotifications(scenario, items);
            onView(isRoot()).perform(waitFor(300));

            onView(withText("Test Notification Title"))
                    .check(matches(isDisplayed()));
        }
    }

    // US 01.04.01: Clicking a general notification opens an AlertDialog
    @Test
    public void testGeneralNotificationClickOpensDialog() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            List<NotificationItem> items = new ArrayList<>();
            items.add(createNotification(
                    "notif-2",
                    "Normal Notification",
                    "Normal notification message body.",
                    "general",
                    null,
                    false
            ));

            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(items.get(0)));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText("Normal Notification")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("Normal notification message body.")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("OK")).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    // US 01.04.01: OK button dismisses the general notification dialog
    @Test
    public void testGeneralNotificationDialogOkDismisses() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            NotificationItem item = createNotification(
                    "notif-5",
                    "Dismiss Dialog Notification",
                    "Dialog should close when OK is tapped.",
                    "general",
                    null,
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);

            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText("Dismiss Dialog Notification")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("OK")).inRoot(isDialog())
                    .perform(click());

            onView(isRoot()).perform(waitFor(300));

            onView(withText("OK"))
                    .check(doesNotExist());
        }
    }

    // US 01.04.01: Clicking an invitation notification navigates to event details
    @Test
    public void testInvitationNotificationNavigatesToEventDetails() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            NotificationItem item = createNotification(
                    "notif-3",
                    "You've been invited!",
                    "You have been invited to an event.",
                    "event_invitation",
                    "event-123",
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);
            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            intended(allOf(
                    hasComponent(EntrantEventDetailsActivity.class.getName()),
                    hasExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "event-123"),
                    hasExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, TEST_USER_ID)
            ));
        }
    }

    // US 01.04.01: Invitation without eventId falls back to a dialog instead of navigating
    @Test
    public void testInvitationWithoutEventIdShowsDialog() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            NotificationItem item = createNotification(
                    "notif-4",
                    "Invitation (no event)",
                    "Event ID is missing.",
                    "event_invitation",
                    null,
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);
            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText("Invitation (no event)")).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    // US 01.04.01: Unread notification shows "NEW" badge with highlight background
    @Test
    public void testUnreadNotificationShowsNewBadge() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            List<NotificationItem> items = new ArrayList<>();
            items.add(createNotification(
                    "notif-6",
                    "Unread Notification",
                    "This should show a NEW badge.",
                    "general",
                    null,
                    false
            ));

            injectNotifications(scenario, items);
            onView(isRoot()).perform(waitFor(300));

            onView(withText("Unread Notification"))
                    .check(matches(isDisplayed()));

            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView rv =
                        activity.findViewById(R.id.rvNotifications);
                Assert.assertNotNull("RecyclerView should exist", rv);

                View itemView = rv.findViewHolderForAdapterPosition(0).itemView;
                Assert.assertNotNull("First item view should exist", itemView);

                // Verify "NEW" badge is visible
                View tvNew = itemView.findViewById(R.id.tvNotificationNew);
                Assert.assertNotNull("tvNotificationNew should exist", tvNew);
                Assert.assertEquals("NEW badge should be VISIBLE for unread notification",
                        View.VISIBLE, tvNew.getVisibility());

                // Verify highlight background color (#EEF3FF)
                android.graphics.drawable.ColorDrawable bg =
                        (android.graphics.drawable.ColorDrawable) itemView.getBackground();
                Assert.assertNotNull("Item should have a background color", bg);
                Assert.assertEquals("Unread item should have #EEF3FF background",
                        android.graphics.Color.parseColor("#EEF3FF"), bg.getColor());
            });
        }
    }
}
