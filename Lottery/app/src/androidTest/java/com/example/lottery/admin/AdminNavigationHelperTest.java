package com.example.lottery.admin;

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
 * Comprehensive navigation tests for {@link com.example.lottery.util.AdminNavigationHelper}.
 * Verifies that every admin screen's bottom-navigation bar routes to the correct
 * target Activity with the expected Intent extras ({@code role} and {@code userId}).
 */
@RunWith(AndroidJUnit4.class)
public class AdminNavigationHelperTest {

    private static final String TEST_USER_ID = "nav_test_admin";
    private static final String TEST_EVENT_ID = "nav_test_event";
    private Context context;
    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        db = FirebaseFirestore.getInstance();

        // Seed a test event so detail screens don't finish() early
        Event event = new Event();
        event.setEventId(TEST_EVENT_ID);
        event.setTitle("Nav Test Event");
        event.setDetails("Navigation test event.");
        event.setOrganizerId("test_organizer");
        event.touch();
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).set(event), 10, TimeUnit.SECONDS);

        Intents.init();
    }

    @After
    public void tearDown() throws Exception {
        AdminImageDetailsActivity.testEvent = null;
        Intents.release();
        try {
            Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Cleanup failure should not fail the test
        }
    }

    /**
     * Stub every admin Activity EXCEPT the given source so that the source can
     * actually launch while navigation targets are intercepted.
     */
    private void stubAllAdminActivitiesExcept(Class<?> source) {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        if (source != AdminBrowseEventsActivity.class)
            intending(hasComponent(AdminBrowseEventsActivity.class.getName())).respondWith(ok);
        if (source != AdminBrowseProfilesActivity.class)
            intending(hasComponent(AdminBrowseProfilesActivity.class.getName())).respondWith(ok);
        if (source != AdminBrowseImagesActivity.class)
            intending(hasComponent(AdminBrowseImagesActivity.class.getName())).respondWith(ok);
        if (source != AdminBrowseLogsActivity.class)
            intending(hasComponent(AdminBrowseLogsActivity.class.getName())).respondWith(ok);
        if (source != AdminProfileActivity.class)
            intending(hasComponent(AdminProfileActivity.class.getName())).respondWith(ok);
        if (source != AdminEventDetailsActivity.class)
            intending(hasComponent(AdminEventDetailsActivity.class.getName())).respondWith(ok);
        if (source != AdminImageDetailsActivity.class)
            intending(hasComponent(AdminImageDetailsActivity.class.getName())).respondWith(ok);
    }

    private Intent adminIntent(Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.putExtra("userId", TEST_USER_ID);
        intent.putExtra("role", "admin");
        return intent;
    }

    private Matcher<Intent> intentTo(Class<?> target) {
        return allOf(
                hasComponent(target.getName()),
                hasExtra("role", "admin"),
                hasExtra("userId", TEST_USER_ID)
        );
    }

    private ActivityScenario<?> launchScreen(Class<?> source, Intent intent) {
        stubAllAdminActivitiesExcept(source);
        return ActivityScenario.launch(intent);
    }

    // ================================================================
    // From AdminBrowseEventsActivity  (currentTab = EVENTS)
    // ================================================================

    @Test
    public void fromEvents_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseEventsActivity.class, adminIntent(AdminBrowseEventsActivity.class))) {
            onView(ViewMatchers.withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromEvents_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseEventsActivity.class, adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromEvents_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseEventsActivity.class, adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromEvents_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseEventsActivity.class, adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseProfilesActivity  (currentTab = PROFILES)
    // ================================================================

    @Test
    public void fromProfiles_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromProfiles_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromProfiles_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromProfiles_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseImagesActivity  (currentTab = IMAGES)
    // ================================================================

    @Test
    public void fromImages_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseImagesActivity.class, adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromImages_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseImagesActivity.class, adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromImages_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseImagesActivity.class, adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromImages_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseImagesActivity.class, adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseLogsActivity  (currentTab = LOGS)
    // ================================================================

    @Test
    public void fromLogs_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseLogsActivity.class, adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromLogs_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseLogsActivity.class, adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromLogs_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseLogsActivity.class, adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromLogs_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseLogsActivity.class, adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminProfileActivity  (currentTab = SETTINGS)
    // ================================================================

    @Test
    public void fromSettings_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminProfileActivity.class, adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromSettings_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminProfileActivity.class, adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromSettings_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminProfileActivity.class, adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromSettings_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminProfileActivity.class, adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    // ================================================================
    // From AdminEventDetailsActivity  (currentTab = EVENTS, finishOnNavigate)
    // Detail screens navigate on ALL tabs, including the highlighted one.
    // ================================================================

    private Intent eventDetailIntent() {
        Intent intent = adminIntent(AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        return intent;
    }

    @Test
    public void fromEventDetails_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminImageDetailsActivity  (currentTab = IMAGES, finishOnNavigate)
    // Detail screens navigate on ALL tabs, including the highlighted one.
    // ================================================================

    private Intent imageDetailIntent() {
        Event event = new Event();
        event.setTitle("Nav Test Event");
        event.setDetails("Navigation test.");
        event.setPosterBase64("data:image/png;base64,iVBORw0KGgo=");
        AdminImageDetailsActivity.testEvent = event;

        Intent intent = adminIntent(AdminImageDetailsActivity.class);
        intent.putExtra("eventId", "nav_test_image_event");
        return intent;
    }

    @Test
    public void fromImageDetails_toEvents() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminImageDetailsActivity.class, imageDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toProfiles() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminImageDetailsActivity.class, imageDetailIntent())) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toImages() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminImageDetailsActivity.class, imageDetailIntent())) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toLogs() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminImageDetailsActivity.class, imageDetailIntent())) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toSettings() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminImageDetailsActivity.class, imageDetailIntent())) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // Current-tab no-op — clicking the active tab fires no new intent
    // (US 03.01.01, US 03.02.01 – admin browse navigation)
    // ================================================================

    /**
     * Clicking the PROFILES tab while on AdminBrowseProfilesActivity should be a no-op.
     */
    @Test
    public void fromProfiles_clickProfiles_isNoOp() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            int before = Intents.getIntents().size();
            onView(withId(R.id.nav_profiles)).perform(click());
            assertEquals("Clicking current tab should be a no-op",
                    before, Intents.getIntents().size());
        }
    }

    /**
     * Clicking the SETTINGS tab while on AdminProfileActivity should be a no-op.
     */
    @Test
    public void fromSettings_clickSettings_isNoOp() {
        try (ActivityScenario<?> ignored =
                     launchScreen(AdminProfileActivity.class, adminIntent(AdminProfileActivity.class))) {
            int before = Intents.getIntents().size();
            onView(withId(R.id.nav_admin_settings)).perform(click());
            assertEquals("Clicking current tab should be a no-op",
                    before, Intents.getIntents().size());
        }
    }

    // ================================================================
    // finish() behaviour — EVENTS (HOME) never finishes itself;
    // non-EVENTS tabs finish when navigating away; detail screens
    // always finish.
    // (US 03.01.01 – admin browse events navigation)
    // ================================================================

    /**
     * EVENTS is the root tab; navigating away should NOT finish it.
     */
    @Test
    public void fromEvents_toProfiles_doesNotFinishSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(AdminBrowseEventsActivity.class, adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            scenario.onActivity(activity ->
                    assertFalse("EVENTS (HOME) should not finish itself", activity.isFinishing()));
        }
    }

    /**
     * Non-EVENTS tabs should finish themselves when navigating away.
     */
    @Test
    public void fromProfiles_toEvents_finishesSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(AdminBrowseProfilesActivity.class, adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            scenario.onActivity(activity ->
                    assertTrue("Non-EVENTS tab should finish on navigate", activity.isFinishing()));
        }
    }

    /**
     * Detail screen (finishOnNavigate) should always finish when navigating.
     */
    @Test
    public void fromEventDetails_toEvents_finishesSelf() {
        try (ActivityScenario<?> scenario =
                     launchScreen(AdminEventDetailsActivity.class, eventDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            scenario.onActivity(activity ->
                    assertTrue("Detail screen should finish on navigate", activity.isFinishing()));
        }
    }
}
