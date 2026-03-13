package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * EntrantMainActivity is the main dashboard for entrants after signing in.
 *
 * <p>This activity displays a list of available events retrieved from Firestore.
 * Entrants can browse events and select one to view additional details and
 * join or leave the waiting list.
 *
 * <p>The activity contains:
 * <ul>
 *     <li>A RecyclerView displaying available events</li>
 *     <li>An empty-state message when no events are available</li>
 *     <li>A bottom navigation bar for notifications, home, QR scanning, and settings</li>
 * </ul>
 *
 * <p>When an event is selected, the activity launches
 * {@link EntrantEventDetailsActivity} and passes the event ID and user ID.
 *
 * <p>This screen represents the main entry point for entrants to interact with
 * events and waiting lists in the application.
 */
public class EntrantMainActivity extends AppCompatActivity {

    /** RecyclerView used to display the list of events */
    private RecyclerView rvEvents;

    /** Container shown when no events exist */
    private LinearLayout emptyStateContainer;

    /** Bottom navigation items */
    private LinearLayout navNotifications;
    private LinearLayout navHome;
    private LinearLayout navQrScan;
    private LinearLayout navSettings;

    /** Firestore database reference */
    private FirebaseFirestore db;

    /** List of events retrieved from Firestore */
    private final List<Event> eventList = new ArrayList<>();

    /** Adapter used to display events inside the RecyclerView */
    private EntrantEventAdapter adapter;

    /** Currently signed-in entrant's user ID */
    private String userId;

    /**
     * Called when the activity is created.
     *
     * <p>This method initializes:
     * <ul>
     *     <li>Firestore connection</li>
     *     <li>RecyclerView and adapter</li>
     *     <li>Bottom navigation listeners</li>
     *     <li>Event data retrieval from Firestore</li>
     * </ul>
     *
     * @param savedInstanceState saved activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_main);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvEvents = findViewById(R.id.rvEvents);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);

        navNotifications = findViewById(R.id.nav_notifications);
        navHome = findViewById(R.id.nav_home);
        navQrScan = findViewById(R.id.nav_qr_scan);
        navSettings = findViewById(R.id.nav_settings);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EntrantEventAdapter(eventList, event -> openEventDetails(event));
        rvEvents.setAdapter(adapter);

        setupBottomNavigation();
        loadEvents();
    }

    /**
     * Configures the bottom navigation bar.
     *
     * <p>The navigation options include:
     * <ul>
     *     <li>Notifications screen</li>
     *     <li>Home screen</li>
     *     <li>QR code scanner (placeholder)</li>
     *     <li>Settings screen (placeholder)</li>
     * </ul>
     */
    private void setupBottomNavigation() {

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        navHome.setOnClickListener(v -> {
            // Already on the home screen
        });

        navQrScan.setOnClickListener(v -> {
            Toast.makeText(this, "QR Scan not implemented yet", Toast.LENGTH_SHORT).show();
        });

        navSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Loads all available events from the Firestore "events" collection.
     *
     * <p>This method retrieves event documents, converts them to {@link Event}
     * objects, and updates the RecyclerView adapter.
     *
     * <p>If no events exist, an empty-state message is displayed.
     */
    private void loadEvents() {

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    eventList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }

                    adapter.notifyDataSetChanged();

                    if (eventList.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        rvEvents.setVisibility(View.GONE);
                    } else {
                        emptyStateContainer.setVisibility(View.GONE);
                        rvEvents.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Opens the event details screen when an event is selected.
     *
     * <p>The selected event ID and the current user ID are passed to
     * {@link EntrantEventDetailsActivity}.
     *
     * @param event the event selected by the entrant
     */
    private void openEventDetails(Event event) {

        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}