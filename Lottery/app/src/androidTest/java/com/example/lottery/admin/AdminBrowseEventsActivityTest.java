package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented tests for {@link AdminBrowseEventsActivity}.
 * Covers US 03.04.01: As an administrator, I want to be able to browse events.
 * Covers US 03.01.01: As an administrator, I want to be able to remove events.
 */
@RunWith(AndroidJUnit4.class)
public class AdminBrowseEventsActivityTest {
    private static final long FIRESTORE_TIMEOUT_SECONDS = 45;
    private static final String OPEN_EVENT_ID = "admin_event_swimming_course_beginners";
    private static final String PENDING_EVENT_ID = "admin_event_community_soccer_draw";
    private static final String CLOSED_EVENT_ID = "admin_event_charity_potluck_closed";

    private static final String OPEN_EVENT_TITLE = "Swimming Course for Beginners";
    private static final String PENDING_EVENT_TITLE = "Community Soccer Night Draw";
    private static final String CLOSED_EVENT_TITLE = "Charity Potluck Wrap-Up";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Rule
    public ActivityScenarioRule<AdminBrowseEventsActivity> activityRule =
            new ActivityScenarioRule<>(AdminBrowseEventsActivity.class);

    @Before
    public void setUp() {
        Intents.init();
        // Stub AdminEventDetailsActivity so clicking an event doesn't actually launch it
        intending(hasComponent(AdminEventDetailsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private void seedEvent(String eventId, String title, String organizerId, String displayState)
            throws InterruptedException, ExecutionException, TimeoutException {
        ensureFirestoreNetworkEnabled();
        Timestamp now = Timestamp.now();
        Date nowDate = now.toDate();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("organizerId", organizerId);
        event.put("details", "Administrator browse coverage for " + title);
        event.put("capacity", 24);
        event.put("waitingListLimit", 8);
        event.put("createdAt", now);

        if ("open".equals(displayState)) {
            event.put("scheduledDateTime", new Timestamp(new Date(nowDate.getTime() + TimeUnit.DAYS.toMillis(10))));
            event.put("registrationDeadline", new Timestamp(new Date(nowDate.getTime() + TimeUnit.DAYS.toMillis(5))));
            event.put("drawDate", new Timestamp(new Date(nowDate.getTime() + TimeUnit.DAYS.toMillis(6))));
        } else if ("pending".equals(displayState)) {
            event.put("scheduledDateTime", new Timestamp(new Date(nowDate.getTime() + TimeUnit.DAYS.toMillis(15))));
            event.put("registrationDeadline", new Timestamp(new Date(nowDate.getTime() - TimeUnit.DAYS.toMillis(1))));
            event.put("drawDate", new Timestamp(new Date(nowDate.getTime() + TimeUnit.DAYS.toMillis(1))));
        } else {
            event.put("status", "closed");
            event.put("scheduledDateTime", new Timestamp(new Date(nowDate.getTime() - TimeUnit.DAYS.toMillis(3))));
            event.put("registrationDeadline", new Timestamp(new Date(nowDate.getTime() - TimeUnit.DAYS.toMillis(10))));
            event.put("drawDate", new Timestamp(new Date(nowDate.getTime() - TimeUnit.DAYS.toMillis(5))));
        }

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void deleteEvent(String eventId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private String uniqueId(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private void ensureFirestoreNetworkEnabled()
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.enableNetwork(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void waitForEventsLoaded(ActivityScenario<AdminBrowseEventsActivity> scenario) throws InterruptedException {
        boolean[] found = {false};
        for (int attempt = 0; attempt < 20; attempt++) {
            scenario.onActivity(activity -> {
                int adapterCount = ((androidx.recyclerview.widget.RecyclerView) activity.findViewById(R.id.rvEvents))
                        .getAdapter().getItemCount();
                found[0] = adapterCount >= 3;
            });
            if (found[0]) {
                return;
            }
            Thread.sleep(250);
        }
        assertTrue("Expected seeded events to load into RecyclerView", found[0]);
    }

    // US 03.04.01: Admin should see event browser with section title and event list
    @Test
    public void testAdminBrowseEventsScreenIsDisplayed() {
        onView(withId(R.id.tvAllEventsTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvAllEventsTitle))
                .check(matches(withText(R.string.admin_all_events_title)));

        // RecyclerView starts empty, only check it's VISIBLE or not
        onView(withId(R.id.rvEvents)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    // US 03.01.01: Clicking an event should navigate to event details for removal
    @Test
    public void testOnEventClickLaunchesAdminEventDetailsActivity() {
        activityRule.getScenario().onActivity(activity -> {
            Event event = new Event();
            event.setEventId("admin_click_event_id");
            activity.onEventClick(event);
        });

        intended(hasComponent(AdminEventDetailsActivity.class.getName()));
        intended(hasExtra("eventId", "admin_click_event_id"));
    }

    // US 03.04.01: Admin event browser should load real Firestore events and display
    // distinct open, pending, and closed event titles in the list.
    @Test
    public void adminBrowseEvents_loadsSeededFirestoreEvents() throws Exception {
        String openEventId = uniqueId(OPEN_EVENT_ID);
        String pendingEventId = uniqueId(PENDING_EVENT_ID);
        String closedEventId = uniqueId(CLOSED_EVENT_ID);
        String openEventTitle = "Swimming Course for Beginners Morning Session";
        String pendingEventTitle = "Community Soccer Night Registration Draw";
        String closedEventTitle = "Charity Potluck Volunteer Wrap-Up";

        ensureFirestoreNetworkEnabled();
        seedEvent(openEventId, openEventTitle, "coach_nadia_rahman", "open");
        seedEvent(pendingEventId, pendingEventTitle, "samuel_turner", "pending");
        seedEvent(closedEventId, closedEventTitle, "grace_lee", "closed");

        try {
            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminBrowseEventsActivity.class);
            intent.putExtra("userId", "admin_jordan_clark");

            try (ActivityScenario<AdminBrowseEventsActivity> scenario = ActivityScenario.launch(intent)) {
                waitForEventsLoaded(scenario);

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(hasDescendant(withText(openEventTitle)))
                );
                onView(withId(R.id.rvEvents)).check(matches(hasDescendant(withText(openEventTitle))));

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(hasDescendant(withText(pendingEventTitle)))
                );
                onView(withId(R.id.rvEvents)).check(matches(hasDescendant(withText(pendingEventTitle))));

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(hasDescendant(withText(closedEventTitle)))
                );
                onView(withId(R.id.rvEvents)).check(matches(hasDescendant(withText(closedEventTitle))));

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(allOf(
                                hasDescendant(withText(openEventTitle)),
                                hasDescendant(withText("OPEN"))
                        ))
                );

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(allOf(
                                hasDescendant(withText(pendingEventTitle)),
                                hasDescendant(withText("PENDING"))
                        ))
                );

                onView(withId(R.id.rvEvents)).perform(
                        RecyclerViewActions.scrollTo(allOf(
                                hasDescendant(withText(closedEventTitle)),
                                hasDescendant(withText("CLOSED"))
                        ))
                );

                scenario.onActivity(activity -> {
                    int active = Integer.parseInt(((TextView) activity.findViewById(R.id.tvActiveCount)).getText().toString());
                    int pending = Integer.parseInt(((TextView) activity.findViewById(R.id.tvPendingCount)).getText().toString());
                    int closed = Integer.parseInt(((TextView) activity.findViewById(R.id.tvClosedCount)).getText().toString());
                    int total = Integer.parseInt(((TextView) activity.findViewById(R.id.tvTotalCount)).getText().toString());
                    assertTrue("At least 1 active event expected", active >= 1);
                    assertTrue("At least 1 pending event expected", pending >= 1);
                    assertTrue("At least 1 closed event expected", closed >= 1);
                    assertTrue("Total should be at least 3", total >= 3);
                });
            }
        } finally {
            try {
                deleteEvent(openEventId);
                deleteEvent(pendingEventId);
                deleteEvent(closedEventId);
            } catch (Exception ignored) {
            }
        }
    }
}
