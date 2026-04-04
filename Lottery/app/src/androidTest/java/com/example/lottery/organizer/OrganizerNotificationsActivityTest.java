package com.example.lottery.organizer;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.adapter.OrganizerNotificationEventAdapter;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationsActivityTest {

    private static final String TEST_USER_ID = "test_organizer_456";
    private static final String TEST_EVENT_ID = "event_123";

    private static final String ENTRANT_WAITLISTED = "test_entrant_waitlisted";
    private static final String ENTRANT_INVITED = "test_entrant_invited";
    private static final String ENTRANT_ACCEPTED = "test_entrant_accepted";
    private static final String ENTRANT_CANCELLED = "test_entrant_cancelled";

    private FirebaseFirestore db;

    /**
     * Seed test users and waitingList entrants
     * so that each notification group has a real recipient.
     * Also ensures no stale events exist for TEST_USER_ID.
     */
    @Before
    public void seedTestData() throws InterruptedException {
        db = FirebaseFirestore.getInstance();

        // Clean up any stale events owned by the test organizer to ensure
        // testNoEventsEmptyState is hermetic and not affected by external state.
        CountDownLatch cleanLatch = new CountDownLatch(1);
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", TEST_USER_ID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        cleanLatch.countDown();
                        return;
                    }
                    int[] remaining = {snapshot.size()};
                    for (QueryDocumentSnapshot doc : snapshot) {
                        doc.getReference().delete().addOnCompleteListener(t -> {
                            if (--remaining[0] == 0) cleanLatch.countDown();
                        });
                    }
                })
                .addOnFailureListener(e -> cleanLatch.countDown());
        cleanLatch.await(10, TimeUnit.SECONDS);

        CountDownLatch latch = new CountDownLatch(8);

        // Seed 4 test users with notifications enabled
        String[] entrantIds = {ENTRANT_WAITLISTED, ENTRANT_INVITED, ENTRANT_ACCEPTED, ENTRANT_CANCELLED};
        for (String id : entrantIds) {
            Map<String, Object> user = new HashMap<>();
            user.put("name", id);
            user.put("notificationsEnabled", true);
            db.collection(FirestorePaths.USERS).document(id).set(user)
                    .addOnCompleteListener(t -> latch.countDown());
        }

        // Seed 4 waitingList entries with different statuses
        String[] statuses = {"waitlisted", "invited", "accepted", "cancelled"};
        for (int i = 0; i < entrantIds.length; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", statuses[i]);
            db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                    .document(entrantIds[i]).set(entry)
                    .addOnCompleteListener(t -> latch.countDown());
        }

        latch.await(10, TimeUnit.SECONDS);
    }

    /**
     * Clean up all test data from Firestore:
     * notifications, notification recipients, user inbox items,
     * waitingList entries, and test user documents.
     */
    @After
    public void cleanUpTestData() throws InterruptedException {
        Thread.sleep(2000);

        CountDownLatch latch = new CountDownLatch(1);

        // 1. Delete notifications + their recipients sub-collection
        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("senderId", TEST_USER_ID)
                .whereEqualTo("eventId", TEST_EVENT_ID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        latch.countDown();
                        return;
                    }
                    int[] remaining = {snapshot.size()};
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String notifId = doc.getId();
                        // Delete recipients sub-collection entries
                        db.collection(FirestorePaths.notificationRecipients(notifId))
                                .get().addOnSuccessListener(recipSnap -> {
                                    for (QueryDocumentSnapshot r : recipSnap) {
                                        r.getReference().delete();
                                    }
                                });
                        // Delete the notification document itself
                        doc.getReference().delete().addOnCompleteListener(t -> {
                            if (--remaining[0] == 0) latch.countDown();
                        });
                    }
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);

        // 2. Delete seeded waitingList entries, user inbox items, and user documents
        String[] entrantIds = {ENTRANT_WAITLISTED, ENTRANT_INVITED, ENTRANT_ACCEPTED, ENTRANT_CANCELLED};
        CountDownLatch latch2 = new CountDownLatch(entrantIds.length * 3);
        for (String id : entrantIds) {
            db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID)).document(id)
                    .delete().addOnCompleteListener(t -> latch2.countDown());
            // Delete inbox items written by the notification flow
            db.collection(FirestorePaths.userInbox(id))
                    .get().addOnSuccessListener(inboxSnap -> {
                        for (QueryDocumentSnapshot doc : inboxSnap) {
                            doc.getReference().delete();
                        }
                        latch2.countDown();
                    }).addOnFailureListener(e -> latch2.countDown());
            db.collection(FirestorePaths.USERS).document(id)
                    .delete().addOnCompleteListener(t -> latch2.countDown());
        }

        latch2.await(10, TimeUnit.SECONDS);
    }

    /**
     * Queries Firestore to verify that a notification document was actually persisted
     * with the expected group value for the test organizer and event.
     */
    private void assertNotificationPersistedWithGroup(String expectedGroup) throws InterruptedException {
        // Wait for the async Firestore write triggered by sendNotification()
        Thread.sleep(3000);

        CountDownLatch latch = new CountDownLatch(1);
        boolean[] found = {false};

        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("senderId", TEST_USER_ID)
                .whereEqualTo("eventId", TEST_EVENT_ID)
                .whereEqualTo("group", expectedGroup)
                .get()
                .addOnSuccessListener(snapshot -> {
                    found[0] = !snapshot.isEmpty();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Notification with group '" + expectedGroup + "' should exist in Firestore", found[0]);
    }

    private String waitForNotificationIdWithGroup(String expectedGroup) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection(FirestorePaths.NOTIFICATIONS)
                            .whereEqualTo("senderId", TEST_USER_ID)
                            .whereEqualTo("eventId", TEST_EVENT_ID)
                            .whereEqualTo("group", expectedGroup)
                            .get(),
                    10,
                    TimeUnit.SECONDS
            );

            if (!snapshot.isEmpty()) {
                return snapshot.getDocuments().get(0).getId();
            }
            Thread.sleep(250);
        }

        throw new AssertionError("Timed out waiting for notification group " + expectedGroup);
    }

    private void waitForNotificationDelivery(String notificationId, String expectedRecipientId) throws Exception {
        String[] allEntrantIds = {
                ENTRANT_WAITLISTED,
                ENTRANT_INVITED,
                ENTRANT_ACCEPTED,
                ENTRANT_CANCELLED
        };

        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            QuerySnapshot recipientSnapshot = Tasks.await(
                    db.collection(FirestorePaths.notificationRecipients(notificationId)).get(),
                    10,
                    TimeUnit.SECONDS
            );

            boolean inboxStateMatches = true;
            for (String entrantId : allEntrantIds) {
                boolean inboxExists = Tasks.await(
                        db.collection(FirestorePaths.userInbox(entrantId))
                                .document(notificationId)
                                .get(),
                        10,
                        TimeUnit.SECONDS
                ).exists();
                boolean shouldExist = expectedRecipientId.equals(entrantId);
                if (inboxExists != shouldExist) {
                    inboxStateMatches = false;
                    break;
                }
            }

            if (recipientSnapshot.size() == 1
                    && expectedRecipientId.equals(recipientSnapshot.getDocuments().get(0).getString("userId"))
                    && inboxStateMatches) {
                return;
            }

            lastError = new AssertionError(
                    "Timed out waiting for recipient and inbox delivery for notification " + notificationId
            );
            Thread.sleep(250);
        }

        throw lastError;
    }

    private void assertNotificationDeliveredToOnlyRecipient(String group, String expectedRecipientId) throws Exception {
        String notificationId = waitForNotificationIdWithGroup(group);
        waitForNotificationDelivery(notificationId, expectedRecipientId);

        QuerySnapshot recipientSnapshot = Tasks.await(
                db.collection(FirestorePaths.notificationRecipients(notificationId)).get(),
                10,
                TimeUnit.SECONDS
        );
        assertEquals("Expected exactly one recipient record for group " + group,
                1, recipientSnapshot.size());
        assertEquals(expectedRecipientId, recipientSnapshot.getDocuments().get(0).getString("userId"));

        String[] allEntrantIds = {
                ENTRANT_WAITLISTED,
                ENTRANT_INVITED,
                ENTRANT_ACCEPTED,
                ENTRANT_CANCELLED
        };

        for (String entrantId : allEntrantIds) {
            boolean inboxExists = Tasks.await(
                    db.collection(FirestorePaths.userInbox(entrantId))
                            .document(notificationId)
                            .get(),
                    10,
                    TimeUnit.SECONDS
            ).exists();

            if (expectedRecipientId.equals(entrantId)) {
                assertTrue("Expected inbox delivery for " + entrantId, inboxExists);
            } else {
                assertFalse("Unexpected inbox delivery for " + entrantId, inboxExists);
            }
        }
    }

    /**
     * Queries Firestore to verify that no notification was persisted
     * for the test organizer and event (used for cancel/empty-message tests).
     */
    private void assertNoNotificationPersisted() throws InterruptedException {
        Thread.sleep(2000);

        CountDownLatch latch = new CountDownLatch(1);
        boolean[] found = {false};

        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("senderId", TEST_USER_ID)
                .whereEqualTo("eventId", TEST_EVENT_ID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    found[0] = !snapshot.isEmpty();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
        assertFalse("No notification should exist in Firestore", found[0]);
    }

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
            event.setEventId(TEST_EVENT_ID);
            event.setTitle("Mock Test Event");
            mockList.add(event);

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

    /**
     * US 02.07.01 – Activity finishes immediately when no userId is provided.
     */
    @Test
    public void testActivityFinishesWithoutUserId() {
        // Clear SharedPreferences so the fallback lookup also returns null
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit().remove("userId").commit();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerNotificationsActivity.class);
        // No userId extra — Activity should call finish() in onCreate
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(DESTROYED, scenario.getState());
        }
    }

    /**
     * US 02.07.01 – "No events found" empty state is shown when organizer has no events.
     */
    @Test
    public void testNoEventsEmptyState() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            // TEST_USER_ID has no real events in Firestore, so the empty state should appear
            Thread.sleep(2000);
            onView(withId(R.id.tvNoEvents)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.07.01 – Cancelling the notification dialog does not send a notification.
     */
    @Test
    public void testCancelNotificationDialog() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyWaiting)).perform(click());
            onView(withText("Notify Waiting List")).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("This should not be sent"), closeSoftKeyboard());
            onView(withText("Cancel")).inRoot(isDialog()).perform(click());

            onView(withText("Notify Waiting List")).check(doesNotExist());
        }
        assertNoNotificationPersisted();
    }

    /**
     * US 02.07.01 – Sending an empty message does not create a notification.
     */
    @Test
    public void testEmptyMessageNotSent() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyWaiting)).perform(click());
            onView(withText("Notify Waiting List")).inRoot(isDialog()).check(matches(isDisplayed()));

            // Leave the input empty and click Send
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            // Dialog should be dismissed (Send button always dismisses the AlertDialog)
            onView(withText("Notify Waiting List")).check(doesNotExist());
        }
        assertNoNotificationPersisted();
    }

    /**
     * US 02.07.01 – Notification activity launches and displays the event list.
     */
    @Test
    public void testActivityLaunch() {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            onView(withId(R.id.tvTitle)).check(matches(withText("Notifications")));
            onView(withId(R.id.rvOrganizerEvents)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.07.01 – Organizer can send a notification to all entrants on the waiting list.
     */
    @Test
    public void testSendNotificationToWaitlist() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyWaiting)).perform(click());
            onView(withText("Notify Waiting List")).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Hello waiting list!"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            onView(withText("Notify Waiting List")).check(doesNotExist());
        }
        assertNotificationPersistedWithGroup("waitlisted");
    }

    /**
     * US 02.07.01 – Sending to the waiting list writes a delivered recipient record
     * and inbox notification only for waitlisted entrants.
     */
    @Test
    public void testSendNotificationToWaitlist_deliversRecipientAndInbox() throws Exception {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyWaiting)).perform(click());
            onView(withText("Notify Waiting List")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Waitlist delivery check"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());
        }

        assertNotificationDeliveredToOnlyRecipient("waitlisted", ENTRANT_WAITLISTED);
    }

    /**
     * US 02.07.02 – Organizer can send a notification to all selected (invited) entrants.
     */
    @Test
    public void testSendNotificationToInvited() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyMore)).perform(click());
            onView(withText("Notify Invited")).perform(click());
            onView(withText("Notify Invited Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Hello invited entrants!"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            onView(withText("Notify Invited Entrants")).check(doesNotExist());
        }
        assertNotificationPersistedWithGroup("invited");
    }

    /**
     * US 02.07.02 – Sending to selected entrants writes a delivered recipient
     * record and inbox notification only for invited entrants.
     */
    @Test
    public void testSendNotificationToInvited_deliversRecipientAndInbox() throws Exception {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyMore)).perform(click());
            onView(withText("Notify Invited")).perform(click());
            onView(withText("Notify Invited Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Invited delivery check"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());
        }

        assertNotificationDeliveredToOnlyRecipient("invited", ENTRANT_INVITED);
    }

    /**
     * Regression coverage: accepted-group notifications are an organizer UX affordance
     * in the app, but they do not map to a direct User Story in project_problem_descr.md.
     */
    @Test
    public void testSendNotificationToAccepted() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyMore)).perform(click());
            onView(withText("Notify Accepted")).perform(click());
            onView(withText("Notify Accepted Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Hello accepted entrants!"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            onView(withText("Notify Accepted Entrants")).check(doesNotExist());
        }
        assertNotificationPersistedWithGroup("accepted");
    }

    /**
     * US 02.07.03 – Organizer can send a notification to all cancelled entrants.
     */
    @Test
    public void testSendNotificationToCancelled() throws InterruptedException {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyMore)).perform(click());
            onView(withText("Notify Cancelled")).perform(click());
            onView(withText("Notify Cancelled Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Hello cancelled entrants!"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            onView(withText("Notify Cancelled Entrants")).check(doesNotExist());
        }
        assertNotificationPersistedWithGroup("cancelled");
    }

    /**
     * US 02.07.03 – Sending to cancelled entrants writes a delivered recipient
     * record and inbox notification only for cancelled entrants.
     */
    @Test
    public void testSendNotificationToCancelled_deliversRecipientAndInbox() throws Exception {
        try (ActivityScenario<OrganizerNotificationsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            injectMockEvent(scenario);

            onView(withId(R.id.btnNotifyMore)).perform(click());
            onView(withText("Notify Cancelled")).perform(click());
            onView(withText("Notify Cancelled Entrants")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withId(R.id.etNotificationContent)).inRoot(isDialog())
                    .perform(typeText("Cancelled delivery check"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());
        }

        assertNotificationDeliveredToOnlyRecipient("cancelled", ENTRANT_CANCELLED);
    }
}
