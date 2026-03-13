package com.example.lottery.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link WaitlistUiStateUtil}.
 *
 * <p>These tests verify that the waitlist button text correctly reflects
 * the user's current waitlist state.
 *
 * <ul>
 *   <li><b>US 01.01.01</b> – Join the waiting list for a specific event.</li>
 *   <li><b>US 01.01.02</b> – Leave the waiting list for a specific event.</li>
 * </ul>
 *
 * <p>The utility method determines whether the UI should display
 * "Join Wait List" or "Leave Wait List" depending on whether the
 * user is already in the event's waiting list.
 */
public class WaitlistUiStateUtilTest {

    /**
     * Verifies that the button text is "Join Wait List"
     * when the user is not currently in the waiting list.
     */
    @Test
    public void getWaitlistButtonText_returnsJoin_whenUserNotInWaitlist() {
        String result = WaitlistUiStateUtil.getWaitlistButtonText(false);
        assertEquals("Join Wait List", result);
    }

    /**
     * Verifies that the button text is "Leave Wait List"
     * when the user is already in the waiting list.
     */
    @Test
    public void getWaitlistButtonText_returnsLeave_whenUserInWaitlist() {
        String result = WaitlistUiStateUtil.getWaitlistButtonText(true);
        assertEquals("Leave Wait List", result);
    }
}