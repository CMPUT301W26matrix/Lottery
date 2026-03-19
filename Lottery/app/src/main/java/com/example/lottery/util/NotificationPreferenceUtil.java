package com.example.lottery.util;

/**
 * Utility methods for notification preference handling.
 */
public final class NotificationPreferenceUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private NotificationPreferenceUtil() {
        // Utility class
    }

    /**
     * Returns whether the user should receive newly created notifications.
     *
     * @param notificationsEnabled stored Firestore value
     * @return true if notifications should be created, false otherwise
     */
    public static boolean shouldReceiveNewNotifications(Boolean notificationsEnabled) {
        return notificationsEnabled == null || notificationsEnabled;
    }

    /**
     * Returns the switch state that should be shown in the UI.
     *
     * @param notificationsEnabled stored Firestore value
     * @return true if switch should be on, false otherwise
     */
    public static boolean getSwitchState(Boolean notificationsEnabled) {
        return notificationsEnabled == null || notificationsEnabled;
    }

    /**
     * Returns the text shown under the notification toggle.
     *
     * @param notificationsEnabled current toggle state
     * @return status text
     */
    public static String getStatusText(boolean notificationsEnabled) {
        if (notificationsEnabled) {
            return "You will receive notifications from organizers and admins.";
        }
        return "You have opted out of notifications.";
    }
}