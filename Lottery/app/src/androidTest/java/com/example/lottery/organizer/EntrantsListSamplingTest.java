package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
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
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for the sampling (lottery draw) flow in EntrantsListActivity.
 * <p>
 * US 02.05.01: Send notification to chosen entrants to sign up (lottery win).
 * US 02.05.02: Sample a specified number of attendees from the waiting list.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsListSamplingTest {

    private static final String TEST_EVENT_ID = "test_sampling_event";
    private static final String TEST_USER_ID = "test_sampling_organizer";
    private static final int EVENT_CAPACITY = 10;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEntrantIds = new HashSet<>();
    private final Set<String> seededUserIds = new HashSet<>();

    /**
     * Seed the event document and waitlisted entrants BEFORE launching the Activity,
     * because EntrantsListActivity reads capacity from Firestore in onCreate.
     */
    @Before
    public void seedTestData() throws Exception {
        // Seed the event document with capacity
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", TEST_EVENT_ID);
        event.put("title", "Sampling Test Event");
        event.put("organizerId", TEST_USER_ID);
        event.put("capacity", EVENT_CAPACITY);
        event.put("status", "open");
        event.put("private", false);
        event.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).set(event),
                10, TimeUnit.SECONDS);

        // Seed 3 waitlisted entrants
        for (int i = 1; i <= 3; i++) {
            String userId = "test_sample_entrant_" + i;
            seedWaitlistedEntrant(userId, "Entrant " + i, "entrant" + i + "@test.com");
            seedUserDocument(userId);
        }
    }

    @After
    public void cleanUpTestData() throws Exception {
        // Wait for async writes from sampling/notifications to complete
        Thread.sleep(3000);

        // Clean up waitingList entries
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID)).get(),
                10, TimeUnit.SECONDS)) {
            Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
        }

        // Clean up notification documents created by autoNotifyDrawResults
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.NOTIFICATIONS)
                        .whereEqualTo("eventId", TEST_EVENT_ID).get(),
                10, TimeUnit.SECONDS)) {
            String notifId = doc.getId();
            // Delete recipients sub-collection
            for (QueryDocumentSnapshot r : Tasks.await(
                    db.collection(FirestorePaths.notificationRecipients(notifId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(r.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
        }

        // Clean up user inbox items and user documents
        for (String userId : seededUserIds) {
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.userInbox(userId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            Tasks.await(db.collection(FirestorePaths.USERS).document(userId).delete(),
                    10, TimeUnit.SECONDS);
        }

        // Clean up the event document
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
    }

    private void seedWaitlistedEntrant(String userId, String name, String email) throws Exception {
        seededEntrantIds.add(userId);
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", name);
        record.put("email", email);
        record.put("status", InvitationFlowUtil.STATUS_WAITLISTED);
        record.put("registeredAt", Timestamp.now());
        record.put("waitlistedAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                .document(userId).set(record), 10, TimeUnit.SECONDS);
    }

    private void seedUserDocument(String userId) throws Exception {
        seededUserIds.add(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", userId);
        user.put("email", userId + "@test.com");
        user.put("notificationsEnabled", true);
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user),
                10, TimeUnit.SECONDS);
    }

    private Intent createLaunchIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantsListActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);
        return intent;
    }

    /**
     * US 02.05.02 – Organizer samples 2 out of 3 waitlisted entrants.
     * Verifies that exactly 2 are set to "invited" and 1 to "not_selected" in Firestore.
     */
    @Test
    public void testSampling_changesEntrantStatuses() throws Exception {
        try (ActivityScenario<EntrantsListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            // Wait for the waitlist to load
            Thread.sleep(3000);

            // Open sample dialog
            onView(withId(R.id.entrants_list_sample_btn)).perform(click());
            onView(withText("Sample Winners")).check(matches(isDisplayed()));

            // Enter sample size = 2
            onView(withId(R.id.input_sampling_size))
                    .perform(typeText("2"), closeSoftKeyboard());
            onView(withText("Ok")).perform(click());

            // Wait for Firestore batch commit
            Thread.sleep(3000);
        }

        // Verify Firestore: exactly 2 invited + 1 not_selected
        int invited = 0;
        int notSelected = 0;
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID)).get(),
                10, TimeUnit.SECONDS)) {
            String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
            if ("invited".equals(status)) invited++;
            else if ("not_selected".equals(status)) notSelected++;
        }
        assertEquals("Should have exactly 2 invited entrants", 2, invited);
        assertEquals("Should have exactly 1 not-selected entrant", 1, notSelected);
    }

    /**
     * US 02.05.01 – After sampling, notifications are automatically sent to
     * chosen (invited) entrants informing them they won the lottery.
     */
    @Test
    public void testSampling_createsWinnerNotification() throws Exception {
        try (ActivityScenario<EntrantsListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            Thread.sleep(3000);

            onView(withId(R.id.entrants_list_sample_btn)).perform(click());
            onView(withId(R.id.input_sampling_size))
                    .perform(typeText("2"), closeSoftKeyboard());
            onView(withText("Ok")).perform(click());

            Thread.sleep(3000);
        }

        // Verify a winner notification document exists
        boolean winnerNotifFound = false;
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.NOTIFICATIONS)
                        .whereEqualTo("eventId", TEST_EVENT_ID)
                        .whereEqualTo("type", "event_invitation").get(),
                10, TimeUnit.SECONDS)) {
            String title = doc.getString("title");
            assertNotNull(title);
            winnerNotifFound = true;
        }
        assertEquals("Winner notification should exist in Firestore", true, winnerNotifFound);
    }

    /**
     * US 02.05.01 – After sampling, a notification is also sent to
     * non-selected entrants informing them they were not chosen.
     */
    @Test
    public void testSampling_createsNonWinnerNotification() throws Exception {
        try (ActivityScenario<EntrantsListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            Thread.sleep(3000);

            onView(withId(R.id.entrants_list_sample_btn)).perform(click());
            onView(withId(R.id.input_sampling_size))
                    .perform(typeText("2"), closeSoftKeyboard());
            onView(withText("Ok")).perform(click());

            Thread.sleep(3000);
        }

        // Verify a non-winner notification document exists
        boolean nonWinnerNotifFound = false;
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.NOTIFICATIONS)
                        .whereEqualTo("eventId", TEST_EVENT_ID)
                        .whereEqualTo("type", "draw_result").get(),
                10, TimeUnit.SECONDS)) {
            assertNotNull(doc.getString("title"));
            nonWinnerNotifFound = true;
        }
        assertEquals("Non-winner notification should exist in Firestore", true, nonWinnerNotifFound);
    }

    /**
     * US 02.05.01 – Winner notifications are delivered to each invited entrant's inbox.
     */
    @Test
    public void testSampling_deliversToWinnerInbox() throws Exception {
        try (ActivityScenario<EntrantsListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            Thread.sleep(3000);

            onView(withId(R.id.entrants_list_sample_btn)).perform(click());
            onView(withId(R.id.input_sampling_size))
                    .perform(typeText("2"), closeSoftKeyboard());
            onView(withText("Ok")).perform(click());

            Thread.sleep(3000);
        }

        // Find which entrants were invited, then check their inbox
        int inboxCount = 0;
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID)).get(),
                10, TimeUnit.SECONDS)) {
            String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
            if ("invited".equals(status)) {
                String userId = doc.getId();
                for (QueryDocumentSnapshot inbox : Tasks.await(
                        db.collection(FirestorePaths.userInbox(userId)).get(),
                        10, TimeUnit.SECONDS)) {
                    if ("event_invitation".equals(inbox.getString("type"))) {
                        inboxCount++;
                    }
                }
            }
        }
        assertEquals("Each invited entrant should have a winner notification in inbox", 2, inboxCount);
    }

    /**
     * US 02.05.02 – Sampling with size exceeding maxSampleSize
     * (min of remaining capacity and waitlisted count) is rejected,
     * and no status changes occur.
     */
    @Test
    public void testSampling_rejectsOversizedSample() throws Exception {
        try (ActivityScenario<EntrantsListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            Thread.sleep(3000);

            onView(withId(R.id.entrants_list_sample_btn)).perform(click());

            // Enter sample size larger than capacity (3 entrants but ask for 20)
            onView(withId(R.id.input_sampling_size))
                    .perform(typeText("20"), closeSoftKeyboard());
            onView(withText("Ok")).perform(click());

            Thread.sleep(2000);
        }

        // All entrants should still be waitlisted (no change)
        for (QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID)).get(),
                10, TimeUnit.SECONDS)) {
            String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
            assertEquals("Entrant should still be waitlisted after rejected sample",
                    "waitlisted", status);
        }
    }
}
