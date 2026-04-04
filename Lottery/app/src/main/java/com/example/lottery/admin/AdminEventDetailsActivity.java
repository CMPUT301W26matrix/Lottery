package com.example.lottery.admin;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.R;
import com.example.lottery.fragment.CommentBottomSheet;
import com.example.lottery.model.Event;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.PosterImageLoader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Activity that displays the details of a specific event from an administrator's perspective.
 * Administrators can view event information and have the authority to delete the event
 * and all its associated data (waiting lists, notifications, comments, etc.).
 */
public class AdminEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminEventDetails";
    private static final String EXTRA_EVENT_ID = "eventId";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
    );

    private ImageView ivEventPoster;
    private TextView tvEventTitle;
    private TextView tvPlace;
    private TextView tvScheduledDate;
    private TextView tvEventEndDate;
    private TextView tvRegistrationStart;
    private TextView tvRegistrationDeadline;
    private TextView tvDrawDate;
    private TextView tvWaitingListCapacity;
    private TextView tvEventDetails;
    private TextView tvLocationRequirement;
    private Button btnDeleteEvent;
    private FirebaseFirestore db;
    private String eventId;
    private String userId;

    /**
     * Initializes the activity, sets up UI components, and retrieves the event ID from the intent.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_event_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_scroll_view), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvPlace = findViewById(R.id.tvPlace);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvEventEndDate = findViewById(R.id.tvEventEndDate);
        tvRegistrationStart = findViewById(R.id.tvRegistrationStart);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvDrawDate = findViewById(R.id.tvDrawDate);
        tvWaitingListCapacity = findViewById(R.id.tvWaitingListCapacity);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        ImageButton btnComments = findViewById(R.id.btnComments);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnComments.setOnClickListener(v -> {
            CommentBottomSheet bottomSheet = CommentBottomSheet.newInstanceForAdmin(eventId);
            bottomSheet.show(getSupportFragmentManager(), "comment_bottom_sheet");
        });

        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.EVENTS, userId, true);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_missing, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Fetches the latest event details from Firestore whenever the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        }
    }

    /**
     * Displays a confirmation dialog to the administrator before proceeding with event deletion.
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_deletion)
                .setMessage(R.string.confirm_delete_event)
                .setPositiveButton(R.string.delete, (d, which) -> deleteEvent())
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();

        // Make the Delete button visually distinct (red) to match Organizer/Entrant style
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        // Soften title size slightly to match unified style (18sp)
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }

    /**
     * Initiates the multi-step deletion process for an event, including cleaning up all
     * associated sub-collections, notifications, and user inbox entries.
     */
    private void deleteEvent() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteEvent.setEnabled(false);

        collectAffectedUserIds(userIds -> {
            deleteSubCollections(subCollSuccess -> {
                if (!subCollSuccess) {
                    abortDelete(getString(R.string.failed_to_clean_event_data));
                    return;
                }
                deleteInboxEntriesForUsers(userIds, inboxSuccess -> {
                    if (!inboxSuccess) {
                        abortDelete(getString(R.string.failed_to_clean_inbox));
                        return;
                    }
                    deleteEventNotifications(notifSuccess -> {
                        if (!notifSuccess) {
                            abortDelete(getString(R.string.failed_to_clean_notifications));
                            return;
                        }
                        deleteEventDocument();
                    });
                });
            });
        });
    }

    /**
     * Handles any failure during the deletion process by logging the error and notifying the user.
     *
     * @param message The error message to display and log.
     */
    private void abortDelete(String message) {
        Log.e(TAG, message);
        runOnUiThread(() -> {
            btnDeleteEvent.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Identifies all users associated with the event (organizer, co-organizers, participants)
     * so their inbox entries and notifications can be cleaned up.
     *
     * @param onComplete Callback invoked with the set of unique user IDs found.
     */
    private void collectAffectedUserIds(Consumer<Set<String>> onComplete) {
        Set<String> userIds = new HashSet<>();
        AtomicInteger done = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        Runnable checkDone = () -> {
            if (done.incrementAndGet() == 2) {
                onComplete.accept(userIds);
            }
        };
        Runnable loadParticipantIds = () -> {
            db.collection(FirestorePaths.eventWaitingList(eventId)).get()
                    .addOnSuccessListener(snap -> {
                        for (QueryDocumentSnapshot doc : snap) {
                            userIds.add(doc.getId());
                        }
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        hasFailure.set(true);
                        checkDone.run();
                    });
            db.collection(FirestorePaths.eventCoOrganizers(eventId)).get()
                    .addOnSuccessListener(snap -> {
                        for (QueryDocumentSnapshot doc : snap) {
                            userIds.add(doc.getId());
                        }
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> {
                        hasFailure.set(true);
                        checkDone.run();
                    });
        };
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String organizerId = documentSnapshot.getString("organizerId");
                        if (organizerId != null) userIds.add(organizerId);
                    }
                    loadParticipantIds.run();
                })
                .addOnFailureListener(e -> {
                    hasFailure.set(true);
                    loadParticipantIds.run();
                });
    }

    /**
     * Removes inbox entries related to this event from the personal inboxes of all affected users.
     *
     * @param userIds    The set of user IDs to clean up.
     * @param onComplete Callback invoked with a boolean indicating success or failure.
     */
    private void deleteInboxEntriesForUsers(Set<String> userIds, Consumer<Boolean> onComplete) {
        if (userIds.isEmpty()) {
            onComplete.accept(true);
            return;
        }
        AtomicInteger doneCount = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        for (String uid : userIds) {
            db.collection(FirestorePaths.userInbox(uid))
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            if (doneCount.incrementAndGet() == userIds.size())
                                onComplete.accept(!hasFailure.get());
                            return;
                        }
                        AtomicInteger docCount = new AtomicInteger(0);
                        for (QueryDocumentSnapshot doc : snap) {
                            doc.getReference().delete().addOnCompleteListener(t -> {
                                if (!t.isSuccessful()) hasFailure.set(true);
                                if (docCount.incrementAndGet() == snap.size()) {
                                    if (doneCount.incrementAndGet() == userIds.size())
                                        onComplete.accept(!hasFailure.get());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        hasFailure.set(true);
                        if (doneCount.incrementAndGet() == userIds.size()) onComplete.accept(false);
                    });
        }
    }

    /**
     * Deletes all notifications related to this event, including their recipient sub-collections.
     *
     * @param onComplete Callback invoked with a boolean indicating success or failure.
     */
    private void deleteEventNotifications(Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }
                    AtomicInteger doneCount = new AtomicInteger(0);
                    AtomicBoolean hasFailure = new AtomicBoolean(false);
                    for (QueryDocumentSnapshot doc : snap) {
                        db.collection(FirestorePaths.notificationRecipients(doc.getId())).get()
                                .addOnSuccessListener(recSnap -> {
                                    AtomicInteger recDone = new AtomicInteger(0);
                                    if (recSnap.isEmpty()) {
                                        doc.getReference().delete().addOnCompleteListener(t -> {
                                            if (!t.isSuccessful()) hasFailure.set(true);
                                            if (doneCount.incrementAndGet() == snap.size())
                                                onComplete.accept(!hasFailure.get());
                                        });
                                    } else {
                                        for (QueryDocumentSnapshot recDoc : recSnap) {
                                            recDoc.getReference().delete().addOnCompleteListener(t -> {
                                                if (recDone.incrementAndGet() == recSnap.size()) {
                                                    doc.getReference().delete().addOnCompleteListener(t2 -> {
                                                        if (!t2.isSuccessful())
                                                            hasFailure.set(true);
                                                        if (doneCount.incrementAndGet() == snap.size())
                                                            onComplete.accept(!hasFailure.get());
                                                    });
                                                }
                                            });
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> onComplete.accept(false));
    }

    /**
     * Deletes all documents within the event's sub-collections (waiting list, co-organizers, comments).
     *
     * @param onComplete Callback invoked with a boolean indicating success or failure.
     */
    private void deleteSubCollections(Consumer<Boolean> onComplete) {
        String[] paths = {
                FirestorePaths.eventWaitingList(eventId),
                FirestorePaths.eventCoOrganizers(eventId),
                FirestorePaths.eventComments(eventId)
        };
        AtomicInteger completed = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        for (String path : paths) {
            db.collection(path).get().addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    if (completed.incrementAndGet() == paths.length)
                        onComplete.accept(!hasFailure.get());
                    return;
                }
                AtomicInteger docsDeleted = new AtomicInteger(0);
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    doc.getReference().delete().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) hasFailure.set(true);
                        if (docsDeleted.incrementAndGet() == queryDocumentSnapshots.size()) {
                            if (completed.incrementAndGet() == paths.length)
                                onComplete.accept(!hasFailure.get());
                        }
                    });
                }
            }).addOnFailureListener(e -> {
                hasFailure.set(true);
                if (completed.incrementAndGet() == paths.length) onComplete.accept(false);
            });
        }
    }

    /**
     * Final step of the deletion process: deletes the main event document from Firestore.
     */
    private void deleteEventDocument() {
        db.collection(FirestorePaths.EVENTS).document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.event_deleted_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> abortDelete(getString(R.string.failed_to_delete_event)));
    }

    /**
     * Fetches the event details from the Firestore database.
     */
    private void fetchEventDetails() {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) displayEventDetails(event);
                    } else {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event details", e);
                    Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Populates the activity's views with the data from the provided Event object.
     *
     * @param event The Event object containing the details to display.
     */
    private void displayEventDetails(Event event) {
        tvEventTitle.setText(event.getTitle());

        if (event.getPlace() != null && !event.getPlace().isEmpty()) {
            tvPlace.setText(event.getPlace());
            tvPlace.setVisibility(View.VISIBLE);
        } else {
            tvPlace.setVisibility(View.GONE);
        }

        tvScheduledDate.setText(event.getScheduledDateTime() != null ? dateFormat.format(event.getScheduledDateTime().toDate()) : "");
        tvEventEndDate.setText(event.getEventEndDateTime() != null ? dateFormat.format(event.getEventEndDateTime().toDate()) : "");
        tvRegistrationStart.setText(event.getRegistrationStart() != null ? dateFormat.format(event.getRegistrationStart().toDate()) : "");
        tvRegistrationDeadline.setText(event.getRegistrationDeadline() != null ? dateFormat.format(event.getRegistrationDeadline().toDate()) : "");
        tvDrawDate.setText(event.getDrawDate() != null ? dateFormat.format(event.getDrawDate().toDate()) : "");
        tvWaitingListCapacity.setText(event.getWaitingListLimit() != null ? String.valueOf(event.getWaitingListLimit()) : getString(R.string.unlimited));
        tvEventDetails.setText(event.getDetails());

        if (event.isRequireLocation()) {
            tvLocationRequirement.setVisibility(View.VISIBLE);
            tvLocationRequirement.setText(R.string.location_verification_required);
        } else {
            tvLocationRequirement.setVisibility(View.GONE);
        }

        PosterImageLoader.load(ivEventPoster, event.getPosterBase64(), R.drawable.event_placeholder);
    }
}
