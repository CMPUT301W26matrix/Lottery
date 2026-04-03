package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instrumented tests for {@link EntrantEventHistoryActivity}.
 * Seeds real Firestore events and waitingList entries, then verifies the entrant
 * can see their registration history with correct status badges.
 * <p>
 * Covers: US 01.02.03
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventHistoryActivityTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();
    private final Set<String> seededUserIds = new HashSet<>();

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10, TimeUnit.SECONDS);
        }
        for (String userId : seededUserIds) {
            Tasks.await(db.collection(FirestorePaths.USERS).document(userId).delete(),
                    10, TimeUnit.SECONDS);
        }
        seededEventIds.clear();
        seededUserIds.clear();
    }

    private Intent createIntent(String userId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                EntrantEventHistoryActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    private void seedEvent(String eventId, String title, String organizerId) throws Exception {
        seededEventIds.add(eventId);
        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("details", "Test event details");
        event.put("place", "Community Centre");
        event.put("capacity", 20L);
        event.put("organizerId", organizerId);
        event.put("scheduledDateTime", new Timestamp(
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))));
        event.put("registrationDeadline", new Timestamp(
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5))));
        event.put("private", false);
        event.put("createdAt", now);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private void seedOrganizer(String userId, String username) throws Exception {
        seededUserIds.add(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("email", "organizer@example.com");
        user.put("role", "ORGANIZER");
        user.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user),
                10, TimeUnit.SECONDS);
    }

    private void seedWaitlistEntry(String eventId, String userId, String status, int offsetMinutes)
            throws Exception {
        Timestamp registeredAt = new Timestamp(
                new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(offsetMinutes)));
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", "Test Entrant");
        record.put("email", "test@example.com");
        record.put("status", status);
        record.put("registeredAt", registeredAt);
        record.put("waitlistedAt", registeredAt);
        Tasks.await(db.collection(FirestorePaths.eventWaitingList(eventId))
                .document(userId).set(record), 10, TimeUnit.SECONDS);
    }

    private void waitForHistoryLoaded(
            ActivityScenario<EntrantEventHistoryActivity> scenario, int expectedCount) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            AtomicInteger count = new AtomicInteger(-1);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEventHistory);
                if (rv != null && rv.getAdapter() != null) {
                    count.set(rv.getAdapter().getItemCount());
                }
            });
            if (count.get() >= expectedCount) return;
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(500);
        }
        throw new AssertionError("Timed out waiting for history list to load " + expectedCount + " items");
    }

    /**
     * US 01.02.03: An entrant with multiple registrations across different events sees
     * all events in their history list, regardless of whether they were selected or not.
     */
    @Test
    public void historyLoadsSeededRegistrations() throws Exception {
        String userId = "history_user_" + UUID.randomUUID();
        String organizerId = "history_organizer_" + UUID.randomUUID();
        String eventId1 = "history_event_1_" + UUID.randomUUID();
        String eventId2 = "history_event_2_" + UUID.randomUUID();

        seedOrganizer(organizerId, "Coach Thompson");
        seedEvent(eventId1, "Swimming Lessons", organizerId);
        seedEvent(eventId2, "Piano Recital", organizerId);
        seedWaitlistEntry(eventId1, userId, InvitationFlowUtil.STATUS_WAITLISTED, 60);
        seedWaitlistEntry(eventId2, userId, InvitationFlowUtil.STATUS_INVITED, 30);

        try (ActivityScenario<EntrantEventHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForHistoryLoaded(scenario, 2);

            // Verify both events appear by checking adapter count
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEventHistory);
                assertEquals("History should contain 2 events", 2, rv.getAdapter().getItemCount());
            });
        }
    }

    /**
     * US 01.02.03: An entrant with no registrations sees the empty state message
     * instead of the event history list.
     */
    @Test
    public void emptyHistory_showsEmptyState() throws Exception {
        String userId = "history_empty_user_" + UUID.randomUUID();

        try (ActivityScenario<EntrantEventHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            // Wait for loading to finish
            for (int attempt = 0; attempt < 20; attempt++) {
                AtomicBoolean emptyVisible = new AtomicBoolean(false);
                scenario.onActivity(activity -> {
                    View empty = activity.findViewById(R.id.emptyStateContainer);
                    View progress = activity.findViewById(R.id.progressBar);
                    emptyVisible.set(empty.getVisibility() == View.VISIBLE
                            && progress.getVisibility() == View.GONE);
                });
                if (emptyVisible.get()) break;
                Thread.sleep(500);
            }

            scenario.onActivity(activity -> {
                View empty = activity.findViewById(R.id.emptyStateContainer);
                View rv = activity.findViewById(R.id.rvEventHistory);
                assertEquals("Empty state should be visible", View.VISIBLE, empty.getVisibility());
                assertEquals("RecyclerView should be hidden", View.GONE, rv.getVisibility());
            });
        }
    }

    /**
     * US 01.02.03: The history list displays correct user-friendly status badges:
     * "WAITING LIST" for waitlisted, "SELECTED" for invited, "CONFIRMED" for accepted.
     */
    @Test
    public void historyShowsCorrectStatusBadges() throws Exception {
        String userId = "history_badge_user_" + UUID.randomUUID();
        String organizerId = "history_badge_org_" + UUID.randomUUID();
        String eventWaitlisted = "history_wl_event_" + UUID.randomUUID();
        String eventInvited = "history_inv_event_" + UUID.randomUUID();
        String eventAccepted = "history_acc_event_" + UUID.randomUUID();

        seedOrganizer(organizerId, "Coach Smith");
        seedEvent(eventWaitlisted, "Yoga Class", organizerId);
        seedEvent(eventInvited, "Dance Workshop", organizerId);
        seedEvent(eventAccepted, "Art Session", organizerId);
        seedWaitlistEntry(eventWaitlisted, userId, InvitationFlowUtil.STATUS_WAITLISTED, 90);
        seedWaitlistEntry(eventInvited, userId, InvitationFlowUtil.STATUS_INVITED, 60);
        seedWaitlistEntry(eventAccepted, userId, InvitationFlowUtil.STATUS_ACCEPTED, 30);

        try (ActivityScenario<EntrantEventHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(userId))) {
            waitForHistoryLoaded(scenario, 3);

            // Verify status badges by scrolling to items with expected text.
            // RecyclerViewActions.scrollTo ensures items are rendered before matching.
            onView(withId(R.id.rvEventHistory))
                    .perform(scrollTo(hasDescendant(withText("WAITING LIST"))));
            onView(withId(R.id.rvEventHistory))
                    .perform(scrollTo(hasDescendant(withText("SELECTED"))));
            onView(withId(R.id.rvEventHistory))
                    .perform(scrollTo(hasDescendant(withText("CONFIRMED"))));
        }
    }
}
