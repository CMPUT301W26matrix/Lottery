package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
 * Instrumented tests for the entrant comment feature on events.
 * Uses {@link EntrantEventDetailsActivity} to open the {@link com.example.lottery.fragment.CommentBottomSheet}
 * and verifies comment posting and viewing against real Firestore data.
 * <p>
 * Covers: US 01.08.01, US 01.08.02
 */
@RunWith(AndroidJUnit4.class)
public class EntrantCommentTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();
    private final Set<String> seededUserIds = new HashSet<>();

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            // Delete comments
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.eventComments(eventId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            // Delete waitingList entries
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            // Delete event
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

    private void seedEvent(String eventId) throws Exception {
        seededEventIds.add(eventId);
        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Community Dance Class");
        event.put("details", "Open to all skill levels");
        event.put("place", "Recreation Centre");
        event.put("capacity", 30L);
        event.put("scheduledDateTime", now);
        event.put("registrationStart", now);
        event.put("registrationDeadline", new Timestamp(
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))));
        event.put("requireLocation", false);
        event.put("waitingListLimit", 100L);
        event.put("private", false);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private void seedUser(String userId, String username) throws Exception {
        seededUserIds.add(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("email", "entrant@example.com");
        user.put("role", "ENTRANT");
        user.put("deviceId", "test_device");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", false);
        user.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user),
                10, TimeUnit.SECONDS);
    }

    private void seedComment(String eventId, String authorId, String authorName, String content)
            throws Exception {
        Map<String, Object> comment = new HashMap<>();
        comment.put("eventId", eventId);
        comment.put("authorId", authorId);
        comment.put("authorName", authorName);
        comment.put("authorRole", "entrant");
        comment.put("content", content);
        comment.put("createdAt", Timestamp.now());
        comment.put("deleted", false);
        Tasks.await(db.collection(FirestorePaths.eventComments(eventId)).add(comment),
                10, TimeUnit.SECONDS);
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

    /**
     * US 01.08.01: An entrant posts a comment on an event. The comment is persisted
     * in Firestore with the correct content, author info, and "entrant" role.
     */
    @Test
    public void postComment_writesToFirestore() throws Exception {
        String eventId = "comment_post_event_" + UUID.randomUUID();
        String userId = "comment_post_user_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId, "Jordan Kim");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            // Wait for event details to load
            waitForCondition(scenario, activity -> {
                TextView title = activity.findViewById(R.id.tvEventTitle);
                return title.getText().toString().contains("Dance");
            });

            // Open comment bottom sheet
            onView(withId(R.id.btnComments)).perform(click());
            Thread.sleep(1000);

            // Type and submit comment
            onView(withId(R.id.etComment))
                    .perform(replaceText("Great event, looking forward to it!"), closeSoftKeyboard());
            onView(withId(R.id.btnPostComment)).perform(click());

            // Verify Firestore: comment document created
            boolean found = false;
            for (int attempt = 0; attempt < 20; attempt++) {
                QuerySnapshot snapshot = Tasks.await(
                        db.collection(FirestorePaths.eventComments(eventId)).get(),
                        10, TimeUnit.SECONDS);
                for (QueryDocumentSnapshot doc : snapshot) {
                    if ("Great event, looking forward to it!".equals(doc.getString("content"))
                            && userId.equals(doc.getString("authorId"))
                            && "entrant".equals(doc.getString("authorRole"))) {
                        found = true;
                        // Also verify author name
                        assertEquals("Jordan Kim", doc.getString("authorName"));
                        assertNotNull("createdAt should be set", doc.getTimestamp("createdAt"));
                        break;
                    }
                }
                if (found) break;
                Thread.sleep(250);
            }
            assertTrue("Comment should be persisted in Firestore", found);
        }
    }

    /**
     * US 01.08.02: An entrant can view existing comments on an event. Comments seeded
     * in Firestore are displayed in the comment bottom sheet.
     */
    @Test
    public void viewComments_displaysSeededComment() throws Exception {
        String eventId = "comment_view_event_" + UUID.randomUUID();
        String userId = "comment_view_user_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId, "Alex Rivera");
        seedComment(eventId, "other_user_123", "Sam Park", "This class is amazing!");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView title = activity.findViewById(R.id.tvEventTitle);
                return title.getText().toString().contains("Dance");
            });

            // Open comment bottom sheet
            onView(withId(R.id.btnComments)).perform(click());
            Thread.sleep(2000);

            // Verify seeded comment is displayed
            onView(withText("This class is amazing!")).check(matches(isDisplayed()));
            onView(withText("Sam Park")).check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.08.01: Submitting an empty comment does not create a Firestore document.
     * Verifies the negative path.
     */
    @Test
    public void emptyComment_doesNotPost() throws Exception {
        String eventId = "comment_empty_event_" + UUID.randomUUID();
        String userId = "comment_empty_user_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId, "Test User");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView title = activity.findViewById(R.id.tvEventTitle);
                return title.getText().toString().contains("Dance");
            });

            // Open comment bottom sheet
            onView(withId(R.id.btnComments)).perform(click());
            Thread.sleep(1000);

            // Click post with empty input
            onView(withId(R.id.btnPostComment)).perform(click());

            // Wait and verify no comment was created
            Thread.sleep(2000);
            QuerySnapshot snapshot = Tasks.await(
                    db.collection(FirestorePaths.eventComments(eventId)).get(),
                    10, TimeUnit.SECONDS);
            assertTrue("No comment should be created for empty input", snapshot.isEmpty());
        }
    }
}
