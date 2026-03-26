package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/**
 * Android instrumentation tests for {@link EntrantEventDetailsActivity}.
 *
 * <p>These tests verify that:
 * <ul>
 *     <li>The screen launches and shows its basic structure</li>
 *     <li>Bottom navigation is visible</li>
 *     <li>Invitation controls exist and start hidden when appropriate</li>
 *     <li>The waitlist action button is present</li>
 *     <li>Entrant UI does not expose organizer-only controls</li>
 *     <li>Accessibility mode does not break the screen</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsActivityTest {

    /** SharedPreferences file used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for entrant accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private SharedPreferences prefs;

    @Rule
    public ActivityScenarioRule<EntrantEventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    /**
     * Creates a fresh launch intent with unique test event id.
     *
     * @return intent for launching the activity
     */
    private static Intent createIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(
                EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "test_event_id_" + UUID.randomUUID()
        );
        intent.putExtra(
                EntrantEventDetailsActivity.EXTRA_USER_ID,
                "test_user_id"
        );
        return intent;
    }

    /**
     * Sets up a clean SharedPreferences state before each test.
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean(KEY_ACCESSIBILITY_MODE, false).apply();
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
     * Verifies that the basic event details screen structure is visible on launch.
     */
    @Test
    public void testInitialUIState() {
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(withText("Event Details")));
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        onView(withId(R.id.btnComments)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that bottom navigation is displayed.
     */
    @Test
    public void testBottomNavigationIsDisplayed() {
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_history)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_qr_scan)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that invitation-related UI elements exist and that hidden containers
     * are initially not shown.
     */
    @Test
    public void testInvitationUiElementsExist() {
        onView(withId(R.id.btnAcceptInvite)).check(matches(withText(R.string.accept_invite)));
        onView(withId(R.id.btnDeclineInvite)).check(matches(withText(R.string.decline_invite)));
        onView(withId(R.id.invitationButtonsContainer))
                .check(matches(withEffectiveVisibility(GONE)));
        onView(withId(R.id.registrationEndedContainer))
                .check(matches(withEffectiveVisibility(GONE)));
    }

    /**
     * Verifies that the waitlist action button is visible by default and clickable.
     */
    @Test
    public void testWaitlistActionButtonIsVisibleByDefault() {
        onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
        onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.join_wait_list)));
        onView(withId(R.id.btnWaitlistAction)).check(matches(isClickable()));
    }

    /**
     * Verifies that organizer-only controls are not present on the entrant event details screen.
     */
    @Test
    public void testEntrantScreenDoesNotExposeOrganizerEditButton() {
        onView(withId(R.id.btnEditEvent)).check(doesNotExist());
    }

    /**
     * Verifies that enabling accessibility mode does not prevent the screen from loading
     * and keeps key UI elements visible.
     */
    @Test
    public void testEventDetailsWorksWithAccessibilityEnabled() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences localPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        localPrefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, true).apply();

        try (var scenario = androidx.test.core.app.ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.tvEventDetailsTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
        }
    }
}