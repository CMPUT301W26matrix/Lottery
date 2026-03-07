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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MainActivity serves as the Organizer Dashboard (US 02.01.01).
 * Displays summary statistics and a list of managed events.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "MainActivity";

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents, tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
    private FirebaseFirestore db;

    /**
     * Simulation of the user's role.
     * Set to true to simulate an Organizer (US 02.01.01 access).
     */
    private boolean isOrganizer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Bind UI Components from activity_main.xml
        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        
        // Setup RecyclerView with Adapter
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        // Initialize Custom Bottom Navigation (IDs from layout_bottom_nav.xml)
        setupNavigation();

        // Initial data load
        loadOrganizerEvents();
    }

    /**
     * Sets up click listeners for the custom bottom navigation bar.
     */
    private void setupNavigation() {
        // Find the creation button inside the included layout
        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, CreateEventActivity.class));
            });
        }

        // Setup other navigation buttons with simple Toasts for now
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show());
        }

        View btnHistory = findViewById(R.id.nav_calendar);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> Toast.makeText(this, "History Coming Soon", Toast.LENGTH_SHORT).show());
        }

        View btnFavorites = findViewById(R.id.nav_notifications);
        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> Toast.makeText(this, "Favorites Coming Soon", Toast.LENGTH_SHORT).show());
        }

        View btnProfile = findViewById(R.id.nav_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> Toast.makeText(this, "Profile Coming Soon", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the dashboard
        loadOrganizerEvents();
    }

    /**
     * US 02.01.01: Loads events from Firestore and updates the dashboard.
     * Includes simple logic to calculate Active vs Closed stats for the prototype.
     */
    private void loadOrganizerEvents() {
        db.collection("events")
                .orderBy("scheduledDateTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    int active = 0;
                    int closed = 0;
                    Date now = new Date();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        eventList.add(event);

                        // Simple Prototype Stat Calculation
                        if (event.getScheduledDateTime() != null && event.getScheduledDateTime().after(now)) {
                            active++;
                        } else {
                            closed++;
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateSummaryStats(active, closed, 0, eventList.size());
                    
                    // Toggle empty state visibility
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the count values on the top summary cards.
     */
    private void updateSummaryStats(int active, int closed, int pending, int total) {
        tvActiveCount.setText(String.valueOf(active));
        tvClosedCount.setText(String.valueOf(closed));
        tvPendingCount.setText(String.valueOf(pending));
        tvTotalCount.setText(String.valueOf(total));
    }

    /**
     * Navigates to the details page when an event card is clicked.
     */
    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
