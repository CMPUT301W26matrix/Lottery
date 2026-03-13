package com.example.lottery.util;

/**
 * Utility class used to determine the type of lottery notification.
 *
 * <p>This helper supports the notification user stories:
 *
 * <ul>
 *   <li><b>US 01.04.01</b> – Receive a notification when selected to participate (win the lottery).</li>
 *   <li><b>US 01.04.02</b> – Receive a notification when not selected to participate (lose the lottery).</li>
 * </ul>
 *
 * <p>The methods interpret the notification type stored in Firestore and allow
 * application logic and tests to determine whether the notification represents
 * a winning or losing result.
 */
public final class NotificationTypeUtil {

    /**
     * Private constructor to prevent instantiation since this is a utility class.
     */
    private NotificationTypeUtil() {
        // Utility class
    }

    /**
     * Determines whether a notification represents a lottery win.
     *
     * @param type the notification type string stored in the database
     * @return true if the notification represents a winning lottery result, false otherwise
     */
    public static boolean isWinningNotification(String type) {
        return "win".equalsIgnoreCase(type);
    }

    /**
     * Determines whether a notification represents a lottery loss.
     *
     * @param type the notification type string stored in the database
     * @return true if the notification represents a losing lottery result, false otherwise
     */
    public static boolean isLosingNotification(String type) {
        return "lose".equalsIgnoreCase(type);
    }
}