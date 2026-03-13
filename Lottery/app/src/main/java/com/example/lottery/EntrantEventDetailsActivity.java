package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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
 *     <li>Accept or decline event invitations (when user has won the lottery)</li>
 *     <li>View notifications related to the entrant</li>
 * </ul>
 *
 * <p>The activity expects two intent extras:
 * <ul>
 *     <li>{@link #EXTRA_EVENT_ID} – the ID of the selected event</li>
 *     <li>{@link #EXTRA_USER_ID} – the ID of the current entrant</li>
 * </ul>
 * </p>
 */
public class EntrantEventDetailsActivity extends AppCompatActivity {

    /**
     * Intent extra key used to pass the event ID to this activity.
     */
    public static final String EXTRA_EVENT_ID = "eventId";

    /**
     * Intent extra key used to pass the user ID to this activity.
     */
    public static final String EXTRA_USER_ID = "userId";
    /**
     * Date format used to display registration dates.
     */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    /**
     * Displays the event title.
     */
    private TextView tvEventTitle;
    /**
     * Displays the event registration period.
     */
    private TextView tvRegistrationPeriod;
    /**
     * Displays the number of people currently in the waitlist.
     */
    private TextView tvWaitlistCount;
    /**
     * Notification badge indicating unread notifications.
     */
    private TextView tvNotificationBadge;
    /**
     * Displays the event description.
     */
    private TextView tvEventDescription;
    /**
     * Button used to join or leave the waitlist.
     */
    private Button btnWaitlistAction;
    /**
     * Container for the accept/decline invitation buttons
     */
    private LinearLayout invitationButtonsContainer;
    /**
     * Button used to accept an event invitation
     */
    private Button btnAcceptInvite;
    /**
     * Button used to decline an event invitation
     */
    private Button btnDeclineInvite;
    /**
     * Container for the registration period ended message
     */
    private LinearLayout registrationEndedContainer;
    /**
     * Button used to open the notifications screen.
     */
    private ImageButton btnNotifications;
    /**
     * Button used to close the activity.
     */

    private LinearLayout navHome;
    // private LinearLayout navQrScan;  --> implement later
    // private LinearLayout navSettings; --> implement later

    /**
     * Button used to close the activity.
     */
    private ImageButton btnClose;
    /**
     * Displays the event poster image.
     */
    private ImageView ivEventPoster;
    /**
     * Firestore database instance used to retrieve and update event data.
     */
    private FirebaseFirestore db;
    /**
     * ID of the selected event.
     */
    private String eventId;
    /**
     * ID of the current user (entrant).
     */
    private String userId;
    /**
     * Indicates whether the current entrant is already in the waitlist.
     */
    private boolean isInWaitlist = false;
    /**
     * Number of entrants currently in the waitlist.
     */
    private int waitlistCount = 0;
    /**
     * Indicates whether the user has been invited to the event (won the lottery)
     */
    private boolean isInvited = false;
    /**
     * Indicates whether the user has accepted the invitation
     */
    private boolean hasAcceptedInvite = false;
    /**
     * Indicates whether the user has declined the invitation
     */
    private boolean hasDeclinedInvite = false;

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

        // NEW: Initialize invitation buttons and containers
        invitationButtonsContainer = findViewById(R.id.invitationButtonsContainer);
        btnAcceptInvite = findViewById(R.id.btnAcceptInvite);
        btnDeclineInvite = findViewById(R.id.btnDeclineInvite);
        registrationEndedContainer = findViewById(R.id.registrationEndedContainer);

        navHome = findViewById(R.id.nav_home);
        btnClose = findViewById(R.id.btnBack);
        ivEventPoster = findViewById(R.id.ivEventPoster);

        readIntentData();
        if (eventId == null || userId == null) {
            return;
        }

        loadEventDetails();
        checkUserEventStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        // NEW: Set up invitation button click listeners
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

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("isAnonymous", false);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
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
        checkUserEventStatus();
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
            Toast.makeText(this, R.string.missing_event_or_user_info, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
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
                        title = getString(R.string.event_details_title);
                    }

                    if (details == null || details.isEmpty()) {
                        details = getString(R.string.event_description_unavailable);
                    }

                    tvEventTitle.setText(title);
                    tvEventDescription.setText(details);
                    tvRegistrationPeriod.setText(
                            buildRegistrationText(registrationStart, registrationDeadline, eventEndDate, drawDate)
                    );

                    loadPosterImage(posterUri);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Builds a readable registration period string based on available timestamps.
     */
    private String buildRegistrationText(Timestamp start, Timestamp deadline, Timestamp endDate, Timestamp drawDate) {
        if (start != null && deadline != null) {
            return getString(R.string.registration_period_range,
                    dateFormat.format(start.toDate()), dateFormat.format(deadline.toDate()));
        } else if (deadline != null && drawDate != null) {
            return getString(R.string.registration_closes_with_draw,
                    dateFormat.format(deadline.toDate()), dateFormat.format(drawDate.toDate()));
        } else if (deadline != null) {
            return getString(R.string.registration_closes, dateFormat.format(deadline.toDate()));
        } else if (endDate != null) {
            return getString(R.string.event_ends, dateFormat.format(endDate.toDate()));
        } else {
            return getString(R.string.registration_details_unavailable);
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
     * Checks the user's relationship with the event (waitlisted, invited, accepted, declined)
     */
    private void checkUserEventStatus() {
        DocumentReference entrantRef = db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId);

        entrantRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String status = InvitationFlowUtil.normalizeEntrantStatus(documentSnapshot.getString("status"));

                // Check if user is invited (won the lottery)
                if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
                    isInWaitlist = false;
                    isInvited = true;
                    hasAcceptedInvite = false;
                    hasDeclinedInvite = false;
                    showInvitationButtons();
                }
                // Check if user has accepted the invitation
                else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                    isInWaitlist = false;
                    isInvited = false;
                    hasAcceptedInvite = true;
                    hasDeclinedInvite = false;
                    showAcceptedState();
                }
                // Check if user has declined the invitation
                else if (InvitationFlowUtil.STATUS_DECLINED.equals(status)) {
                    isInWaitlist = false;
                    isInvited = false;
                    hasAcceptedInvite = false;
                    hasDeclinedInvite = true;
                    showDeclinedState();
                }
                // Check if user is in waitlist
                else if (InvitationFlowUtil.STATUS_WAITING.equals(status)) {
                    isInWaitlist = true;
                    isInvited = false;
                    hasAcceptedInvite = false;
                    hasDeclinedInvite = false;
                    showWaitlistButton();
                } else {
                    // User has no special status
                    resetToDefaultState();
                }
            } else {
                // User is not in the entrants collection at all
                resetToDefaultState();
            }
        }).addOnFailureListener(e -> {
            // On failure, reset to default state
            resetToDefaultState();
        });
    }

    /**
     * Shows the invitation buttons (Accept/Decline) and hides the waitlist button
     */
    private void showInvitationButtons() {
        btnWaitlistAction.setVisibility(View.GONE);
        invitationButtonsContainer.setVisibility(View.VISIBLE);
        registrationEndedContainer.setVisibility(View.GONE);

        // Ensure the event details are at full opacity
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Shows the accepted state with "Cancel Event Membership" button
     */
    private void showAcceptedState() {
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        btnWaitlistAction.setVisibility(View.VISIBLE);
        btnWaitlistAction.setText(R.string.cancel_event_membership);

        // Ensure the event details are at full opacity
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Shows the declined state with dimmed opacity and registration ended message
     */
    private void showDeclinedState() {
        invitationButtonsContainer.setVisibility(View.GONE);
        btnWaitlistAction.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.VISIBLE);

        // Dim the event details
        findViewById(R.id.scrollView).setAlpha(0.5f);
    }

    /**
     * Shows the waitlist join/leave button
     */
    private void showWaitlistButton() {
        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        btnWaitlistAction.setVisibility(View.VISIBLE);

        if (isInWaitlist) {
            btnWaitlistAction.setText(R.string.leave_wait_list);
        } else {
            btnWaitlistAction.setText(R.string.join_wait_list);
        }

        // Ensure the event details are at full opacity
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Resets to default state (no special relationship with event)
     */
    private void resetToDefaultState() {
        isInWaitlist = false;
        isInvited = false;
        hasAcceptedInvite = false;
        hasDeclinedInvite = false;

        invitationButtonsContainer.setVisibility(View.GONE);
        registrationEndedContainer.setVisibility(View.GONE);
        btnWaitlistAction.setVisibility(View.VISIBLE);
        btnWaitlistAction.setText(R.string.join_wait_list);

        // Ensure the event details are at full opacity
        findViewById(R.id.scrollView).setAlpha(1.0f);
    }

    /**
     * Accepts the event invitation
     */
    private void acceptInvitation() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", InvitationFlowUtil.STATUS_ACCEPTED);
        updates.put("responseTime", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    isInvited = false;
                    hasAcceptedInvite = true;
                    hasDeclinedInvite = false;
                    showAcceptedState();
                    syncWinningNotificationDecision(InvitationFlowUtil.RESPONSE_ACCEPTED);
                    Toast.makeText(this, R.string.invitation_accepted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_accept_invitation, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Declines the event invitation
     */
    private void declineInvitation() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", InvitationFlowUtil.STATUS_DECLINED);
        updates.put("responseTime", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    isInvited = false;
                    hasAcceptedInvite = false;
                    hasDeclinedInvite = true;
                    showDeclinedState();
                    syncWinningNotificationDecision(InvitationFlowUtil.RESPONSE_REJECTED);
                    Toast.makeText(this, R.string.invitation_declined, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_decline_invitation, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Removes an accepted entrant from the event.
     */
    private void cancelAcceptedInvitation() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", InvitationFlowUtil.STATUS_DECLINED);
        updates.put("responseTime", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    isInvited = false;
                    hasAcceptedInvite = false;
                    hasDeclinedInvite = true;
                    showDeclinedState();
                    syncWinningNotificationDecision(InvitationFlowUtil.RESPONSE_CANCELLED);
                    Toast.makeText(this, R.string.membership_cancelled, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_cancel_membership, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Syncs win notifications so the notification inbox reflects decisions made in event details.
     *
     * @param response handled response value
     */
    private void syncWinningNotificationDecision(String response) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    boolean hasWinningNotifications = false;
                    Map<String, Object> updates = InvitationFlowUtil.buildHandledNotificationUpdate(response);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (!"win".equalsIgnoreCase(document.getString("type"))) {
                            continue;
                        }

                        batch.update(document.getReference(), updates);
                        hasWinningNotifications = true;
                    }

                    if (!hasWinningNotifications) {
                        checkUnreadNotifications();
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> checkUnreadNotifications())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, R.string.failed_to_update_notification, Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_notification, Toast.LENGTH_SHORT).show()
                );
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
                    tvWaitlistCount.setText(getString(R.string.people_in_waitlist, waitlistCount));
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
                    btnWaitlistAction.setText(R.string.leave_wait_list);
                    loadWaitlistCount();
                    Toast.makeText(this, R.string.joined_waitlist, Toast.LENGTH_SHORT).show();
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
                    btnWaitlistAction.setText(R.string.join_wait_list);
                    loadWaitlistCount();
                    Toast.makeText(this, R.string.left_waitlist, Toast.LENGTH_SHORT).show();
                });
    }
}
