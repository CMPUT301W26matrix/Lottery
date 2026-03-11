package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Displays detailed information about a selected event for an entrant.
 *
 * <p>This activity allows an entrant to:
 * <ul>
 *     <li>View event information such as title, description, and registration period</li>
 *     <li>View the current number of entrants in the waitlist</li>
 *     <li>Join or leave the event waitlist</li>
 *     <li>View notifications related to the entrant</li>
 * </ul>
 *
 * <p>The activity retrieves event information from Firebase Firestore and updates
 * the UI accordingly.</p>
 *
 * <p>The activity expects two intent extras:
 * <ul>
 *     <li>{@link #EXTRA_EVENT_ID} – the ID of the selected event</li>
 *     <li>{@link #EXTRA_USER_ID} – the ID of the current entrant</li>
 * </ul>
 * </p>
 */
public class EntrantEventDetailsActivity extends AppCompatActivity {

    /** Intent extra key used to pass the event ID to this activity. */
    public static final String EXTRA_EVENT_ID = "eventId";

    /** Intent extra key used to pass the user ID to this activity. */
    public static final String EXTRA_USER_ID = "userId";

    /** Displays the event title. */
    private TextView tvEventTitle;

    /** Displays the event registration period. */
    private TextView tvRegistrationPeriod;

    /** Displays the number of people currently in the waitlist. */
    private TextView tvWaitlistCount;

    /** Notification badge indicating unread notifications. */
    private TextView tvNotificationBadge;

    /** Displays the event description. */
    private TextView tvEventDescription;

    /** Button used to join or leave the waitlist. */
    private Button btnWaitlistAction;

    /** Button used to open the notifications screen. */
    private ImageButton btnNotifications;

    /** Button used to close the activity. */
    private ImageButton btnClose;

    /** Displays the event poster image. */
    private ImageView ivEventPoster;

    /** Firestore database instance used to retrieve and update event data. */
    private FirebaseFirestore db;

    /** ID of the selected event. */
    private String eventId;

    /** ID of the current user (entrant). */
    private String userId;

    /** Indicates whether the current entrant is already in the waitlist. */
    private boolean isInWaitlist = false;

    /** Number of entrants currently in the waitlist. */
    private int waitlistCount = 0;

    /** Date format used to display registration dates. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * Initializes the activity, retrieves intent data, binds UI components,
     * and loads event information from Firestore.
     *
     * @param savedInstanceState the previously saved state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_event_details);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvRegistrationPeriod = findViewById(R.id.tvRegistrationPeriod);
        tvWaitlistCount = findViewById(R.id.tvWaitlistCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvEventDescription = findViewById(R.id.tvEventDescription);
        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnClose = findViewById(R.id.btnClose);
        ivEventPoster = findViewById(R.id.ivEventPoster);

        readIntentData();
        if (eventId == null || userId == null) {
            return;
        }

        loadEventDetails();
        checkWaitlistStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        btnWaitlistAction.setOnClickListener(v -> {
            if (isInWaitlist) {
                leaveWaitlist();
            } else {
                joinWaitlist();
            }
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> finish());
    }

    /**
     * Refreshes event data whenever the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId == null || userId == null) {
            return;
        }

        loadEventDetails();
        checkWaitlistStatus();
        loadWaitlistCount();
        checkUnreadNotifications();
    }

    /**
     * Reads event and user identifiers from the launching intent.
     * If required data is missing, the activity closes.
     */
    private void readIntentData() {
        Intent intent = getIntent();
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        userId = intent.getStringExtra(EXTRA_USER_ID);

        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Missing event or user information", Toast.LENGTH_SHORT).show();
            eventId = null;
            userId = null;
            finish();
        }
    }

    /**
     * Retrieves event details from Firestore and updates the UI.
     */
    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = getFirstNonEmptyString(
                            documentSnapshot.getString("title"),
                            documentSnapshot.getString("eventTitle"),
                            documentSnapshot.getString("name")
                    );

                    String details = getFirstNonEmptyString(
                            documentSnapshot.getString("details"),
                            documentSnapshot.getString("description")
                    );

                    Timestamp registrationStart = documentSnapshot.getTimestamp("registrationStartDate");
                    Timestamp registrationDeadline = documentSnapshot.getTimestamp("registrationDeadline");
                    Timestamp eventEndDate = documentSnapshot.getTimestamp("eventEndDate");
                    Timestamp drawDate = documentSnapshot.getTimestamp("drawDate");

                    String posterUri = documentSnapshot.getString("posterUri");

                    if (title == null || title.isEmpty()) {
                        title = "Event Details";
                    }

                    if (details == null || details.isEmpty()) {
                        details = "No event description available.";
                    }

                    tvEventTitle.setText(title);
                    tvEventDescription.setText(details);
                    tvRegistrationPeriod.setText(
                            buildRegistrationText(registrationStart, registrationDeadline, eventEndDate, drawDate)
                    );

                    loadPosterImage(posterUri);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Builds a readable registration period string based on available timestamps.
     */
    private String buildRegistrationText(Timestamp start, Timestamp deadline, Timestamp endDate, Timestamp drawDate) {
        if (start != null && deadline != null) {
            return "Registration Period: " + dateFormat.format(start.toDate()) +
                    " to " + dateFormat.format(deadline.toDate());
        } else if (deadline != null && drawDate != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate()) +
                    " | Draw date: " + dateFormat.format(drawDate.toDate());
        } else if (deadline != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate());
        } else if (endDate != null) {
            return "Event ends: " + dateFormat.format(endDate.toDate());
        } else {
            return "Registration details unavailable";
        }
    }

    /**
     * Loads and displays the event poster image.
     */
    private void loadPosterImage(String posterUri) {
        if (posterUri == null || posterUri.isEmpty()) {
            ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        try {
            Uri uri = Uri.parse(posterUri);

            if ("content".equals(uri.getScheme())
                    || "file".equals(uri.getScheme())
                    || "android.resource".equals(uri.getScheme())) {
                ivEventPoster.setImageURI(uri);
            } else {
                ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    /**
     * Returns the first non-empty string from a list of possible values.
     */
    private String getFirstNonEmptyString(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Checks whether the current entrant is already in the event waitlist.
     */
    private void checkWaitlistStatus() {
        DocumentReference entrantRef = db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId);

        entrantRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()
                    && "waiting".equalsIgnoreCase(documentSnapshot.getString("status"))) {
                isInWaitlist = true;
                btnWaitlistAction.setText("Leave Wait List");
            } else {
                isInWaitlist = false;
                btnWaitlistAction.setText("Join Wait List");
            }
        });
    }

    /**
     * Retrieves the number of entrants currently in the waitlist.
     */
    private void loadWaitlistCount() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waitlistCount = queryDocumentSnapshots.size();
                    tvWaitlistCount.setText("People in Waitlist: " + waitlistCount);
                });
    }

    /**
     * Checks whether the entrant has unread notifications.
     */
    private void checkUnreadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Adds the entrant to the event waitlist in Firestore.
     */
    private void joinWaitlist() {
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("userId", userId);
        entrantData.put("status", "waiting");
        entrantData.put("registrationTime", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = true;
                    btnWaitlistAction.setText("Leave Wait List");
                    loadWaitlistCount();
                    Toast.makeText(this, "Joined waitlist", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes the entrant from the event waitlist.
     */
    private void leaveWaitlist() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    btnWaitlistAction.setText("Join Wait List");
                    loadWaitlistCount();
                    Toast.makeText(this, "Left waitlist", Toast.LENGTH_SHORT).show();
                });
    }
}