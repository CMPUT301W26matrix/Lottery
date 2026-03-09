package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity to display the details of a specific event and handle registration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the event record from Firestore using the supplied event ID.</li>
 *   <li>Render the poster, title, schedule, deadline, and description.</li>
 *   <li>Surface organizer-configured requirements such as geolocation.</li>
 *   <li>Enforce US 02.03.01: Disables registration when waiting list is full.</li>
 *   <li>Writes registration data to Firestore 'entrants' sub-collection (US 02.01.01).</li>
 *   <li>Keep the custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvScheduledDate, tvRegistrationDeadline, tvEventDetails, tvLocationRequirement;
    private TextView tvFullMessage, tvWaitingListCapacity;
    private Button btnRegister;
    private FirebaseFirestore db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /** The current event being displayed. */
    private Event currentEvent;
    /** Flag indicating if the waiting list has reached its capacity. */
    private boolean isEventFull = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        tvFullMessage = findViewById(R.id.tvFullMessage);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        btnRegister = findViewById(R.id.btnRegister);

        db = FirebaseFirestore.getInstance();

        setupNavigation();

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            fetchEventDetails(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    /**
     * Sets up click listeners for the bottom navigation bar and other navigation elements.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                startActivity(new Intent(this, CreateEventActivity.class));
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
                            checkWaitingListCapacity(currentEvent);
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
     * Checks if the waiting list has reached its capacity and updates the UI accordingly.
     *
     * @param event The event to check capacity for.
     */
    private void checkWaitingListCapacity(Event event) {
        if (event.getWaitingListLimit() == null) {
            updateRegistrationState(false);
            return;
        }

        db.collection("events").document(event.getEventId())
                .collection("entrants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentCount = queryDocumentSnapshots.size();
                    updateRegistrationState(currentCount >= event.getWaitingListLimit());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error counting entrants", e));
    }

    /**
     * Updates the registration button state and displays a message if the event is full.
     *
     * @param isFull True if the waiting list is full, false otherwise.
     */
    private void updateRegistrationState(boolean isFull) {
        isEventFull = isFull;
        if (isFull) {
            btnRegister.setAlpha(0.5f);
            tvFullMessage.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setAlpha(1.0f);
            tvFullMessage.setVisibility(View.GONE);
        }
    }

    /**
     * Implements the actual registration by writing to the Firestore 'entrants' sub-collection.
     * 
     * <p>Enforces waiting list capacity rules (US 02.03.01) and saves entrant data (US 02.01.01).</p>
     */
    private void handleRegistration() {
        if (isEventFull) {
            new AlertDialog.Builder(this)
                    .setTitle("Registration Unavailable")
                    .setMessage("This event's waiting list has reached its maximum capacity. You cannot register at this time.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (currentEvent == null) return;

        // Use Android ID as a persistent unique identifier for the entrant
        String entrantId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("entrantId", entrantId);
        entrantData.put("status", "waiting");
        entrantData.put("registrationTime", Timestamp.now());

        // US 02.01.01: Add user to Firestore entrants sub-collection
        db.collection("events").document(currentEvent.getEventId())
                .collection("entrants").document(entrantId)
                .set(entrantData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully joined the waiting list!", Toast.LENGTH_SHORT).show();
                    // Refresh capacity check after joining
                    checkWaitingListCapacity(currentEvent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waiting list", e);
                    Toast.makeText(this, "Failed to join waiting list. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());

        if (event.getScheduledDateTime() != null) {
            tvScheduledDate.setText(dateFormat.format(event.getScheduledDateTime()));
        }

        if (event.getRegistrationDeadline() != null) {
            tvRegistrationDeadline.setText(dateFormat.format(event.getRegistrationDeadline()));
        }

        if (tvWaitingListCapacity != null) {
            String capacityLabel = (event.getWaitingListLimit() == null) ? "Unlimited" : String.valueOf(event.getWaitingListLimit());
            tvWaitingListCapacity.setText("Waiting List Capacity: " + capacityLabel);
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
        if (posterUriString != null && !posterUriString.isEmpty()) {
            try {
                Uri posterUri = Uri.parse(posterUriString);
                ivEventPoster.setImageURI(null);
                ivEventPoster.setImageURI(posterUri);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load event poster", e);
                ivEventPoster.setImageResource(R.drawable.event_placeholder);
            }
        } else {
            ivEventPoster.setImageResource(R.drawable.event_placeholder);
        }
    }
}
