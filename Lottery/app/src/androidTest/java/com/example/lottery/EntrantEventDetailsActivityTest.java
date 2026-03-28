package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

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
        onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
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
     * Verifies that the entrant screen does not expose organizer-only editing controls.
     */
    @Test
    public void testEntrantScreenDoesNotExposeOrganizerEditButton() {
        onView(withId(R.id.btnEditEvent)).check(doesNotExist());
    }
}