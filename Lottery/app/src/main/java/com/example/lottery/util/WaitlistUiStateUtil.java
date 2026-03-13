package com.example.lottery.util;

/**
 * Utility class that determines the correct text to display on the
 * waitlist action button in the entrant event details screen.
 *
 * <p>This helper supports the following user stories:
 *
 * <ul>
 *   <li><b>US 01.01.01</b> – Join the waiting list for a specific event.</li>
 *   <li><b>US 01.01.02</b> – Leave the waiting list for a specific event.</li>
 * </ul>
 *
 * <p>The button text changes depending on whether the entrant is already
 * part of the event's waiting list.
 *
 * <ul>
 *   <li>If the user is not in the waitlist → "Join Wait List"</li>
 *   <li>If the user is already in the waitlist → "Leave Wait List"</li>
 * </ul>
 *
 * <p>This logic is separated into a utility class so it can be easily
 * unit tested without relying on Android UI components.
 */
public final class WaitlistUiStateUtil {

    /**
     * Private constructor to prevent instantiation since this is a utility class.
     */
    private WaitlistUiStateUtil() {
        // Utility class
    }

    /**
     * Returns the appropriate button text based on the user's waitlist status.
     *
     * @param isInWaitlist true if the entrant is currently in the event's waitlist
     * @return "Leave Wait List" if the user is already in the waitlist,
     *         otherwise "Join Wait List"
     */
    public static String getWaitlistButtonText(boolean isInWaitlist) {
        return isInWaitlist ? "Leave Wait List" : "Join Wait List";
    }
}