package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
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
 * Main activity for the entrant user role.
 * Displays a list of events and user statistics.
 *
 * <p>Entrants primarily discover active events from the 'events' collection
 * where status is 'open'.</p>
 */
public class EntrantMainActivity extends AppCompatActivity {

    private final List<Event> eventList = new ArrayList<>();
    private RecyclerView rvEvents;
    private EntrantEventAdapter adapter;
    private View emptyStateContainer;
    private TextView tvActiveCount, tvJoinedCount, tvNotificationBadge;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        rvEvents = findViewById(R.id.rvEvents);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvJoinedCount = findViewById(R.id.tvJoinedCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        adapter = new EntrantEventAdapter(eventList, this::openEventDetails);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadEvents();
        loadStats();
        checkUnreadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        checkUnreadNotifications();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            // Already home
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantQrScanActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    /**
     * Loads active events from Firestore where status is 'open'.
     */
    private void loadEvents() {
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads user-specific statistics from Firestore.
     * Active count reflects 'open' events.
     * Joined count queries across all 'waitingList' subcollections for the current user.
     */
    private void loadStats() {
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (tvActiveCount != null) {
                        tvActiveCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    }
                });

        if (userId != null) {
            db.collectionGroup(FirestorePaths.WAITING_LIST)
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvJoinedCount != null) {
                            tvJoinedCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                        }
                    });
        }
    }

    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}
