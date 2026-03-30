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

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Comprehensive navigation tests for {@link com.example.lottery.util.AdminNavigationHelper}.
 * Verifies that every admin screen's bottom-navigation bar routes to the correct
 * target Activity with the expected Intent extras ({@code role} and {@code userId}).
 */
@RunWith(AndroidJUnit4.class)
public class AdminNavigationHelperTest {

    private static final String TEST_USER_ID = "nav_test_admin";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();
        stubAllAdminActivities();
    }

    @After
    public void tearDown() {
        AdminImageDetailsActivity.testEvent = null;
        Intents.release();
    }

    /** Stub every admin Activity so tapping a nav button never actually launches it. */
    private void stubAllAdminActivities() {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        intending(hasComponent(AdminBrowseEventsActivity.class.getName())).respondWith(ok);
        intending(hasComponent(AdminBrowseProfilesActivity.class.getName())).respondWith(ok);
        intending(hasComponent(AdminBrowseImagesActivity.class.getName())).respondWith(ok);
        intending(hasComponent(AdminBrowseLogsActivity.class.getName())).respondWith(ok);
        intending(hasComponent(AdminProfileActivity.class.getName())).respondWith(ok);
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

    // ================================================================
    // From AdminBrowseEventsActivity  (currentTab = EVENTS)
    // ================================================================

    @Test
    public void fromEvents_toProfiles() {
        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromEvents_toImages() {
        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromEvents_toLogs() {
        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromEvents_toSettings() {
        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseEventsActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseProfilesActivity  (currentTab = PROFILES)
    // ================================================================

    @Test
    public void fromProfiles_toEvents() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromProfiles_toImages() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromProfiles_toLogs() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromProfiles_toSettings() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseProfilesActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseImagesActivity  (currentTab = IMAGES)
    // ================================================================

    @Test
    public void fromImages_toEvents() {
        try (ActivityScenario<AdminBrowseImagesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromImages_toProfiles() {
        try (ActivityScenario<AdminBrowseImagesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromImages_toLogs() {
        try (ActivityScenario<AdminBrowseImagesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromImages_toSettings() {
        try (ActivityScenario<AdminBrowseImagesActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseImagesActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminBrowseLogsActivity  (currentTab = LOGS)
    // ================================================================

    @Test
    public void fromLogs_toEvents() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromLogs_toProfiles() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromLogs_toImages() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromLogs_toSettings() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminBrowseLogsActivity.class))) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }

    // ================================================================
    // From AdminProfileActivity  (currentTab = SETTINGS)
    // ================================================================

    @Test
    public void fromSettings_toEvents() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromSettings_toProfiles() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromSettings_toImages() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminProfileActivity.class))) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromSettings_toLogs() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(adminIntent(AdminProfileActivity.class))) {
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
        intent.putExtra("eventId", "nav_test_event");
        return intent;
    }

    @Test
    public void fromEventDetails_toEvents() {
        try (ActivityScenario<AdminEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toProfiles() {
        try (ActivityScenario<AdminEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toImages() {
        try (ActivityScenario<AdminEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toLogs() {
        try (ActivityScenario<AdminEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromEventDetails_toSettings() {
        try (ActivityScenario<AdminEventDetailsActivity> ignored =
                     ActivityScenario.launch(eventDetailIntent())) {
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
        event.setPosterUri("https://example.com/poster.png");
        AdminImageDetailsActivity.testEvent = event;

        Intent intent = adminIntent(AdminImageDetailsActivity.class);
        intent.putExtra("eventId", "nav_test_image_event");
        return intent;
    }

    @Test
    public void fromImageDetails_toEvents() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored =
                     ActivityScenario.launch(imageDetailIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(intentTo(AdminBrowseEventsActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toProfiles() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored =
                     ActivityScenario.launch(imageDetailIntent())) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(intentTo(AdminBrowseProfilesActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toImages() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored =
                     ActivityScenario.launch(imageDetailIntent())) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(intentTo(AdminBrowseImagesActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toLogs() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored =
                     ActivityScenario.launch(imageDetailIntent())) {
            onView(withId(R.id.nav_logs)).perform(click());
            intended(intentTo(AdminBrowseLogsActivity.class));
        }
    }

    @Test
    public void fromImageDetails_toSettings() {
        try (ActivityScenario<AdminImageDetailsActivity> ignored =
                     ActivityScenario.launch(imageDetailIntent())) {
            onView(withId(R.id.nav_admin_settings)).perform(click());
            intended(intentTo(AdminProfileActivity.class));
        }
    }
}
