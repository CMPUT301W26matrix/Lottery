package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.WaitlistPromotionUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Activity that displays detailed information about a specific event from an entrant's perspective.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch and display event metadata (title, description, registration period, poster).</li>
 *   <li>Monitor and display the entrant's current status for the event (waitlisted, invited, accepted, etc.).</li>
 *   <li>Provide UI actions for joining/leaving the waitlist or responding to invitations.</li>
 *   <li>Manage navigation within the entrant's scope.</li>
 * </ul>
 * </p>
 */
public class EntrantEventDetailsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    /**
     * Extra key for passing the Event ID.
     */
    public static final String EXTRA_EVENT_ID = "eventId";
    /**
     * Extra key for passing the User ID.
     */
    public static final String EXTRA_USER_ID = "userId";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private TextView tvEventTitle, tvRegistrationPeriod, tvWaitlistCount, tvNotificationBadge, tvEventDescription, tvCoOrganizerStatus;
    private Button btnWaitlistAction, btnAcceptInvite, btnDeclineInvite;
    private LinearLayout invitationButtonsContainer, registrationEndedContainer;
    private View navHome, navNotifications, navQrScan, navProfile;
    private ImageButton btnClose, btnComments;
    private ImageView ivEventPoster;

    private FirebaseFirestore db;
    private String eventId;
    private String userId;
    private String userName;
    private String userEmail;

    private boolean isInWaitlist = false;
    private int waitlistCount = 0;
    private boolean isInvited = false;
    private boolean hasAcceptedInvite = false;
    private boolean isCancelled = false;
    private boolean wasWaitlisted = false;
    private boolean isCoOrganizer = false;

    private ListenerRegistration waitlistListener;

    /**
     * Initializes the activity, sets up view references, and triggers initial data loading.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b>Note: Otherwise it is null.</b>
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
        tvCoOrganizerStatus = findViewById(R.id.tvCoOrganizerStatus);
        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);

        invitationButtonsContainer = findViewById(R.id.invitationButtonsContainer);
        btnAcceptInvite = findViewById(R.id.btnAcceptInvite);
        btnDeclineInvite = findViewById(R.id.btnDeclineInvite);
        registrationEndedContainer = findViewById(R.id.registrationEndedContainer);

        navHome = findViewById(R.id.nav_home);
        navNotifications = findViewById(R.id.nav_history);
        navQrScan = findViewById(R.id.nav_qr_scan);
        navProfile = findViewById(R.id.nav_profile);
        
        btnClose = findViewById(R.id.btnBack);
        btnComments = findViewById(R.id.btnComments);
        ivEventPoster = findViewById(R.id.ivEventPoster);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }
        userName = prefs.getString("userName", "");
        userEmail = prefs.getString("userEmail", "");
        
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null || userId == null) {
            Toast.makeText(this, R.string.missing_event_or_user_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventDetails();
        checkCoOrganizerStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        btnAcceptInvite.setOnClickListener(v -> acceptInvitation());
        btnDeclineInvite.setOnClickListener(v -> declineInvitation());

        btnWaitlistAction.setOnClickListener(v -> {
            if (isCoOrganizer) {
                Toast.makeText(this, "Co-organizers cannot join the waitlist", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasAcceptedInvite) {
                cancelAcceptedInvitation();
            } else if (isInWaitlist) {
                leaveWaitlist();
            } else {
                joinWaitlist();
            }
        });

        btnComments.setOnClickListener(v -> {
            EntrantCommentBottomSheet bottomSheet = EntrantCommentBottomSheet.newInstance(eventId, userId, userName, isCoOrganizer);
            bottomSheet.show(getSupportFragmentManager(), "comment_bottom_sheet");
        });

        setupNavigation();
        btnClose.setOnClickListener(v -> finish());

        applyAccessibilityMode();
    }

    /**
     * Sets up click listeners for the custom bottom navigation bar items.
     */
    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantMainActivity.class);
                intent.putExtra("userId", userId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantEventHistoryActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        if (navQrScan != null) {
            navQrScan.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantQrScanActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        updateNavigationSelection();
    }

    /**
     * Resets the visual state of navigation items to ensure consistency.
     */
    private void updateNavigationSelection() {
        resetNavItem(findViewById(R.id.iv_nav_home), findViewById(R.id.tv_nav_home));
        resetNavItem(findViewById(R.id.iv_nav_history), findViewById(R.id.tv_nav_history));
    }

    /**
     * Helper to reset the tint and color of a navigation item.
     *
     * @param iv The icon ImageView.
     * @param tv The text label TextView.
     */
    private void resetNavItem(ImageView iv, TextView tv) {
        if (iv != null) iv.setColorFilter(getResources().getColor(R.color.text_gray));
        if (tv != null) tv.setTextColor(getResources().getColor(R.color.text_gray));
    }

    /**
     * Refreshes data when the activity returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && userId != null) {
            loadEventDetails();
            checkCoOrganizerStatus();
            loadWaitlistCount();
            checkUnreadNotifications();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (waitlistListener != null) {
            waitlistListener.remove();
            waitlistListener = null;
        }
    }

    /**
     * Fetches event metadata from Firestore and populates the UI.
     */
    private void loadEventDetails() {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;
                    tvEventTitle.setText(documentSnapshot.getString("title"));
                    tvEventDescription.setText(documentSnapshot.getString("details"));

                    Timestamp end = documentSnapshot.getTimestamp("registrationDeadline");
                    if (end != null) {
                        tvRegistrationPeriod.setText(String.format("Deadline: %s", dateFormat.format(end.toDate())));
                    }
                    PosterImageLoader.load(ivEventPoster, documentSnapshot.getString("posterUri"), R.drawable.event_placeholder);
                });
    }

    private void checkCoOrganizerStatus() {
        db.collection(FirestorePaths.eventCoOrganizers(eventId)).document(userId).get()
                .addOnSuccessListener(doc -> {
                    isCoOrganizer = doc.exists();
                    if (isCoOrganizer) {
                        if (tvCoOrganizerStatus != null) {
                            tvCoOrganizerStatus.setVisibility(View.VISIBLE);
                            tvCoOrganizerStatus.setText("You are a co-organizer for this event");
                        }
                        btnWaitlistAction.setVisibility(View.GONE);
                        invitationButtonsContainer.setVisibility(View.GONE);
                    } else {
                        if (tvCoOrganizerStatus != null) tvCoOrganizerStatus.setVisibility(View.GONE);
                        checkUserEventStatus();
                    }
                })
                .addOnFailureListener(e -> checkUserEventStatus());
    }

    /**
     * Checks the user's specific participation record for this event in Firestore.
     */
    private void checkUserEventStatus() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        isInvited = InvitationFlowUtil.STATUS_INVITED.equals(status);
                        hasAcceptedInvite = InvitationFlowUtil.STATUS_ACCEPTED.equals(status);
                        isCancelled = InvitationFlowUtil.STATUS_CANCELLED.equals(status);
                        isInWaitlist = InvitationFlowUtil.STATUS_WAITLISTED.equals(status);

                        // Check if they were ever waitlisted to distinguish US 01.05.07 from US 01.05.04
                        wasWaitlisted = doc.getTimestamp("waitlistedAt") != null;

                        updateUIBasedOnStatus();
                    } else {
                        resetToDefaultState();
                    }
                });
    }

    /**
     * Updates the visibility and text of action buttons based on the user's current event status.
     */
    private void updateUIBasedOnStatus() {
        if (isCoOrganizer) return; // Handled in checkCoOrganizerStatus

        if (isInvited) {
            btnWaitlistAction.setVisibility(View.GONE);
            invitationButtonsContainer.setVisibility(View.VISIBLE);
            registrationEndedContainer.setVisibility(View.GONE);
        } else if (hasAcceptedInvite) {
            invitationButtonsContainer.setVisibility(View.GONE);
            btnWaitlistAction.setVisibility(View.VISIBLE);
            btnWaitlistAction.setText(R.string.cancel_event_membership);
        } else if (isCancelled) {
            invitationButtonsContainer.setVisibility(View.GONE);
            btnWaitlistAction.setVisibility(View.GONE);
            registrationEndedContainer.setVisibility(View.VISIBLE);
        } else {
            btnWaitlistAction.setVisibility(View.VISIBLE);
            btnWaitlistAction.setText(isInWaitlist ? R.string.leave_wait_list : R.string.join_wait_list);
        }
    }

    /**
     * Resets the local status flags to their default (not participating) values.
     */
    private void resetToDefaultState() {
        isInWaitlist = false;
        isInvited = false;
        hasAcceptedInvite = false;
        isCancelled = false;
        wasWaitlisted = false;
        updateUIBasedOnStatus();
    }

    /**
     * Performs the "Accept Invitation" action by updating the Firestore record.
     */
    private void acceptInvitation() {
        Map<String, Object> updates;
        if (isInvited && !wasWaitlisted) {
            // US 01.05.07: Accepting an invitation to JOIN THE WAITING LIST for a private event
            updates = InvitationFlowUtil.buildWaitlistJoinUpdate();
        } else {
            // US 01.05.04: Accepting a spot after being selected from the waitlist (lottery win)
            updates = InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(InvitationFlowUtil.RESPONSE_ACCEPTED);
        }

        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    if (!wasWaitlisted) {
                        Toast.makeText(this, R.string.joined_waitlist, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Performs the "Decline Invitation" action by updating the Firestore record.
     */
    private void declineInvitation() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .update(InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(InvitationFlowUtil.RESPONSE_DECLINED))
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    // AUTO-PROMOTION: Check if someone else can take this spot
                    WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(db, eventId);
                });
    }

    /**
     * Performs the "Cancel Membership" action for a user who previously accepted an invite.
     */
    private void cancelAcceptedInvitation() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .update(InvitationFlowUtil.buildCancelledEntrantUpdate())
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    // AUTO-PROMOTION: Check if someone else can take this spot
                    WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(db, eventId);
                });
    }

    /**
     * Retrieves the current count of people in the waitlist for this event.
     */
    private void loadWaitlistCount() {
        if (waitlistListener != null) {
            waitlistListener.remove();
        }
        waitlistListener = db.collection(FirestorePaths.eventWaitingList(eventId))
                .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITLISTED)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;
                    waitlistCount = queryDocumentSnapshots.size();
                    tvWaitlistCount.setText(getString(R.string.people_in_waitlist, waitlistCount));
                });
    }

    /**
     * Checks if the user has any unread notifications in their inbox.
     */
    private void checkUnreadNotifications() {
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (tvNotificationBadge != null) {
                        tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
    }

    /**
     * Logic for joining the waitlist for the event.
     */
    private void joinWaitlist() {
        if (isCoOrganizer) {
            Toast.makeText(this, "Co-organizers cannot join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }
        Timestamp now = Timestamp.now();
        EntrantEvent record = new EntrantEvent();
        record.setUserId(userId);
        record.setUserName(userName);
        record.setEmail(userEmail);
        record.setStatus(InvitationFlowUtil.STATUS_WAITLISTED);
        record.setRegisteredAt(now);
        record.setWaitlistedAt(now);

        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .set(record)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = true;
                    updateUIBasedOnStatus();
                    Toast.makeText(this, R.string.joined_waitlist, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Logic for leaving the waitlist for the event.
     */
    private void leaveWaitlist() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    updateUIBasedOnStatus();
                    Toast.makeText(this, R.string.left_waitlist, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Applies accessibility mode styling to the event details screen.
     * Enlarges key UI elements without breaking layout.
     */
    private void applyAccessibilityMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);

        float titleSize = isEnabled ? 26f : 24f;
        float subtitleSize = isEnabled ? 15f : 13f;
        float descSize = isEnabled ? 16f : 14f;
        float buttonTextSize = isEnabled ? 18f : 16f;

        int buttonHeight = isEnabled ? dpToPx(56) : dpToPx(48);

        if (tvEventTitle != null) {
            tvEventTitle.setTextSize(titleSize);
        }

        if (tvRegistrationPeriod != null) {
            tvRegistrationPeriod.setTextSize(subtitleSize);
        }

        if (tvWaitlistCount != null) {
            tvWaitlistCount.setTextSize(subtitleSize);
        }

        if (tvEventDescription != null) {
            tvEventDescription.setTextSize(descSize);
        }

        if (tvCoOrganizerStatus != null) {
            tvCoOrganizerStatus.setTextSize(descSize);
        }

        if (btnWaitlistAction != null) {
            btnWaitlistAction.setTextSize(buttonTextSize);
            btnWaitlistAction.setMinHeight(buttonHeight);
        }

        if (btnAcceptInvite != null) {
            btnAcceptInvite.setTextSize(buttonTextSize);
            btnAcceptInvite.setMinHeight(buttonHeight);
        }

        if (btnDeclineInvite != null) {
            btnDeclineInvite.setTextSize(buttonTextSize);
            btnDeclineInvite.setMinHeight(buttonHeight);
        }

        // Bottom navigation labels
        View bottomNavContainer = findViewById(R.id.bottomNav);
        float navTextSize = isEnabled ? 14.5f : 12f;

        if (bottomNavContainer != null) {
            updateBottomNavTextRecursively(bottomNavContainer, navTextSize);
        }
    }

    /**
     * Recursively updates only the TextViews inside the bottom navigation container.
     */
    private void updateBottomNavTextRecursively(View view, float textSize) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(textSize);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateBottomNavTextRecursively(group.getChildAt(i), textSize);
            }
        }
    }


    /**
     * Converts dp to pixels.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
