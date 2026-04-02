package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for organizer QR management.
 *
 * US 02.01.01: Create a public event and generate a promotional QR code.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerQrFlowTest {

    private static final String TEST_USER_ID = "test_organizer_123";
    private static final String OTHER_USER_ID = "other_organizer_456";
    private static final String OWN_EVENT_ID = "qr_flow_event_" + Long.toHexString(System.currentTimeMillis());
    private static final String OTHER_EVENT_ID = OWN_EVENT_ID + "_other";

    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        deleteEventsForOrganizer(TEST_USER_ID);
        deleteEventsForOrganizer(OTHER_USER_ID);
        seedEvent(OWN_EVENT_ID, TEST_USER_ID, "Organizer QR Event", "QR_CONTENT_FOR_ORGANIZER");
        seedEvent(OTHER_EVENT_ID, OTHER_USER_ID, "Other Organizer QR Event", "QR_CONTENT_FOR_OTHER");
    }

    @After
    public void tearDown() throws Exception {
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(OWN_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(OTHER_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
    }

    private void deleteEventsForOrganizer(String organizerId) throws Exception {
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.EVENTS)
                        .whereEqualTo("organizerId", organizerId)
                        .get(),
                10,
                TimeUnit.SECONDS
        )) {
            Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
        }
    }

    private void seedEvent(String eventId, String organizerId, String title, String qrCodeContent) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("organizerId", organizerId);
        event.put("qrCodeContent", qrCodeContent);
        event.put("status", "open");
        event.put("private", false);
        event.put("createdAt", Timestamp.now());

        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private Intent createLaunchIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        return new Intent(context, OrganizerQrEventListActivity.class)
                .putExtra("userId", TEST_USER_ID);
    }

    private void waitForQrEventCount(ActivityScenario<OrganizerQrEventListActivity> scenario, int expectedCount)
            throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            int[] actualCount = new int[1];
            scenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvQrEvents);
                actualCount[0] = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
            });

            if (actualCount[0] == expectedCount) {
                return;
            }

            lastError = new AssertionError("Timed out waiting for QR event count " + expectedCount
                    + " but saw " + actualCount[0]);
            Thread.sleep(250);
        }
        throw lastError;
    }

    /**
     * US 02.01.01: The organizer QR list loads only the current organizer's
     * events so generated QR codes can be managed per organizer account.
     */
    @Test
    public void testQrEventListLoadsOnlyCurrentOrganizersEvents() throws Exception {
        try (ActivityScenario<OrganizerQrEventListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            waitForQrEventCount(scenario, 1);

            onView(withText("Organizer QR Event")).check(matches(isDisplayed()));
            onView(withText("Other Organizer QR Event")).check(doesNotExist());

            scenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvQrEvents);
                assertEquals(1, recyclerView.getAdapter().getItemCount());
            });
        }
    }

    /**
     * US 02.01.01: Selecting an organizer event from the QR list opens the
     * QR detail screen with that event's title and generated QR image.
     */
    @Test
    public void testSelectingQrEventOpensDetailScreen() throws Exception {
        try (ActivityScenario<OrganizerQrEventListActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            waitForQrEventCount(scenario, 1);

            onView(withId(R.id.rvQrEvents)).perform(
                    RecyclerViewActions.actionOnItem(
                            hasDescendant(withText("Organizer QR Event")),
                            click()
                    )
            );

            onView(withId(R.id.tvDetailEventTitle)).check(matches(withText("Organizer QR Event")));
            onView(withId(R.id.ivQrCodeLarge)).check(matches(isDisplayed()));
        }
    }
}
