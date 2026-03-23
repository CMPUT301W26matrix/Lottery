package com.example.lottery.util;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lottery.LotteryApplication;

/**
 * Utility for resolving the current user ID from Intent extras
 * with a SharedPreferences fallback.
 */
public final class SessionUtil {

    private static final String KEY_USER_ID = "userId";

    private SessionUtil() { }

    /**
     * Resolves the user ID by checking the Intent extra first,
     * then falling back to SharedPreferences.
     *
     * @return the user ID, or {@code null} if unavailable from both sources.
     */
    @Nullable
    public static String resolveUserId(@NonNull Activity activity) {
        String userId = activity.getIntent().getStringExtra(KEY_USER_ID);
        if (userId == null || userId.isEmpty()) {
            SharedPreferences prefs = activity.getSharedPreferences(
                    LotteryApplication.PREFS_NAME, Activity.MODE_PRIVATE);
            userId = prefs.getString(KEY_USER_ID, null);
        }
        return (userId != null && !userId.isEmpty()) ? userId : null;
    }
}
