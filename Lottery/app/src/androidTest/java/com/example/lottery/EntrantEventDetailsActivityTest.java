package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;

/**
 * Android instrumentation tests for {@link EntrantEventDetailsActivity}.
 *
 * <p>These tests verify the basic entrant event-details UI structure and ensure
 * that wow-factor ticket controls are present in the layout and hidden by default
 * until the accepted entrant state is reached.
 */
public class EntrantEventDetailsActivityTest {

    /**
     * Launch rule for the activity under test.
     */
    @Rule
    public ActivityScenarioRule<EntrantEventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(createIntent());

    /**
     * Creates a fresh intent for launching the activity with required extras.
     *
     * @return intent containing event and user extras
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
     * Verifies that the title and back button are displayed on launch.
     */
    @Test
    public void testInitialUIState() {
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.tvEventDetailsTitle)).check(matches(withText("Event Details")));
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that the bottom navigation is displayed.
     */
    @Test
    public void testBottomNavigationIsDisplayed() {
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_explore)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that invitation-related UI elements exist and are hidden by default.
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
     * Verifies that the waitlist action button is visible by default.
     */
    @Test
    public void testWaitlistActionButtonIsVisibleByDefault() {
        onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
        onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.join_wait_list)));
    }

    /**
     * Verifies that the confirmation ticket button exists in the layout and is
     * hidden by default until the entrant reaches an accepted state.
     */
    @Test
    public void testDownloadTicketButtonExistsAndIsHiddenByDefault() {
        onView(withId(R.id.btnDownloadTicket))
                .check(matches(withEffectiveVisibility(GONE)));
    }

    /**
     * Verifies that the confirmation ticket button has the correct text from string resources.
     */
    @Test
    public void testDownloadTicketButtonHasCorrectText() {
        onView(withId(R.id.btnDownloadTicket))
                .check(matches(withText(R.string.download_confirmation_ticket)));
    }

    /**
     * Verifies that the entrant screen does not expose organizer-only editing controls.
     */
    @Test
    public void testEntrantScreenDoesNotExposeOrganizerEditButton() {
        onView(withId(R.id.btnEditEvent)).check(doesNotExist());
    }

    /**
     * Verifies that launching without EXTRA_SOURCE_TAB defaults to highlighting the EXPLORE tab.
     */
    @Test
    public void testDefaultSourceTabHighlightsExplore() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "test_event_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "test_user_id");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                ImageView historyIcon = activity.findViewById(R.id.iv_nav_history);
                assertEquals(activeColor, exploreIcon.getImageTintList().getDefaultColor());
                assertEquals(inactiveColor, historyIcon.getImageTintList().getDefaultColor());
            });
        }
    }

    /**
     * Verifies that launching with EXTRA_SOURCE_TAB = "MY_EVENTS" highlights the My Events tab.
     */
    @Test
    public void testSourceTabMyEventsHighlightsMyEvents() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "test_event_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "test_user_id");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_SOURCE_TAB, "MY_EVENTS");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                ImageView historyIcon = activity.findViewById(R.id.iv_nav_history);
                assertEquals(inactiveColor, exploreIcon.getImageTintList().getDefaultColor());
                assertEquals(activeColor, historyIcon.getImageTintList().getDefaultColor());
            });
        }
    }

    /**
     * Verifies that an invalid EXTRA_SOURCE_TAB falls back to EXPLORE.
     */
    @Test
    public void testInvalidSourceTabFallsBackToExplore() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "test_event_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "test_user_id");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_SOURCE_TAB, "INVALID_TAB");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                assertEquals(activeColor, exploreIcon.getImageTintList().getDefaultColor());
            });
        }
    }
}
