package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.PosterImageLoader;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Displays event details for an entrant and controls all entrant-side event actions.
 *
 * <p>Source of truth for entrant state:</p>
 * <pre>
 * events/{eventId}/entrant_events/{userId}
 * </pre>
 *
 * <p>UI behavior by status:</p>
 * <ul>
 *     <li>waiting -> show leave waitlist button</li>
 *     <li>invited -> show accept / decline buttons</li>
 *     <li>accepted -> show cancel membership button</li>
 *     <li>declined -> hide action button and show declined state</li>
 *     <li>no document -> show join waitlist button</li>
 * </ul>
 */
public class EntrantEventDetailsActivity extends AppCompatActivity {

    /** Intent extra key for event ID. */
    public static final String EXTRA_EVENT_ID = "eventId";

    /** Intent extra key for user ID. */
    public static final String EXTRA_USER_ID = "userId";

    /** Formatter for displayed dates. */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private TextView tvEventTitle;
    private TextView tvRegistrationPeriod;
    private TextView tvWaitlistCount;
    private TextView tvNotificationBadge;
    private TextView tvEventDescription;

    private Button btnWaitlistAction;
    private Button btnAcceptInvite;
    private Button btnDeclineInvite;

    private LinearLayout invitationButtonsContainer;
    private LinearLayout registrationEndedContainer;
    private LinearLayout navHome;
    private LinearLayout navNotifications;

    private ImageButton btnClose;
    private ImageView ivEventPoster;

    private FirebaseFirestore db;
    private String eventId;
    private String userId;

    private boolean isInWaitlist = false;
    private boolean hasAcceptedInvite = false;
    private boolean hasDeclinedInvite = false;

    /**
     * Initializes UI, reads intent data, and loads event + entrant state.
     *
     * @param savedInstanceState previously saved state if activity is recreated
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

        initializeViews();
        readIntentData();

        if (eventId == null || userId == null) {
            return;
        }

        refreshAll();

        btnAcceptInvite.setOnClickListener(v -> acceptInvitation());
        btnDeclineInvite.setOnClickListener(v -> declineInvitation());

        btnWaitlistAction.setOnClickListener(v -> {
            if (hasAcceptedInvite) {
                cancelAcceptedInvitation();
            } else if (isInWaitlist) {
                leaveWaitlist();
            } else {
                joinWaitlist();
            }
        });

        navHome.setOnClickListener(v -> openHome());
        navNotifications.setOnClickListener(v -> openNotifications());
        btnClose.setOnClickListener(v -> finish());
    }

    /**
     * Refreshes UI and status when returning to this screen.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId == null || userId == null) {
            return;
        }
        refreshAll();
    }

    /**
     * Binds all views from XML.
     */
    private void initializeViews() {
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvRegistrationPeriod = findViewById(R.id.tvRegistrationPeriod);
        tvWaitlistCount = findViewById(R.id.tvWaitlistCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvEventDescription = findViewById(R.id.tvEventDescription);

        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);
        btnAcceptInvite = findViewById(R.id.btnAcceptInvite);
        btnDeclineInvite = findViewById(R.id.btnDeclineInvite);

        invitationButtonsContainer = findViewById(R.id.invitationButtonsContainer);
        registrationEndedContainer = findViewById(R.id.registrationEndedContainer);

        navHome = findViewById(R.id.nav_home);
        navNotifications = findViewById(R.id.nav_history);

        btnClose = findViewById(R.id.btnBack);
        ivEventPoster = findViewById(R.id.ivEventPoster);
    }

    /**
     * Reads event and user IDs from the intent.
     */
    private void readIntentData() {
        Intent intent = getIntent();
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        userId = intent.getStringExtra(EXTRA_USER_ID);

        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Missing event/user info", Toast.LENGTH_SHORT).show();
            eventId = null;
            userId = null;
            finish();
        }
    }

    /**
     * Reloads all event and entrant-dependent UI.
     */
    private void refreshAll() {
        loadEventDetails();
        checkUserEventStatus();
        loadWaitlistCount();
        checkUnreadNotifications();
    }

    /**
     * Loads event details and updates visible event information.
     */
    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
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
                        details = "Description unavailable";
                    }

                    tvEventTitle.setText(title);
                    tvEventDescription.setText(details);
                    tvRegistrationPeriod.setText(
                            buildRegistrationText(registrationStart, registrationDeadline, eventEndDate, drawDate)
                    );

                    PosterImageLoader.load(ivEventPoster, posterUri, android.R.drawable.ic_menu_gallery);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Builds the registration text line based on available timestamps.
     *
     * @param start registration start timestamp
     * @param deadline registration deadline timestamp
     * @param endDate event end timestamp
     * @param drawDate draw timestamp
     * @return formatted registration text
     */
    private String buildRegistrationText(Timestamp start, Timestamp deadline, Timestamp endDate, Timestamp drawDate) {
        if (start != null && deadline != null) {
            return "Registration Period: " + dateFormat.format(start.toDate()) + " - " + dateFormat.format(deadline.toDate());
        } else if (deadline != null && drawDate != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate()) + " | Draw date: " + dateFormat.format(drawDate.toDate());
        } else if (deadline != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate());
        } else if (endDate != null) {
            return "Event ends: " + dateFormat.format(endDate.toDate());
        } else {
            return "Registration details unavailable";
        }
    }

    /**
     * Returns the first non-empty string from a set of candidates.
     *
     * @param values possible string values
     * @return first non-empty string or null
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
     * Reads the entrant's current event status from entrant_events and updates the UI.
     */
    private void checkUserEventStatus() {
        DocumentReference entrantRef = db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .document(userId);

        entrantRef.get().addOnSuccessListener(documentSnapshot -> {
            resetFlags();

            if (!documentSnapshot.exists()) {
                showJoinState();
                return;
            }

            String status = InvitationFlowUtil.normalizeEntrantStatus(documentSnapshot.getString("status"));

            if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
                showInvitationButtons();
            } else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                showAcceptedState();
            } else if (InvitationFlowUtil.STATUS_DECLINED.equals(status)) {
                showDeclinedState();
            } else if (InvitationFlowUtil.STATUS_WAITING.equals(status)) {
                showWaitlistState();
            } else {
                showJoinState();
            }
        }).addOnFailureListener(e -> showJoinState());
    }

    /**
     * Resets status flags before rebuilding UI state.
     */
    private void resetFlags() {
        isInWaitlist = false;
        hasAcceptedInvite = false;
        hasDeclinedInvite = false;
    }

    /**
     * Shows accept / decline buttons for invited entrants.
     */
    private void showInvitationButtons() {
        btnWaitlistAction.setVisibility(View.GONE);
        invitationButtonsContainer.setVisibility(View.VISIBLE);
        registrationEndedContainer.setVisibility(View.GONE);
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Shows accepted state and allows cancellation of accepted membership.
     */
    private void showAcceptedState() {
        hasAcceptedInvite = true;
        btnWaitlistAction.setVisibility(View.VISIBLE);
        btnWaitlistAction.setText("Cancel Membership");
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Shows declined/cancelled state.
     */
    private void showDeclinedState() {
        hasDeclinedInvite = true;
        btnWaitlistAction.setVisibility(View.GONE);
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.VISIBLE);
        findViewById(R.id.scrollView).setAlpha(0.5f);
    }

    /**
     * Shows leave-waitlist state for entrants currently waiting.
     */
    private void showWaitlistState() {
        isInWaitlist = true;
        btnWaitlistAction.setVisibility(View.VISIBLE);
        btnWaitlistAction.setText("Leave Waitlist");
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Shows join-waitlist state for entrants not yet in entrant_events.
     */
    private void showJoinState() {
        btnWaitlistAction.setVisibility(View.VISIBLE);
        btnWaitlistAction.setText("Join Wait List");
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Accepts an invitation by changing status to accepted.
     */
    private void acceptInvitation() {
        updateStatus(InvitationFlowUtil.STATUS_ACCEPTED, "acceptedAt");
    }

    /**
     * Declines an invitation by changing status to declined.
     */
    private void declineInvitation() {
        updateStatus(InvitationFlowUtil.STATUS_DECLINED, "cancelledAt");
    }

    /**
     * Cancels an accepted membership by changing status to declined.
     */
    private void cancelAcceptedInvitation() {
        updateStatus(InvitationFlowUtil.STATUS_DECLINED, "cancelledAt");
    }

    /**
     * Updates the entrant status and timestamp field in entrant_events.
     *
     * @param status new entrant status
     * @param timeField timestamp field to set
     */
    private void updateStatus(String status, String timeField) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put(timeField, Timestamp.now());
        updates.put("updatedAt", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> refreshAll())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update event status", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Loads the count of entrants with status waiting.
     *
     * <p>IMPORTANT: Firebase data must use status = waiting for all waiting users.</p>
     */
    private void loadWaitlistCount() {
        db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    tvWaitlistCount.setText("People in Waitlist: " + count);
                });
    }

    /**
     * Shows unread notification badge if the user has unread notifications.
     */
    private void checkUnreadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    tvNotificationBadge.setVisibility(snapshot.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    /**
     * Adds the user to entrant_events with waiting status.
     */
    private void joinWaitlist() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("status", InvitationFlowUtil.STATUS_WAITING);
        data.put("joinedWaitlistAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .document(userId)
                .set(data)
                .addOnSuccessListener(unused -> refreshAll())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Removes the user from entrant_events.
     */
    private void leaveWaitlist() {
        db.collection("events")
                .document(eventId)
                .collection("entrant_events")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> refreshAll())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave waitlist", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Opens entrant home screen.
     */
    private void openHome() {
        Intent intent = new Intent(this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    /**
     * Opens notifications screen.
     */
    private void openNotifications() {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}