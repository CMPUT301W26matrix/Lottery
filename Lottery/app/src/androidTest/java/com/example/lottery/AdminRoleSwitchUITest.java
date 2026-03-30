package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.util.AdminRoleManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    // ---- Navigation integration tests ----

    // US 03.09.01: Settings nav from event browser should open admin profile with userId
    @Test
    public void testSettingsNavFromEventBrowser() {
        Intent intent = new Intent(context, AdminBrowseEventsActivity.class);
        intent.putExtra("userId", TEST_ADMIN_ID);

        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(intent)) {
            intending(hasComponent(AdminProfileActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.nav_admin_settings)).perform(click());

            intended(allOf(
                    hasComponent(AdminProfileActivity.class.getName()),
                    hasExtra("userId", TEST_ADMIN_ID)));
        }
    }

    // US 03.09.01: Settings nav from image browser should open admin profile with userId
    @Test
    public void testSettingsNavFromImageBrowser() {
        Intent intent = new Intent(context, AdminBrowseImagesActivity.class);
        intent.putExtra("userId", TEST_ADMIN_ID);

        try (ActivityScenario<AdminBrowseImagesActivity> ignored =
                     ActivityScenario.launch(intent)) {
            intending(hasComponent(AdminProfileActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.nav_admin_settings)).perform(click());

            intended(allOf(
                    hasComponent(AdminProfileActivity.class.getName()),
                    hasExtra("userId", TEST_ADMIN_ID)));
        }
    }

    // US 03.09.01: Settings nav from profile browser should open admin profile with userId
    @Test
    public void testSettingsNavFromProfileBrowser() {
        Intent intent = new Intent(context, AdminBrowseProfilesActivity.class);
        intent.putExtra("userId", TEST_ADMIN_ID);
        intent.putExtra("role", "admin");

        try (ActivityScenario<AdminBrowseProfilesActivity> ignored =
                     ActivityScenario.launch(intent)) {
            intending(hasComponent(AdminProfileActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.nav_admin_settings)).perform(click());

            intended(allOf(
                    hasComponent(AdminProfileActivity.class.getName()),
                    hasExtra("userId", TEST_ADMIN_ID)));
        }
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
}
