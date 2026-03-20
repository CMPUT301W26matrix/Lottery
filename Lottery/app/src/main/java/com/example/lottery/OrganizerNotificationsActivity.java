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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerNotificationsActivity allows organizers to manage and send notifications
 * to different groups of entrants (Waiting List, Selected, Cancelled).
 */
public class OrganizerNotificationsActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private OrganizerNotificationEventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_notifications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        rvEvents = findViewById(R.id.rvOrganizerEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);

        eventList = new ArrayList<>();
        adapter = new OrganizerNotificationEventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupNavigation();
        loadOrganizerEvents();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_create_container).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerCreateEventActivity.class));
        });

        findViewById(R.id.nav_notifications).setOnClickListener(v -> {
            // Already here
        });

        findViewById(R.id.nav_qr_code).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerQrEventListActivity.class));
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerProfileActivity.class));
        });
    }

    /**
     * Loads events owned by the current organizer to manage notifications.
     */
    private void loadOrganizerEvents() {
        // Ensure data isolation by filtering with current user's UID
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .whereEqualTo("organizerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }
}
