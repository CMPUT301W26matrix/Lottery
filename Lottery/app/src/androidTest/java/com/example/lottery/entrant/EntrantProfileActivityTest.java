package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumented tests for {@link EntrantProfileActivity}.
 * Tests the profile view/edit/delete user flows and notification opt-out.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantProfileActivityTest {

    private static final String TEST_USER_ID = "test_entrant_user_id";
    private static final String ORIGINAL_NAME = "Mia Thompson";
    private static final String ORIGINAL_EMAIL = "mia.thompson@gmail.com";
    private static final String ORIGINAL_PHONE = "7805550101";

    private FirebaseFirestore db;
    private final Set<String> seededEventIds = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        Intents.init();
        db = FirebaseFirestore.getInstance();
        seedUserDocument();
    }

    @After
    public void tearDown() throws Exception {
        // Re-authenticate so cleanup Firestore calls succeed even after signOut tests
        try {
            Tasks.await(
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously(),
                    10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        // Clean up any seeded event data
        for (String eventId : seededEventIds) {
            try {
                for (QueryDocumentSnapshot doc : Tasks.await(
                        db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                        10, TimeUnit.SECONDS)) {
                    Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
                }
                Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                        10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        seededEventIds.clear();

        // Delete user document (may already be gone from delete profile test)
        try {
            Tasks.await(
                    db.collection(FirestorePaths.USERS).document(TEST_USER_ID).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        Intents.release();
    }

    private Intent createIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantProfileActivity.class
        );
        intent.putExtra("userId", TEST_USER_ID);
        return intent;
    }

    private void seedUserDocument() throws Exception {
        Map<String, Object> user = new HashMap<>();
        Timestamp now = Timestamp.now();
        user.put("userId", TEST_USER_ID);
        user.put("username", ORIGINAL_NAME);
        user.put("email", ORIGINAL_EMAIL);
        user.put("phone", ORIGINAL_PHONE);
        user.put("role", "ENTRANT");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", false);
        user.put("createdAt", now);
        user.put("updatedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.USERS).document(TEST_USER_ID).set(user),
                10,
                TimeUnit.SECONDS
        );
    }

    private DocumentSnapshot getUserDocument() throws Exception {
        return Tasks.await(
                db.collection(FirestorePaths.USERS).document(TEST_USER_ID).get(),
                10,
                TimeUnit.SECONDS
        );
    }

    private void waitForFirestoreField(String fieldName, Object expectedValue) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            DocumentSnapshot doc = getUserDocument();
            Object actualValue = doc.get(fieldName);
            if ((expectedValue == null && actualValue == null)
                    || (expectedValue != null && expectedValue.equals(actualValue))) {
                return;
            }
            lastError = new AssertionError(
                    "Expected Firestore field " + fieldName + " to be " + expectedValue
                            + " but was " + actualValue
            );
            Thread.sleep(250);
        }
        throw lastError;
    }

    private void waitForActivityCondition(
            ActivityScenario<EntrantProfileActivity> scenario,
            java.util.function.Predicate<EntrantProfileActivity> condition
    ) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            scenario.onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) {
                return;
            }
            lastError = new AssertionError("Timed out waiting for activity condition");
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(250);
        }
        throw lastError;
    }

    /**
     * US 01.02.01: A saved entrant profile loads the stored name, email, and
     * optional phone number so the entrant can review their personal information.
     */
    @Test
    public void testProfileView_displaysSavedPersonalInformation() throws Exception {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            waitForActivityCondition(scenario, activity -> ORIGINAL_NAME.contentEquals(
                    ((android.widget.TextView) activity.findViewById(R.id.tv_profile_name)).getText()
            ));

            onView(withId(R.id.layout_profile_view_container)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_profile_name)).check(matches(withText(ORIGINAL_NAME)));
            onView(withId(R.id.tv_profile_email)).check(matches(withText(ORIGINAL_EMAIL)));
            onView(withId(R.id.tv_profile_phone)).check(matches(withText(ORIGINAL_PHONE)));
        }
    }

    /**
     * US 01.02.02: Saving profile edits updates the stored entrant information
     * and returns the screen to read-only mode with the new values visible.
     */
    @Test
    public void testSaveProfile_updatesFirestoreAndReturnsToViewMode() throws Exception {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            waitForActivityCondition(scenario, activity -> ORIGINAL_NAME.contentEquals(
                    ((android.widget.TextView) activity.findViewById(R.id.tv_profile_name)).getText()
            ));

            onView(withId(R.id.btn_edit_profile)).perform(click());
            onView(withId(R.id.et_edit_name)).perform(replaceText("Updated Entrant"), closeSoftKeyboard());
            onView(withId(R.id.et_edit_email)).perform(replaceText("updated.entrant@example.com"), closeSoftKeyboard());
            onView(withId(R.id.et_edit_phone)).perform(replaceText("7805550999"), closeSoftKeyboard());
            onView(withId(R.id.btn_save_profile)).perform(scrollTo(), click());

            waitForFirestoreField("username", "Updated Entrant");
            waitForFirestoreField("email", "updated.entrant@example.com");
            waitForFirestoreField("phone", "7805550999");
            waitForActivityCondition(scenario, activity -> activity.findViewById(R.id.layout_profile_view_container)
                    .getVisibility() == android.view.View.VISIBLE);

            onView(withId(R.id.layout_profile_view_container)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_profile_name)).check(matches(withText("Updated Entrant")));
            onView(withId(R.id.tv_profile_email)).check(matches(withText("updated.entrant@example.com")));
            onView(withId(R.id.tv_profile_phone)).check(matches(withText("7805550999")));
        }
    }

    /**
     * US 01.02.02: Entrant enters edit mode → makes changes → cancels → returns to
     * read-only view without saving, and the stored profile remains unchanged.
     */
    @Test
    public void testEditThenCancel_returnsToViewModeWithoutPersistingChanges() throws Exception {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            waitForActivityCondition(scenario, activity -> ORIGINAL_NAME.contentEquals(
                    ((android.widget.TextView) activity.findViewById(R.id.tv_profile_name)).getText()
            ));

            onView(withId(R.id.layout_profile_view_container)).check(matches(isDisplayed()));

            onView(withId(R.id.btn_edit_profile)).perform(click());
            onView(withId(R.id.layout_profile_edit_container)).check(matches(isDisplayed()));
            onView(withId(R.id.layout_profile_view_container)).check(matches(not(isDisplayed())));
            onView(withId(R.id.et_edit_name)).perform(replaceText("Unsaved Name"), closeSoftKeyboard());

            onView(withId(R.id.btn_cancel_edit)).perform(click());
            onView(withId(R.id.layout_profile_view_container)).check(matches(isDisplayed()));
            onView(withId(R.id.layout_profile_edit_container)).check(matches(not(isDisplayed())));

            DocumentSnapshot doc = getUserDocument();
            assertEquals(ORIGINAL_NAME, doc.getString("username"));
            assertEquals(ORIGINAL_EMAIL, doc.getString("email"));
            assertEquals(ORIGINAL_PHONE, doc.getString("phone"));
            onView(withId(R.id.tv_profile_name)).check(matches(withText(ORIGINAL_NAME)));
        }
    }

    /**
     * US 01.02.04: Entrant clicks "Delete Profile" → a confirmation dialog appears
     * asking them to confirm, preventing accidental deletion.
     */
    @Test
    public void testDeleteProfile_showsConfirmationDialog() {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.btn_delete_profile)).perform(scrollTo(), click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText(R.string.delete_profile_message)).check(matches(isDisplayed()));
            onView(withText(R.string.cancel)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.04.03: Entrant toggles the notifications switch off → the switch state
     * and stored preference both update, confirming they can opt out and back in.
     */
    @Test
    public void testNotificationsSwitch_persistsOptOutAndOptIn() throws Exception {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            waitForFirestoreField("notificationsEnabled", true);

            onView(withId(R.id.sw_notifications)).perform(scrollTo(), click());
            waitForFirestoreField("notificationsEnabled", false);

            onView(withId(R.id.sw_notifications)).perform(scrollTo(), click());
            waitForFirestoreField("notificationsEnabled", true);
        }
    }

    /**
     * US 01.05.05: Entrant clicks the "Lottery Guidelines" button → navigates to
     * EntrantLotteryGuidelinesActivity so they can view the selection criteria.
     */
    @Test
    public void testLotteryGuidelines_navigatesToGuidelinesActivity() {
        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.btn_lottery_guidelines)).perform(scrollTo(), click());

            intended(hasComponent(EntrantLotteryGuidelinesActivity.class.getName()));
        }
    }

    /**
     * US 01.02.04: Confirming profile deletion removes the user document from Firestore
     * and cleans up all associated waitingList records across events.
     */
    @Test
    public void testDeleteProfile_confirmedRemovesFirestoreDocument() throws Exception {
        // Seed an event with a waitingList entry for this user
        String eventId = "delete_test_event_" + UUID.randomUUID();
        seededEventIds.add(eventId);

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Beginner Cooking Workshop");
        event.put("capacity", 10L);
        event.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);

        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("userId", TEST_USER_ID);
        waitlistEntry.put("userName", ORIGINAL_NAME);
        waitlistEntry.put("status", "waitlisted");
        waitlistEntry.put("registeredAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.eventWaitingList(eventId))
                .document(TEST_USER_ID).set(waitlistEntry), 10, TimeUnit.SECONDS);

        try (ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(createIntent())) {
            waitForActivityCondition(scenario, activity -> ORIGINAL_NAME.contentEquals(
                    ((android.widget.TextView) activity.findViewById(R.id.tv_profile_name)).getText()
            ));

            onView(withId(R.id.btn_delete_profile)).perform(scrollTo(), click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // Click the positive "Delete Profile" button in the dialog
            onView(withId(android.R.id.button1)).perform(click());

            // Verify Firestore: user document deleted
            for (int attempt = 0; attempt < 30; attempt++) {
                DocumentSnapshot doc = Tasks.await(
                        db.collection(FirestorePaths.USERS).document(TEST_USER_ID).get(),
                        10, TimeUnit.SECONDS);
                if (!doc.exists()) {
                    // Also verify waitlist entry was cleaned up
                    DocumentSnapshot wlDoc = Tasks.await(
                            db.collection(FirestorePaths.eventWaitingList(eventId))
                                    .document(TEST_USER_ID).get(),
                            10, TimeUnit.SECONDS);
                    assertFalse("WaitingList entry should be deleted after profile deletion",
                            wlDoc.exists());
                    return;
                }
                Thread.sleep(500);
            }
            throw new AssertionError("User document should be deleted from Firestore");
        }
    }

    /**
     * US 01.05.05: The lottery guidelines activity displays the actual selection
     * criteria content including "How It Works" and "Random Selection" sections,
     * so entrants can understand the lottery process.
     */
    @Test
    public void testLotteryGuidelines_displaysSelectionContent() {
        Intent guidelinesIntent = new Intent(ApplicationProvider.getApplicationContext(),
                EntrantLotteryGuidelinesActivity.class);
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(guidelinesIntent)) {
            onView(withId(R.id.tv_guidelines_content)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("How It Works"))));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("Random Selection"))));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("Notifications"))));
        }
    }
}
