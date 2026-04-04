package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.MainActivity;
import com.example.lottery.R;
import com.example.lottery.entrant.EntrantMainActivity;
import com.example.lottery.entrant.EntrantProfileActivity;
import com.example.lottery.organizer.OrganizerBrowseEventsActivity;
import com.example.lottery.organizer.OrganizerProfileActivity;
import com.example.lottery.util.AdminRoleManager;
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

/**
 * Tests for the admin role switching mechanism and navigation integration.
 * Covers US 03.09.01: As an administrator, I want to be able to switch
 *     between entrant and organizer roles.
 */
@RunWith(AndroidJUnit4.class)
public class AdminRoleSwitchUITest {

    private static final String TEST_ADMIN_ID = "admin_test_device";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AdminRoleManager.clearAdminRoleSession(context);
        Intents.init();
    }

    @After
    public void tearDown() {
        AdminRoleManager.clearAdminRoleSession(context);
        Intents.release();
    }

    // ---- AdminRoleManager unit tests ----

    // US 03.09.01: Setting admin role session should persist the admin user ID
    @Test
    public void testSetAdminRoleSession() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        assertTrue(AdminRoleManager.isAdminRoleSession(context));
        assertEquals(TEST_ADMIN_ID, AdminRoleManager.getAdminUserId(context));
    }

    // US 03.09.01: No admin role session should be active by default
    @Test
    public void testNoSessionByDefault() {
        assertFalse(AdminRoleManager.isAdminRoleSession(context));
        assertNull(AdminRoleManager.getAdminUserId(context));
    }

    // US 03.09.01: Clearing admin session should remove all role state
    @Test
    public void testClearAdminRoleSession() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);
        AdminRoleManager.clearAdminRoleSession(context);

        assertFalse(AdminRoleManager.isAdminRoleSession(context));
        assertNull(AdminRoleManager.getAdminUserId(context));
    }

    // US 03.09.01: Setting a new session should overwrite the previous admin ID
    @Test
    public void testSessionOverwrite() {
        AdminRoleManager.setAdminRoleSession(context, "admin_first");
        AdminRoleManager.setAdminRoleSession(context, "admin_second");

        assertTrue(AdminRoleManager.isAdminRoleSession(context));
        assertEquals("admin_second", AdminRoleManager.getAdminUserId(context));
    }

    // US 03.09.01: Admin role session should not interfere with other SharedPreferences
    @Test
    public void testSessionIsolation() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        context.getSharedPreferences("OtherPrefs", Context.MODE_PRIVATE)
                .edit().putBoolean("other_flag", true).apply();

        assertTrue(AdminRoleManager.isAdminRoleSession(context));
        assertEquals(TEST_ADMIN_ID, AdminRoleManager.getAdminUserId(context));
    }

    // ---- Logout return-to-admin tests ----

    // US 03.09.01: Entrant logout during admin role session should return to AdminProfileActivity
    @Test
    public void testEntrantLogoutReturnsToAdminProfile() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        Intent intent = new Intent(context, EntrantProfileActivity.class);
        intent.putExtra("userId", "entrant_test_device");
        intent.putExtra("isAdminRole", true);

        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_log_out)).perform(scrollTo(), click());

            intended(hasComponent(AdminProfileActivity.class.getName()));
            intended(hasExtra("userId", TEST_ADMIN_ID));
        }
    }

    // US 03.09.01: Organizer logout during admin role session should return to AdminProfileActivity
    @Test
    public void testOrganizerLogoutReturnsToAdminProfile() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        Intent intent = new Intent(context, OrganizerProfileActivity.class);
        intent.putExtra("userId", "organizer_test_device");
        intent.putExtra("isAdminRole", true);

        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_log_out)).perform(scrollTo(), click());

            intended(hasComponent(AdminProfileActivity.class.getName()));
            intended(hasExtra("userId", TEST_ADMIN_ID));
        }
    }

    // US 03.09.01: Regular entrant logout (no admin session) should return to MainActivity
    @Test
    public void testRegularEntrantLogoutGoesToMainActivity() {
        // Ensure no admin session is active
        AdminRoleManager.clearAdminRoleSession(context);

        Intent intent = new Intent(context, EntrantProfileActivity.class);
        intent.putExtra("userId", "entrant_regular_user");

        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_log_out)).perform(scrollTo(), click());

            intended(hasComponent(MainActivity.class.getName()));
        }
    }

    // US 03.09.01: Regular organizer logout (no admin session) should return to MainActivity
    @Test
    public void testRegularOrganizerLogoutGoesToMainActivity() {
        AdminRoleManager.clearAdminRoleSession(context);

        Intent intent = new Intent(context, OrganizerProfileActivity.class);
        intent.putExtra("userId", "organizer_regular_user");

        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_log_out)).perform(scrollTo(), click());

            intended(hasComponent(MainActivity.class.getName()));
        }
    }

    // US 03.09.01: Admin session should be cleared after returning from role
    @Test
    public void testAdminSessionClearedAfterRoleLogout() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        Intent intent = new Intent(context, EntrantProfileActivity.class);
        intent.putExtra("userId", "entrant_test_device");
        intent.putExtra("isAdminRole", true);

        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_log_out)).perform(scrollTo(), click());

            // Session should be cleared after logout
            assertFalse(AdminRoleManager.isAdminRoleSession(context));
            assertNull(AdminRoleManager.getAdminUserId(context));
        }
    }

    /**
     * Seeds a complete role profile document in Firestore at users/{roleUserId}
     * so that AdminProfileActivity.checkAndSwitchToRole routes through
     * navigateToRoleMain() instead of navigateToProfileCompletion().
     */
    private void seedRoleProfile(FirebaseFirestore db,
                                 String roleUserId,
                                 String role,
                                 String deviceId,
                                 String username,
                                 String email) throws Exception {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", roleUserId);
        user.put("role", role);
        user.put("deviceId", deviceId);
        user.put("username", username);
        user.put("email", email);
        user.put("phone", "");
        user.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.USERS).document(roleUserId).set(user),
                10, TimeUnit.SECONDS
        );
    }

    private void waitForAdminSession() throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            if (AdminRoleManager.isAdminRoleSession(context)) return;
            Thread.sleep(250);
        }
    }

    // US 03.09.01: Clicking "Switch to Entrant" from AdminProfileActivity should
    // navigate to EntrantMainActivity with the derived entrant userId + isAdminRole
    // extra, and AdminRoleManager session should be active.
    @Test
    public void adminSwitchToEntrant_navigatesAndSetsAdminRoleSession() throws Exception {
        String deviceId = "test_device_admin_switch_entrant";
        String entrantUserId = "entrant_" + deviceId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        seedRoleProfile(db, entrantUserId, "ENTRANT", deviceId,
                "Admin As Entrant", "admin.entrant@test.com");

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("deviceId", deviceId).commit();

        // Stub the navigation target so we can verify the outgoing intent without
        // actually launching EntrantMainActivity.
        intending(hasComponent(EntrantMainActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        try {
            Intent intent = new Intent(context, AdminProfileActivity.class);
            intent.putExtra("userId", TEST_ADMIN_ID);

            try (ActivityScenario<AdminProfileActivity> ignored =
                         ActivityScenario.launch(intent)) {
                onView(withId(R.id.btn_switch_to_entrant)).perform(click());
                waitForAdminSession();
            }

            intended(allOf(
                    hasComponent(EntrantMainActivity.class.getName()),
                    hasExtra("userId", entrantUserId),
                    hasExtra("isAdminRole", true)
            ));

            assertTrue("Admin role session should be active after forward switch",
                    AdminRoleManager.isAdminRoleSession(context));
            assertEquals("Admin user ID should be preserved in session",
                    TEST_ADMIN_ID, AdminRoleManager.getAdminUserId(context));
        } finally {
            prefs.edit().remove("deviceId").commit();
            try {
                Tasks.await(
                        db.collection(FirestorePaths.USERS).document(entrantUserId).delete(),
                        10, TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }
        }
    }

    // US 03.09.01: Clicking "Switch to Organizer" from AdminProfileActivity should
    // navigate to OrganizerBrowseEventsActivity with the derived organizer userId
    // + isAdminRole extra, and AdminRoleManager session should be active.
    @Test
    public void adminSwitchToOrganizer_navigatesAndSetsAdminRoleSession() throws Exception {
        String deviceId = "test_device_admin_switch_organizer";
        String organizerUserId = "organizer_" + deviceId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        seedRoleProfile(db, organizerUserId, "ORGANIZER", deviceId,
                "Admin As Organizer", "admin.organizer@test.com");

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("deviceId", deviceId).commit();

        intending(hasComponent(OrganizerBrowseEventsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        try {
            Intent intent = new Intent(context, AdminProfileActivity.class);
            intent.putExtra("userId", TEST_ADMIN_ID);

            try (ActivityScenario<AdminProfileActivity> ignored =
                         ActivityScenario.launch(intent)) {
                onView(withId(R.id.btn_switch_to_organizer)).perform(click());
                waitForAdminSession();
            }

            intended(allOf(
                    hasComponent(OrganizerBrowseEventsActivity.class.getName()),
                    hasExtra("userId", organizerUserId),
                    hasExtra("isAdminRole", true)
            ));

            assertTrue("Admin role session should be active after forward switch",
                    AdminRoleManager.isAdminRoleSession(context));
            assertEquals("Admin user ID should be preserved in session",
                    TEST_ADMIN_ID, AdminRoleManager.getAdminUserId(context));
        } finally {
            prefs.edit().remove("deviceId").commit();
            try {
                Tasks.await(
                        db.collection(FirestorePaths.USERS).document(organizerUserId).delete(),
                        10, TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }
        }
    }

    // US 03.09.01: Switching to a role whose profile exists but has empty username
    // and email should route to EntrantProfileActivity with forceEdit=true so the
    // admin can complete the profile before using that role.
    @Test
    public void adminSwitchToEntrant_existingIncompleteProfile_routesToProfileCompletion()
            throws Exception {
        String deviceId = "test_device_admin_switch_incomplete_entrant";
        String entrantUserId = "entrant_" + deviceId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Seed a role profile with empty username and email to force the
        // navigateToProfileCompletion branch.
        seedRoleProfile(db, entrantUserId, "ENTRANT", deviceId, "", "");

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("deviceId", deviceId).commit();

        intending(hasComponent(EntrantProfileActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        try {
            Intent intent = new Intent(context, AdminProfileActivity.class);
            intent.putExtra("userId", TEST_ADMIN_ID);

            try (ActivityScenario<AdminProfileActivity> ignored =
                         ActivityScenario.launch(intent)) {
                onView(withId(R.id.btn_switch_to_entrant)).perform(click());
                waitForAdminSession();
            }

            intended(allOf(
                    hasComponent(EntrantProfileActivity.class.getName()),
                    hasExtra("userId", entrantUserId),
                    hasExtra("forceEdit", true),
                    hasExtra("isAdminRole", true)
            ));

            assertTrue("Admin role session should still activate on completion path",
                    AdminRoleManager.isAdminRoleSession(context));
            assertEquals(TEST_ADMIN_ID, AdminRoleManager.getAdminUserId(context));
        } finally {
            prefs.edit().remove("deviceId").commit();
            try {
                Tasks.await(
                        db.collection(FirestorePaths.USERS).document(entrantUserId).delete(),
                        10, TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }
        }
    }

    // US 03.09.01: Switching to a role whose profile does not yet exist should
    // create the role profile document in Firestore and then route to
    // EntrantProfileActivity with forceEdit=true for first-time completion.
    @Test
    public void adminSwitchToEntrant_missingProfile_createsProfileAndRoutesToCompletion()
            throws Exception {
        String deviceId = "test_device_admin_switch_create_entrant";
        String entrantUserId = "entrant_" + deviceId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Make sure the role profile does not exist before the test runs.
        try {
            Tasks.await(
                    db.collection(FirestorePaths.USERS).document(entrantUserId).delete(),
                    10, TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
        }

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("deviceId", deviceId).commit();

        intending(hasComponent(EntrantProfileActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        try {
            Intent intent = new Intent(context, AdminProfileActivity.class);
            intent.putExtra("userId", TEST_ADMIN_ID);

            try (ActivityScenario<AdminProfileActivity> ignored =
                         ActivityScenario.launch(intent)) {
                onView(withId(R.id.btn_switch_to_entrant)).perform(click());
                waitForAdminSession();
            }

            intended(allOf(
                    hasComponent(EntrantProfileActivity.class.getName()),
                    hasExtra("userId", entrantUserId),
                    hasExtra("forceEdit", true),
                    hasExtra("isAdminRole", true)
            ));

            // Verify the role profile was actually created in Firestore by
            // createRoleProfile before navigation fired.
            DocumentSnapshot created = Tasks.await(
                    db.collection(FirestorePaths.USERS).document(entrantUserId).get(),
                    10, TimeUnit.SECONDS
            );
            assertTrue("Role profile document should have been created in Firestore",
                    created.exists());
            assertEquals("ENTRANT", created.getString("role"));
            assertEquals(deviceId, created.getString("deviceId"));

            assertTrue("Admin role session should be active after createRoleProfile path",
                    AdminRoleManager.isAdminRoleSession(context));
        } finally {
            prefs.edit().remove("deviceId").commit();
            try {
                Tasks.await(
                        db.collection(FirestorePaths.USERS).document(entrantUserId).delete(),
                        10, TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }
        }
    }

    // US 03.09.01: If the admin session has no deviceId in SharedPreferences, the
    // switch buttons should early-exit without starting any role profile lookup,
    // without navigating away, and without setting an admin role session.
    @Test
    public void adminSwitch_withoutDeviceId_doesNotNavigateOrActivateSession() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("deviceId").commit();

        Intent intent = new Intent(context, AdminProfileActivity.class);
        intent.putExtra("userId", TEST_ADMIN_ID);

        try (ActivityScenario<AdminProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_switch_to_entrant)).perform(click());
            onView(withId(R.id.btn_switch_to_organizer)).perform(click());

            assertFalse("Admin role session should not activate without deviceId",
                    AdminRoleManager.isAdminRoleSession(context));
            assertNull("Admin user ID should remain null without deviceId",
                    AdminRoleManager.getAdminUserId(context));

            scenario.onActivity(activity ->
                    assertFalse("AdminProfileActivity should remain alive after early exit",
                            activity.isFinishing())
            );
        }
    }
}
