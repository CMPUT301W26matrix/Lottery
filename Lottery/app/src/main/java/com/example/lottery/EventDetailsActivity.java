package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.github.bumptech.glide.Glide; // Required for future cloud storage loading
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to display the details of a specific event.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the event record from Firestore using the supplied event ID.</li>
 *   <li>Render the poster, title, schedule, deadline, and description.</li>
 *   <li>Surface organizer-configured requirements such as geolocation.</li>
 *   <li>Keep the branch's custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    /** Poster preview at the top of the details screen. */
    private ImageView ivEventPoster;
    /** Primary content fields for the selected event. */
    private TextView tvEventTitle, tvScheduledDate, tvRegistrationDeadline, tvEventDetails, tvLocationRequirement;
    /** Firestore access for event lookup. */
    private FirebaseFirestore db;
    /** Shared formatter for event timestamps. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * Binds the screen, initializes Firestore, and loads the requested event document.
     *
     * @param savedInstanceState previously saved activity state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize UI components
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);

        db = FirebaseFirestore.getInstance();

        // Setup common navigation listeners
        setupNavigation();

        // Get eventId from intent passed by the caller
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            fetchEventDetails(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Sets up click listeners for the custom bottom navigation bar included in the layout.
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
                finish();
            });
        }

        // Other buttons can be mapped to Toasts or future activities
        View btnHistory = findViewById(R.id.nav_calendar);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> Toast.makeText(this, "History Coming Soon", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Loads the event document from Firestore and updates the UI when found.
     *
     * @param eventId Firestore document ID for the event
     */
    private void fetchEventDetails(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            updateUI(event);
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
     * Populates the details screen from the fetched {@link Event}.
     *
     * <p>Poster URIs are currently local device URIs, so failures fall back to the placeholder
     * image instead of breaking the screen.</p>
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

        if (tvLocationRequirement != null) {
            if (event.isRequireLocation()) {
                tvLocationRequirement.setText("Location Verification Required");
                tvLocationRequirement.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
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
