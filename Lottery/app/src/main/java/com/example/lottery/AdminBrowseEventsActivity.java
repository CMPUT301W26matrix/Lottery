package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_browse_events);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Get userId from intent or shared preferences
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

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

        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.EVENTS, userId);
        // Override Events tab to scroll to top instead of no-op
        findViewById(R.id.nav_home).setOnClickListener(v -> rvEvents.smoothScrollToPosition(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Loads events from the Firestore 'events' collection.
     *
     * <p>This method clears the existing list, fetches all documents, and repopulates the list.
     * After fetching, it updates the RecyclerView and the summary statistics UI.</p>
     */
    private void loadEvents() {
        adapter.clearCountsCache();
        db.collection(FirestorePaths.EVENTS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    int active = 0;
                    int closed = 0;
                    int pending = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            if (event == null) continue;

                            event.setEventId(document.getId());
                            eventList.add(event);

                            String displayStatus = EventAdapter.resolveDisplayStatus(event);
                            switch (displayStatus) {
                                case "open":
                                    active++;
                                    break;
                                case "pending":
                                    pending++;
                                    break;
                                default:
                                    closed++;
                                    break;
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


    /**
     * Handles clicks on individual event items in the RecyclerView.
     *
     * @param event The Event object that was clicked.
     */
    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
