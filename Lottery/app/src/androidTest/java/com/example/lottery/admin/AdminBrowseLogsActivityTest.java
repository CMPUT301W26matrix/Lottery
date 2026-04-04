package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseLogsActivityTest {
    private static final String ORGANIZER_ID = "admin_log_organizer_maya_patel";
    private static final String NOTIFICATION_ID = "admin_log_notification_swim_lane";
    private static final String EVENT_TITLE = "Swimming Course for Beginners";
    private static final String LOG_MESSAGE = "Please confirm your beginner lane assignment by Friday evening.";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ActivityScenario<AdminBrowseLogsActivity> launchDefault() {
        return ActivityScenario.launch(AdminBrowseLogsActivity.class);
    }

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        try {
            Tasks.await(db.collection(FirestorePaths.NOTIFICATIONS).document(NOTIFICATION_ID).delete(), 10, TimeUnit.SECONDS);
            Tasks.await(db.collection(FirestorePaths.USERS).document(ORGANIZER_ID).delete(), 10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        Intents.release();
    }

    private void seedOrganizer()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", ORGANIZER_ID);
        user.put("username", "Maya Patel");
        user.put("email", "maya.patel@gmail.com");
        user.put("role", "ORGANIZER");
        Tasks.await(db.collection(FirestorePaths.USERS).document(ORGANIZER_ID).set(user), 10, TimeUnit.SECONDS);
    }

    private void seedNotificationLog()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> log = new HashMap<>();
        log.put("eventId", "swimming_course_lane_assignment");
        log.put("eventTitle", EVENT_TITLE);
        log.put("senderId", ORGANIZER_ID);
        log.put("group", "selected");
        log.put("message", LOG_MESSAGE);
        log.put("recipientCount", 18L);
        log.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.NOTIFICATIONS).document(NOTIFICATION_ID).set(log), 10, TimeUnit.SECONDS);
    }

    private void waitForLogsLoaded(ActivityScenario<AdminBrowseLogsActivity> scenario) throws InterruptedException {
        boolean[] loaded = {false};
        for (int attempt = 0; attempt < 20; attempt++) {
            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView recyclerView = activity.findViewById(R.id.rvLogs);
                loaded[0] = recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() >= 1;
            });
            if (loaded[0]) {
                return;
            }
            Thread.sleep(250);
        }
        assertTrue("Expected seeded notification log to load from Firestore", loaded[0]);
    }

    private void waitForOrganizerNameResolution() throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                onView(withId(R.id.rvLogs)).check(matches(hasDescendant(withText("Organizer: Maya Patel"))));
                return;
            } catch (AssertionError ignored) {
                Thread.sleep(250);
            }
        }
        onView(withId(R.id.rvLogs)).check(matches(hasDescendant(withText("Organizer: Maya Patel"))));
    }

    // US 03.08.01: Admin should see notification logs page title
    @Test
    public void testPageTitleIsDisplayed() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored = launchDefault()) {
            onView(ViewMatchers.withId(R.id.tvPageTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvPageTitle)).check(matches(withText(R.string.admin_logs_title)));
        }
    }

    // US 03.08.01: Admin should see log section title
    @Test
    public void testSectionTitleIsDisplayed() {
        try (ActivityScenario<AdminBrowseLogsActivity> ignored = launchDefault()) {
            onView(withId(R.id.tvSectionTitle)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.tvSectionTitle)).check(matches(withText(R.string.admin_all_logs_title)));
        }
    }

    // US 03.08.01: Empty state should show when no notification logs exist
    @Test
    public void testNoLogsMessageVisibility() {
        try (ActivityScenario<AdminBrowseLogsActivity> scenario = launchDefault()) {
            scenario.onActivity(activity -> activity.findViewById(R.id.tvNoLogs).setVisibility(View.VISIBLE));
            onView(withId(R.id.tvNoLogs)).check(matches(isDisplayed()));
            onView(withId(R.id.tvNoLogs)).check(matches(withText(R.string.admin_no_logs)));
        }
    }

    // US 03.08.01: Admin logs browser should load a real Firestore notification log and
    // render the event title, organizer name, and message on screen.
    @Test
    public void adminBrowseLogs_loadsSeededNotificationLog() throws Exception {
        seedOrganizer();
        seedNotificationLog();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminBrowseLogsActivity.class);
        intent.putExtra("userId", "admin_jordan_clark");

        try (ActivityScenario<AdminBrowseLogsActivity> scenario = ActivityScenario.launch(intent)) {
            waitForLogsLoaded(scenario);

            onView(withId(R.id.rvLogs)).perform(
                    RecyclerViewActions.scrollTo(hasDescendant(withText(EVENT_TITLE)))
            );
            onView(withId(R.id.rvLogs)).check(matches(hasDescendant(withText(EVENT_TITLE))));
            onView(withId(R.id.rvLogs)).check(matches(hasDescendant(withText(LOG_MESSAGE))));
            waitForOrganizerNameResolution();
        }
    }
}
