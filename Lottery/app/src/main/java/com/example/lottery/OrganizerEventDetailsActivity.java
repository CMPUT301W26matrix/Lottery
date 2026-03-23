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
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.SessionUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to display the details of a specific event for the organizer.
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvScheduledDate, tvRegistrationDeadline, tvDrawDate, tvEventDetails, tvLocationRequirement;
    private TextView tvWaitingListCapacity, tvEntrantCounts;
    private Button btnEditEvent;
    private FirebaseFirestore db;

    private Event currentEvent;
    private String eventId;
    private String userId;

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
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        tvEntrantCounts = findViewById(R.id.tvEntrantCounts);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        Button btnViewWaitingList = findViewById(R.id.btnViewWaitingList);

        // Remove references to deleted UI components if they were in the layout but no longer in model
        // tvEventEndDate = findViewById(R.id.tvEventEndDate);
        // tvRegistrationStart = findViewById(R.id.tvRegistrationStart);

        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("eventId");
        userId = SessionUtil.resolveUserId(this);

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (userId == null) {
            Toast.makeText(this, R.string.missing_user_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupNavigation();
        fetchEventDetails(eventId);
        fetchEntrantCounts(eventId);

        btnEditEvent.setOnClickListener(v -> handleEditEvent());

        btnViewWaitingList.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantsListActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null) {
            fetchEventDetails(eventId);
            fetchEntrantCounts(eventId);
        }
    }

    private void setupNavigation() {
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

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
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
                Intent intent = new Intent(this, OrganizerQrEventListActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
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
    }

    private void fetchEventDetails(String eventId) {
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(documentSnapshot.getId());
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
     * Fetches real-time counts from the waitingList subcollection.
     */
    private void fetchEntrantCounts(String eventId) {
        db.collection(FirestorePaths.eventWaitingList(eventId))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int waitlisted = 0;
                    int invited = 0;
                    int accepted = 0;
                    int cancelled = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        switch (status) {
                            case InvitationFlowUtil.STATUS_WAITLISTED: waitlisted++; break;
                            case InvitationFlowUtil.STATUS_INVITED: invited++; break;
                            case InvitationFlowUtil.STATUS_ACCEPTED: accepted++; break;
                            case InvitationFlowUtil.STATUS_CANCELLED: cancelled++; break;
                        }
                    }

                    if (tvEntrantCounts != null) {
                        tvEntrantCounts.setText(String.format(Locale.getDefault(),
                                "Waitlisted: %d | Invited: %d | Accepted: %d | Cancelled: %d",
                                waitlisted, invited, accepted, cancelled));
                    }
                });
    }

    private void handleEditEvent() {
        Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
        tvEventDetails.setText(event.getDetails() != null ? event.getDetails() : "");

        if (event.getScheduledDateTime() != null)
            tvScheduledDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        
        // registrationStartDate and eventEndDate removed from Event model

        if (event.getRegistrationDeadline() != null)
            tvRegistrationDeadline.setText(dateFormat.format(event.getRegistrationDeadline().toDate()));
        if (event.getDrawDate() != null)
            tvDrawDate.setText(dateFormat.format(event.getDrawDate().toDate()));

        if (tvWaitingListCapacity != null) {
            String capacityLabel = (event.getWaitingListLimit() == null)
                    ? "Unlimited"
                    : String.valueOf(event.getWaitingListLimit());
            tvWaitingListCapacity.setText(capacityLabel);
        }

        if (tvLocationRequirement != null) {
            tvLocationRequirement.setVisibility(event.isRequireLocation() ? View.VISIBLE : View.GONE);
        }

        PosterImageLoader.load(ivEventPoster, event.getPosterUri(), R.drawable.event_placeholder);
    }
}
