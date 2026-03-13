package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Android instrumentation tests for {@link EntrantEventDetailsActivity}.
 *
 * <p>These tests verify the correct UI behavior of the waitlist button
 * depending on whether the entrant is currently in the event waitlist.
 *
 * <ul>
 *   <li><b>US 01.01.01</b> – Join the waiting list for a specific event.</li>
 *   <li><b>US 01.01.02</b> – Leave the waiting list for a specific event.</li>
 * </ul>
 *
 * <p>The tests launch the activity using {@link ActivityScenario} and
 * verify that the waitlist action button displays the correct text
 * using Espresso view assertions.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsActivityTest {

    /**
     * Verifies that the waitlist button displays "Join Wait List"
     * when the user is not currently in the event waiting list.
     *
     * <p>This corresponds to:
     * <b>US 01.01.01 – Join the waiting list for a specific event.</b>
     */
    @Test
    public void joinWaitlistButton_isDisplayed() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailsActivity.class
        );

        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "10766c8d-b1e6-4b95-96e2-92742e8063b2");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID,
                "6xygP8FXpxATgAkKmj27");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.btnWaitlistAction))
                    .check(matches(withText("Join Wait List")));
        }
    }

    /**
     * Verifies that the waitlist button displays "Leave Wait List"
     * when the user is already part of the event waiting list.
     *
     * <p>This corresponds to:
     * <b>US 01.01.02 – Leave the waiting list for a specific event.</b>
     */
    @Test
    public void leaveWaitlistButton_isDisplayed_forUserAlreadyInWaitlist() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailsActivity.class
        );

        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "10766c8d-b1e6-4b95-96e2-92742e8063b2");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID,
                "6xygP8FXpxATgAkKmj27");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.btnWaitlistAction))
                    .check(matches(withText("Leave Wait List")));
        }
    }
}