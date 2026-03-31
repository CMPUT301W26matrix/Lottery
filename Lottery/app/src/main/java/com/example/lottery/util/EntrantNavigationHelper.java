package com.example.lottery.util;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.lottery.EntrantEventHistoryActivity;
import com.example.lottery.EntrantMainActivity;
import com.example.lottery.EntrantProfileActivity;
import com.example.lottery.EntrantQrScanActivity;
import com.example.lottery.R;

/**
 * Centralised helper that wires up the entrant bottom-navigation bar
 * ({@code layout_bottom_nav_entrant.xml}) in every entrant screen.
 *
 * <p>Call {@link #setup(Activity, EntrantTab, String)} from {@code onCreate}
 * after {@code setContentView}.  Admin-role extras are read from
 * {@link AdminRoleManager} automatically.</p>
 */
public final class EntrantNavigationHelper {

    private static final int[] TAB_VIEW_IDS = {
            R.id.nav_home,
            R.id.nav_history,
            R.id.nav_qr_scan,
            R.id.nav_profile
    };
    private static final int[][] TAB_ICON_TEXT_IDS = {
            {R.id.iv_nav_home, R.id.tv_nav_home},
            {R.id.iv_nav_history, R.id.tv_nav_history},
            {R.id.iv_nav_qr_scan, R.id.tv_nav_qr_scan},
            {R.id.iv_nav_profile, R.id.tv_nav_profile}
    };
    private static final Class<?>[] TAB_TARGETS = {
            EntrantMainActivity.class,
            EntrantEventHistoryActivity.class,
            EntrantQrScanActivity.class,
            EntrantProfileActivity.class
    };

    private EntrantNavigationHelper() {
    }

    /**
     * Highlights the active tab and wires every tab's click listener.
     *
     * @param activity   the hosting Activity
     * @param currentTab which tab this screen represents
     * @param userId     the entrant's userId
     */
    public static void setup(Activity activity, EntrantTab currentTab, String userId) {
        setup(activity, currentTab, userId, false);
    }

    /**
     * Highlights the active tab and wires every tab's click listener.
     *
     * @param activity         the hosting Activity
     * @param currentTab       which tab this screen represents
     * @param userId           the entrant's userId
     * @param finishOnNavigate if {@code true}, calls {@code finish()} after
     *                         starting the target Activity (use for detail screens)
     */
    public static void setup(Activity activity, EntrantTab currentTab,
                             String userId, boolean finishOnNavigate) {
        boolean isAdminRole = AdminRoleManager.isAdminRoleSession(activity);
        String adminUserId = isAdminRole ? AdminRoleManager.getAdminUserId(activity) : null;

        highlightTab(activity, currentTab);
        setupClickListeners(activity, currentTab, userId,
                isAdminRole, adminUserId, finishOnNavigate);
    }

    /**
     * Replaces all tab click listeners with no-ops.
     * Call this after {@link #setup} when the user must complete their profile
     * before navigating away.
     */
    public static void disableNavigation(Activity activity) {
        for (int id : TAB_VIEW_IDS) {
            View btn = activity.findViewById(id);
            if (btn != null) btn.setOnClickListener(v -> { /* blocked */ });
        }
    }

    private static void highlightTab(Activity activity, EntrantTab activeTab) {
        int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        EntrantTab[] tabs = EntrantTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int color = (tabs[i] == activeTab) ? activeColor : inactiveColor;
            ImageView icon = activity.findViewById(TAB_ICON_TEXT_IDS[i][0]);
            TextView text = activity.findViewById(TAB_ICON_TEXT_IDS[i][1]);
            if (icon != null) icon.setImageTintList(ColorStateList.valueOf(color));
            if (text != null) text.setTextColor(color);
        }
    }

    private static void setupClickListeners(Activity activity, EntrantTab currentTab,
                                            String userId, boolean isAdminRole,
                                            String adminUserId, boolean finishOnNavigate) {
        EntrantTab[] tabs = EntrantTab.values();
        for (int i = 0; i < tabs.length; i++) {
            View btn = activity.findViewById(TAB_VIEW_IDS[i]);
            if (btn == null) continue;

            if (tabs[i] == currentTab && !finishOnNavigate) {
                btn.setOnClickListener(v -> { /* already on this tab */ });
            } else {
                Class<?> target = TAB_TARGETS[i];
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, target);
                    intent.putExtra("userId", userId);
                    if (isAdminRole) {
                        intent.putExtra("isAdminRole", true);
                        intent.putExtra("adminUserId", adminUserId);
                    }
                    if (target == EntrantMainActivity.class) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }
                    activity.startActivity(intent);
                    // Detail screens: only finish when going Home.
                    // Tab screens: always finish unless this IS Home.
                    if (finishOnNavigate) {
                        if (target == EntrantMainActivity.class) {
                            activity.finish();
                        }
                    } else if (currentTab != EntrantTab.HOME) {
                        activity.finish();
                    }
                });
            }
        }
    }

    /**
     * The four tabs in the entrant bottom-navigation bar.
     */
    public enum EntrantTab {
        HOME, HISTORY, QR_SCAN, PROFILE
    }
}
