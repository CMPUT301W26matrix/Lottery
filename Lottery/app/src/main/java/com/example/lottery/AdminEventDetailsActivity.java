package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * AdminEventDetailsActivity displays a read-only administrator view of a specific event.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetches the event record from Firestore using the supplied event ID.</li>
 *   <li>Renders the poster, title, schedule, registration dates, and description.</li>
 *   <li>Surfaces organizer-configured requirements such as geolocation.</li>
 *   <li>Keeps the custom admin bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class AdminEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminEventDetails";
    private static final String EXTRA_EVENT_ID = "eventId";

    /**
     * Formatter used for displaying event-related date fields.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
    );

    /**
     * ImageView used for displaying the event poster.
     */
    private ImageView ivEventPoster;
    /**
     * TextViews for rendering the main event metadata and details.
     */
    private TextView tvEventTitle;
    private TextView tvScheduledDate;
    private TextView tvEventEndDate;
    private TextView tvRegistrationStart;
    private TextView tvRegistrationDeadline;
    private TextView tvDrawDate;
    private TextView tvWaitingListCapacity;
    private TextView tvEventDetails;
    private TextView tvLocationRequirement;
    private Button btnDeleteEvent;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Identifier of the event currently being displayed.
     */
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_details);

        db = FirebaseFirestore.getInstance();

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);

        // Set click listener for the delete button
        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());

        setupNavigation();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchEventDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        }
    }

    /**
     * Sets up click listeners for the admin bottom navigation bar.
     */
    private void setupNavigation() {
        View btnEvents = findViewById(R.id.nav_home);
        if (btnEvents != null) {
            btnEvents.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_profiles_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_images_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_logs_coming_soon, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Launches a confirmation dialog before deleting the event for confirmation.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Do you confirm the deletion of this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null).show();
    }

    private void deleteEvent() {
        // If eventId is null or empty, show a toast message
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the delete button
        btnDeleteEvent.setEnabled(false);

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalEntrants = queryDocumentSnapshots.size();
                    // In case there are no entrants, delete the event document
                    if (totalEntrants == 0) {
                        deleteEventDocument();
                        return;
                    }

                    // Delete each entrant document
                    int[] deletedEntrants = {0};
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference()
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    deletedEntrants[0]++;
                                    if (deletedEntrants[0] == totalEntrants) {
                                        deleteEventDocument();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting event entrant", e);
                                    btnDeleteEvent.setEnabled(true);
                                    Toast.makeText(this, "Failed to delete event entrants", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event entrants", e);
                    btnDeleteEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to delete event entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes the event document after related entrant records have been removed.
     */
    private void deleteEventDocument() {
        db.collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AdminBrowseEventsActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    btnDeleteEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the event details from Firestore.
     */
    private void fetchEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Event event = documentSnapshot.toObject(Event.class);
                    if (event == null) {
                        Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateUi(event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin event details", e);
                    Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUi(Event event) {
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

        String capacityLabel = event.getWaitingListLimit() == null
                ? getString(R.string.unlimited)
                : String.valueOf(event.getWaitingListLimit());
        tvWaitingListCapacity.setText(capacityLabel);

        if (event.isRequireLocation()) {
            tvLocationRequirement.setVisibility(View.VISIBLE);
            tvLocationRequirement.setText(R.string.location_verification_required);
        } else {
            tvLocationRequirement.setVisibility(View.GONE);
        }

        String posterUriString = event.getPosterUri();
        if (posterUriString == null || posterUriString.isEmpty()) {
            ivEventPoster.setImageResource(R.drawable.event_placeholder);
            return;
        }

        try {
            Uri posterUri = Uri.parse(posterUriString);
            ivEventPoster.setImageURI(null);
            ivEventPoster.setImageURI(posterUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load admin event poster", e);
            ivEventPoster.setImageResource(R.drawable.event_placeholder);
        }
    }
}
