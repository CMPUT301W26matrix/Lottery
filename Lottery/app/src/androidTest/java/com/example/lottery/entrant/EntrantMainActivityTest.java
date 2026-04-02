package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Android instrumentation tests for {@link EntrantMainActivity}.
 * Tests user flows for browsing and filtering events.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMainActivityTest {

    private static final String TEST_PRIVATE_EVENT_ID = "test_private_event_visibility";
    private FirebaseFirestore db;

    /**
     * Seed a private event to verify it does NOT appear in entrant listings.
     */
    @Before
    public void seedTestData() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", TEST_PRIVATE_EVENT_ID);
        event.put("title", "Hidden Private Event");
        event.put("private", true);
        event.put("status", "open");
        event.put("capacity", 10);
        event.put("createdAt", Timestamp.now());
        db.collection(FirestorePaths.EVENTS).document(TEST_PRIVATE_EVENT_ID).set(event)
                .addOnCompleteListener(t -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
    }

    @After
    public void cleanUpTestData() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        db.collection(FirestorePaths.EVENTS).document(TEST_PRIVATE_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);
    }

    private ActivityScenario<EntrantMainActivity> launchActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantMainActivity.class
        );
        intent.putExtra("userId", "test_user_id");
        return ActivityScenario.launch(intent);
    }

    // US 01.01.03: As an entrant, I want to be able to see a list of events that I can join the waiting list for.
    @Test
    public void browseEvents_showsEventListAndBrowseTabs() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            onView(ViewMatchers.withId(R.id.rvEvents))
                    .check(matches(isAssignableFrom(RecyclerView.class)));
            onView(withId(R.id.chipBrowseAll)).check(matches(isDisplayed()));
            onView(withId(R.id.chipBrowseNew)).check(matches(isDisplayed()));
            onView(withId(R.id.chipBrowseRecommended)).check(matches(isDisplayed()));
            onView(withId(R.id.cgCategories)).check(matches(isDisplayed()));
        }
    }

    // US 01.01.04: As an entrant, I want to filter events based on my availability and event capacity.
    @Test
    public void filterByAvailability_selectTimeRangeAndApply() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Default should show "All Dates"
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText(R.string.filter_all_dates)));

            // Select "Today" — narrow to events happening today
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("Today")).perform(click());
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("Today")));

            // Change to "This Week" — broaden to this week's events
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("This Week")).perform(click());
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("This Week")));

            // Reset back to "All Dates"
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("All Dates")).perform(click());
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("All Dates")));
        }
    }

    // US 01.01.04: As an entrant, I want to filter events based on my availability and event capacity.
    @Test
    public void filterByCapacity_toggleSpotsAvailableChip() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Initially unchecked
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isNotChecked()));

            // Toggle on — filter to events with remaining capacity and open registration
            onView(withId(R.id.chipSpotsAvailable)).perform(click());
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isChecked()));

            // Toggle off — show all events again
            onView(withId(R.id.chipSpotsAvailable)).perform(click());
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isNotChecked()));
        }
    }

    // US 01.01.04: As an entrant, I want to filter events based on my availability and event capacity.
    @Test
    public void filterCombined_applyTimeAndCapacityFiltersTogether() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Apply availability filter (time)
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("This Week")).perform(click());
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("This Week")));

            // Apply capacity filter (spots available)
            onView(withId(R.id.chipSpotsAvailable)).perform(click());
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isChecked()));

            // Both filters should remain active simultaneously
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("This Week")));
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isChecked()));
        }
    }

    // US 01.01.05: As an entrant, I want to search for events by keyword to find events based on my interests.
    @Test
    public void searchEvents_openSearchAndTypeKeyword() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Tap the search area to open the search field
            onView(withId(R.id.llSearchToggle)).perform(click());
            onView(withId(R.id.tilSearch)).check(matches(isDisplayed()));

            // Type a keyword to search
            onView(withId(R.id.etSearch))
                    .perform(replaceText("dance"), closeSoftKeyboard());

            // The event list should still be visible (filtered by search)
            onView(withId(R.id.rvEvents)).check(matches(isAssignableFrom(RecyclerView.class)));

            // Close search — tap again to dismiss and clear
            onView(withId(R.id.llSearchToggle)).perform(click());
        }
    }

    // US 01.01.06: As an entrant, I want to use keyword search with filtering to narrow my event search.
    @Test
    public void searchWithFilters_combineSearchAndAllFilters() {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Open search and type keyword
            onView(withId(R.id.llSearchToggle)).perform(click());
            onView(withId(R.id.etSearch))
                    .perform(replaceText("swim"), closeSoftKeyboard());

            // Apply availability filter (time) on top of search
            onView(withId(R.id.btnTimeFilter)).perform(click());
            onView(withText("This Week")).perform(click());

            // Apply capacity filter (spots available) on top of search + time
            onView(withId(R.id.chipSpotsAvailable)).perform(click());

            // All filters and search should be active together
            onView(withId(R.id.btnTimeFilter))
                    .check(matches(withText("This Week")));
            onView(withId(R.id.chipSpotsAvailable)).check(matches(isChecked()));
            onView(withId(R.id.tilSearch)).check(matches(isDisplayed()));
        }
    }

    // US 02.01.02: Private events should not be displayed in the entrant event listing.
    @Test
    public void privateEvent_notDisplayedInEntrantListing() throws InterruptedException {
        try (ActivityScenario<EntrantMainActivity> scenario = launchActivity()) {
            // Wait for Firestore to load events
            Thread.sleep(3000);

            // Verify the private event title does NOT appear anywhere in the list
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rvEvents);
                RecyclerView.Adapter<?> adapter = rv.getAdapter();
                if (adapter == null) return;

                // Check that the seeded private event is not in the adapter's data
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    android.view.View itemView = rv.findViewHolderForAdapterPosition(i) != null
                            ? rv.findViewHolderForAdapterPosition(i).itemView : null;
                    if (itemView != null) {
                        android.widget.TextView tvTitle = itemView.findViewById(R.id.tvEventTitle);
                        if (tvTitle != null) {
                            org.junit.Assert.assertNotEquals(
                                    "Private event should not appear in entrant listing",
                                    "Hidden Private Event",
                                    tvTitle.getText().toString()
                            );
                        }
                    }
                }
            });
        }
    }
}
