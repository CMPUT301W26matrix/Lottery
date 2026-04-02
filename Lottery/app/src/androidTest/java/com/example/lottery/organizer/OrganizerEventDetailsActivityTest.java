package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for OrganizerEventDetailsActivity.
 * Verifies that the activity correctly handles intents and displays event data.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsActivityTest {

    private static final String TEST_PRIVATE_EVENT_ID = "test_private_event_details";
    private static final String TEST_PUBLIC_EVENT_ID = "test_public_event_details";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String TEST_INVITE_TARGET = "test_invite_target_user";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        Intents.init();
        db = FirebaseFirestore.getInstance();

        // Seed a private event and a public event for invite-related tests
        CountDownLatch latch = new CountDownLatch(3);

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", TEST_PRIVATE_EVENT_ID);
        event.put("title", "Test Private Event");
        event.put("organizerId", TEST_USER_ID);
        event.put("private", true);
        event.put("capacity", 10);
        event.put("status", "open");
        event.put("createdAt", Timestamp.now());
        db.collection(FirestorePaths.EVENTS).document(TEST_PRIVATE_EVENT_ID).set(event)
                .addOnCompleteListener(t -> latch.countDown());

        // Seed a public event
        Map<String, Object> publicEvent = new HashMap<>();
        publicEvent.put("eventId", TEST_PUBLIC_EVENT_ID);
        publicEvent.put("title", "Test Public Event");
        publicEvent.put("organizerId", TEST_USER_ID);
        publicEvent.put("private", false);
        publicEvent.put("capacity", 20);
        publicEvent.put("status", "open");
        publicEvent.put("createdAt", Timestamp.now());
        db.collection(FirestorePaths.EVENTS).document(TEST_PUBLIC_EVENT_ID).set(publicEvent)
                .addOnCompleteListener(t -> latch.countDown());

        // Seed an entrant user that can be invited
        Map<String, Object> user = new HashMap<>();
        user.put("userId", TEST_INVITE_TARGET);
        user.put("username", "InviteTestUser");
        user.put("email", "invite@test.com");
        user.put("role", "ENTRANT");
        user.put("notificationsEnabled", true);
        db.collection(FirestorePaths.USERS).document(TEST_INVITE_TARGET).set(user)
                .addOnCompleteListener(t -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws InterruptedException {
        Intents.release();

        // Clean up seeded data
        CountDownLatch latch = new CountDownLatch(3);
        db.collection(FirestorePaths.EVENTS).document(TEST_PRIVATE_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.EVENTS).document(TEST_PUBLIC_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.USERS).document(TEST_INVITE_TARGET)
                .delete().addOnCompleteListener(t -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);

        // Clean up any waitingList entries and inbox items created by invite flow
        CountDownLatch latch2 = new CountDownLatch(2);
        db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                .get().addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
                    latch2.countDown();
                }).addOnFailureListener(e -> latch2.countDown());
        db.collection(FirestorePaths.userInbox(TEST_INVITE_TARGET))
                .get().addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
                    latch2.countDown();
                }).addOnFailureListener(e -> latch2.countDown());
        latch2.await(10, TimeUnit.SECONDS);
    }

    /**
     * US 02.01.04: Opening an organizer's event details page with an eventId
     * shows the main event details surface for that event.
     */
    @Test
    public void testActivityLaunchWithIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_event_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Check if the main components are displayed
            onView(ViewMatchers.withId(R.id.tvEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.04 / US 02.04.01: The organizer event-details page renders the
     * core schedule and waiting-list controls needed to manage the event.
     */
    @Test
    public void testUIDisplay() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "dummy_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvDetailsHeader))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            onView(withId(R.id.tvScheduledDate))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            onView(withId(R.id.btnViewWaitingList))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    /**
     * US 02.04.01: The organizer event-details page reserves space for the event
     * poster so visual information is available to entrants.
     */
    @Test
    public void testPosterImageViewVisibility() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "test_poster_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the poster container and image are displayed
            onView(withId(R.id.cvPoster)).check(matches(isDisplayed()));
            onView(withId(R.id.ivEventPoster)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.04.02: Editing an event poster starts from the organizer details page
     * and forwards the selected eventId into the edit flow.
     */
    @Test
    public void testEditEventButtonLaunchesEditScreen() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "edit_target_event");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnEditEvent)).check(matches(isDisplayed()));
            onView(withId(R.id.btnEditEvent)).perform(click());

            intended(allOf(
                    hasComponent(OrganizerCreateEventActivity.class.getName()),
                    hasExtra("eventId", "edit_target_event")
            ));
        }
    }

    /**
     * US 03.01.01: Organizer event details do not expose the administrator-only
     * event removal control reserved for admin moderation.
     */
    @Test
    public void testOrganizerScreenDoesNotExposeAdminDeleteButton() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "organizer_event_id");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).check(doesNotExist());
        }
    }

    /**
     * US 02.02.01 / US 02.06.01: Clicking "View Waiting List" navigates to
     * EntrantsListActivity with the correct eventId, so the organizer can
     * view entrant lists and see where they joined from on a map.
     */
    @Test
    public void testViewWaitingList_navigatesToEntrantsList() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "nav_test_event");
        intent.putExtra("userId", "test_user_id");

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnViewWaitingList)).perform(scrollTo(), click());

            intended(allOf(
                    hasComponent(EntrantsListActivity.class.getName()),
                    hasExtra("eventId", "nav_test_event")
            ));
        }
    }

    /**
     * US 02.01.03: The invite button is visible when the event is private,
     * allowing the organizer to invite specific entrants.
     */
    @Test
    public void testInviteButtonVisibleForPrivateEvent() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for Firestore to load the event
            Thread.sleep(3000);
            onView(withId(R.id.btnInviteEntrant)).perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.03: The private event chip is displayed for private events.
     */
    @Test
    public void testPrivateChipVisibleForPrivateEvent() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.chipPrivate)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.03: Clicking the invite button opens the invite entrant dialog.
     */
    @Test
    public void testInviteButtonOpensDialog() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnInviteEntrant)).perform(scrollTo(), click());

            // Verify the invite dialog's search input is displayed
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.03: The invite button is NOT visible for public events.
     */
    @Test
    public void testInviteButtonHiddenForPublicEvent() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PUBLIC_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for Firestore to load the real public event
            Thread.sleep(3000);
            onView(withId(R.id.btnInviteEntrant))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }
}
