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

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Comprehensive navigation tests for {@link com.example.lottery.util.OrganizerNavigationHelper}.
 * Verifies that every organizer screen's bottom-navigation bar (including the
 * centre Create-Event FAB) routes to the correct target Activity with the
 * expected {@code userId} extra.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerNavigationHelperTest {

    private static final String TEST_USER_ID = "nav_test_organizer";
    private static final String TEST_EVENT_ID = "nav_test_event";
    private Context context;
    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        db = FirebaseFirestore.getInstance();

        // Seed a test event so OrganizerEventDetailsActivity doesn't finish() early
        Event event = new Event();
        event.setEventId(TEST_EVENT_ID);
        event.setTitle("Nav Test Event");
        event.setDetails("Navigation test event.");
        event.setOrganizerId(TEST_USER_ID);
        event.touch();
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).set(event), 10, TimeUnit.SECONDS);

        Intents.init();
    }

    @After
    public void tearDown() throws Exception {
        Intents.release();
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
    }

    /**
     * Stub every organizer Activity EXCEPT the given source so that the source can
     * actually launch while navigation targets are intercepted.
     */
    private void stubAllOrganizerActivitiesExcept(Class<?> source) {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        if (source != OrganizerBrowseEventsActivity.class)
            intending(hasComponent(OrganizerBrowseEventsActivity.class.getName())).respondWith(ok);
        if (source != OrganizerNotificationsActivity.class)
            intending(hasComponent(OrganizerNotificationsActivity.class.getName())).respondWith(ok);
        if (source != OrganizerQrEventListActivity.class)
            intending(hasComponent(OrganizerQrEventListActivity.class.getName())).respondWith(ok);
        if (source != OrganizerProfileActivity.class)
            intending(hasComponent(OrganizerProfileActivity.class.getName())).respondWith(ok);
        if (source != OrganizerCreateEventActivity.class)
            intending(hasComponent(OrganizerCreateEventActivity.class.getName())).respondWith(ok);
        if (source != OrganizerEventDetailsActivity.class)
            intending(hasComponent(OrganizerEventDetailsActivity.class.getName())).respondWith(ok);
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

    private ActivityScenario<?> launchScreen(Class<?> source, Intent intent) {
        stubAllOrganizerActivitiesExcept(source);
        return ActivityScenario.launch(intent);
    }

    // ================================================================
    // From OrganizerBrowseEventsActivity  (currentTab = HOME)
    // ================================================================

    @Test
    public void fromHome_toNotifications() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerBrowseEventsActivity.class, organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromHome_toQrCode() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerBrowseEventsActivity.class, organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromHome_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerBrowseEventsActivity.class, organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromHome_toCreate() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerBrowseEventsActivity.class, organizerIntent(OrganizerBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerNotificationsActivity  (currentTab = NOTIFICATIONS)
    // ================================================================

    @Test
    public void fromNotifications_toHome() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerNotificationsActivity.class, organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromNotifications_toQrCode() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerNotificationsActivity.class, organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromNotifications_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerNotificationsActivity.class, organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromNotifications_toCreate() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerNotificationsActivity.class, organizerIntent(OrganizerNotificationsActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerQrEventListActivity  (currentTab = QR_CODE)
    // ================================================================

    @Test
    public void fromQrCode_toHome() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerQrEventListActivity.class, organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromQrCode_toNotifications() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerQrEventListActivity.class, organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromQrCode_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerQrEventListActivity.class, organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromQrCode_toCreate() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerQrEventListActivity.class, organizerIntent(OrganizerQrEventListActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerProfileActivity  (currentTab = PROFILE)
    // ================================================================

    @Test
    public void fromProfile_toHome() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerProfileActivity.class, organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromProfile_toNotifications() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerProfileActivity.class, organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromProfile_toQrCode() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerProfileActivity.class, organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromProfile_toCreate() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerProfileActivity.class, organizerIntent(OrganizerProfileActivity.class))) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }

    // ================================================================
    // From OrganizerEventDetailsActivity  (currentTab = HOME, finishOnNavigate)
    // ================================================================

    private Intent eventDetailIntent() {
        Intent intent = organizerIntent(OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        return intent;
    }

    @Test
    public void fromEventDetails_toHome() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(OrganizerBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toNotifications() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_notifications)).perform(click());
            intended(intentTo(OrganizerNotificationsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toQrCode() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_qr_code)).perform(click());
            intended(intentTo(OrganizerQrEventListActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toProfile() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_profile)).perform(click());
            intended(intentTo(OrganizerProfileActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toCreate() {
        try (ActivityScenario<?> ignored =
                     launchScreen(OrganizerEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_create_container)).perform(click());
            intended(intentTo(OrganizerCreateEventActivity.class));
        }
    }
}
