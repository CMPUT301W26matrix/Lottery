package com.example.lottery.organizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.adapter.OrganizerQrEventAdapter;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.OrganizerNavigationHelper;
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

        OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.QR_CODE, userId);
        loadEvents();
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
