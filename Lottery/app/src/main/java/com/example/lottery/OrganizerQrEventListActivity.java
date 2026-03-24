package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    /**
     * Adapter for binding event data to the RecyclerView.
     */
    private OrganizerQrEventAdapter adapter;
    /**
     * Data source for the event list.
     */
    private List<Event> eventList;
    /**
     * Firebase Firestore instance for database access.
     */
    private FirebaseFirestore db;
    private String userId;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_qr_event_list);

        // Standard way to handle system bars (notches, status bars)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

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

        // Initialize adapter with click listener to open QR detail view
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
        // Highlight current selection
        View navQr = findViewById(R.id.nav_qr_code);
        if (navQr instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) navQr;
            if (ll.getChildCount() >= 2) {
                View iv = ll.getChildAt(0);
                View tv = ll.getChildAt(1);
                if (iv instanceof ImageView) ((ImageView) iv).setColorFilter(getResources().getColor(R.color.primary_blue));
                if (tv instanceof TextView) ((TextView) tv).setTextColor(getResources().getColor(R.color.primary_blue));
            }
        }
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
