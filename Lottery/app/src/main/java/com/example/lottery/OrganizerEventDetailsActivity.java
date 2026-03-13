package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.Event;
import com.example.lottery.util.PosterImageLoader;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to display the details of a specific event and handle registration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the event record from Firestore using the supplied event ID.</li>
 *   <li>Render the poster, title, schedule, deadline, and description.</li>
 *   <li>Surface organizer-configured requirements such as geolocation.</li>
 *   <li>Keep the custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvScheduledDate, tvEventEndDate, tvRegistrationStart,
            tvRegistrationDeadline, tvDrawDate, tvEventDetails, tvLocationRequirement;
    private TextView tvFullMessage, tvWaitingListCapacity;
    private Button btnEditEvent;
    private FirebaseFirestore db;
    /**
     * The current event being displayed.
     */
    private Event currentEvent;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_event_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        Button btnEditEvent = findViewById(R.id.btnEditEvent);
        Button btnViewWaitingList = findViewById(R.id.btnViewWaitingList);
        db = FirebaseFirestore.getInstance();

        setupNavigation();

        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            fetchEventDetails(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEditEvent.setOnClickListener(v -> handleEditEvent());
        btnViewWaitingList.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantsListActivity.class);
            intent.putExtra("eventId", eventId); // pass event ID
            startActivity(intent);
        });
    }

    /**
     * Refreshes the displayed event details whenever the activity returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null) {
            fetchEventDetails(eventId);
        }
    }

    /**
     * Sets up click listeners for the bottom navigation bar and other navigation elements.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                startActivity(new Intent(this, OrganizerCreateEventActivity.class));
            });
        }

        View btnHistory = findViewById(R.id.nav_calendar);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                    Toast.makeText(this, "History Coming Soon", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Fetches the event details from Firestore.
     *
     * @param eventId The unique identifier of the event.
     */
    private void fetchEventDetails(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            updateUI(currentEvent);
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event details", e);
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Launches OrganizerCreateEventActivity in edit mode for the currently displayed event.
     */
    private void handleEditEvent() {
        Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());

        tvScheduledDate.setText(event.getScheduledDateTime() != null
                ? dateFormat.format(event.getScheduledDateTime()) : "");
        tvEventEndDate.setText(event.getEventEndDate() != null
                ? dateFormat.format(event.getEventEndDate()) : "");
        tvRegistrationStart.setText(event.getRegistrationStartDate() != null
                ? dateFormat.format(event.getRegistrationStartDate()) : "");
        tvRegistrationDeadline.setText(event.getRegistrationDeadline() != null
                ? dateFormat.format(event.getRegistrationDeadline()) : "");
        tvDrawDate.setText(event.getDrawDate() != null
                ? dateFormat.format(event.getDrawDate()) : "");

        if (tvWaitingListCapacity != null) {
            String capacityLabel = (event.getWaitingListLimit() == null) ? "Unlimited" : String.valueOf(event.getWaitingListLimit());
            tvWaitingListCapacity.setText(capacityLabel);
        }

        if (tvLocationRequirement != null) {
            if (event.isRequireLocation()) {
                tvLocationRequirement.setText("Location Verification Required");
                tvLocationRequirement.setVisibility(View.VISIBLE);
            } else {
                tvLocationRequirement.setVisibility(View.GONE);
            }
        }

        String posterUriString = event.getPosterUri();
        PosterImageLoader.load(ivEventPoster, posterUriString, R.drawable.event_placeholder);
    }
}
