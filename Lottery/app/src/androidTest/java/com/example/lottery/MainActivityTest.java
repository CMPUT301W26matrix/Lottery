package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.admin.AdminSignInActivity;
import com.example.lottery.entrant.EntrantMainActivity;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Matcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private static final String TAG = "MainActivityTest";

    private ActivityScenario<MainActivity> scenario;
    private FirebaseFirestore db;
    private String androidId;
    private boolean intentsInitialized;
    private boolean seededExistingEntrant;

    // Snapshot of any pre-existing doc for this device, so tearDown can restore
    // the original state instead of destroying the user's real entrant profile.
    private boolean originalEntrantExisted;
    private Map<String, Object> originalEntrantData;

    @Before
    public void setUp() {
        Intents.init();
        intentsInitialized = true;
        db = FirebaseFirestore.getInstance();
        androidId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        seededExistingEntrant = false;

        // Capture existing doc (if any) before any test mutates it. tearDown
        // uses this to restore state so tests don't wipe real profile data.
        originalEntrantExisted = false;
        originalEntrantData = null;
        if (androidId != null && !androidId.isEmpty()) {
            try {
                DocumentSnapshot snapshot = Tasks.await(
                        db.collection(FirestorePaths.USERS).document("entrant_" + androidId).get(),
                        10,
                        TimeUnit.SECONDS
                );
                originalEntrantExisted = snapshot.exists();
                originalEntrantData = originalEntrantExisted ? snapshot.getData() : null;
            } catch (Exception snapshotError) {
                Log.w(TAG, "Failed to snapshot entrant_" + androidId + " before test", snapshotError);
            }
        }

        clearSharedPreferences();
    }

    @After
    public void tearDown() {
        try {
            if (scenario != null) {
                scenario.close();
                scenario = null;
            }

            if (androidId != null && !androidId.isEmpty()) {
                try {
                    if (originalEntrantExisted && originalEntrantData != null) {
                        // Restore the original document exactly as we found it.
                        Tasks.await(
                                db.collection(FirestorePaths.USERS).document("entrant_" + androidId).set(originalEntrantData),
                                5,
                                TimeUnit.SECONDS
                        );
                    } else if (seededExistingEntrant) {
                        // Nothing pre-existed; remove test residue created by seed.
                        Tasks.await(
                                db.collection(FirestorePaths.USERS).document("entrant_" + androidId).delete(),
                                5,
                                TimeUnit.SECONDS
                        );
                    }
                } catch (Exception cleanupError) {
                    Log.w(TAG, "Best-effort Firestore cleanup failed for entrant_" + androidId, cleanupError);
                }
            }
            clearSharedPreferences();
        } finally {
            if (intentsInitialized) {
                Intents.release();
                intentsInitialized = false;
            }
        }
    }

    private void clearSharedPreferences() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private void launchMainActivity() {
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    private void seedExistingEntrantForCurrentDevice() throws Exception {
        Map<String, Object> user = new HashMap<>();
        Timestamp now = Timestamp.now();
        user.put("userId", "entrant_" + androidId);
        user.put("role", "ENTRANT");
        user.put("deviceId", androidId);
        user.put("username", "Existing Entrant");
        user.put("email", "entrant@example.com");
        user.put("phone", "7805550101");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", false);
        user.put("createdAt", now);
        user.put("updatedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.USERS).document("entrant_" + androidId).set(user),
                10,
                TimeUnit.SECONDS
        );
        seededExistingEntrant = true;
    }

    private void waitForIntent(Matcher<Intent> matcher) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                intended(matcher);
                return;
            } catch (AssertionError error) {
                lastError = error;
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Thread.sleep(250);
            }
        }
        throw lastError;
    }

    /**
     * US 01.07.01: The device-based entry screen exposes the entrant role button
     * without asking for a username or password first.
     */
    @Test
    public void testEntrantButtonIsDisplayed() {
        launchMainActivity();
        onView(withId(R.id.entrant_login_button))
                .check(matches(isDisplayed()));
    }

    /**
     * US 01.07.01: The device-based entry screen exposes the organizer role button
     * without asking for a username or password first.
     */
    @Test
    public void testOrganizerButtonIsDisplayed() {
        launchMainActivity();
        onView(withId(R.id.organizer_login_button))
                .check(matches(isDisplayed()));
    }

    /**
     * US 01.07.01: The role selection screen also exposes the admin path so the
     * user can choose a role from the same entry point.
     */
    @Test
    public void testAdminButtonIsDisplayed() {
        launchMainActivity();
        onView(withId(R.id.admin_login_button))
                .check(matches(isDisplayed()));
    }

    /**
     * US 01.07.01: The entry screen exposes device-based role selection instead of
     * a username/password form, so entrants can begin sign-in from the device alone.
     */
    @Test
    public void testChooseYourRoleTextIsDisplayed() {
        launchMainActivity();
        onView(withId(R.id.tvChooseRole)).check(matches(isDisplayed()));
        onView(withId(R.id.tvChooseRole)).check(matches(withText(R.string.choose_your_role)));
    }

    /**
     * US 01.07.01: Administrators are the only role that detours to an explicit
     * sign-in screen; entrant/organizer flows remain device-based.
     */
    @Test
    public void testSwitchToAdminSignInActivity() {
        launchMainActivity();
        onView(withId(R.id.admin_login_button)).perform(click());
        intended(hasComponent(AdminSignInActivity.class.getName()));
    }

    /**
     * US 01.07.01: Tapping the entrant role logs in the profile associated with this
     * device and opens the entrant flow without asking for credentials.
     */
    @Test
    public void testDeviceBasedIdentificationLaunchesEntrantFlow() throws Exception {
        assertNotNull("ANDROID_ID should be available for device-based login", androidId);
        seedExistingEntrantForCurrentDevice();
        launchMainActivity();

        onView(withId(R.id.entrant_login_button)).perform(click());

        waitForIntent(hasComponent(EntrantMainActivity.class.getName()));
        waitForIntent(hasExtra("userId", "entrant_" + androidId));

        Context context = ApplicationProvider.getApplicationContext();
        assertEquals("entrant_" + androidId,
                context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("userId", null));
        assertEquals(androidId,
                context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("deviceId", null));
    }
}
