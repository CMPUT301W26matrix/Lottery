package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumented tests for {@link EntrantMapActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMapActivityTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            for (QueryDocumentSnapshot snapshot : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10,
                    TimeUnit.SECONDS
            )) {
                Tasks.await(snapshot.getReference().delete(), 10, TimeUnit.SECONDS);
            }

            Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }
        seededEventIds.clear();
    }

    private Intent createIntent(String eventId) {
        return new Intent(ApplicationProvider.getApplicationContext(), EntrantMapActivity.class)
                .putExtra("eventId", eventId);
    }

    private void seedEvent(String eventId, boolean requireLocation) throws Exception {
        seededEventIds.add(eventId);

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Beginner Swimming Lessons");
        event.put("organizerId", "community_rec_swim");
        event.put("capacity", 10L);
        event.put("requireLocation", requireLocation);
        event.put("createdAt", Timestamp.now());

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedEntrantLocation(String eventId, String userId, double latitude, double longitude)
            throws Exception {
        Map<String, Object> record = new HashMap<>();
        Timestamp now = Timestamp.now();
        record.put("userId", userId);
        record.put("userName", "Jordan Kim");
        record.put("email", userId + "@example.com");
        record.put("status", InvitationFlowUtil.STATUS_WAITLISTED);
        record.put("registeredAt", now);
        record.put("waitlistedAt", now);
        record.put("location", new GeoPoint(latitude, longitude));

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void waitForCondition(
            ActivityScenario<EntrantMapActivity> scenario,
            java.util.function.Predicate<EntrantMapActivity> condition
    ) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 24; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            scenario.onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) {
                return;
            }
            lastError = new AssertionError("Timed out waiting for entrant map condition");
            Thread.sleep(250);
        }
        throw lastError;
    }

    /**
     * US 02.02.02: When an event requires location and waitlist entrants have saved
     * coordinates, the organizer map screen loads the event data and shows the map.
     */
    @Test
    public void testMapLoadsSavedEntrantLocations() throws Exception {
        String eventId = "map_event_" + UUID.randomUUID();
        seedEvent(eventId, true);
        seedEntrantLocation(eventId, "map_user_1", 53.5461, -113.4938);
        seedEntrantLocation(eventId, "map_user_2", 51.0447, -114.0719);

        try (ActivityScenario<EntrantMapActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId))) {
            waitForCondition(scenario, activity -> {
                View mapView = activity.findViewById(R.id.mapView);
                View mapState = activity.findViewById(R.id.tvMapState);
                return mapView.getVisibility() == View.VISIBLE && mapState.getVisibility() == View.GONE;
            });

            onView(withId(R.id.mapView)).check(matches(isDisplayed()));
            onView(withId(R.id.tvMapState)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 02.02.02: If the event does not require location, the map screen shows
     * the organizer-facing empty state instead of attempting to render entrant markers.
     */
    @Test
    public void testMapShowsLocationNotRequiredState() throws Exception {
        String eventId = "map_disabled_" + UUID.randomUUID();
        seedEvent(eventId, false);

        try (ActivityScenario<EntrantMapActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId))) {
            waitForCondition(scenario, activity -> {
                View mapState = activity.findViewById(R.id.tvMapState);
                return mapState.getVisibility() == View.VISIBLE
                        && activity.getString(R.string.location_not_required_for_event)
                        .contentEquals(((android.widget.TextView) mapState).getText());
            });

            onView(withId(R.id.tvMapState))
                    .check(matches(withText(R.string.location_not_required_for_event)));
            onView(withId(R.id.mapView)).check(matches(withEffectiveVisibility(GONE)));
        }
    }
}
