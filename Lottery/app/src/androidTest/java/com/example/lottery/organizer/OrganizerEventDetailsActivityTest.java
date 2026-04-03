package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
    private static final String TEST_ORGANIZER_NAME = "Organizer Owner";
    private static final String TEST_INVITE_TARGET = "test_invite_target_user";
    private static final String TEST_COORG_TARGET = "test_coorg_target_user";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        Intents.init();
        db = FirebaseFirestore.getInstance();

        // Seed a private event and a public event for invite-related tests
        CountDownLatch latch = new CountDownLatch(5);

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

        // Seed organizer profile so comment/co-organizer flows can resolve display name
        Map<String, Object> organizer = new HashMap<>();
        organizer.put("userId", TEST_USER_ID);
        organizer.put("username", TEST_ORGANIZER_NAME);
        organizer.put("email", "organizer@test.com");
        organizer.put("role", "ORGANIZER");
        db.collection(FirestorePaths.USERS).document(TEST_USER_ID).set(organizer)
                .addOnCompleteListener(t -> latch.countDown());

        // Seed an entrant user that can be invited
        Map<String, Object> user = new HashMap<>();
        user.put("userId", TEST_INVITE_TARGET);
        user.put("username", "InviteTestUser");
        user.put("email", "invite@test.com");
        user.put("phone", "7809991234");
        user.put("role", "ENTRANT");
        user.put("notificationsEnabled", true);
        db.collection(FirestorePaths.USERS).document(TEST_INVITE_TARGET).set(user)
                .addOnCompleteListener(t -> latch.countDown());

        // Seed an entrant user that can be assigned as co-organizer
        Map<String, Object> coOrgUser = new HashMap<>();
        coOrgUser.put("userId", TEST_COORG_TARGET);
        coOrgUser.put("username", "CoOrg Candidate");
        coOrgUser.put("email", "coorg@test.com");
        coOrgUser.put("phone", "7801112222");
        coOrgUser.put("role", "ENTRANT");
        coOrgUser.put("notificationsEnabled", true);
        db.collection(FirestorePaths.USERS).document(TEST_COORG_TARGET).set(coOrgUser)
                .addOnCompleteListener(t -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws InterruptedException {
        Intents.release();

        // Clean up seeded data
        CountDownLatch latch = new CountDownLatch(5);
        db.collection(FirestorePaths.EVENTS).document(TEST_PRIVATE_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.EVENTS).document(TEST_PUBLIC_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.USERS).document(TEST_USER_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.USERS).document(TEST_INVITE_TARGET)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection(FirestorePaths.USERS).document(TEST_COORG_TARGET)
                .delete().addOnCompleteListener(t -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);

        // Clean up waitingList entries, inbox items, comments, and co-organizer docs
        CountDownLatch latch2 = new CountDownLatch(5);
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
        db.collection(FirestorePaths.userInbox(TEST_COORG_TARGET))
                .get().addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
                    latch2.countDown();
                }).addOnFailureListener(e -> latch2.countDown());
        db.collection(FirestorePaths.eventComments(TEST_PRIVATE_EVENT_ID))
                .get().addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
                    latch2.countDown();
                }).addOnFailureListener(e -> latch2.countDown());
        db.collection(FirestorePaths.eventCoOrganizers(TEST_PRIVATE_EVENT_ID))
                .get().addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
                    latch2.countDown();
                }).addOnFailureListener(e -> latch2.countDown());
        latch2.await(10, TimeUnit.SECONDS);
    }

    private ViewAction clickChildViewWithId(int id) {
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(id);
                if (child != null) {
                    child.performClick();
                }
            }
        };
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

    /**
     * US 02.01.03: Searching for an entrant by name and inviting them
     * writes a waitingList entry and an inbox notification in Firestore.
     */
    @Test
    public void testInviteEntrant_searchAndInviteWritesToFirestore() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);

            // Open invite dialog
            onView(withId(R.id.btnInviteEntrant)).perform(scrollTo(), click());
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));

            // Search for the seeded entrant by name
            onView(withId(R.id.etSearch)).perform(replaceText("InviteTestUser"), closeSoftKeyboard());

            // Wait for debounce (300ms) + Firestore query
            Thread.sleep(3000);

            // Click the first search result to trigger invite
            onView(withId(R.id.rvResults)).perform(
                    androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));

            // Wait for Firestore writes
            Thread.sleep(3000);
        }

        // Verify waitingList entry was created
        boolean waitlistExists = Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_INVITE_TARGET).get(),
                10, TimeUnit.SECONDS).exists();
        assertTrue("Invited entrant should have a waitingList entry", waitlistExists);

        // Verify inbox notification was created
        QuerySnapshot inboxSnap = Tasks.await(
                db.collection(FirestorePaths.userInbox(TEST_INVITE_TARGET)).get(),
                10, TimeUnit.SECONDS);
        assertFalse("Invited entrant should have an inbox notification", inboxSnap.isEmpty());
    }

    /**
     * US 02.01.03: Searching by email finds the entrant and invite writes to Firestore.
     */
    @Test
    public void testInviteEntrant_searchByEmailWritesToFirestore() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnInviteEntrant)).perform(scrollTo(), click());
            onView(withId(R.id.etSearch)).perform(replaceText("invite@test"), closeSoftKeyboard());
            Thread.sleep(3000);
            onView(withId(R.id.rvResults)).perform(
                    androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));
            Thread.sleep(3000);
        }

        boolean waitlistExists = Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_INVITE_TARGET).get(),
                10, TimeUnit.SECONDS).exists();
        assertTrue("Email search + invite should create waitingList entry", waitlistExists);
    }

    /**
     * US 02.01.03: Searching by phone number finds the entrant and invite writes to Firestore.
     */
    @Test
    public void testInviteEntrant_searchByPhoneWritesToFirestore() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnInviteEntrant)).perform(scrollTo(), click());
            onView(withId(R.id.etSearch)).perform(replaceText("7809991234"), closeSoftKeyboard());
            Thread.sleep(3000);
            onView(withId(R.id.rvResults)).perform(
                    androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));
            Thread.sleep(3000);
        }

        boolean waitlistExists = Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_INVITE_TARGET).get(),
                10, TimeUnit.SECONDS).exists();
        assertTrue("Phone search + invite should create waitingList entry", waitlistExists);
    }

    /**
     * US 02.08.02: Organizer can post a comment on their event,
     * which is persisted in Firestore.
     */
    @Test
    public void testOrganizerPostsComment_writesToFirestore() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);

            // Open comment bottom sheet
            onView(withId(R.id.btnComments)).perform(scrollTo(), click());
            Thread.sleep(1000);

            // Type and post a comment
            onView(withId(R.id.etComment)).perform(replaceText("Organizer test comment"), closeSoftKeyboard());
            onView(withId(R.id.btnPostComment)).perform(click());

            // Wait for Firestore write
            Thread.sleep(3000);
        }

        // Verify comment exists in Firestore with authorRole = "organizer"
        QuerySnapshot commentsSnap = Tasks.await(
                db.collection(FirestorePaths.eventComments(TEST_PRIVATE_EVENT_ID)).get(),
                10, TimeUnit.SECONDS);

        boolean organizerCommentFound = false;
        for (QueryDocumentSnapshot doc : commentsSnap) {
            if ("organizer".equals(doc.getString("authorRole"))
                    && "Organizer test comment".equals(doc.getString("content"))) {
                organizerCommentFound = true;
                break;
            }
        }
        assertTrue("Organizer comment should be persisted in Firestore", organizerCommentFound);
    }

    /**
     * US 02.08.01: Organizer can delete an entrant comment from the event comments
     * sheet, and the comment document is removed from Firestore.
     */
    @Test
    public void testOrganizerDeletesComment_removesFirestoreDocument() throws Exception {
        String commentId = "comment_to_delete";
        Map<String, Object> comment = new HashMap<>();
        comment.put("eventId", TEST_PRIVATE_EVENT_ID);
        comment.put("authorId", TEST_INVITE_TARGET);
        comment.put("authorName", "InviteTestUser");
        comment.put("authorRole", "entrant");
        comment.put("content", "Needs moderation");
        comment.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventComments(TEST_PRIVATE_EVENT_ID))
                        .document(commentId).set(comment),
                10, TimeUnit.SECONDS
        );

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnComments)).perform(scrollTo(), click());
            Thread.sleep(1500);
            onView(withText("Needs moderation")).check(matches(isDisplayed()));
            onView(withId(R.id.rvComments)).perform(
                    RecyclerViewActions.actionOnItemAtPosition(0,
                            clickChildViewWithId(R.id.btnDeleteComment)));
            Thread.sleep(2500);
        }

        boolean exists = Tasks.await(
                db.collection(FirestorePaths.eventComments(TEST_PRIVATE_EVENT_ID))
                        .document(commentId).get(),
                10, TimeUnit.SECONDS
        ).exists();
        assertFalse("Deleted comment should be removed from Firestore", exists);
    }

    /**
     * US 02.09.01: Assigning a co-organizer from the organizer event details screen
     * writes the co-organizer record, removes any waitlist entry, and delivers an
     * inbox notification to the selected entrant.
     */
    @Test
    public void testAssignCoOrganizer_writesFirestoreAndNotification() throws Exception {
        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("userId", TEST_COORG_TARGET);
        waitlistEntry.put("userName", "CoOrg Candidate");
        waitlistEntry.put("email", "coorg@test.com");
        waitlistEntry.put("status", "waitlisted");
        waitlistEntry.put("waitlistedAt", Timestamp.now());
        waitlistEntry.put("registeredAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_COORG_TARGET).set(waitlistEntry),
                10, TimeUnit.SECONDS
        );

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_PRIVATE_EVENT_ID);
        intent.putExtra("userId", TEST_USER_ID);

        try (ActivityScenario<OrganizerEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnCoOrganizers)).perform(scrollTo(), click());
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.etSearch)).perform(replaceText("CoOrg"), closeSoftKeyboard());
            Thread.sleep(3000);
            onView(withId(R.id.rvResults)).perform(
                    RecyclerViewActions.actionOnItem(
                            hasDescendant(withText("CoOrg Candidate")), click()));
            Thread.sleep(3000);
        }

        boolean coOrganizerExists = Tasks.await(
                db.collection(FirestorePaths.eventCoOrganizers(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_COORG_TARGET).get(),
                10, TimeUnit.SECONDS
        ).exists();
        assertTrue("Selected entrant should be written to the coOrganizers subcollection",
                coOrganizerExists);

        boolean waitlistExists = Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_PRIVATE_EVENT_ID))
                        .document(TEST_COORG_TARGET).get(),
                10, TimeUnit.SECONDS
        ).exists();
        assertFalse("Assigned co-organizer should be removed from the waitlist", waitlistExists);

        QuerySnapshot inboxSnap = Tasks.await(
                db.collection(FirestorePaths.userInbox(TEST_COORG_TARGET)).get(),
                10, TimeUnit.SECONDS);
        boolean notificationFound = false;
        for (QueryDocumentSnapshot doc : inboxSnap) {
            if ("co_organizer_assignment".equals(doc.getString("type"))
                    && TEST_PRIVATE_EVENT_ID.equals(doc.getString("eventId"))
                    && TEST_USER_ID.equals(doc.getString("senderId"))) {
                notificationFound = true;
                break;
            }
        }
        assertTrue("Assigned co-organizer should receive an inbox notification", notificationFound);
    }
}
