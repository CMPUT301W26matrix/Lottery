package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.model.NotificationItem;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * TEMPORARILY DISABLED: Stable instrumented tests for NotificationsActivity.
 * <p>
 * REASON:
 * - This test suite is currently blocked by a Firestore/protobuf runtime dependency conflict during androidTest.
 * - Error: java.lang.RuntimeException: Internal error in Cloud Firestore
 * - Caused by: java.lang.NoSuchMethodError involving com.google.protobuf.GeneratedMessageLite
 * - This should be revisited after a thorough Gradle/Firebase dependency cleanup.
 */
@Ignore("Blocked by Firestore/protobuf androidTest runtime conflict. Revisit after dependency cleanup.")
@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

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
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, "test-user-123");
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

    @Test
    public void testActivityLaunch() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.rvNotifications))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnBack))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testEmptyStateShowsNoNotificationsMessage() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            injectNotifications(scenario, new ArrayList<>());
            onView(isRoot()).perform(waitFor(300));

            onView(withId(R.id.tvNoNotifications))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testNotificationDisplay() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

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

    @Test
    public void testNotificationClickOpensDialog() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

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

            // Directly trigger click callback instead of clicking RecyclerView item,
            // to avoid real Firestore read/write side effects from production click flow.
            scenario.onActivity(activity -> activity.onNotificationClick(items.get(0)));

            onView(isRoot()).perform(waitFor(300));

            onView(withText("Normal Notification")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("Normal notification message body.")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("OK")).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testNormalNotificationDialogOkButtonDismissesDialog() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

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
            onView(isRoot()).perform(waitFor(300));

            onView(withText("Dismiss Dialog Notification")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("OK")).inRoot(isDialog())
                    .perform(click());

            onView(isRoot()).perform(waitFor(300));

            onView(withText("Dismiss Dialog Notification"))
                    .check(doesNotExist());
        }
    }

    @Test
    public void testInvitationNotificationClickShowsActionButtons() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            NotificationItem item = createNotification(
                    "notif-3",
                    "Invitation Notification",
                    "You have been invited to an event.",
                    "event_invitation",
                    "event-123",
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);

            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            onView(isRoot()).perform(waitFor(300));

            onView(withText("Invitation Notification")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("You have been invited to an event.")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText(R.string.accept_invite)).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText(R.string.reject)).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText(R.string.close)).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testInvitationDialogAcceptButtonExists() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            NotificationItem item = createNotification(
                    "notif-7",
                    "Invitation Accept Test",
                    "Accept button should be shown.",
                    "event_invitation",
                    "event-789",
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);

            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            onView(isRoot()).perform(waitFor(300));

            onView(withText(R.string.accept_invite)).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testInvitationDialogRejectButtonExists() {
        try (ActivityScenario<NotificationsActivity> scenario = launchActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            NotificationItem item = createNotification(
                    "notif-8",
                    "Invitation Reject Test",
                    "Reject button should be shown.",
                    "event_invitation",
                    "event-999",
                    false
            );

            List<NotificationItem> items = new ArrayList<>();
            items.add(item);

            injectNotifications(scenario, items);

            scenario.onActivity(activity -> activity.onNotificationClick(item));
            onView(isRoot()).perform(waitFor(300));

            onView(withText(R.string.reject)).inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }
}
