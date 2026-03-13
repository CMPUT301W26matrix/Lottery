package com.example.lottery.util;

/**
 * Utility class for determining whether the entrant event list
 * or the empty state should be shown.
 *
 * <p>Supports:
 * <b>US 01.01.03</b> – entrant can see a list of events available
 * to join the waiting list for.
 */
public final class EventListUiStateUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private EventListUiStateUtil() {
        // Utility class
    }

    /**
     * Returns true if the empty state should be shown.
     *
     * @param eventCount number of available events
     * @return true when there are no events, false otherwise
     */
    public static boolean shouldShowEmptyState(int eventCount) {
        return eventCount == 0;
    }

    /**
     * Returns true if the event list should be shown.
     *
     * @param eventCount number of available events
     * @return true when there is at least one event, false otherwise
     */
    public static boolean shouldShowEventList(int eventCount) {
        return eventCount > 0;
    }
}