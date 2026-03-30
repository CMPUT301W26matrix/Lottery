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
 * Comprehensive navigation tests for {@link com.example.lottery.util.OrganizerNavigationHelper}.
 * Verifies that every organizer screen's bottom-navigation bar (including the
 * centre Create-Event FAB) routes to the correct target Activity with the
 * expected {@code userId} extra.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerNavigationHelperTest {

    private static final String TEST_USER_ID = "nav_test_organizer";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();
        stubAllOrganizerActivities();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private void stubAllOrganizerActivities() {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        intending(hasComponent(OrganizerBrowseEventsActivity.class.getName())).respondWith(ok);
        intending(hasComponent(OrganizerNotificationsActivity.class.getName())).respondWith(ok);
        intending(hasComponent(OrganizerQrEventListActivity.class.getName())).respondWith(ok);
        intending(hasComponent(OrganizerProfileActivity.class.getName())).respondWith(ok);
        intending(hasComponent(OrganizerCreateEventActivity.class.getName())).respondWith(ok);
    }

    private Intent organizerIntent(Class<?> cls) {
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
    // From OrganizerBrowseEventsActivity  (currentTab = HOME)
    // ================================================================

    @Test
    public void fromHome_toNotifications() {
        try (ActivityScenario<OrganizerBrowseEventsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromHome_toQrCode() {
        try (ActivityScenario<OrganizerBrowseEventsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromHome_toProfile() {
        try (ActivityScenario<OrganizerBrowseEventsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromHome_toCreate() {
        try (ActivityScenario<OrganizerBrowseEventsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerNotificationsActivity  (currentTab = NOTIFICATIONS)
    // ================================================================

    @Test
    public void fromNotifications_toHome() {
        try (ActivityScenario<OrganizerNotificationsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromNotifications_toQrCode() {
        try (ActivityScenario<OrganizerNotificationsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromNotifications_toProfile() {
        try (ActivityScenario<OrganizerNotificationsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromNotifications_toCreate() {
        try (ActivityScenario<OrganizerNotificationsActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerQrEventListActivity  (currentTab = QR_CODE)
    // ================================================================

    @Test
    public void fromQrCode_toHome() {
        try (ActivityScenario<OrganizerQrEventListActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromQrCode_toNotifications() {
        try (ActivityScenario<OrganizerQrEventListActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromQrCode_toProfile() {
        try (ActivityScenario<OrganizerQrEventListActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromQrCode_toCreate() {
        try (ActivityScenario<OrganizerQrEventListActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerProfileActivity  (currentTab = PROFILE)
    // ================================================================

    @Test
    public void fromProfile_toHome() {
        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromProfile_toNotifications() {
        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromProfile_toQrCode() {
        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromProfile_toCreate() {
        try (ActivityScenario<OrganizerProfileActivity> ignored =
                     ActivityScenario.launch(organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerEventDetailsActivity  (currentTab = HOME, finishOnNavigate)
    // ================================================================

    private Intent eventDetailIntent() {
        Intent intent = organizerIntent(OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", "nav_test_event");
        return intent;
    }

    @Test
    public void fromEventDetails_toHome() {
        try (ActivityScenario<OrganizerEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toNotifications() {
        try (ActivityScenario<OrganizerEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toQrCode() {
        try (ActivityScenario<OrganizerEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toProfile() {
        try (ActivityScenario<OrganizerEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toCreate() {
        try (ActivityScenario<OrganizerEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }
}
