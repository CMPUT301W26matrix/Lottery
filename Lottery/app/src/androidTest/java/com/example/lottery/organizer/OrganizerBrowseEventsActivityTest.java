package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OrganizerBrowseEventsActivityTest {

    private static final String TEST_USER_ID = "test_organizer_id";
    private static final String OTHER_USER_ID = "other_organizer_id";
    private static final String OPEN_EVENT_ID = "browse_open_" + Long.toHexString(System.currentTimeMillis());
    private static final String PENDING_EVENT_ID = OPEN_EVENT_ID + "_pending";
    private static final String CLOSED_EVENT_ID = OPEN_EVENT_ID + "_closed";
    private static final String FOREIGN_EVENT_ID = OPEN_EVENT_ID + "_foreign";

    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        Intents.init();
        db = FirebaseFirestore.getInstance();
        deleteEventsForOrganizer(TEST_USER_ID);
        deleteEventsForOrganizer(OTHER_USER_ID);

        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DAY_OF_YEAR, 10);
        Date openEventStart = calendar.getTime();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date pendingRegDeadline = calendar.getTime();
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        Date pendingDrawDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        Date pendingEventStart = calendar.getTime();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -10);
        Date closedEventStart = calendar.getTime();

        seedEvent(OPEN_EVENT_ID, TEST_USER_ID, "Owned Open Event", "open",
                openEventStart, null, null);
        seedEvent(PENDING_EVENT_ID, TEST_USER_ID, "Owned Pending Event", "open",
                pendingEventStart, pendingRegDeadline, pendingDrawDate);
        seedEvent(CLOSED_EVENT_ID, TEST_USER_ID, "Owned Closed Event", "closed",
                closedEventStart, null, null);
        seedEvent(FOREIGN_EVENT_ID, OTHER_USER_ID, "Foreign Organizer Event", "open",
                openEventStart, null, null);
    }

    @After
    public void tearDown() throws Exception {
        Intents.release();
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(OPEN_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(PENDING_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(CLOSED_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(FOREIGN_EVENT_ID).delete(),
                10, TimeUnit.SECONDS);
    }

    private void deleteEventsForOrganizer(String organizerId) throws Exception {
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : Tasks.await(
                db.collection(FirestorePaths.EVENTS)
                        .whereEqualTo("organizerId", organizerId)
                        .get(),
                10,
                TimeUnit.SECONDS
        )) {
            Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
        }
    }

    private void seedEvent(String eventId,
                           String organizerId,
                           String title,
                           String status,
                           Date scheduledDateTime,
                           Date registrationDeadline,
                           Date drawDate) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("organizerId", organizerId);
        event.put("status", status);
        event.put("private", false);
        event.put("capacity", 20);
        event.put("createdAt", Timestamp.now());
        if (scheduledDateTime != null) {
            event.put("scheduledDateTime", new Timestamp(scheduledDateTime));
        }
        if (registrationDeadline != null) {
            event.put("registrationDeadline", new Timestamp(registrationDeadline));
        }
        if (drawDate != null) {
            event.put("drawDate", new Timestamp(drawDate));
        }

        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS);
    }

    private Intent createLaunchIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(), OrganizerBrowseEventsActivity.class)
                .putExtra("userId", TEST_USER_ID);
    }

    private void waitForEventListAndSummary(ActivityScenario<OrganizerBrowseEventsActivity> scenario) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            boolean[] ready = new boolean[1];
            scenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvEvents);
                String active = ((android.widget.TextView) activity.findViewById(R.id.tvActiveCount))
                        .getText().toString();
                String closed = ((android.widget.TextView) activity.findViewById(R.id.tvClosedCount))
                        .getText().toString();
                String pending = ((android.widget.TextView) activity.findViewById(R.id.tvPendingCount))
                        .getText().toString();
                String total = ((android.widget.TextView) activity.findViewById(R.id.tvTotalCount))
                        .getText().toString();

                ready[0] = recyclerView.getAdapter() != null
                        && recyclerView.getAdapter().getItemCount() == 3
                        && "1".equals(active)
                        && "1".equals(closed)
                        && "1".equals(pending)
                        && "3".equals(total);
            });

            if (ready[0]) {
                return;
            }

            lastError = new AssertionError("Timed out waiting for organizer browse events data");
            Thread.sleep(250);
        }
        throw lastError;
    }

    /**
     * US 02.01.04: The organizer event browser loads the organizer's existing
     * events and derived summary counts so those events can be opened and managed.
     */
    @Test
    public void testOrganizerBrowseEventsLoadsOwnedEventsAndSummaryCounts() throws Exception {
        try (ActivityScenario<OrganizerBrowseEventsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            waitForEventListAndSummary(scenario);

            scenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvEvents);
                assertEquals(3, recyclerView.getAdapter().getItemCount());

                // Verify owned events are present and foreign event is absent
                // Use adapter data instead of view holders to avoid NPE on small screens
                boolean foundOpen = false, foundPending = false, foundClosed = false, foundForeign = false;
                for (int i = 0; i < recyclerView.getAdapter().getItemCount(); i++) {
                    recyclerView.scrollToPosition(i);
                    // Force layout so the ViewHolder is bound
                    recyclerView.measure(
                            View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(recyclerView.getHeight(), View.MeasureSpec.EXACTLY));
                    recyclerView.layout(recyclerView.getLeft(), recyclerView.getTop(),
                            recyclerView.getRight(), recyclerView.getBottom());
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(i);
                    if (vh == null) continue;
                    String title = ((android.widget.TextView) vh.itemView.findViewById(R.id.tvEventTitle))
                            .getText().toString();
                    if ("Owned Open Event".equals(title)) foundOpen = true;
                    if ("Owned Pending Event".equals(title)) foundPending = true;
                    if ("Owned Closed Event".equals(title)) foundClosed = true;
                    if ("Foreign Organizer Event".equals(title)) foundForeign = true;
                }
                assertTrue("Owned Open Event should be present", foundOpen);
                assertTrue("Owned Pending Event should be present", foundPending);
                assertTrue("Owned Closed Event should be present", foundClosed);
                assertFalse("Foreign Organizer Event should not be present", foundForeign);

                assertEquals("1", ((android.widget.TextView) activity.findViewById(R.id.tvActiveCount))
                        .getText().toString());
                assertEquals("1", ((android.widget.TextView) activity.findViewById(R.id.tvClosedCount))
                        .getText().toString());
                assertEquals("1", ((android.widget.TextView) activity.findViewById(R.id.tvPendingCount))
                        .getText().toString());
                assertEquals("3", ((android.widget.TextView) activity.findViewById(R.id.tvTotalCount))
                        .getText().toString());
            });
        }
    }

    /**
     * US 02.01.04: Selecting an event from the organizer browser opens the
     * organizer event details screen for that specific existing event.
     */
    @Test
    public void testSelectingOrganizerEventLaunchesDetailsActivity() throws Exception {
        try (ActivityScenario<OrganizerBrowseEventsActivity> scenario = ActivityScenario.launch(createLaunchIntent())) {
            waitForEventListAndSummary(scenario);

            onView(withId(R.id.rvEvents)).perform(
                    RecyclerViewActions.actionOnItem(
                            hasDescendant(withText("Owned Open Event")),
                            click()
                    )
            );

            intended(allOf(
                    hasComponent(OrganizerEventDetailsActivity.class.getName()),
                    hasExtra("eventId", OPEN_EVENT_ID),
                    hasExtra("userId", TEST_USER_ID)
            ));
        }
    }
}
