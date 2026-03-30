package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

    private static final String TEST_EVENT_ID = "test_poster_base64_event";
    private static final String TEST_POSTER = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        db = FirebaseFirestore.getInstance();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", TEST_EVENT_ID);
        event.put("title", "Poster Test Event");
        event.put("details", "Testing poster Base64 storage.");
        event.put("organizerId", "test_organizer");
        event.put("posterBase64", TEST_POSTER);
        event.put("status", "open");

        Tasks.await(db.collection(FirestorePaths.EVENTS)
                .document(TEST_EVENT_ID).set(event), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.collection(FirestorePaths.EVENTS)
                .document(TEST_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
    }

    // US 02.04.01: posterBase64 field should be persisted in Firestore
    @Test
    public void testPosterBase64IsStoredInFirestore()
            throws InterruptedException, ExecutionException, TimeoutException {
        DocumentSnapshot doc = Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).get(),
                10, TimeUnit.SECONDS);
        assertEquals(TEST_POSTER, doc.getString("posterBase64"));
    }

    // US 03.03.01: Clearing posterBase64 to null should remove the poster from Firestore
    @Test
    public void testClearPosterBase64InFirestore()
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID)
                .update("posterBase64", null), 10, TimeUnit.SECONDS);

        DocumentSnapshot doc = Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).get(),
                10, TimeUnit.SECONDS);
        assertNull("posterBase64 should be null after clearing", doc.getString("posterBase64"));
        assertNotNull("Event document should still exist", doc.getData());
    }

    // US 03.03.01: Delete button should be disabled when poster is null
    @Test
    public void testDeleteButtonDisabledWhenNoPoster() {
        Event event = new Event();
        event.setTitle("No Poster Event");
        event.setDetails("Event without poster.");
        event.setPosterBase64(null);
        AdminImageDetailsActivity.testEvent = event;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminImageDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteImage)).check(matches(not(isEnabled())));
        } finally {
            AdminImageDetailsActivity.testEvent = null;
        }
    }

    // US 03.03.01: Delete button should be enabled when poster exists
    @Test
    public void testDeleteButtonEnabledWhenPosterExists() {
        Event event = new Event();
        event.setTitle("Has Poster Event");
        event.setDetails("Event with poster.");
        event.setPosterBase64(TEST_POSTER);
        AdminImageDetailsActivity.testEvent = event;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminImageDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteImage)).check(matches(isEnabled()));
        } finally {
            AdminImageDetailsActivity.testEvent = null;
        }
    }

    // US 03.03.01: Clicking delete should show confirmation dialog
    @Test
    public void testDeletePosterShowsConfirmation() {
        Event event = new Event();
        event.setTitle("Delete Poster Event");
        event.setDetails("Testing delete confirmation.");
        event.setPosterBase64(TEST_POSTER);
        AdminImageDetailsActivity.testEvent = event;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminImageDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteImage)).perform(click());
            onView(withText(R.string.confirm_deletion)).check(matches(isDisplayed()));
            onView(withText(R.string.confirm_delete_image)).check(matches(isDisplayed()));
        } finally {
            AdminImageDetailsActivity.testEvent = null;
        }
    }
}
