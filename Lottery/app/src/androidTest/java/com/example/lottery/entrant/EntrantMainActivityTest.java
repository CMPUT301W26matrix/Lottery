package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Android instrumentation tests for {@link EntrantMainActivity}.
 * Tests event browsing, filtering, search, and participated-event exclusion.
 *
 * <p>All tests seed real Firestore data and verify list-level results,
 * not just control states.</p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMainActivityTest {

    private static final String TEST_USER_ID = "test_entrant_main_user";
    private static final long LOAD_WAIT_MS = 5000;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();

    // ── Shared seed data IDs ──
    private static final String PUBLIC_EVENT_ID = "test_explore_public_event";
    private static final String PRIVATE_EVENT_ID = "test_explore_private_event";
    private static final String PARTICIPATED_EVENT_ID = "test_explore_participated_event";
    private static final String SEARCH_EVENT_ID = "test_explore_search_event";
    private static final String TODAY_EVENT_ID = "test_explore_today_event";
    private static final String FAR_FUTURE_EVENT_ID = "test_explore_far_future_event";
    private static final String FULL_WAITLIST_EVENT_ID = "test_explore_full_waitlist_event";
    private static final String OPEN_WAITLIST_EVENT_ID = "test_explore_open_waitlist_event";

    @Before
    public void seedTestData() throws Exception {
        Timestamp now = Timestamp.now();
        // Fixed to today 23:59:59 — always within "today" regardless of run time
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        todayCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        todayCal.set(java.util.Calendar.MINUTE, 59);
        todayCal.set(java.util.Calendar.SECOND, 59);
        todayCal.set(java.util.Calendar.MILLISECOND, 0);
        Timestamp todayLater = new Timestamp(todayCal.getTime());
        // +30 days: far future, outside "This Week"
        Timestamp farFuture = new Timestamp(
                new java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
        // +7 days: default future for general events
        Timestamp nextWeek = new Timestamp(
                new java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

        // 1. Public open event (scheduled next week) — should appear in Explore
        seedEvent(PUBLIC_EVENT_ID, "Community Swimming Lessons", "Sports",
                nextWeek, nextWeek, false, 100);

        // 2. Private event — should NOT appear in Explore
        seedEvent(PRIVATE_EVENT_ID, "Hidden Private Gathering", "Social",
                nextWeek, nextWeek, true, 100);

        // 3. Already participated event — should NOT appear in Explore
        seedEvent(PARTICIPATED_EVENT_ID, "Already Joined Yoga Class", "Sports",
                nextWeek, nextWeek, false, 100);
        seedEntrantStatus(PARTICIPATED_EVENT_ID, TEST_USER_ID,
                InvitationFlowUtil.STATUS_WAITLISTED);

        // 4. Search test event
        seedEvent(SEARCH_EVENT_ID, "Advanced Dance Workshop", "Music",
                nextWeek, nextWeek, false, 100);

        // 5. Today event — for time filter testing
        seedEvent(TODAY_EVENT_ID, "Today's Morning Yoga", "Sports",
                todayLater, nextWeek, false, 100);

        // 6. Far future event (30 days out) — should be excluded by Today/This Week filters
        seedEvent(FAR_FUTURE_EVENT_ID, "Far Future Gala Night", "Social",
                farFuture, farFuture, false, 100);

        // 7. Full waitlist event (limit=2, 2 waitlisted) — Spots Available should exclude
        seedEvent(FULL_WAITLIST_EVENT_ID, "Full Waitlist Pottery", "Other",
                nextWeek, nextWeek, false, 2);
        seedWaitlistCount(FULL_WAITLIST_EVENT_ID, 2);

        // 8. Open waitlist event (limit=10, 1 waitlisted) — Spots Available should keep
        seedEvent(OPEN_WAITLIST_EVENT_ID, "Open Waitlist Painting", "Other",
                nextWeek, nextWeek, false, 10);
        seedWaitlistCount(OPEN_WAITLIST_EVENT_ID, 1);
    }

    @After
    public void cleanUpTestData() throws Exception {
        for (String eventId : seededEventIds) {
            for (QueryDocumentSnapshot snapshot : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(snapshot.getReference().delete(), 10, TimeUnit.SECONDS);
            }
            Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10, TimeUnit.SECONDS);
        }
        seededEventIds.clear();
    }

    // ── Seed helpers ──

    private void seedEvent(String eventId, String title, String category,
                           Timestamp scheduledDateTime, Timestamp registrationDeadline,
                           boolean isPrivate, int waitingListLimit) throws Exception {
        seededEventIds.add(eventId);
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("details", "Community event: " + title);
        event.put("status", "open");
        event.put("private", isPrivate);
        event.put("category", category);
        event.put("capacity", 50);
        event.put("waitingListLimit", waitingListLimit);
        event.put("scheduledDateTime", scheduledDateTime);
        event.put("registrationDeadline", registrationDeadline);
        event.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private void seedEntrantStatus(String eventId, String userId, String status) throws Exception {
        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", "Riley Brooks");
        record.put("email", "entrant@gmail.com");
        record.put("status", status);
        record.put("registeredAt", now);
        record.put("waitlistedAt", now);
        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId))
                        .document(userId).set(record),
                10, TimeUnit.SECONDS);
    }

    private void seedWaitlistCount(String eventId, int count) throws Exception {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        for (int i = 0; i < count; i++) {
            String fakeUserId = "waitlisted_user_" + eventId + "_" + i;
            Map<String, Object> record = new HashMap<>();
            record.put("userId", fakeUserId);
            record.put("userName", "Waitlisted " + i);
            record.put("email", "waitlisted" + i + "@example.com");
            record.put("status", InvitationFlowUtil.STATUS_WAITLISTED);
            record.put("registeredAt", now);
            record.put("waitlistedAt", now);
            batch.set(
                    db.collection(FirestorePaths.eventWaitingList(eventId))
                            .document(fakeUserId),
                    record);
        }
        Tasks.await(batch.commit(), 10, TimeUnit.SECONDS);
    }

    private ActivityScenario<EntrantMainActivity> launchActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantMainActivity.class);
        intent.putExtra("userId", TEST_USER_ID);
        return ActivityScenario.launch(intent);
    }

    private boolean recyclerViewContainsTitle(RecyclerView rv, String title) {
        RecyclerView.Adapter<?> adapter = rv.getAdapter();
        if (adapter == null) return false;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                android.widget.TextView tv = holder.itemView.findViewById(R.id.tvEventTitle);
                if (tv != null && title.equals(tv.getText().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    // US 01.01.03 — See a list of events to join the waiting list
    // ══════════════════════════════════════════════════════════════

    // US 01.01.03: Public open event appears in Explore list.
    @Test
    public void browseEvents_showsPublicOpenEvent() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertTrue("Public open event should appear in Explore",
                        recyclerViewContainsTitle(rv, "Community Swimming Lessons"));
            });
        }
    }

    // US 01.01.03: Participated event does not appear in Explore.
    @Test
    public void browseEvents_excludesParticipatedEvent() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertFalse("Participated event should NOT appear in Explore",
                        recyclerViewContainsTitle(rv, "Already Joined Yoga Class"));
            });
        }
    }

    // US 01.01.03: Declined event does not appear in Explore.
    @Test
    public void browseEvents_excludesDeclinedEvent() throws Exception {
        String declinedEventId = "test_explore_declined_event";
        seedEvent(declinedEventId, "Declined Cooking Class", "Social",
                new Timestamp(new java.util.Date(
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))),
                new Timestamp(new java.util.Date(
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))),
                false, 100);
        seedEntrantStatus(declinedEventId, TEST_USER_ID,
                InvitationFlowUtil.STATUS_CANCELLED);

        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertFalse("Declined event should NOT appear in Explore",
                        recyclerViewContainsTitle(rv, "Declined Cooking Class"));
            });
        }
    }

    // US 01.01.03: Not-selected event does not appear in Explore.
    @Test
    public void browseEvents_excludesNotSelectedEvent() throws Exception {
        String notSelectedEventId = "test_explore_not_selected_event";
        seedEvent(notSelectedEventId, "Not Selected Art Show", "Other",
                new Timestamp(new java.util.Date(
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))),
                new Timestamp(new java.util.Date(
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))),
                false, 100);
        seedEntrantStatus(notSelectedEventId, TEST_USER_ID,
                InvitationFlowUtil.STATUS_NOT_SELECTED);

        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertFalse("Not-selected event should NOT appear in Explore",
                        recyclerViewContainsTitle(rv, "Not Selected Art Show"));
            });
        }
    }

    // ══════════════════════════════════════════════════════════════
    // US 01.01.04 — Filter by availability and event capacity
    // ══════════════════════════════════════════════════════════════

    // US 01.01.04: "Today" time filter excludes events scheduled beyond today.
    @Test
    public void filterByAvailability_todayFilterExcludesFarFutureEvent()
            throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            // Apply "Today" filter
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("Today")).perform(click());
            Thread.sleep(1000);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                // Today's event should still appear
                assertTrue("Today's event should appear under Today filter",
                        recyclerViewContainsTitle(rv, "Today's Morning Yoga"));
                // Far future event (30 days out) should be excluded
                assertFalse("Far future event should NOT appear under Today filter",
                        recyclerViewContainsTitle(rv, "Far Future Gala Night"));
                // Next-week event should also be excluded by Today filter
                assertFalse("Next-week event should NOT appear under Today filter",
                        recyclerViewContainsTitle(rv, "Community Swimming Lessons"));
            });
        }
    }

    // US 01.01.04: "This Week" time filter excludes events beyond 7 days.
    @Test
    public void filterByAvailability_thisWeekFilterExcludesFarFutureEvent()
            throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            // Apply "This Week" filter
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("This Week")).perform(click());
            Thread.sleep(1000);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                // Today's event is within this week
                assertTrue("Today's event should appear under This Week filter",
                        recyclerViewContainsTitle(rv, "Today's Morning Yoga"));
                // 30-day-out event should be excluded
                assertFalse("Far future event should NOT appear under This Week filter",
                        recyclerViewContainsTitle(rv, "Far Future Gala Night"));
            });
        }
    }

    // US 01.01.04: Spots Available filter excludes events whose waitlist is full.
    @Test
    public void filterByCapacity_spotsAvailableExcludesFullWaitlist()
            throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            // Enable Spots Available filter
            onView(withId(R.id.chipSpotsAvailable)).perform(click());
            Thread.sleep(3000); // Wait for waitlist count queries

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                // Full waitlist (limit=2, 2 waitlisted) should be excluded
                assertFalse("Full-waitlist event should NOT appear with Spots Available",
                        recyclerViewContainsTitle(rv, "Full Waitlist Pottery"));
                // Open waitlist (limit=10, 1 waitlisted) should still appear
                assertTrue("Open-waitlist event should appear with Spots Available",
                        recyclerViewContainsTitle(rv, "Open Waitlist Painting"));
            });
        }
    }

    // US 01.01.04: Combined time + capacity filter narrows results correctly.
    @Test
    public void filterCombined_timeAndCapacityFilterResults() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            // Apply "Today" filter — only today's event should remain
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("Today")).perform(click());
            Thread.sleep(1000);

            // Also enable Spots Available
            onView(withId(R.id.chipSpotsAvailable)).perform(click());
            Thread.sleep(3000);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                // Today's yoga event survives both filters (today + waitlist has room)
                assertTrue("Today's event should survive combined time + capacity filter",
                        recyclerViewContainsTitle(rv, "Today's Morning Yoga"));
                // Far future and next-week events excluded by time
                assertFalse("Far future event excluded by time filter",
                        recyclerViewContainsTitle(rv, "Far Future Gala Night"));
                assertFalse("Next-week event excluded by time filter",
                        recyclerViewContainsTitle(rv, "Community Swimming Lessons"));
                // Full waitlist event excluded by capacity (and also by time)
                assertFalse("Full waitlist event excluded",
                        recyclerViewContainsTitle(rv, "Full Waitlist Pottery"));
            });
        }
    }

    // ══════════════════════════════════════════════════════════════
    // US 01.01.05 — Search by keyword
    // ══════════════════════════════════════════════════════════════

    // US 01.01.05: Keyword search shows matching events and hides non-matching.
    @Test
    public void searchEvents_filtersByKeyword() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            onView(withId(R.id.llSearchToggle)).perform(click());
            onView(withId(R.id.tilSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.etSearch))
                    .perform(replaceText("dance"), closeSoftKeyboard());
            Thread.sleep(1000);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertTrue("Dance event should appear in search results",
                        recyclerViewContainsTitle(rv, "Advanced Dance Workshop"));
                assertFalse("Non-matching event should be filtered out by search",
                        recyclerViewContainsTitle(rv, "Community Swimming Lessons"));
            });

            onView(withId(R.id.llSearchToggle)).perform(click());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // US 01.01.06 — Keyword search combined with filtering
    // ══════════════════════════════════════════════════════════════

    // US 01.01.06: Search + time filter combined narrows results.
    @Test
    public void searchWithFilters_combinedResultsAreCorrect() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);

            // Search for "yoga" — matches "Today's Morning Yoga" (today)
            // and "Already Joined Yoga Class" (but excluded by participation filter)
            onView(withId(R.id.llSearchToggle)).perform(click());
            onView(withId(R.id.etSearch))
                    .perform(replaceText("yoga"), closeSoftKeyboard());
            Thread.sleep(1000);

            // Apply "Today" time filter
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("Today")).perform(click());
            Thread.sleep(1000);

            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                // Today's yoga event matches both search and time filter
                assertTrue("Today's yoga event should appear with search + today filter",
                        recyclerViewContainsTitle(rv, "Today's Morning Yoga"));
                // Dance event doesn't match search keyword "yoga"
                assertFalse("Dance event should NOT appear for yoga search",
                        recyclerViewContainsTitle(rv, "Advanced Dance Workshop"));
                // Swimming event doesn't match search keyword "yoga"
                assertFalse("Swimming event should NOT appear for yoga search",
                        recyclerViewContainsTitle(rv, "Community Swimming Lessons"));
            });

            onView(withId(R.id.llSearchToggle)).perform(click());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // US 02.01.02 — Private events not visible on event listing
    // ══════════════════════════════════════════════════════════════

    // US 02.01.02: Private event does not appear in the entrant Explore listing.
    @Test
    public void privateEvent_notDisplayedInEntrantListing() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            Thread.sleep(LOAD_WAIT_MS);
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                assertFalse("Private event should NOT appear in Explore",
                        recyclerViewContainsTitle(rv, "Hidden Private Gathering"));
            });
        }
    }
}
