package com.example.lottery;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.ConfirmationTicketGenerator;
import com.example.lottery.util.EntrantNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.WaitlistPromotionUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.IOException;
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
 *   <li>Handle location collection if required by the event.</li>
 *   <li>Allow accepted entrants to generate and open a confirmation ticket PDF.</li>
 * </ul>
 * </p>
 */
public class EntrantEventDetailsActivity extends AppCompatActivity {

    /**
     * Extra key for passing the Event ID.
     */
    public static final String EXTRA_EVENT_ID = "eventId";

    /**
     * Extra key for passing the User ID.
     */
    public static final String EXTRA_USER_ID = "userId";

    private static final String TAG = "EntrantEventDetails";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private TextView tvEventTitle;
    private TextView tvPlace;
    private TextView tvRegistrationPeriod;
    private TextView tvScheduledDate;
    private TextView tvEventEndDate;
    private TextView tvRegistrationStart;
    private TextView tvRegistrationDeadline;
    private TextView tvDrawDate;
    private TextView tvWaitlistCount;
    private TextView tvNotificationBadge;
    private TextView tvEventDescription;
    private TextView tvCoOrganizerStatus;
    private TextView btnShowMore;

    private Button btnWaitlistAction;
    private Button btnAcceptInvite;
    private Button btnDeclineInvite;
    private Button btnDownloadTicket;

    private LinearLayout invitationButtonsContainer;
    private LinearLayout registrationEndedContainer;

    private ImageButton btnClose;
    private ImageButton btnComments;
    private ImageView ivEventPoster;
    private View cvEventDescription;

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

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
    private boolean eventRequiresLocation = false;
    private boolean userGeolocationEnabled = false;
    private Integer eventWaitingListLimit;
    private Timestamp eventRegistrationDeadline;
    private boolean isDescriptionExpanded = false;

    private ListenerRegistration waitlistListener;
    private boolean isAcceptingInviteMode = false;
    /**
     * True once event metadata has been successfully loaded from Firestore.
     */
    private boolean eventDetailsLoaded = false;
    /**
     * True if the most recent event metadata fetch failed.
     */
    private boolean eventDetailsFailed = false;

    /**
     * Initializes the activity, sets up view references, and triggers initial data loading.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_event_details);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();

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
        loadUserProfileSettings();
        checkCoOrganizerStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        btnAcceptInvite.setOnClickListener(v -> handleActionWithLocationCheck(true));
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
                handleActionWithLocationCheck(false);
            }
        });

        btnDownloadTicket.setOnClickListener(v -> generateAndOpenConfirmationTicket());

        btnComments.setOnClickListener(v -> {
            CommentBottomSheet bottomSheet =
                    CommentBottomSheet.newInstance(eventId, userId, userName, isCoOrganizer);
            bottomSheet.show(getSupportFragmentManager(), "comment_bottom_sheet");
        });

        EntrantNavigationHelper.setup(this, EntrantNavigationHelper.EntrantTab.HOME, userId, true);
        btnClose.setOnClickListener(v -> finish());

        btnShowMore.setOnClickListener(v -> toggleDescription());
    }

    /**
     * Finds and stores references to views in the layout.
     */
    private void initializeViews() {
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvPlace = findViewById(R.id.tvPlace);
        tvRegistrationPeriod = findViewById(R.id.tvRegistrationPeriod);
        tvWaitlistCount = findViewById(R.id.tvWaitlistCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvEventDescription = findViewById(R.id.tvEventDescription);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvCoOrganizerStatus = findViewById(R.id.tvCoOrganizerStatus);
        btnShowMore = findViewById(R.id.btnShowMore);

        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);
        btnAcceptInvite = findViewById(R.id.btnAcceptInvite);
        btnDeclineInvite = findViewById(R.id.btnDeclineInvite);
        btnDownloadTicket = findViewById(R.id.btnDownloadTicket);

        invitationButtonsContainer = findViewById(R.id.invitationButtonsContainer);
        registrationEndedContainer = findViewById(R.id.registrationEndedContainer);

        btnClose = findViewById(R.id.btnBack);
        btnComments = findViewById(R.id.btnComments);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        cvEventDescription = findViewById(R.id.cvEventDescription);
    }

    /**
     * Toggles the description text view between collapsed and expanded states.
     */
    private void toggleDescription() {
        if (isDescriptionExpanded) {
            tvEventDescription.setMaxLines(3);
            btnShowMore.setText("Show more");
            isDescriptionExpanded = false;
        } else {
            tvEventDescription.setMaxLines(Integer.MAX_VALUE);
            btnShowMore.setText("Show less");
            isDescriptionExpanded = true;
        }
    }

    /**
     * Loads user profile settings needed for event actions.
     */
    private void loadUserProfileSettings() {
        db.collection(FirestorePaths.USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean geo = doc.getBoolean("geolocationEnabled");
                        userGeolocationEnabled = geo != null && geo;
                    }
                });
    }

    /**
     * Refreshes data when the activity returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && userId != null) {
            loadEventDetails();
            loadUserProfileSettings();
            checkCoOrganizerStatus();
            loadWaitlistCount();
            checkUnreadNotifications();
        }
    }

    /**
     * Removes active listeners when the activity stops.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (waitlistListener != null) {
            waitlistListener.remove();
            waitlistListener = null;
        }
    }    /**
     * Requests location permissions and continues the current action if permission is granted.
     */
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                        || result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                    startLocationCollection();
                } else {
                    Toast.makeText(this, "Location is required to proceed", Toast.LENGTH_LONG).show();
                }
            });

    /**
     * Fetches event metadata from Firestore and populates the UI.
     */
    private void loadEventDetails() {
        eventDetailsFailed = false;
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }
                    String title = documentSnapshot.getString("title");
                    String details = documentSnapshot.getString("details");
                    tvEventTitle.setText(title != null ? title : "");

                    String place = documentSnapshot.getString("place");
                    if (place != null && !place.trim().isEmpty()) {
                        tvPlace.setText(place);
                        tvPlace.setVisibility(View.VISIBLE);
                    } else {
                        tvPlace.setVisibility(View.GONE);
                    }

                    if (details == null || details.trim().isEmpty()) {
                        cvEventDescription.setVisibility(View.GONE);
                    } else {
                        cvEventDescription.setVisibility(View.VISIBLE);
                        tvEventDescription.setText(details);
                        // Determine if "Show more" is needed
                        tvEventDescription.post(() -> {
                            Layout layout = tvEventDescription.getLayout();
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
                                // Fallback if layout is not yet ready
                                if (tvEventDescription.getLineCount() > 3) {
                                    btnShowMore.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }

                    Timestamp start = documentSnapshot.getTimestamp("scheduledDateTime");
                    if (start != null)
                        tvScheduledDate.setText(dateFormat.format(start.toDate()));
                    Timestamp endDateTime = documentSnapshot.getTimestamp("eventEndDateTime");
                    if (endDateTime != null)
                        tvEventEndDate.setText(dateFormat.format(endDateTime.toDate()));
                    Timestamp regStart = documentSnapshot.getTimestamp("registrationStart");
                    if (regStart != null)
                        tvRegistrationStart.setText(dateFormat.format(regStart.toDate()));
                    Timestamp end = documentSnapshot.getTimestamp("registrationDeadline");
                    if (end != null) {
                        tvRegistrationPeriod.setText(
                                String.format("Deadline: %s", dateFormat.format(end.toDate()))
                        );
                        tvRegistrationDeadline.setText(dateFormat.format(end.toDate()));
                    }
                    Timestamp draw = documentSnapshot.getTimestamp("drawDate");
                    if (draw != null)
                        tvDrawDate.setText(dateFormat.format(draw.toDate()));

                    PosterImageLoader.load(
                            ivEventPoster,
                            documentSnapshot.getString("posterBase64"),
                            R.drawable.event_placeholder
                    );

                    Boolean reqLoc = documentSnapshot.getBoolean("requireLocation");
                    eventRequiresLocation = reqLoc != null && reqLoc;

                    Long wlLimit = documentSnapshot.getLong("waitingListLimit");
                    eventWaitingListLimit = wlLimit != null ? wlLimit.intValue() : null;
                    eventRegistrationDeadline = documentSnapshot.getTimestamp("registrationDeadline");

                    eventDetailsLoaded = true;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event details", e);
                    eventDetailsFailed = true;
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles an action that may require location before completion.
     *
     * @param isInviteFlow True when the user is accepting an invitation; false when joining waitlist.
     */
    private void handleActionWithLocationCheck(boolean isInviteFlow) {
        this.isAcceptingInviteMode = isInviteFlow;

        if (!eventRequiresLocation) {
            finalizeAction(null);
            return;
        }

        if (!userGeolocationEnabled) {
            showGeolocationDisabledDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("This event requires your location to proceed. Do you agree to provide it?")
                .setPositiveButton("Agree", (dialog, which) -> startLocationCollection())
                .setNegativeButton("Decline", null)
                .show();
    }

    /**
     * Shows a dialog explaining that geolocation must be enabled first.
     */
    private void showGeolocationDisabledDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Geolocation Disabled")
                .setMessage("This event requires geolocation. Please enable it in your Profile preferences to proceed.")
                .setPositiveButton("Go to Profile", (dialog, which) -> {
                    Intent intent = new Intent(this, EntrantProfileActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Starts location collection or requests permission if needed.
     */
    private void startLocationCollection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, this::finalizeAction)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get fresh location", e);
                        finalizeAction(null);
                    });
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Finalizes the current action once optional location collection is complete.
     *
     * @param location The collected location, or null if unavailable.
     */
    private void finalizeAction(Location location) {
        if (isAcceptingInviteMode) {
            acceptInvitation(location);
        } else {
            joinWaitlist(location);
        }
    }

    /**
     * Checks whether the current user is a co-organizer for this event.
     */
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
                        btnDownloadTicket.setVisibility(View.GONE);
                    } else {
                        if (tvCoOrganizerStatus != null) {
                            tvCoOrganizerStatus.setVisibility(View.GONE);
                        }
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
                        String status =
                                InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));

                        isInvited = InvitationFlowUtil.STATUS_INVITED.equals(status);
                        hasAcceptedInvite = InvitationFlowUtil.STATUS_ACCEPTED.equals(status);
                        isCancelled = InvitationFlowUtil.STATUS_CANCELLED.equals(status);
                        isInWaitlist = InvitationFlowUtil.STATUS_WAITLISTED.equals(status);

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
        if (isCoOrganizer) {
            btnDownloadTicket.setVisibility(View.GONE);
            return;
        }

        btnDownloadTicket.setVisibility(hasAcceptedInvite ? View.VISIBLE : View.GONE);

        if (isInvited) {
            btnWaitlistAction.setVisibility(View.GONE);
            invitationButtonsContainer.setVisibility(View.VISIBLE);
            registrationEndedContainer.setVisibility(View.GONE);
        } else if (hasAcceptedInvite) {
            invitationButtonsContainer.setVisibility(View.GONE);
            btnWaitlistAction.setVisibility(View.VISIBLE);
            btnWaitlistAction.setText(R.string.cancel_event_membership);
            registrationEndedContainer.setVisibility(View.GONE);
        } else if (isCancelled) {
            invitationButtonsContainer.setVisibility(View.GONE);
            btnWaitlistAction.setVisibility(View.GONE);
            registrationEndedContainer.setVisibility(View.VISIBLE);
        } else {
            btnWaitlistAction.setVisibility(View.VISIBLE);
            btnWaitlistAction.setText(isInWaitlist
                    ? R.string.leave_wait_list
                    : R.string.join_wait_list);
            registrationEndedContainer.setVisibility(View.GONE);
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
     * Generates a confirmation ticket PDF and opens it using an external PDF viewer.
     */
    private void generateAndOpenConfirmationTicket() {
        if (!eventDetailsLoaded) {
            Toast.makeText(this,
                    eventDetailsFailed ? R.string.event_details_load_failed : R.string.event_details_loading,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String eventTitle = tvEventTitle.getText() != null
                    ? tvEventTitle.getText().toString()
                    : "";

            File pdfFile = ConfirmationTicketGenerator.generateTicket(
                    this,
                    eventTitle,
                    userName,
                    eventId
            );

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            Intent openPdfIntent = new Intent(Intent.ACTION_VIEW);
            openPdfIntent.setDataAndType(pdfUri, "application/pdf");
            openPdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            openPdfIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            Intent chooser = Intent.createChooser(openPdfIntent, getString(R.string.open_confirmation_ticket));
            startActivity(chooser);

            Toast.makeText(this, R.string.ticket_generated, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to generate confirmation ticket", e);
            Toast.makeText(this, R.string.ticket_generation_failed, Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No app available to open PDF", e);
            Toast.makeText(this, R.string.ticket_no_pdf_app, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Performs the accept-invitation action by updating the Firestore record.
     *
     * @param location The user's current location if required by the event, otherwise null.
     */
    private void acceptInvitation(Location location) {
        Map<String, Object> updates;

        if (isInvited && !wasWaitlisted) {
            updates = InvitationFlowUtil.buildWaitlistJoinUpdate();
        } else {
            updates = InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(
                    InvitationFlowUtil.RESPONSE_ACCEPTED
            );
        }

        if (location != null) {
            updates.put("location", new GeoPoint(location.getLatitude(), location.getLongitude()));
        }

        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    if (!wasWaitlisted) {
                        Toast.makeText(this, R.string.joined_waitlist, Toast.LENGTH_SHORT).show();
                    }
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
                    if (e != null || queryDocumentSnapshots == null) {
                        return;
                    }
                    waitlistCount = queryDocumentSnapshots.size();
                    tvWaitlistCount.setText(getString(R.string.people_in_waitlist, waitlistCount));
                });
    }

    /**
     * Checks if the user has any unread notifications in their inbox.
     */
    private void checkUnreadNotifications() {
        db.collection(FirestorePaths.userInbox(userId))
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (tvNotificationBadge != null) {
                        tvNotificationBadge.setVisibility(
                                querySnapshot.isEmpty() ? View.GONE : View.VISIBLE
                        );
                    }
                });
    }

    /**
     * Joins the current entrant to the waitlist for the event.
     *
     * @param location The user's current location if required by the event, otherwise null.
     */
    private void joinWaitlist(Location location) {
        if (isCoOrganizer) {
            Toast.makeText(this, "Co-organizers cannot join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!eventDetailsLoaded) {
            Toast.makeText(this, "Loading event details, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventRegistrationDeadline != null && eventRegistrationDeadline.toDate().before(new java.util.Date())) {
            Toast.makeText(this, "Registration deadline has passed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventWaitingListLimit != null && waitlistCount >= eventWaitingListLimit) {
            Toast.makeText(this, "Waiting list is full", Toast.LENGTH_SHORT).show();
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

        if (location != null) {
            record.setLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
        }

        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .set(record)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = true;
                    updateUIBasedOnStatus();
                    Toast.makeText(this, R.string.joined_waitlist, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Declines the current invitation and triggers replacement promotion if needed.
     */
    private void declineInvitation() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .update(InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(
                        InvitationFlowUtil.RESPONSE_DECLINED
                ))
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(db, eventId);
                });
    }

    /**
     * Cancels a previously accepted invitation and triggers replacement promotion if needed.
     */
    private void cancelAcceptedInvitation() {
        db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId)
                .update(InvitationFlowUtil.buildCancelledEntrantUpdate())
                .addOnSuccessListener(unused -> {
                    checkUserEventStatus();
                    WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(db, eventId);
                });
    }

    /**
     * Removes the current entrant from the waitlist for the event.
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


}
