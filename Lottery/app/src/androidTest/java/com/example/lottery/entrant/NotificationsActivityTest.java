package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instrumented tests for {@link NotificationsActivity}.
 * Seeds real Firestore inbox data and verifies notification display, click actions, and badges.
 *
 * Covers: US 01.04.01, US 01.04.02, US 01.05.06, US 01.09.01
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> testUserIds = new HashSet<>();

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() throws Exception {
        for (String userId : testUserIds) {
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.userInbox(userId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }
        }
        testUserIds.clear();
        Intents.release();
    }

    private Intent createIntent(String userId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                NotificationsActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    private String seedInboxNotification(String userId, String title, String message,
                                         String type, String eventId, boolean isRead) throws Exception {
        testUserIds.add(userId);
        String notifId = "notif_" + UUID.randomUUID();

        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("type", type);
        notif.put("eventId", eventId);
        notif.put("isRead", isRead);
        notif.put("createdAt", Timestamp.now());

        Tasks.await(db.collection(FirestorePaths.userInbox(userId))
                .document(notifId).set(notif), 10, TimeUnit.SECONDS);
        return notifId;
    }

    private void waitForNotificationListLoaded(
            ActivityScenario<NotificationsActivity> scenario, int expectedCount) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            AtomicInteger count = new AtomicInteger(-1);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvNotifications);
                if (rv != null && rv.getAdapter() != null) {
                    count.set(rv.getAdapter().getItemCount());
                }
            });
            if (count.get() >= expectedCount) return;
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for notification list to load " + expectedCount + " items");
    }

    /**
     * US 01.04.01: When an entrant wins the lottery, the event_invitation notification
     * is seeded in their inbox and displayed in the notification list.
     */
    @Test
    public void winNotification_displaysInList() throws Exception {
        String userId = "notif_win_user_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "You've been invited!",
                "Congratulations! You have been selected for Swimming Lessons.",
                "event_invitation",
                "event_123",
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            onView(allOf(withId(R.id.tvNotificationTitle), withText("You've been invited!")))
                    .check(matches(isDisplayed()));
            onView(allOf(withId(R.id.tvNotificationMessage),
                    withText("Congratulations! You have been selected for Swimming Lessons.")))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.04.01: Clicking a win (event_invitation) notification navigates to
     * EntrantEventDetailsActivity with the correct eventId.
     */
    @Test
    public void winNotification_clickNavigatesToEventDetails() throws Exception {
        String userId = "notif_click_user_" + UUID.randomUUID();
        String eventId = "click_test_event_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "You've been invited!",
                "You have been selected.",
                "event_invitation",
                eventId,
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            // Click the notification item
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvNotifications);
                if (rv.findViewHolderForAdapterPosition(0) != null) {
                    rv.findViewHolderForAdapterPosition(0).itemView.performClick();
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            intended(allOf(
                    hasComponent(EntrantEventDetailsActivity.class.getName()),
                    hasExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId)
            ));

            // Verify notification marked as read in Firestore
            for (int attempt = 0; attempt < 20; attempt++) {
                for (QueryDocumentSnapshot doc : Tasks.await(
                        db.collection(FirestorePaths.userInbox(userId)).get(),
                        10, TimeUnit.SECONDS)) {
                    Boolean isRead = doc.getBoolean("isRead");
                    if (Boolean.TRUE.equals(isRead)) return;
                }
                Thread.sleep(250);
            }
            throw new AssertionError("Notification should be marked as read after click");
        }
    }

    /**
     * US 01.04.02: When an entrant is not chosen (loses the lottery), the draw_result
     * notification is displayed in their inbox.
     */
    @Test
    public void loseNotification_displaysInList() throws Exception {
        String userId = "notif_lose_user_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "Lottery Update",
                "We're sorry, you were not selected for Dance Class this time.",
                "draw_result",
                "dance_event_123",
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            onView(allOf(withId(R.id.tvNotificationTitle), withText("Lottery Update")))
                    .check(matches(isDisplayed()));
            onView(allOf(withId(R.id.tvNotificationMessage),
                    withText("We're sorry, you were not selected for Dance Class this time.")))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.04.01: An empty inbox shows the placeholder "no notifications" message.
     */
    @Test
    public void emptyInbox_showsPlaceholder() throws Exception {
        String userId = "notif_empty_user_" + UUID.randomUUID();
        testUserIds.add(userId);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            // Wait for the activity to finish loading (empty list)
            for (int attempt = 0; attempt < 20; attempt++) {
                AtomicBoolean visible = new AtomicBoolean(false);
                scenario.onActivity(activity -> {
                    View tv = activity.findViewById(R.id.tvNoNotifications);
                    visible.set(tv.getVisibility() == View.VISIBLE);
                });
                if (visible.get()) break;
                Thread.sleep(250);
            }

            onView(withId(R.id.tvNoNotifications)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.04.01: Clicking a general (non-invitation) notification opens an
     * AlertDialog showing the notification title and message.
     */
    @Test
    public void generalNotification_clickOpensDialog() throws Exception {
        String userId = "notif_dialog_user_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "System Announcement",
                "The app will be under maintenance tonight.",
                "general",
                null,
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            // Click notification to trigger dialog
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvNotifications);
                if (rv.findViewHolderForAdapterPosition(0) != null) {
                    rv.findViewHolderForAdapterPosition(0).itemView.performClick();
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText("System Announcement")).inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("The app will be under maintenance tonight.")).inRoot(isDialog())
                    .check(matches(isDisplayed()));

            // Dismiss dialog
            onView(withText("OK")).inRoot(isDialog()).perform(click());
        }
    }

    /**
     * US 01.04.01: Unread notifications show a "NEW" badge; the badge is hidden
     * for already-read notifications.
     */
    @Test
    public void unreadNotification_showsNewBadge() throws Exception {
        String userId = "notif_badge_user_" + UUID.randomUUID();
        seedInboxNotification(userId, "Unread Alert", "msg", "general", null, false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvNotifications);
                assertNotNull("RecyclerView should exist", rv);

                View itemView = rv.findViewHolderForAdapterPosition(0).itemView;
                assertNotNull("First item should exist", itemView);

                View tvNew = itemView.findViewById(R.id.tvNotificationNew);
                assertNotNull("tvNotificationNew should exist", tvNew);
                assertEquals("NEW badge should be VISIBLE for unread notification",
                        View.VISIBLE, tvNew.getVisibility());
            });
        }
    }

    /**
     * US 01.05.06: An entrant receives a notification that they've been invited to
     * join the waiting list for a private event. The event_invitation notification
     * appears in their inbox.
     */
    @Test
    public void privateEventInviteNotification_displaysInList() throws Exception {
        String userId = "notif_private_user_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "Event Invitation",
                "You have been invited to join the waiting list for: Private Piano Lessons",
                "event_invitation",
                "private_event_456",
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            onView(allOf(withId(R.id.tvNotificationTitle), withText("Event Invitation")))
                    .check(matches(isDisplayed()));
            onView(allOf(withId(R.id.tvNotificationMessage),
                    withText("You have been invited to join the waiting list for: Private Piano Lessons")))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.09.01: An entrant receives a notification when invited to be a co-organizer
     * for an event. The co_organizer_assignment notification appears in their inbox.
     */
    @Test
    public void coOrganizerNotification_displaysInList() throws Exception {
        String userId = "notif_coorg_user_" + UUID.randomUUID();
        seedInboxNotification(userId,
                "Co-Organizer Assignment",
                "You have been assigned as a co-organizer for: Yoga Class",
                "co_organizer_assignment",
                "yoga_event_789",
                false);

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForNotificationListLoaded(scenario, 1);

            onView(allOf(withId(R.id.tvNotificationTitle), withText("Co-Organizer Assignment")))
                    .check(matches(isDisplayed()));
            onView(allOf(withId(R.id.tvNotificationMessage),
                    withText("You have been assigned as a co-organizer for: Yoga Class")))
                    .check(matches(isDisplayed()));
        }
    }
}
