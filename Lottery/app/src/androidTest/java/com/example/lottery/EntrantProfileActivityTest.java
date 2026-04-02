package com.example.lottery;

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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.entrant.EntrantLotteryGuidelinesActivity;
import com.example.lottery.entrant.EntrantProfileActivity;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumented tests for {@link EntrantProfileActivity}.
 * Tests the profile view/edit/delete user flows and notification opt-out.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantProfileActivityTest {

    private static final String TEST_USER_ID = "test_entrant_user_id";
    private static final String ORIGINAL_NAME = "Existing Entrant";
    private static final String ORIGINAL_EMAIL = "existing.entrant@example.com";
    private static final String ORIGINAL_PHONE = "7805550101";

    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        Intents.init();
        db = FirebaseFirestore.getInstance();
        seedUserDocument();
    }

    @After
    public void tearDown() throws Exception {
        Tasks.await(
                db.collection(FirestorePaths.USERS).document(TEST_USER_ID).delete(),
                10,
                TimeUnit.SECONDS
        );
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
}
