package com.example.lottery.util;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.lottery.admin.AdminBrowseEventsActivity;
import com.example.lottery.admin.AdminBrowseImagesActivity;
import com.example.lottery.admin.AdminBrowseLogsActivity;
import com.example.lottery.admin.AdminBrowseProfilesActivity;
import com.example.lottery.admin.AdminProfileActivity;
import com.example.lottery.R;

/**
 * Centralised helper that wires up the admin bottom-navigation bar
 * ({@code layout_bottom_nav_admin.xml}) in every admin screen.
 *
 * <p>Call {@link #setup(Activity, AdminTab, String)} (or the overload with
 * {@code finishOnNavigate}) from {@code onCreate} after {@code setContentView}.
 */
public final class AdminNavigationHelper {

    private static final int[] TAB_VIEW_IDS = {
            R.id.nav_home,
            R.id.nav_profiles,
            R.id.nav_images,
            R.id.nav_logs,
            R.id.nav_admin_settings
    };
    private static final int[][] TAB_ICON_TEXT_IDS = {
            {R.id.nav_home_icon, R.id.nav_home_text},
            {R.id.nav_profiles_icon, R.id.nav_profiles_text},
            {R.id.nav_images_icon, R.id.nav_images_text},
            {R.id.nav_logs_icon, R.id.nav_logs_text},
            {R.id.nav_settings_icon, R.id.nav_settings_text}
    };
    private static final Class<?>[] TAB_TARGETS = {
            AdminBrowseEventsActivity.class,
            AdminBrowseProfilesActivity.class,
            AdminBrowseImagesActivity.class,
            AdminBrowseLogsActivity.class,
            AdminProfileActivity.class
    };

    private AdminNavigationHelper() {
    }

    /**
     * Re-wires a single tab so it navigates to the target Activity
     * <em>without</em> calling {@code finish()} on the source.
     * Use after {@link #setup} with {@code finishOnNavigate = true}
     * to selectively keep the source screen alive for certain tabs.
     */
    public static void overrideTabWithoutFinish(Activity activity, AdminTab tab, String userId) {
        View btn = activity.findViewById(TAB_VIEW_IDS[tab.ordinal()]);
        if (btn == null) return;
        Class<?> target = TAB_TARGETS[tab.ordinal()];
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("role", "admin");
            if (userId != null) {
                intent.putExtra("userId", userId);
            }
            activity.startActivity(intent);
        });
    }

    /**
     * Highlights the active tab and wires every tab's click listener.
     *
     * @param activity   the hosting Activity
     * @param currentTab which tab this screen represents
     * @param userId     the admin's userId (may be {@code null})
     */
    public static void setup(Activity activity, AdminTab currentTab, String userId) {
        setup(activity, currentTab, userId, false);
    }

    /**
     * Highlights the active tab and wires every tab's click listener.
     *
     * @param activity         the hosting Activity
     * @param currentTab       which tab this screen represents
     * @param userId           the admin's userId (may be {@code null})
     * @param finishOnNavigate if {@code true}, calls {@code finish()} after
     *                         starting the target Activity (use for detail screens)
     */
    public static void setup(Activity activity, AdminTab currentTab,
                             String userId, boolean finishOnNavigate) {
        highlightTab(activity, currentTab);
        setupClickListeners(activity, currentTab, userId, finishOnNavigate);
    }

    private static void highlightTab(Activity activity, AdminTab activeTab) {
        int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        AdminTab[] tabs = AdminTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int color = (tabs[i] == activeTab) ? activeColor : inactiveColor;
            ImageView icon = activity.findViewById(TAB_ICON_TEXT_IDS[i][0]);
            TextView text = activity.findViewById(TAB_ICON_TEXT_IDS[i][1]);
            if (icon != null) icon.setImageTintList(ColorStateList.valueOf(color));
            if (text != null) text.setTextColor(color);
        }
    }

    private static void setupClickListeners(Activity activity, AdminTab currentTab,
                                            String userId, boolean finishOnNavigate) {
        AdminTab[] tabs = AdminTab.values();
        for (int i = 0; i < tabs.length; i++) {
            View btn = activity.findViewById(TAB_VIEW_IDS[i]);
            if (btn == null) continue;

            if (tabs[i] == currentTab && !finishOnNavigate) {
                btn.setOnClickListener(v -> { /* already on this tab */ });
            } else {
                Class<?> target = TAB_TARGETS[i];
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, target);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("role", "admin");
                    if (userId != null) {
                        intent.putExtra("userId", userId);
                    }
                    activity.startActivity(intent);
                    if (finishOnNavigate || currentTab != AdminTab.EVENTS) {
                        activity.finish();
                    }
                });
            }
        }
    }

    /**
     * Tabs in the admin bottom-navigation bar.
     *
     * <ul>
     *   <li>{@code EVENTS} – browse / manage all events</li>
     *   <li>{@code PROFILES} – browse / manage user profiles</li>
     *   <li>{@code IMAGES} – browse / manage uploaded images</li>
     *   <li>{@code LOGS} – view system activity logs</li>
     *   <li>{@code SETTINGS} – admin settings</li>
     * </ul>
     */
    public enum AdminTab {
        EVENTS, PROFILES, IMAGES, LOGS, SETTINGS
    }
}
