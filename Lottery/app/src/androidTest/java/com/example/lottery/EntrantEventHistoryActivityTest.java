package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
 * Android tests for {@link EntrantEventHistoryActivity}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The history screen launches successfully</li>
 *     <li>The toolbar and main containers exist</li>
 *     <li>The RecyclerView exists</li>
 *     <li>Accessibility mode does not prevent the screen from opening</li>
 *     <li>Bottom navigation remains visible</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventHistoryActivityTest {

    /** SharedPreferences file used by the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private SharedPreferences prefs;

    /**
     * Sets up a clean preference state before each test.
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
     * Launch helper for the history activity.
     *
     * @return launched ActivityScenario
     */
    private ActivityScenario<EntrantEventHistoryActivity> launchActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventHistoryActivity.class
        );
        intent.putExtra("userId", "test_user_id");
        return ActivityScenario.launch(intent);
    }

    /**
     * Verifies that the history screen opens and the toolbar is visible.
     */
    @Test
    public void historyScreen_opensSuccessfully() {
        try (ActivityScenario<EntrantEventHistoryActivity> scenario = launchActivity()) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the RecyclerView exists in the layout.
     */
    @Test
    public void historyScreen_recyclerViewExists() {
        try (ActivityScenario<EntrantEventHistoryActivity> scenario = launchActivity()) {
            onView(withId(R.id.rvEventHistory))
                    .check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }

    /**
     * Verifies that the main content container exists by checking either the
     * RecyclerView or the empty-state container is present in the layout.
     */
    @Test
    public void historyScreen_mainContentContainerExists() {
        try (ActivityScenario<EntrantEventHistoryActivity> scenario = launchActivity()) {
            try {
                onView(withId(R.id.rvEventHistory))
                        .check(matches(isAssignableFrom(RecyclerView.class)));
            } catch (Throwable t) {
                onView(withId(R.id.emptyStateContainer)).check(matches(isDisplayed()));
            }
        }
    }

    /**
     * Verifies that enabling accessibility mode does not prevent the screen from launching.
     */
    @Test
    public void historyScreen_worksWithAccessibilityEnabled() {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, true).apply();

        try (ActivityScenario<EntrantEventHistoryActivity> scenario = launchActivity()) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the bottom navigation is visible.
     */
    @Test
    public void historyScreen_bottomNavVisible() {
        try (ActivityScenario<EntrantEventHistoryActivity> scenario = launchActivity()) {
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_history)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_qr_scan)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
        }
    }
}