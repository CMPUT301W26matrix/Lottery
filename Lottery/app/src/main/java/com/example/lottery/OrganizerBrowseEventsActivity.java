package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerBrowseEventsActivity serves as the organizer event browser, displaying a summary of published events.
 */
public class OrganizerBrowseEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "OrganizerBrowseEvents";

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents;
    private TextView tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_browse_events);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        
        // Unified userId retrieval
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            userId = prefs.getString("userId", null);
        }

        if (userId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
                Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }
        
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                // Already home
            });
        }

        View btnNotifications = findViewById(R.id.nav_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerNotificationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        View btnQr = findViewById(R.id.nav_qr_code);
        if (btnQr != null) {
            btnQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerQrEventListActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        View btnProfile = findViewById(R.id.nav_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        if (userId == null) return;

        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    int openCount = 0;
                    int closedCount = 0;
                    int cancelledCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            eventList.add(event);

                            String status = event.getStatus();
                            if ("open".equalsIgnoreCase(status)) {
                                openCount++;
                            } else if ("closed".equalsIgnoreCase(status)) {
                                closedCount++;
                            } else if ("cancelled".equalsIgnoreCase(status)) {
                                cancelledCount++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document " + document.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateSummaryStats(openCount, closedCount, cancelledCount, eventList.size());
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase error", e);
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
        Intent intent = new Intent(this, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
