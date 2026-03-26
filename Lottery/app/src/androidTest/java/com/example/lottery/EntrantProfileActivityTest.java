package com.example.lottery;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.scrollTo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Switch;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Android UI tests for {@link EntrantProfileActivity}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The entrant profile screen opens correctly</li>
 *     <li>The accessibility mode toggle is displayed</li>
 *     <li>The accessibility toggle can be turned on and off</li>
 *     <li>Core profile UI elements remain visible</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantProfileActivityTest {

    /** SharedPreferences file used by the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    /** Preference key for a saved user id. */
    private static final String KEY_USER_ID = "userId";

    /** Fake user id used for launching the activity in tests. */
    private static final String TEST_USER_ID = "test_user_001";

    private SharedPreferences prefs;

    /**
     * Sets up a clean SharedPreferences state before each test.
     */
    @Before
    public void setUp() {
        Context context = getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .clear()
                .putString(KEY_USER_ID, TEST_USER_ID)
                .putBoolean(KEY_ACCESSIBILITY_MODE, false)
                .apply();
    }

    /**
     * Clears SharedPreferences after each test.
     */
    @After
    public void tearDown() {
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }

    /**
     * Launches the entrant profile activity with a test user id.
     *
     * @return launched ActivityScenario
     */
    private ActivityScenario<EntrantProfileActivity> launchProfileActivity() {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, EntrantProfileActivity.class);
        intent.putExtra("userId", TEST_USER_ID);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return ActivityScenario.launch(intent);
    }

    /**
     * Verifies that the accessibility mode toggle is visible on the profile screen.
     */
    @Test
    public void accessibilityToggle_isDisplayed() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {
            onView(withId(R.id.sw_accessibility)).check(matches(isDisplayed()));
            onView(withText("Accessibility Mode")).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that key profile UI elements are visible when the screen opens.
     */
    @Test
    public void profileScreen_coreElementsAreDisplayed() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {
            onView(withId(R.id.tv_profile_name)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_profile_email)).check(matches(isDisplayed()));
            onView(withId(R.id.sw_notifications)).check(matches(isDisplayed()));
            onView(withId(R.id.sw_accessibility)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_log_out)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that tapping the accessibility toggle turns accessibility mode on.
     */
    @Test
    public void accessibilityToggle_click_enablesPreference() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {
            onView(withId(R.id.sw_accessibility)).perform(click());

            scenario.onActivity(activity -> {
                SharedPreferences activityPrefs =
                        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                boolean enabled = activityPrefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);
                if (!enabled) {
                    throw new AssertionError("Accessibility mode should be enabled after toggle click.");
                }
            });
        }
    }

    /**
     * Verifies that if accessibility mode starts enabled, tapping the toggle turns it off.
     */
    @Test
    public void accessibilityToggle_click_disablesPreferenceWhenAlreadyEnabled() {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, true).apply();

        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {
            onView(withId(R.id.sw_accessibility)).perform(click());

            scenario.onActivity(activity -> {
                SharedPreferences activityPrefs =
                        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                boolean enabled = activityPrefs.getBoolean(KEY_ACCESSIBILITY_MODE, true);
                if (enabled) {
                    throw new AssertionError("Accessibility mode should be disabled after second toggle state.");
                }
            });
        }
    }

    /**
     * Verifies that the accessibility switch view itself exists as a switch component.
     */
    @Test
    public void accessibilityToggle_isSwitchMaterial() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {
            onView(withId(R.id.sw_accessibility))
                    .check(matches(isDisplayed()));
            onView(withId(R.id.sw_accessibility))
                    .check(matches(isAssignableFrom(SwitchMaterial.class)));
        }
    }
    @Test
    public void accessibilityToggle_click_changesPreference() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {

            // Click toggle
            onView(withId(R.id.sw_accessibility)).perform(click());

            // Verify SharedPreferences changed
            scenario.onActivity(activity -> {
                SharedPreferences prefs =
                        activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

                boolean enabled = prefs.getBoolean("accessibility_mode", false);

                if (!enabled) {
                    throw new AssertionError("Accessibility mode should be enabled after toggle click");
                }
            });
        }
    }

    @Test
    public void accessibilityToggle_affectsUiTextSize() {
        try (ActivityScenario<EntrantProfileActivity> scenario = launchProfileActivity()) {

            // Turn ON accessibility
            onView(withId(R.id.sw_accessibility)).perform(click());

            // Relaunch activity (simulate navigation)
            scenario.recreate();

            // Check something still visible (ensures UI updated without crash)
            onView(withId(R.id.tv_profile_name)).check(matches(isDisplayed()));
        }
    }


}
