package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented tests for poster Base64 storage and deletion.
 *
 * <p>Covers User Stories:</p>
 * <ul>
 *   <li>US 02.04.01: As an organizer, I want to upload an event poster so entrants
 *       can see what the event looks like.</li>
 *   <li>US 03.03.01: As an administrator, I want to be able to remove images.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class PosterBase64InstrumentedTest {
    private static final long FIRESTORE_TIMEOUT_SECONDS = 45;

    private static final String TEST_EVENT_ID = "swimming_course_poster_review";
    private static final String TEST_POSTER = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ";
    private FirebaseFirestore db;

    private void ensureFirestoreNetworkEnabled()
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.enableNetwork(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        db = FirebaseFirestore.getInstance();
        ensureFirestoreNetworkEnabled();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", TEST_EVENT_ID);
        event.put("title", "Swimming Course Poster Review Session");
        event.put("details", "Administrator image moderation coverage for a community swimming course poster.");
        event.put("organizerId", "coach_nadia_rahman");
        event.put("posterBase64", TEST_POSTER);
        event.put("status", "open");

        Tasks.await(db.collection(FirestorePaths.EVENTS)
                .document(TEST_EVENT_ID).set(event), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        try {
            ensureFirestoreNetworkEnabled();
            Tasks.await(db.collection(FirestorePaths.EVENTS)
                    .document(TEST_EVENT_ID).delete(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // US 02.04.01: posterBase64 field should be persisted in Firestore
    @Test
    public void testPosterBase64IsStoredInFirestore()
            throws InterruptedException, ExecutionException, TimeoutException {
        DocumentSnapshot doc = Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).get(),
                FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(TEST_POSTER, doc.getString("posterBase64"));
    }

    // US 03.03.01: Admin deleting an image through the UI should clear posterBase64 in
    // Firestore instead of only exercising a direct database update.
    @Test
    public void testAdminDeletePosterFlow_clearsPosterBase64ViaUi()
            throws InterruptedException, ExecutionException, TimeoutException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        intent.putExtra("userId", "admin_jordan_clark");

        try (ActivityScenario<AdminImageDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteImage)).check(matches(isEnabled()));
            onView(withId(R.id.btnDeleteImage)).perform(click());
            onView(withText(R.string.confirm)).perform(click());
        }

        DocumentSnapshot doc = Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).get(),
                FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNull("posterBase64 should be cleared after admin confirms deletion through the UI",
                doc.getString("posterBase64"));
        assertNotNull("Event document should still exist after poster deletion", doc.getData());
    }
}
