package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
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

/**
 * Instrumented tests for waitlist join/leave operations on {@link EntrantEventDetailsActivity}.
 * Seeds real Firestore data and verifies persistence after UI actions.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsWaitlistTest {

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

    private Intent createIntent(String eventId, String userId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        return intent;
    }

    private void seedOpenEvent(String eventId, long waitingListLimit) throws Exception {
        seededEventIds.add(eventId);
        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Beginner Swimming Lessons");
        event.put("details", "Open to all beginners");
        event.put("place", "Community Pool");
        event.put("capacity", 20L);
        event.put("scheduledDateTime", now);
        event.put("registrationStart", now);
        event.put("registrationDeadline", new Timestamp(
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))));
        event.put("drawDate", new Timestamp(
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(8))));
        event.put("requireLocation", false);
        event.put("waitingListLimit", waitingListLimit);
        event.put("private", false);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private void seedUser(String userId) throws Exception {
        seededUserIds.add(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", "Taylor Singh");
        user.put("email", "test.entrant@example.com");
        user.put("role", "ENTRANT");
        user.put("deviceId", "test_device");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", false);
        user.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user),
                10, TimeUnit.SECONDS);
    }

    private void seedEntrantStatus(String eventId, String userId, String status) throws Exception {
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", "Taylor Singh");
        record.put("email", "test.entrant@example.com");
        record.put("status", status);
        record.put("registeredAt", Timestamp.now());
        record.put("waitlistedAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.eventWaitingList(eventId))
                .document(userId).set(record), 10, TimeUnit.SECONDS);
    }

    private void waitForCondition(
            ActivityScenario<EntrantEventDetailsActivity> scenario,
            java.util.function.Predicate<EntrantEventDetailsActivity> condition) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            scenario.onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) return;
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for activity condition");
    }

    private DocumentSnapshot waitForDocument(String collectionPath, String docId) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            DocumentSnapshot snap = Tasks.await(
                    db.collection(collectionPath).document(docId).get(), 10, TimeUnit.SECONDS);
            if (snap.exists()) return snap;
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for document " + collectionPath + "/" + docId);
    }

    private void waitForDocumentDeletion(String collectionPath, String docId) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            DocumentSnapshot snap = Tasks.await(
                    db.collection(collectionPath).document(docId).get(), 10, TimeUnit.SECONDS);
            if (!snap.exists()) return;
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for document deletion " + collectionPath + "/" + docId);
    }

    /**
     * US 01.01.01 / US 01.06.02: Entrant joins the waiting list for a specific event
     * from the event details page. Verifies that a waitingList record is persisted
     * in Firestore with the correct status and timestamps.
     */
    @Test
    public void joinWaitlist_createsFirestoreRecord() throws Exception {
        String eventId = "test_join_event_" + UUID.randomUUID();
        String userId = "test_join_user_" + UUID.randomUUID();
        seedOpenEvent(eventId, 100L);
        seedUser(userId);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView title = activity.findViewById(R.id.tvEventTitle);
                return title.getText().toString().contains("Swimming");
            });

            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.join_wait_list)));
            onView(withId(R.id.btnWaitlistAction)).perform(click());

            // Verify Firestore: waitingList document created with correct status
            DocumentSnapshot doc = waitForDocument(
                    FirestorePaths.eventWaitingList(eventId), userId);
            assertEquals(InvitationFlowUtil.STATUS_WAITLISTED, doc.getString("status"));
            assertEquals(userId, doc.getString("userId"));
            assertNotNull("registeredAt should be set", doc.getTimestamp("registeredAt"));
            assertNotNull("waitlistedAt should be set", doc.getTimestamp("waitlistedAt"));
        }
    }

    /**
     * US 01.01.02: Entrant leaves the waiting list for a specific event.
     * Verifies that the waitingList record is deleted from Firestore.
     */
    @Test
    public void leaveWaitlist_deletesFirestoreRecord() throws Exception {
        String eventId = "test_leave_event_" + UUID.randomUUID();
        String userId = "test_leave_user_" + UUID.randomUUID();
        seedOpenEvent(eventId, 100L);
        seedUser(userId);
        seedEntrantStatus(eventId, userId, InvitationFlowUtil.STATUS_WAITLISTED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView btn = activity.findViewById(R.id.btnWaitlistAction);
                return activity.getString(R.string.leave_wait_list).contentEquals(btn.getText());
            });

            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.leave_wait_list)));
            onView(withId(R.id.btnWaitlistAction)).perform(click());

            // Verify Firestore: waitingList document deleted
            waitForDocumentDeletion(FirestorePaths.eventWaitingList(eventId), userId);
        }
    }

    /**
     * US 01.01.01: When the waiting list is full (at capacity), clicking "Join Wait List"
     * should NOT create a Firestore record. Verifies the negative path.
     */
    @Test
    public void joinWaitlist_fullWaitlist_doesNotCreateRecord() throws Exception {
        String eventId = "test_full_event_" + UUID.randomUUID();
        String userId = "test_full_user_" + UUID.randomUUID();
        String existingUserId = "existing_entrant_" + UUID.randomUUID();

        seedOpenEvent(eventId, 1L); // waitingListLimit = 1
        seedUser(userId);
        seedEntrantStatus(eventId, existingUserId, InvitationFlowUtil.STATUS_WAITLISTED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView title = activity.findViewById(R.id.tvEventTitle);
                return title.getText().toString().contains("Swimming");
            });

            // Wait for waitlist count to load (should show "1")
            waitForCondition(scenario, activity -> {
                TextView count = activity.findViewById(R.id.tvWaitlistCount);
                return count.getText().toString().contains("1");
            });

            onView(withId(R.id.btnWaitlistAction)).perform(click());

            // Wait and verify the user was NOT added
            Thread.sleep(2000);
            DocumentSnapshot snap = Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).get(),
                    10, TimeUnit.SECONDS);
            assertFalse("User should NOT be added to full waitlist", snap.exists());
        }
    }
}
