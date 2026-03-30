package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();
        stubAllEntrantActivities();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private void stubAllEntrantActivities() {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        intending(hasComponent(EntrantMainActivity.class.getName())).respondWith(ok);
        intending(hasComponent(EntrantEventHistoryActivity.class.getName())).respondWith(ok);
        intending(hasComponent(EntrantQrScanActivity.class.getName())).respondWith(ok);
        intending(hasComponent(EntrantProfileActivity.class.getName())).respondWith(ok);
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

    // ================================================================
    // From EntrantMainActivity  (currentTab = HOME)
    // ================================================================

    @Test
    public void fromHome_toHistory() {
        try (ActivityScenario<EntrantMainActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromHome_toQrScan() {
        try (ActivityScenario<EntrantMainActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromHome_toProfile() {
        try (ActivityScenario<EntrantMainActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantMainActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantEventHistoryActivity  (currentTab = HISTORY)
    // ================================================================

    @Test
    public void fromHistory_toHome() {
        try (ActivityScenario<EntrantEventHistoryActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromHistory_toQrScan() {
        try (ActivityScenario<EntrantEventHistoryActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromHistory_toProfile() {
        try (ActivityScenario<EntrantEventHistoryActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantEventHistoryActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantQrScanActivity  (currentTab = QR_SCAN)
    // ================================================================

    @Test
    public void fromQrScan_toHome() {
        try (ActivityScenario<EntrantQrScanActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromQrScan_toHistory() {
        try (ActivityScenario<EntrantQrScanActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromQrScan_toProfile() {
        try (ActivityScenario<EntrantQrScanActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantQrScanActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }

    // ================================================================
    // From EntrantProfileActivity  (currentTab = PROFILE)
    // ================================================================

    @Test
    public void fromProfile_toHome() {
        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromProfile_toHistory() {
        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromProfile_toQrScan() {
        try (ActivityScenario<EntrantProfileActivity> ignored =
                     ActivityScenario.launch(entrantIntent(EntrantProfileActivity.class))) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    // ================================================================
    // From NotificationsActivity  (currentTab = HISTORY, finishOnNavigate)
    // ================================================================

    private Intent notificationsIntent() {
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, TEST_USER_ID);
        return intent;
    }

    @Test
    public void fromNotifications_toHome() {
        try (ActivityScenario<NotificationsActivity> ignored =
                     ActivityScenario.launch(notificationsIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(EntrantMainActivity.class));
        }
    }

    @Test
    public void fromNotifications_toHistory() {
        try (ActivityScenario<NotificationsActivity> ignored =
                     ActivityScenario.launch(notificationsIntent())) {
            onView(withId(R.id.nav_history)).perform(click());
            intended(intentTo(EntrantEventHistoryActivity.class));
        }
    }

    @Test
    public void fromNotifications_toQrScan() {
        try (ActivityScenario<NotificationsActivity> ignored =
                     ActivityScenario.launch(notificationsIntent())) {
            onView(withId(R.id.nav_qr_scan)).perform(click());
            intended(intentTo(EntrantQrScanActivity.class));
        }
    }

    @Test
    public void fromNotifications_toProfile() {
        try (ActivityScenario<NotificationsActivity> ignored =
                     ActivityScenario.launch(notificationsIntent())) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(EntrantProfileActivity.class));
        }
    }
}
