package com.example.lottery.util;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.lottery.OrganizerBrowseEventsActivity;
import com.example.lottery.OrganizerCreateEventActivity;
import com.example.lottery.OrganizerNotificationsActivity;
import com.example.lottery.OrganizerProfileActivity;
import com.example.lottery.OrganizerQrEventListActivity;
import com.example.lottery.R;

/**
 * Centralised helper that wires up the organizer bottom-navigation bar
 * ({@code layout_bottom_nav_organizer.xml}) in every organizer screen.
 *
 * <p>The centre FAB (Create Event) is handled separately from the four
 * regular tabs.  Admin-role extras are read from {@link AdminRoleManager}
 * automatically.</p>
 */
public final class OrganizerNavigationHelper {

    /** The four regular tabs in the organizer bottom-navigation bar. */
    public enum OrganizerTab {
        HOME, NOTIFICATIONS, QR_CODE, PROFILE
    }

    private static final int[] TAB_VIEW_IDS = {
            R.id.nav_home,
            R.id.nav_notifications,
            R.id.nav_qr_code,
            R.id.nav_profile
    };

    private static final int[][] TAB_ICON_TEXT_IDS = {
            {R.id.iv_nav_home, R.id.tv_nav_home},
            {R.id.iv_nav_notifications, R.id.tv_nav_notifications},
            {R.id.iv_nav_qr_code, R.id.tv_nav_qr_code},
            {R.id.iv_nav_profile, R.id.tv_nav_profile}
    };

    private static final Class<?>[] TAB_TARGETS = {
            OrganizerBrowseEventsActivity.class,
            OrganizerNotificationsActivity.class,
            OrganizerQrEventListActivity.class,
            OrganizerProfileActivity.class
    };

    private OrganizerNavigationHelper() { }

    /**
     * Highlights the active tab and wires every tab's click listener,
     * including the centre Create-Event FAB.
     *
     * @param activity   the hosting Activity
     * @param currentTab which tab this screen represents
     * @param userId     the organizer's userId
     */
    public static void setup(Activity activity, OrganizerTab currentTab, String userId) {
        setup(activity, currentTab, userId, false);
    }

    /**
     * Highlights the active tab and wires every tab's click listener,
     * including the centre Create-Event FAB.
     *
     * @param activity          the hosting Activity
     * @param currentTab        which tab this screen represents
     * @param userId            the organizer's userId
     * @param finishOnNavigate  if {@code true}, calls {@code finish()} after
     *                          starting the target Activity (use for detail screens)
     */
    public static void setup(Activity activity, OrganizerTab currentTab,
                             String userId, boolean finishOnNavigate) {
        boolean isAdminRole = AdminRoleManager.isAdminRoleSession(activity);
        String adminUserId = isAdminRole ? AdminRoleManager.getAdminUserId(activity) : null;

        highlightTab(activity, currentTab);
        setupClickListeners(activity, currentTab, userId,
                isAdminRole, adminUserId, finishOnNavigate);
        setupCreateButton(activity, userId, isAdminRole, adminUserId);
    }

    /**
     * Replaces all tab and FAB click listeners with no-ops.
     * Call this after {@link #setup} when the user must complete their profile
     * before navigating away.
     */
    public static void disableNavigation(Activity activity) {
        for (int id : TAB_VIEW_IDS) {
            View btn = activity.findViewById(id);
            if (btn != null) btn.setOnClickListener(v -> { /* blocked */ });
        }
        View fab = activity.findViewById(R.id.nav_create_container);
        if (fab != null) fab.setOnClickListener(v -> { /* blocked */ });
    }

    private static void highlightTab(Activity activity, OrganizerTab activeTab) {
        int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        OrganizerTab[] tabs = OrganizerTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int color = (tabs[i] == activeTab) ? activeColor : inactiveColor;
            ImageView icon = activity.findViewById(TAB_ICON_TEXT_IDS[i][0]);
            TextView text = activity.findViewById(TAB_ICON_TEXT_IDS[i][1]);
            if (icon != null) icon.setImageTintList(ColorStateList.valueOf(color));
            if (text != null) text.setTextColor(color);
        }
    }

    private static void setupClickListeners(Activity activity, OrganizerTab currentTab,
                                            String userId, boolean isAdminRole,
                                            String adminUserId, boolean finishOnNavigate) {
        OrganizerTab[] tabs = OrganizerTab.values();
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
                    if (target == OrganizerBrowseEventsActivity.class) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }
                    activity.startActivity(intent);
                    if (finishOnNavigate) {
                        if (target == OrganizerBrowseEventsActivity.class) {
                            activity.finish();
                        }
                    } else if (currentTab != OrganizerTab.HOME) {
                        activity.finish();
                    }
                });
            }
        }
    }

    private static void setupCreateButton(Activity activity, String userId,
                                          boolean isAdminRole, String adminUserId) {
        View btn = activity.findViewById(R.id.nav_create_container);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(activity, OrganizerCreateEventActivity.class);
            intent.putExtra("userId", userId);
            if (isAdminRole) {
                intent.putExtra("isAdminRole", true);
                intent.putExtra("adminUserId", adminUserId);
            }
            activity.startActivity(intent);
        });
    }
}
