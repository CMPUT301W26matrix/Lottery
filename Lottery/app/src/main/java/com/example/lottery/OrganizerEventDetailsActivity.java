package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
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
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_USER_ID = "userId";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvScheduledDate, tvEventEndDate, tvRegistrationStart,
            tvRegistrationDeadline, tvDrawDate, tvEventDetails, tvLocationRequirement;
    private TextView tvFullMessage, tvWaitingListCapacity;
    private Button btnRegister;
    private SharedPreferences sharedPreferences;

    private FirebaseFirestore db;
    /**
     * The current event being displayed.
     */
    private Event currentEvent;
    private String eventId;
    /**
     * Flag indicating if the waiting list has reached its capacity.
     */
    private boolean isEventFull = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_details);

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        tvFullMessage = findViewById(R.id.tvFullMessage);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        btnRegister = findViewById(R.id.btnRegister);
        Button btnEditEvent = findViewById(R.id.btnEditEvent);
        Button btnViewWaitingList = findViewById(R.id.btnViewWaitingList);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        setupNavigation();

        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            fetchEventDetails(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnRegister.setOnClickListener(v -> handleRegistration());
        btnEditEvent.setOnClickListener(v -> handleEditEvent());
        btnViewWaitingList.setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitingListActivity.class);
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
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> startActivity(new Intent(this, OrganizerCreateEventActivity.class)));
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
        btnRegister.setEnabled(!isFull);
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

        String entrantId = getSignedInUserId();
        if (entrantId == null || entrantId.isEmpty()) {
            Toast.makeText(this, "Please sign in before joining the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // US 02.01.01: Add user to Firestore entrants sub-collection
        db.collection("events").document(currentEvent.getEventId())
                .collection("entrants").document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "You are already on the waiting list.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> entrantData = new HashMap<>();
                    entrantData.put("entrantId", entrantId);
                    entrantData.put("status", "waiting");
                    entrantData.put("registrationTime", Timestamp.now());
                    doc.getReference()
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
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to join waiting list. Please try again.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Retrieves the signed-in user's ID from SharedPreferences.
     *
     * @return the user ID string, or null if not signed in
     */
    private String getSignedInUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
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
                tvLocationRequirement.setText(getString(R.string.location_verification_required));
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
