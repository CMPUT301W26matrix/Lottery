package com.example.lottery.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to manage admin role switching state.
 * This allows us to detect when an entrant/organizer is actually an admin using a role profile.
 */
public class AdminRoleManager {

    private static final String PREFS_NAME = "AdminRolePrefs";
    private static final String KEY_IS_ADMIN_SESSION = "is_admin_session";
    private static final String KEY_ADMIN_USER_ID = "admin_user_id";

    /**
     * Marks the current session as an admin using a role profile.
     * Call this before navigating from AdminProfileActivity to a role.
     */
    public static void setAdminRoleSession(Context context, String adminUserId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_ADMIN_SESSION, true)
                .putString(KEY_ADMIN_USER_ID, adminUserId)
                .apply();
    }

    /**
     * Checks if the current session is an admin using a role profile.
     */
    public static boolean isAdminRoleSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_ADMIN_SESSION, false);
    }

    /**
     * Gets the admin user ID if this is an admin role session.
     */
    public static String getAdminUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ADMIN_USER_ID, null);
    }

    /**
     * Clears the admin role session (call when logging out or returning to main admin).
     */
    public static void clearAdminRoleSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
