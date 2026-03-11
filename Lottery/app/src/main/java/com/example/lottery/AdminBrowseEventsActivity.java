package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AdminBrowseEventsActivity serves as the administrator event browser, displaying a summary of all events.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Displays a list of all published events in the system.</li>
 *   <li>Provides a summary of event statuses (Active, Closed, Pending, etc.).</li>
 *   <li>Handles navigation to admin-only event detail screens.</li>
 *   <li>Fetches event data from Firestore on creation and resume.</li>
 * </ul>
 * </p>
 */
public class AdminBrowseEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "AdminBrowseEvents";
    /**
     * RecyclerView for displaying the list of events.
     */
    private RecyclerView rvEvents;
    /**
     * Adapter for binding event data to the RecyclerView.
     */
    private EventAdapter adapter;
    /**
     * List to hold the event objects fetched from Firestore.
     */
    private List<Event> eventList;
    /**
     * TextView displayed when no events are found.
     */
    private TextView tvNoEvents;
    /**
     * TextViews for displaying summary statistics of event statuses.
     */
    private TextView tvActiveCount;
    private TextView tvClosedCount;
    private TextView tvPendingCount;
    private TextView tvTotalCount;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        db = FirebaseFirestore.getInstance();

        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Sets up click listeners for the admin navigation elements.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_browse_events_active_tab, Toast.LENGTH_SHORT).show());
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_profiles_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_images_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_logs_coming_soon, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Loads events from the Firestore 'events' collection.
     *
     * <p>This method clears the existing list, fetches all documents, and repopulates the list.
     * It includes a compatibility fix to handle older documents that might use different field names for dates.
     * After fetching, it updates the RecyclerView and the summary statistics UI.</p>
     */
    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    int active = 0;
                    int closed = 0;
                    int pending = 0;
                    Date now = new Date();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);

                            if (event.getScheduledDateTime() == null) {
                                Date oldDate = document.getDate("eventDate");
                                if (oldDate != null) {
                                    event.setScheduledDateTime(oldDate);
                                }
                            }
                            if (event.getRegistrationDeadline() == null) {
                                Date oldDeadline = document.getDate("deadlineDate");
                                if (oldDeadline != null) {
                                    event.setRegistrationDeadline(oldDeadline);
                                }
                            }

                            eventList.add(event);

                            if (event.getDrawDate() != null
                                    && event.getRegistrationDeadline() != null
                                    && event.getRegistrationDeadline().before(now)
                                    && event.getDrawDate().after(now)) {
                                pending++;
                            } else if (event.getScheduledDateTime() != null
                                    && event.getScheduledDateTime().after(now)) {
                                active++;
                            } else {
                                closed++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document " + document.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateSummaryStats(active, closed, pending, eventList.size());
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, R.string.failed_to_load_events, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the summary statistic TextViews with the provided counts.
     *
     * @param active  The number of active events.
     * @param closed  The number of closed events.
     * @param pending The number of pending events.
     * @param total   The total number of events.
     */
    private void updateSummaryStats(int active, int closed, int pending, int total) {
        tvActiveCount.setText(String.valueOf(active));
        tvClosedCount.setText(String.valueOf(closed));
        tvPendingCount.setText(String.valueOf(pending));
        tvTotalCount.setText(String.valueOf(total));
    }

    @Override
    /**
     * Handles clicks on individual event items in the RecyclerView.
     *
     * @param event The Event object that was clicked.
     */
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
