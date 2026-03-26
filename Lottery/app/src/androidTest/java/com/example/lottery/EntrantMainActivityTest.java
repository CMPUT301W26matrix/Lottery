package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Android instrumentation tests for {@link EntrantMainActivity}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The entrant main screen opens successfully</li>
 *     <li>Core browse/filter UI is visible</li>
 *     <li>The screen still opens when accessibility mode is enabled</li>
 *     <li>Bottom navigation is visible</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMainActivityTest {

    /** SharedPreferences file used by the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private SharedPreferences prefs;

    /**
     * Sets up clean preferences before each test.
     */
    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Clears preferences after each test.
     */
    @After
    public void tearDown() {
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }

    /**
     * Launches the entrant main activity with a fake test user id.
     *
     * @return launched ActivityScenario
     */
    private ActivityScenario<EntrantMainActivity> launchMainActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantMainActivity.class
        );
        intent.putExtra("userId", "test_user_id");
        return ActivityScenario.launch(intent);
    }

    /**
     * Verifies that the entrant main screen opens successfully.
     */
    @Test
    public void entrantMainScreen_opensSuccessfully() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchMainActivity()) {
            onView(withId(R.id.tvBrowseEventsTitle))
                    .check(matches(withText(R.string.browse_events)))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvEvents))
                    .check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    /**
     * Verifies that core browse and filter UI is displayed.
     */
    @Test
    public void entrantMainScreen_coreBrowseUiIsDisplayed() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchMainActivity()) {
            onView(withText("Search")).check(matches(isDisplayed()));
            onView(withId(R.id.cgBrowseTabs)).check(matches(isDisplayed()));
            onView(withId(R.id.cgCategories)).check(matches(isDisplayed()));
            onView(withId(R.id.btnTimeFilter)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the quick filter chips are displayed.
     */
    @Test
    public void entrantMainScreen_quickFiltersAreDisplayed() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchMainActivity()) {
            onView(withId(R.id.cgQuickFilters)).check(matches(isDisplayed()));
            onView(withId(R.id.chipAvailable)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the screen still launches when accessibility mode is enabled.
     */
    @Test
    public void entrantMainScreen_launchesWhenAccessibilityEnabled() {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, true).apply();

        try (ActivityScenario<EntrantMainActivity> scenario = launchMainActivity()) {
            onView(withId(R.id.tvBrowseEventsTitle))
                    .check(matches(isDisplayed()));
            onView(withId(R.id.rvEvents))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that bottom navigation is displayed.
     */
    @Test
    public void entrantMainScreen_bottomNavigationIsDisplayed() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchMainActivity()) {
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_history)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_qr_scan)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
        }
    }
}