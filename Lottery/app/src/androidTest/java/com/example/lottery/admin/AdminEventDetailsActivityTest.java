package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
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
 * Instrumented tests for {@link AdminEventDetailsActivity}.
 * Verifies the administrative view of event details, including deletion and comment moderation features.
 *
 * <p>Covers User Stories:</p>
 * <ul>
 *   <li>US 03.01.01: As an administrator, I want to be able to remove events.</li>
 *   <li>US 03.10.01: As an administrator, I want to remove event comments that violate app policy.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class AdminEventDetailsActivityTest {

    private static final String TEST_EVENT_ID = "admin_swimming_course_beginners";
    private static final String TEST_ORGANIZER_ID = "coach_maya_patel";
    private static final String TEST_ENTRANT_ID = "entrant_noah_kim";
    private static final String TEST_COORGANIZER_ID = "assistant_ava_brooks";
    private static final String TEST_COMMENT_ID = "comment_pool_deck_feedback";
    private static final String TEST_NOTIFICATION_ID = "notification_swimming_course_update";
    private static final String TEST_RECIPIENT_ID = "recipient_noah_kim";
    private static final String TEST_COMMENT_CONTENT = "Pool deck was too crowded during the beginner session.";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        db = FirebaseFirestore.getInstance();

        seedUser(TEST_ORGANIZER_ID, "Maya Patel", "maya.patel@gmail.com", "ORGANIZER", "7805550101");
        seedUser(TEST_ENTRANT_ID, "Noah Kim", "noah.kim@gmail.com", "ENTRANT", "7805550102");
        seedUser(TEST_COORGANIZER_ID, "Ava Brooks", "ava.brooks@gmail.com", "ORGANIZER", "7805550103");

        Map<String, Object> testEvent = new HashMap<>();
        testEvent.put("eventId", TEST_EVENT_ID);
        testEvent.put("title", "Swimming Course for Beginners");
        testEvent.put("details", "An introductory pool safety and swimming fundamentals course for new adult swimmers.");
        testEvent.put("organizerId", TEST_ORGANIZER_ID);
        testEvent.put("place", "Mill Woods Recreation Centre");
        testEvent.put("requireLocation", true);
        testEvent.put("createdAt", Timestamp.now());
        testEvent.put("status", "open");
        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).set(testEvent),
                10,
                TimeUnit.SECONDS
        );

        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("userId", TEST_ENTRANT_ID);
        waitlistEntry.put("status", "waitlisted");
        waitlistEntry.put("joinedAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                        .document(TEST_ENTRANT_ID)
                        .set(waitlistEntry),
                10,
                TimeUnit.SECONDS
        );

        Map<String, Object> coOrganizerRecord = new HashMap<>();
        coOrganizerRecord.put("userId", TEST_COORGANIZER_ID);
        coOrganizerRecord.put("assignedAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventCoOrganizers(TEST_EVENT_ID))
                        .document(TEST_COORGANIZER_ID)
                        .set(coOrganizerRecord),
                10,
                TimeUnit.SECONDS
        );

        Map<String, Object> comment = new HashMap<>();
        comment.put("eventId", TEST_EVENT_ID);
        comment.put("authorId", TEST_ENTRANT_ID);
        comment.put("authorName", "Noah Kim");
        comment.put("authorRole", "entrant");
        comment.put("content", TEST_COMMENT_CONTENT);
        comment.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventComments(TEST_EVENT_ID))
                        .document(TEST_COMMENT_ID)
                        .set(comment),
                10,
                TimeUnit.SECONDS
        );

        Map<String, Object> notification = new HashMap<>();
        notification.put("eventId", TEST_EVENT_ID);
        notification.put("senderId", TEST_ORGANIZER_ID);
        notification.put("group", "waitlisted");
        notification.put("message", "Swimming Course update: beginner lane assignment changed.");
        notification.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.NOTIFICATIONS)
                        .document(TEST_NOTIFICATION_ID)
                        .set(notification),
                10,
                TimeUnit.SECONDS
        );

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("userId", TEST_ENTRANT_ID);
        recipient.put("deliveredAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.notificationRecipients(TEST_NOTIFICATION_ID))
                        .document(TEST_RECIPIENT_ID)
                        .set(recipient),
                10,
                TimeUnit.SECONDS
        );

        seedInboxEntry(TEST_ORGANIZER_ID, "inbox_swim_course_organizer");
        seedInboxEntry(TEST_ENTRANT_ID, "inbox_swim_course_entrant");
        seedInboxEntry(TEST_COORGANIZER_ID, "inbox_swim_course_assistant");
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        deleteAllDocuments(FirestorePaths.userInbox(TEST_ORGANIZER_ID));
        deleteAllDocuments(FirestorePaths.userInbox(TEST_ENTRANT_ID));
        deleteAllDocuments(FirestorePaths.userInbox(TEST_COORGANIZER_ID));
        deleteAllDocuments(FirestorePaths.notificationRecipients(TEST_NOTIFICATION_ID));
        deleteAllDocuments(FirestorePaths.eventComments(TEST_EVENT_ID));
        deleteAllDocuments(FirestorePaths.eventCoOrganizers(TEST_EVENT_ID));
        deleteAllDocuments(FirestorePaths.eventWaitingList(TEST_EVENT_ID));

        deleteDocument(FirestorePaths.NOTIFICATIONS, TEST_NOTIFICATION_ID);
        deleteDocument(FirestorePaths.EVENTS, TEST_EVENT_ID);
        deleteDocument(FirestorePaths.USERS, TEST_ORGANIZER_ID);
        deleteDocument(FirestorePaths.USERS, TEST_ENTRANT_ID);
        deleteDocument(FirestorePaths.USERS, TEST_COORGANIZER_ID);
    }

    private void seedUser(String userId, String username, String email, String role, String phone)
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("email", email);
        user.put("role", role);
        user.put("phone", phone);
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user), 10, TimeUnit.SECONDS);
    }

    private void seedInboxEntry(String userId, String inboxId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> inboxEntry = new HashMap<>();
        inboxEntry.put("eventId", TEST_EVENT_ID);
        inboxEntry.put("message", "Swimming Course update");
        inboxEntry.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.userInbox(userId)).document(inboxId).set(inboxEntry),
                10,
                TimeUnit.SECONDS
        );
    }

    private void deleteAllDocuments(String collectionPath)
            throws InterruptedException, ExecutionException, TimeoutException {
        for (DocumentSnapshot document : Tasks.await(db.collection(collectionPath).get(), 10, TimeUnit.SECONDS)) {
            Tasks.await(document.getReference().delete(), 10, TimeUnit.SECONDS);
        }
    }

    private void deleteDocument(String collectionPath, String documentId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.collection(collectionPath).document(documentId).delete(), 10, TimeUnit.SECONDS);
    }

    private boolean documentExists(String collectionPath, String documentId)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Tasks.await(
                db.collection(collectionPath).document(documentId).get(),
                10,
                TimeUnit.SECONDS
        ).exists();
    }

    private int countDocuments(String collectionPath)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Tasks.await(db.collection(collectionPath).get(), 10, TimeUnit.SECONDS).size();
    }

    private void waitForDocumentDeletion(String collectionPath, String documentId) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (!documentExists(collectionPath, documentId)) {
                return;
            }
            Thread.sleep(250);
        }
        assertFalse("Document should be deleted: " + collectionPath + "/" + documentId,
                documentExists(collectionPath, documentId));
    }

    private void waitForEmptyCollection(String collectionPath) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (countDocuments(collectionPath) == 0) {
                return;
            }
            Thread.sleep(250);
        }
        assertEquals("Collection should be empty: " + collectionPath, 0, countDocuments(collectionPath));
    }

    /**
     * Scrolls a view into the visible area of a NestedScrollView.
     * Standard scrollTo() is incompatible with NestedScrollView.
     */
    private static ViewAction nestedScrollTo() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isA(View.class);
            }

            @Override
            public String getDescription() {
                return "scroll view inside parent NestedScrollView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ViewParent parent = view.getParent();
                while (parent instanceof View) {
                    if (parent instanceof NestedScrollView) {
                        NestedScrollView scrollView = (NestedScrollView) parent;
                        Rect rect = new Rect();
                        view.getDrawingRect(rect);
                        scrollView.offsetDescendantRectToMyCoords(view, rect);
                        scrollView.requestChildRectangleOnScreen(view, rect, true);
                        uiController.loopMainThreadUntilIdle();
                        return;
                    }
                    parent = parent.getParent();
                }

                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withViewDescription(view.toString())
                        .build();
            }
        };
    }

    /**
     * US 03.01.01: Verifies that the admin event details screen displays all essential elements.
     */
    @Test
    public void testAdminEventDetailsScreenIsDisplayed() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000);
            onView(ViewMatchers.withId(R.id.tvPageHeader)).check(matches(isDisplayed()));
            onView(withId(R.id.tvDetailsHeader)).perform(nestedScrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));

            // Ensure organizer-only controls are not present
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }

    /**
     * US 03.01.01: Verifies that clicking the delete button triggers a confirmation dialog.
     */
    @Test
    public void testDeleteButtonShowsConfirmationDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo(), click());

            onView(withText(R.string.confirm_deletion)).check(matches(isDisplayed()));
            onView(withText(R.string.confirm_delete_event)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 03.01.01: Verifies that cancelling the deletion dialog dismisses it.
     */
    @Test
    public void testDeleteConfirmationCancelDismissesDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo(), click());
            onView(withText(R.string.cancel)).perform(click());

            onView(withText(R.string.confirm_deletion)).check(doesNotExist());
        }
    }

    /**
     * US 03.01.01: Confirming event deletion should remove the event document and all
     * associated admin-managed records from Firestore.
     */
    @Test
    public void testConfirmDeleteEvent_removesEventAndAssociatedData() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo(), click());
            onView(withText(R.string.delete)).perform(click());
        }

        waitForDocumentDeletion(FirestorePaths.EVENTS, TEST_EVENT_ID);
        waitForDocumentDeletion(FirestorePaths.eventWaitingList(TEST_EVENT_ID), TEST_ENTRANT_ID);
        waitForDocumentDeletion(FirestorePaths.eventCoOrganizers(TEST_EVENT_ID), TEST_COORGANIZER_ID);
        waitForDocumentDeletion(FirestorePaths.eventComments(TEST_EVENT_ID), TEST_COMMENT_ID);
        waitForDocumentDeletion(FirestorePaths.NOTIFICATIONS, TEST_NOTIFICATION_ID);
        waitForDocumentDeletion(FirestorePaths.notificationRecipients(TEST_NOTIFICATION_ID), TEST_RECIPIENT_ID);
        waitForEmptyCollection(FirestorePaths.userInbox(TEST_ORGANIZER_ID));
        waitForEmptyCollection(FirestorePaths.userInbox(TEST_ENTRANT_ID));
        waitForEmptyCollection(FirestorePaths.userInbox(TEST_COORGANIZER_ID));
    }

    /**
     * US 03.10.01: Verifies that the comments moderation button is available.
     */
    @Test
    public void testCommentsButtonIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnComments)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 03.10.01: Deleting a violating comment as an admin should remove the comment
     * document from Firestore.
     */
    @Test
    public void testAdminDeletesComment_removesFirestoreDocument() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2500);
            onView(withId(R.id.btnComments)).perform(click());
            Thread.sleep(1500);
            onView(withText(TEST_COMMENT_CONTENT)).check(matches(isDisplayed()));
            onView(withId(R.id.rvComments)).perform(
                    RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btnDeleteComment))
            );
        }

        waitForDocumentDeletion(FirestorePaths.eventComments(TEST_EVENT_ID), TEST_COMMENT_ID);
    }

    private ViewAction clickChildViewWithId(int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isA(View.class);
            }

            @Override
            public String getDescription() {
                return "click child view with specified id";
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
}
