package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
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

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.OrganizerNavigationHelper;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.SessionUtil;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to display the details of a specific event and handle registration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the event record from Firestore using the supplied event ID.</li>
 *   <li>Render the poster (Base64), title, schedule, deadline, and description.</li>
 *   <li>Surface organizer-configured requirements such as geolocation.</li>
 *   <li>Keep the custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvPlace, tvScheduledDate, tvEventEndDate, tvRegistrationStart, tvRegistrationDeadline, tvDrawDate, tvEventDetails, tvLocationRequirement;
    private TextView tvWaitingListCapacity, tvEntrantCounts;
    private TextView btnShowMore;
    private Chip chipCategory, chipPrivate;
    private Button btnInviteEntrant;
    private ImageButton btnEditEvent, btnComments, btnCoOrganizers, btnBack;
    private FirebaseFirestore db;

    private Event currentEvent;
    private String eventId;
    private String userId;
    private String userName;
    private boolean isDescriptionExpanded = false;

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
        tvPlace = findViewById(R.id.tvPlace);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        tvEntrantCounts = findViewById(R.id.tvEntrantCounts);
        btnShowMore = findViewById(R.id.btnShowMore);
        chipCategory = findViewById(R.id.chipCategory);
        chipPrivate = findViewById(R.id.chipPrivate);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnInviteEntrant = findViewById(R.id.btnInviteEntrant);
        btnComments = findViewById(R.id.btnComments);
        btnCoOrganizers = findViewById(R.id.btnCoOrganizers);
        btnBack = findViewById(R.id.btnBack);
        Button btnViewWaitingList = findViewById(R.id.btnViewWaitingList);

        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);

        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("eventId");
        userId = SessionUtil.resolveUserId(this);
        userName = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userName", "");

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

        OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.HOME, userId, true);
        // Fetch authoritative name from Firestore
        db.collection(FirestorePaths.USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("username");
                        if (name != null && !name.isEmpty()) userName = name;
                    }
                });

        fetchEventDetails(eventId);
        fetchEntrantCounts(eventId);

        btnEditEvent.setOnClickListener(v -> handleEditEvent());

        btnViewWaitingList.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantsListActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnInviteEntrant.setOnClickListener(v -> openInviteDialog());

        btnComments.setOnClickListener(v -> {
            CommentBottomSheet bottomSheet = CommentBottomSheet.newInstance(
                    eventId, userId, userName, true);
            bottomSheet.show(getSupportFragmentManager(), "comment_bottom_sheet");
        });

        btnCoOrganizers.setOnClickListener(v -> openCoOrganizerDialog());

        btnShowMore.setOnClickListener(v -> toggleDescription());

        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Toggles the description text view between collapsed and expanded states.
     */
    private void toggleDescription() {
        if (isDescriptionExpanded) {
            tvEventDetails.setMaxLines(3);
            btnShowMore.setText("Show more");
            isDescriptionExpanded = false;
        } else {
            tvEventDetails.setMaxLines(Integer.MAX_VALUE);
            btnShowMore.setText("Show less");
            isDescriptionExpanded = true;
        }
    }

    private void openInviteDialog() {
        if (currentEvent == null) return;
        OrganizerInviteEntrantDialogFragment dialog = OrganizerInviteEntrantDialogFragment.newInstance(
                currentEvent.getEventId(),
                currentEvent.getTitle(),
                userId
        );
        dialog.show(getSupportFragmentManager(), "invite_entrant");
    }

    private void openCoOrganizerDialog() {
        if (currentEvent == null) return;
        OrganizerInviteCoOrganizerDialogFragment dialog = OrganizerInviteCoOrganizerDialogFragment.newInstance(
                currentEvent.getEventId(),
                currentEvent.getTitle(),
                userId
        );
        dialog.show(getSupportFragmentManager(), "invite_co_organizer");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null) {
            fetchEventDetails(eventId);
            fetchEntrantCounts(eventId);
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
                            case InvitationFlowUtil.STATUS_WAITLISTED:
                                waitlisted++;
                                break;
                            case InvitationFlowUtil.STATUS_INVITED:
                                invited++;
                                break;
                            case InvitationFlowUtil.STATUS_ACCEPTED:
                                accepted++;
                                break;
                            case InvitationFlowUtil.STATUS_CANCELLED:
                                cancelled++;
                                break;
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

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");

        if (event.getPlace() != null && !event.getPlace().isEmpty()) {
            tvPlace.setText(event.getPlace());
            tvPlace.setVisibility(View.VISIBLE);
        } else {
            tvPlace.setVisibility(View.GONE);
        }

        tvEventDetails.setText(event.getDetails() != null ? event.getDetails() : "");

        // Determine if "Show more" is needed
        tvEventDetails.post(() -> {
            Layout layout = tvEventDetails.getLayout();
            if (layout != null) {
                int lines = layout.getLineCount();
                if (lines > 0) {
                    if (layout.getEllipsisCount(lines - 1) > 0 || lines > 3) {
                        btnShowMore.setVisibility(View.VISIBLE);
                    } else {
                        btnShowMore.setVisibility(View.GONE);
                    }
                }
            } else {
                if (tvEventDetails.getLineCount() > 3) {
                    btnShowMore.setVisibility(View.VISIBLE);
                }
            }
        });

        if (event.getScheduledDateTime() != null)
            tvScheduledDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        if (event.getEventEndDateTime() != null)
            tvEventEndDate.setText(dateFormat.format(event.getEventEndDateTime().toDate()));
        if (event.getRegistrationStart() != null)
            tvRegistrationStart.setText(dateFormat.format(event.getRegistrationStart().toDate()));
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

        // Update chips
        if (chipCategory != null) {
            chipCategory.setText(event.getCategory() != null ? event.getCategory() : "Other");
        }

        if (chipPrivate != null) {
            chipPrivate.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);
        }

        // Show/Hide Invite button based on private status
        if (btnInviteEntrant != null) {
            btnInviteEntrant.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);
        }

        PosterImageLoader.load(ivEventPoster, event.getPosterBase64(), R.drawable.event_placeholder);
    }
}
