package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a list of events for the organizer to select and view their QR codes.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Initializes the RecyclerView to list all events from the 'events' collection in Firestore.</li>
 *   <li>Configures the custom Toolbar with a back navigation button.</li>
 *   <li>Manages data loading from Firestore and handles the navigation to QR code details.</li>
 *   <li>Provides fallback test data if the database is empty.</li>
 * </ul>
 * </p>
 */
public class OrganizerQrEventListActivity extends AppCompatActivity {

    /** RecyclerView for displaying event items. */
    private RecyclerView rvEvents;
    /** Adapter for binding event data to the RecyclerView. */
    private OrganizerQrEventAdapter adapter;
    /** Data source for the event list. */
    private List<Event> eventList;
    /** Firebase Firestore instance for database access. */
    private FirebaseFirestore db;

    /**
     * Initializes the activity, sets up the Toolbar with back navigation,
     * configures the RecyclerView and adapter, and triggers event loading from Firestore.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the saved state; otherwise null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_qr_event_list);

        // Setup Toolbar with back navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        rvEvents = findViewById(R.id.rvQrEvents);
        eventList = new ArrayList<>();

        // Initialize adapter with click listener to open QR detail view
        adapter = new OrganizerQrEventAdapter(eventList, event -> {
            Intent intent = new Intent(OrganizerQrEventListActivity.this, OrganizerQrCodeDetailActivity.class);
            intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_QR_CONTENT, event.getQrCodeContent());
            startActivity(intent);
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        loadEvents();
    }

    /**
     * Fetches the list of all events created by the current organizer from Firestore.
     * 
     * <p>On success, the event list is updated and the adapter is notified. 
     * If no events are found, a mock event is added for demonstration purposes.</p>
     */
    private void loadEvents() {
        // Obtain current UID from FirebaseAuth to ensure the organizer only sees their own events
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                // Filter by organizerId to maintain data security and isolation
                .whereEqualTo("organizerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        eventList.add(event);
                    }
                    
                    // Fallback for empty database to ensure the list remains functional for testing
                    if (eventList.isEmpty()) {
                        Event fake = new Event();
                        fake.setTitle("Test QR Event");
                        fake.setQrCodeContent("TEST_QR_DATA_12345");
                        eventList.add(fake);
                    }
                    
                    adapter.notifyDataSetChanged();
                });
    }
}
