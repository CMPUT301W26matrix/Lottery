package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a list of events for the organizer to select and view their QR codes.
 */
public class OrganizerQrEventListActivity extends AppCompatActivity {

    private OrganizerQrEventAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_qr_event_list);

        // Fix: Use the custom back button from the layout instead of a Toolbar
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        
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

        RecyclerView rvEvents = findViewById(R.id.rvQrEvents);
        eventList = new ArrayList<>();

        adapter = new OrganizerQrEventAdapter(eventList, event -> {
            Intent intent = new Intent(OrganizerQrEventListActivity.this, OrganizerQrCodeDetailActivity.class);
            intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_QR_CONTENT, event.getQrCodeContent());
            startActivity(intent);
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadEvents();
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
                Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
                intent.putExtra("userId", userId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
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
                // Already here
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
        
        updateNavigationSelection();
    }

    private void updateNavigationSelection() {
        // Implementation for updating UI selection if needed
    }

    private void loadEvents() {
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
