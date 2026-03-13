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
import com.example.lottery.util.QRCodeUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the entrant user role.
 * Displays a list of events and user statistics.
 */
public class EntrantMainActivity extends AppCompatActivity {

    private final List<Event> eventList = new ArrayList<>();
    private RecyclerView rvEvents;
    private EntrantEventAdapter adapter;
    private View emptyStateContainer;
    private TextView tvActiveCount, tvJoinedCount;
    private FirebaseFirestore db;
    private String userId;

    /**
     * Initializes the activity, setting up the UI and loading data.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
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

        adapter = new EntrantEventAdapter(eventList, this::openEventDetails);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadEvents();
        loadStats();
    }

    /**
     * Sets up click listeners for the navigation buttons.
     */
    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            // Already home
        });

        findViewById(R.id.nav_history).setOnClickListener(v ->
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> startQRScanner());

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    /**
     * Starts the QR scanner.
     */
    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan Event QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    /**
     * Handles the QR scan result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String scannedContent = result.getContents();
                String eventId = QRCodeUtils.extractEventId(scannedContent);

                if (eventId == null || eventId.isEmpty()) {
                    Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
                intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId);
                intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
                startActivity(intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Loads events from the Firestore database.
     */
    private void loadEvents() {
        db.collection("events")
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
     * Loads user-specific statistics from the Firestore database.
     */
    private void loadStats() {
        db.collection("events").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (tvActiveCount != null) {
                tvActiveCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });

        if (userId != null) {
            db.collectionGroup("entrants")
                    .whereEqualTo("entrantId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvJoinedCount != null) {
                            tvJoinedCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                        }
                    });
        }
    }

    /**
     * Updates the empty state view based on whether the event list is empty.
     */
    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles the event click event.
     *
     * @param event The event that was clicked.
     */
    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}