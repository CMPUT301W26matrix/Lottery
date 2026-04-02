package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.AdminRoleManager;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Comprehensive navigation tests for {@link com.example.lottery.util.EntrantNavigationHelper}.
 * Verifies that every entrant screen's bottom-navigation bar routes to the correct
 * target Activity with the expected {@code userId} extra.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantNavigationHelperTest {

    private static final String TEST_USER_ID = "nav_test_entrant";
    private static final String TEST_ADMIN_USER_ID = "admin_main";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();
    }

    @After
    public void tearDown() {
        AdminRoleManager.clearAdminRoleSession(context);
        Intents.release();
    }

    /**
     * Stub every entrant Activity EXCEPT the given source so that the source can
     * actually launch while navigation targets are intercepted.
     */
    private void stubAllEntrantActivitiesExcept(Class<?> source) {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        if (source != EntrantMainActivity.class)
            intending(hasComponent(EntrantMainActivity.class.getName())).respondWith(ok);
        if (source != EntrantEventHistoryActivity.class)
            intending(hasComponent(EntrantEventHistoryActivity.class.getName())).respondWith(ok);
        if (source != EntrantQrScanActivity.class)
            intending(hasComponent(EntrantQrScanActivity.class.getName())).respondWith(ok);
        if (source != EntrantProfileActivity.class)
            intending(hasComponent(EntrantProfileActivity.class.getName())).respondWith(ok);
        if (source != NotificationsActivity.class)
            intending(hasComponent(NotificationsActivity.class.getName())).respondWith(ok);
    }

    private Intent entrantIntent(Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.putExtra("userId", TEST_USER_ID);
        return intent;
    }

    private Matcher<Intent> intentTo(Class<?> target) {
        return allOf(
                hasComponent(target.getName()),
                hasExtra("userId", TEST_USER_ID)
        );
    }

    private ActivityScenario<?> launchScreen(Class<?> source, Intent intent) {
        stubAllEntrantActivitiesExcept(source);
        return ActivityScenario.launch(intent);
    }

    // ================================================================
    // From EntrantMainActivity  (currentTab = EXPLORE)
    // ================================================================

    @Test
    public void fromExplore_toHistory() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(ViewMatchers.withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromExplore_toQrScan() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromExplore_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantEventHistoryActivity  (currentTab = HISTORY)
    // ================================================================

    @Test
    public void fromHistory_toExplore() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantEventHistoryActivity.class, entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_explore)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromHistory_toQrScan() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantEventHistoryActivity.class, entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromHistory_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantEventHistoryActivity.class, entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantQrScanActivity  (currentTab = QR_SCAN)
    // ================================================================

    @Test
    public void fromQrScan_toExplore() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantQrScanActivity.class, entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_explore)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromQrScan_toHistory() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantQrScanActivity.class, entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromQrScan_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantQrScanActivity.class, entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantProfileActivity  (currentTab = PROFILE)
    // ================================================================

    @Test
    public void fromProfile_toExplore() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantProfileActivity.class, entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_explore)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromProfile_toHistory() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantProfileActivity.class, entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromProfile_toQrScan() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantProfileActivity.class, entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    // ================================================================
    // From NotificationsActivity  (currentTab = NONE, finishOnNavigate)
    // ================================================================

    private Intent notificationsIntent() {
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, TEST_USER_ID);
        return intent;
    }

    @Test
    public void fromNotifications_toExplore() {
        try (ActivityScenario<?> ignored =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_explore)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromNotifications_toHistory() {
        try (ActivityScenario<?> ignored =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromNotifications_toQrScan() {
        try (ActivityScenario<?> ignored =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromNotifications_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // Current-tab no-op — clicking the active tab fires no new intent
    // (US 01.01.01 – entrant navigation)
    // ================================================================

    /**
     * Clicking the PROFILE tab while on EntrantProfileActivity should be a no-op.
     */
    @Test
    public void fromProfile_clickProfile_isNoOp() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantProfileActivity.class, entrantIntent(EntrantProfileActivity.class))) {
            int before = Intents.getIntents().size();
            onView(withId(R.id.nav_profile)).perform(click());
            assertEquals("Clicking current tab should be a no-op",
                    before, Intents.getIntents().size());
        }
    }

    /**
     * Clicking the HISTORY tab while on EntrantEventHistoryActivity should be a no-op.
     */
    @Test
    public void fromHistory_clickHistory_isNoOp() {
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantEventHistoryActivity.class, entrantIntent(EntrantEventHistoryActivity.class))) {
            int before = Intents.getIntents().size();
            onView(withId(R.id.nav_history)).perform(click());
            assertEquals("Clicking current tab should be a no-op",
                    before, Intents.getIntents().size());
        }
    }

    // ================================================================
    // Admin-role extras — when admin switches to entrant role,
    // navigation intents carry isAdminRole and adminUserId
    // (US 03.09.01 – admin role switch)
    // ================================================================

    /**
     * Navigation from EXPLORE should propagate admin-role extras when in admin session.
     */
    @Test
    public void fromHome_toHistory_adminRole_passesExtras() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_USER_ID);
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(allOf(
                    hasComponent(EntrantEventHistoryActivity.class.getName()),
                    hasExtra("userId", TEST_USER_ID),
                    hasExtra("isAdminRole", true),
                    hasExtra("adminUserId", TEST_ADMIN_USER_ID)
            ));
        }
    }

    /**
     * Navigation to PROFILE should also carry admin-role extras.
     */
    @Test
    public void fromHome_toProfile_adminRole_passesExtras() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_USER_ID);
        try (ActivityScenario<?> ignored =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(allOf(
                    hasComponent(EntrantProfileActivity.class.getName()),
                    hasExtra("userId", TEST_USER_ID),
                    hasExtra("isAdminRole", true),
                    hasExtra("adminUserId", TEST_ADMIN_USER_ID)
            ));
        }
    }

    // ================================================================
    // finish() behaviour — EXPLORE never finishes itself;
    // non-EXPLORE tabs finish when navigating away; detail screens
    // finish only when going to EXPLORE
    // (US 01.01.01 – entrant navigation)
    // ================================================================

    /**
     * EXPLORE is the root tab; navigating away should NOT finish it.
     */
    @Test
    public void fromExplore_toHistory_doesNotFinishSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(EntrantMainActivity.class, entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            scenario.onActivity(activity ->
                    assertFalse("EXPLORE should not finish itself", activity.isFinishing()));
        }
    }

    /**
     * Non-EXPLORE tab should finish itself when navigating away.
     */
    @Test
    public void fromHistory_toExplore_finishesSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(EntrantEventHistoryActivity.class, entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_explore)).perform(click());
            scenario.onActivity(activity ->
                    assertTrue("Non-EXPLORE tab should finish on navigate", activity.isFinishing()));
        }
    }

    /**
     * Detail screen (finishOnNavigate) should finish when navigating to EXPLORE.
     */
    @Test
    public void fromNotifications_toExplore_finishesSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_explore)).perform(click());
            scenario.onActivity(activity ->
                    assertTrue("Detail screen should finish when navigating to EXPLORE",
                            activity.isFinishing()));
        }
    }

    /**
     * Detail screen (finishOnNavigate) should NOT finish when navigating to non-EXPLORE.
     */
    @Test
    public void fromNotifications_toHistory_doesNotFinishSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(NotificationsActivity.class, notificationsIntent())) {
            onView(withId(R.id.nav_history)).perform(click());
            scenario.onActivity(activity ->
                    assertFalse("Detail screen should not finish when navigating to non-EXPLORE",
                            activity.isFinishing()));
        }
    }
}
