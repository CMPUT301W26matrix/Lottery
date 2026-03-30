package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.PosterImageLoader;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * AdminEventDetailsActivity displays a read-only administrator view of a specific event.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetches the event record from Firestore using the supplied event ID.</li>
 *   <li>Renders the poster, title, schedule, registration dates, and description.</li>
 *   <li>Surfaces organizer-configured requirements such as geolocation.</li>
 *   <li>Keeps the custom admin bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class AdminEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminEventDetails";
    private static final String EXTRA_EVENT_ID = "eventId";

    /**
     * Formatter used for displaying event-related date fields.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
    );

    /**
     * ImageView used for displaying the event poster.
     */
    private ImageView ivEventPoster;
    /**
     * TextViews for rendering the main event metadata and details.
     */
    private TextView tvEventTitle;
    private TextView tvScheduledDate;
    private TextView tvEventEndDate;
    private TextView tvRegistrationStart;
    private TextView tvRegistrationDeadline;
    private TextView tvDrawDate;
    private TextView tvWaitingListCapacity;
    private TextView tvEventDetails;
    private TextView tvLocationRequirement;
    private Button btnDeleteEvent;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Identifier of the event currently being displayed.
     */
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_event_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
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

        // Set click listener for the delete button
        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnComments.setOnClickListener(v -> {
            CommentBottomSheet bottomSheet = CommentBottomSheet.newInstanceForAdmin(eventId);
            bottomSheet.show(getSupportFragmentManager(), "comment_bottom_sheet");
        });

        setupNavigation();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_missing, Toast.LENGTH_SHORT).show();
            finish();
        }

        // onResume handles the initial fetch as well as subsequent refreshes
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        }
    }

    /**
     * Sets up click listeners for the admin bottom navigation bar.
     */
    private void setupNavigation() {
        View btnEvents = findViewById(R.id.nav_home);
        if (btnEvents != null) {
            btnEvents.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("role", "admin");
                startActivity(intent);
                finish();
            });
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            });
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseLogsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            });
        }
    }

    /**
     * Launches a confirmation dialog before deleting the event for confirmation.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.confirm_deletion).setMessage(R.string.confirm_delete_event)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteEvent())
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void deleteEvent() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteEvent.setEnabled(false);

        // Step 1: Collect all affected user IDs before deleting sub-collections
        collectAffectedUserIds(userIds -> {
            // Step 2: Delete sub-collections in parallel
            deleteSubCollections(subCollSuccess -> {
                if (!subCollSuccess) {
                    abortDelete(getString(R.string.failed_to_clean_event_data));
                    return;
                }
                // Step 3: Clean inbox entries for all affected users
                deleteInboxEntriesForUsers(userIds, inboxSuccess -> {
                    if (!inboxSuccess) {
                        abortDelete(getString(R.string.failed_to_clean_inbox));
                        return;
                    }
                    // Step 4: Delete notifications (recipients → parent)
                    deleteEventNotifications(notifSuccess -> {
                        if (!notifSuccess) {
                            abortDelete(getString(R.string.failed_to_clean_notifications));
                            return;
                        }
                        // Step 5: Read poster URI from Firestore and delete from Storage
                        readAndDeletePoster(this::deleteEventDocument);
                    });
                });
            });
        });
    }

    private void abortDelete(String message) {
        Log.e(TAG, message);
        runOnUiThread(() -> {
            btnDeleteEvent.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void collectAffectedUserIds(Consumer<Set<String>> onComplete) {
        Set<String> userIds = new HashSet<>();
        AtomicInteger done = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        Runnable checkDone = () -> {
            if (done.incrementAndGet() == 2) {
                if (hasFailure.get()) {
                    Log.w(TAG, "User ID collection incomplete; some inbox entries may not be cleaned");
                }
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
                        Log.w(TAG, "Failed to read waitingList for inbox cleanup", e);
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
                        Log.w(TAG, "Failed to read coOrganizers for inbox cleanup", e);
                        hasFailure.set(true);
                        checkDone.run();
                    });
        };
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "Event document missing while collecting organizer ID for inbox cleanup");
                        hasFailure.set(true);
                    } else {
                        String organizerId = documentSnapshot.getString("organizerId");
                        if (organizerId != null) {
                            userIds.add(organizerId);
                        }
                    }
                    loadParticipantIds.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to read event document for organizer inbox cleanup", e);
                    hasFailure.set(true);
                    loadParticipantIds.run();
                });
    }

    private void deleteInboxEntriesForUsers(Set<String> userIds, Consumer<Boolean> onComplete) {
        if (userIds.isEmpty()) {
            onComplete.accept(true);
            return;
        }
        AtomicInteger processed = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        int total = userIds.size();
        for (String userId : userIds) {
            db.collection(FirestorePaths.userInbox(userId))
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            if (processed.incrementAndGet() == total) {
                                onComplete.accept(!hasFailure.get());
                            }
                            return;
                        }
                        AtomicInteger deleted = new AtomicInteger(0);
                        int docTotal = snap.size();
                        for (QueryDocumentSnapshot doc : snap) {
                            doc.getReference().delete()
                                    .addOnCompleteListener(task -> {
                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "Failed to delete inbox entry for user " + userId, task.getException());
                                            hasFailure.set(true);
                                        }
                                        if (deleted.incrementAndGet() == docTotal) {
                                            if (processed.incrementAndGet() == total) {
                                                onComplete.accept(!hasFailure.get());
                                            }
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to query inbox for user " + userId, e);
                        hasFailure.set(true);
                        if (processed.incrementAndGet() == total) {
                            onComplete.accept(false);
                        }
                    });
        }
    }

    private void deleteSubCollections(Consumer<Boolean> onComplete) {
        AtomicInteger subCallsDone = new AtomicInteger(0);
        AtomicBoolean allSuccess = new AtomicBoolean(true);
        Consumer<Boolean> onEachDone = success -> {
            if (!success) {
                allSuccess.set(false);
            }
            if (subCallsDone.incrementAndGet() == 3) {
                onComplete.accept(allSuccess.get());
            }
        };
        deleteAllDocuments(db.collection(FirestorePaths.eventWaitingList(eventId)), onEachDone);
        deleteAllDocuments(db.collection(FirestorePaths.eventCoOrganizers(eventId)), onEachDone);
        deleteAllDocuments(db.collection(FirestorePaths.eventComments(eventId)), onEachDone);
    }

    private void deleteAllDocuments(CollectionReference colRef, Consumer<Boolean> onComplete) {
        colRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }
                    int total = querySnapshot.size();
                    AtomicInteger completed = new AtomicInteger(0);
                    AtomicBoolean hasFailure = new AtomicBoolean(false);
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.e(TAG, "Error deleting doc in " + colRef.getPath(), task.getException());
                                        hasFailure.set(true);
                                    }
                                    if (completed.incrementAndGet() == total) {
                                        onComplete.accept(!hasFailure.get());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching " + colRef.getPath(), e);
                    onComplete.accept(false);
                });
    }

    private void deleteEventNotifications(Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }
                    int total = querySnapshot.size();
                    AtomicInteger completed = new AtomicInteger(0);
                    AtomicBoolean hasFailure = new AtomicBoolean(false);
                    for (QueryDocumentSnapshot notifDoc : querySnapshot) {
                        String notifId = notifDoc.getId();
                        deleteAllDocuments(
                                db.collection(FirestorePaths.notificationRecipients(notifId)),
                                recipientSuccess -> {
                                    if (!recipientSuccess) {
                                        hasFailure.set(true);
                                        Log.e(TAG, "Recipients cleanup failed for " + notifId + ", skipping parent delete");
                                        if (completed.incrementAndGet() == total) {
                                            onComplete.accept(false);
                                        }
                                        return;
                                    }
                                    notifDoc.getReference().delete()
                                            .addOnCompleteListener(task -> {
                                                if (!task.isSuccessful()) {
                                                    Log.e(TAG, "Error deleting notification " + notifId, task.getException());
                                                    hasFailure.set(true);
                                                }
                                                if (completed.incrementAndGet() == total) {
                                                    onComplete.accept(!hasFailure.get());
                                                }
                                            });
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying notifications for event", e);
                    onComplete.accept(false);
                });
    }

    private void readAndDeletePoster(Runnable onComplete) {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String posterUri = doc.exists() ? doc.getString("posterUri") : null;
                    deletePosterFromStorage(posterUri, onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to read event for poster cleanup", e);
                    onComplete.run();
                });
    }

    private void deletePosterFromStorage(String posterUri, Runnable onComplete) {
        if (posterUri == null || posterUri.trim().isEmpty()) {
            onComplete.run();
            return;
        }
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(posterUri).delete()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Failed to delete poster from storage", task.getException());
                        }
                        onComplete.run();
                    });
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid poster URI, skipping storage delete", e);
            onComplete.run();
        }
    }

    /**
     * Deletes the event document after related entrant records have been removed.
     */
    private void deleteEventDocument() {
        db.collection(FirestorePaths.EVENTS)
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.event_deleted_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    btnDeleteEvent.setEnabled(true);
                    Toast.makeText(this, R.string.failed_to_delete_event, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the event details from Firestore.
     */
    private void fetchEventDetails() {
        db.collection(FirestorePaths.EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Event event = documentSnapshot.toObject(Event.class);
                    if (event == null) {
                        Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateUi(event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin event details", e);
                    Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUi(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());

        tvScheduledDate.setText(event.getScheduledDateTime() != null
                ? dateFormat.format(event.getScheduledDateTime().toDate()) : "");
        tvEventEndDate.setText(event.getEventEndDateTime() != null
                ? dateFormat.format(event.getEventEndDateTime().toDate()) : "");
        tvRegistrationStart.setText(event.getRegistrationStart() != null
                ? dateFormat.format(event.getRegistrationStart().toDate()) : "");
        tvRegistrationDeadline.setText(event.getRegistrationDeadline() != null
                ? dateFormat.format(event.getRegistrationDeadline().toDate()) : "");
        tvDrawDate.setText(event.getDrawDate() != null
                ? dateFormat.format(event.getDrawDate().toDate()) : "");

        String capacityLabel = (event.getWaitingListLimit() == null)
                ? getString(R.string.unlimited)
                : String.valueOf(event.getWaitingListLimit());
        tvWaitingListCapacity.setText(capacityLabel);

        if (event.isRequireLocation()) {
            tvLocationRequirement.setVisibility(View.VISIBLE);
            tvLocationRequirement.setText(R.string.location_verification_required);
        } else {
            tvLocationRequirement.setVisibility(View.GONE);
        }

        PosterImageLoader.load(ivEventPoster, event.getPosterUri(), R.drawable.event_placeholder);
    }
}
