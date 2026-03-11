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

public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "MainActivity";

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents;
    private TextView tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
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

        /* -------------------------------------------------
           TEMPORARY TEST CODE (REMOVE LATER)
           This forces the app to open your entrant screen
        -------------------------------------------------- */
        Intent intent = new Intent(MainActivity.this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "27f4180b-d282-41fc-a240-5d2675d4bf59");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "7EwFNDGwGyR89nPgfPzwD8wG6jq2");
        startActivity(intent);
        finish();

        /* ------------------------------------------------- */

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

                            // Compatibility fix
                            if (event.getScheduledDateTime() == null) {
                                Date oldDate = document.getDate("eventDate");
                                if (oldDate != null) event.setScheduledDateTime(oldDate);
                            }

                            if (event.getRegistrationDeadline() == null) {
                                Date oldDeadline = document.getDate("deadlineDate");
                                if (oldDeadline != null) event.setRegistrationDeadline(oldDeadline);
                            }

                            eventList.add(event);

                            if (event.getScheduledDateTime() != null &&
                                    event.getScheduledDateTime().after(now)) {
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