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
 * MainActivity serves as the Organizer Dashboard.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "MainActivity";

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents, tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        
        // Bind UI Components
        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        
        // Setup RecyclerView
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadOrganizerEvents();
    }

    private void setupNavigation() {
        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, CreateEventActivity.class));
            });
        }
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    /**
     * Loads events and handles compatibility with older database schemas.
     */
    private void loadOrganizerEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    int active = 0;
                    int closed = 0;
                    Date now = new Date();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            
                            // Compatibility fix: If new field is null, check if old field names exist
                            if (event.getScheduledDateTime() == null) {
                                Date oldDate = document.getDate("eventDate");
                                if (oldDate != null) event.setScheduledDateTime(oldDate);
                            }
                            if (event.getRegistrationDeadline() == null) {
                                Date oldDeadline = document.getDate("deadlineDate");
                                if (oldDeadline != null) event.setRegistrationDeadline(oldDeadline);
                            }

                            eventList.add(event);

                            if (event.getScheduledDateTime() != null && event.getScheduledDateTime().after(now)) {
                                active++;
                            } else {
                                closed++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document " + document.getId(), e);
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateSummaryStats(active, closed, 0, eventList.size());
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSummaryStats(int active, int closed, int pending, int total) {
        tvActiveCount.setText(String.valueOf(active));
        tvClosedCount.setText(String.valueOf(closed));
        tvPendingCount.setText(String.valueOf(pending));
        tvTotalCount.setText(String.valueOf(total));
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
