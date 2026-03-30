package com.example.lottery;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.UserDeletionUtil;
import com.example.lottery.util.WaitlistPromotionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Allows administrators to browse all user profiles in the system.
 * Supports filtering by role with three options All / Entrant / Organizer.
 * Includes a "one-shot" deletion mode for safety and UX consistency.
 * When deleting an organizer, their associated events are also removed.
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private static final String TAG = "AdminBrowseProfiles";

    @VisibleForTesting
    ArrayList<User> allUsers;
    @VisibleForTesting
    ArrayList<User> filteredUsers;
    private ListView lvProfiles;
    private TextView tvEmptyProfiles;
    private Button btnEnableDeletion;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterEntrant;
    private MaterialButton btnFilterOrganizer;
    private ProfileAdapter profileAdapter;

    private FirebaseFirestore db;
    private boolean isDeletionModeEnabled = false;
    private String currentFilter = "ALL";
    private String userId;

    /**
     * Initializes the activity, binds UI views, sets up filtering and deletion controls,
     * and loads all user profiles from Firestore.
     *
     * @param savedInstanceState previously saved instance state, or null if none exists.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        lvProfiles = findViewById(R.id.lvProfiles);
        tvEmptyProfiles = findViewById(R.id.tvEmptyProfiles);
        btnEnableDeletion = findViewById(R.id.btnEnableDeleteProfile);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterEntrant = findViewById(R.id.btnFilterEntrant);
        btnFilterOrganizer = findViewById(R.id.btnFilterOrganizer);

        db = FirebaseFirestore.getInstance();

        // Get userId from intent or shared preferences
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, filteredUsers);
        lvProfiles.setAdapter(profileAdapter);

        setupNavigation();
        setupFilterButtons();

        // Simple admin-only access check
        String role = getIntent().getStringExtra("role");
        if (role == null || !role.equals("admin")) {
            Toast.makeText(this, R.string.access_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // When click button, enable deletion mode
        btnEnableDeletion.setOnClickListener(v -> setDeleteMode(true));

        // ListView record the clicked item if in deletion mode start workflow
        lvProfiles.setOnItemClickListener((parent, view, position, id) -> {
            if (!isDeletionModeEnabled) {
                return;
            }

            User selectedUser = filteredUsers.get(position);
            showDeleteConfirmationDialog(selectedUser);
        });

        // onResume handles the initial load as well as subsequent refreshes
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfiles();
    }

    /**
     * Configures the role filter buttons.
     * Sets the default selection to "All" and attaches click listeners
     * that update the current filter and refresh the displayed list.
     */
    private void setupFilterButtons() {
        highlightFilterButton(btnFilterAll);
        bindFilterButton(btnFilterAll, "ALL");
        bindFilterButton(btnFilterEntrant, "ENTRANT");
        bindFilterButton(btnFilterOrganizer, "ORGANIZER");
    }

    /**
     * Binds a filter button so that clicking it sets the current filter and refreshes the list.
     *
     * @param button the MaterialButton to bind.
     * @param filter the role filter value this button represents.
     */
    private void bindFilterButton(MaterialButton button, String filter) {
        button.setOnClickListener(v -> {
            currentFilter = filter;
            highlightFilterButton(button);
            applyFilter();
        });
    }

    /**
     * Updates filter button styles so the active button is highlighted in primary blue
     * and all others are dimmed to gray.
     *
     * @param activeButton the button to highlight as the current selection.
     */
    private void highlightFilterButton(MaterialButton activeButton) {
        int activeColor = ContextCompat.getColor(this, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_gray);

        MaterialButton[] buttons = {btnFilterAll, btnFilterEntrant, btnFilterOrganizer};
        for (MaterialButton btn : buttons) {
            btn.setTextColor(inactiveColor);
            btn.setStrokeColor(ColorStateList.valueOf(inactiveColor));
        }
        activeButton.setTextColor(activeColor);
        activeButton.setStrokeColor(ColorStateList.valueOf(activeColor));
    }

    /**
     * Filters the full user list by the current role filter and updates the ListView.
     * Shows an empty-state message when no users match the selected filter.
     */
    private void applyFilter() {
        filteredUsers.clear();

        // Populate filteredUsers based on the selected role filter
        if ("ALL".equals(currentFilter)) {
            filteredUsers.addAll(allUsers);
        } else {
            for (User user : allUsers) {
                if (currentFilter.equalsIgnoreCase(user.getRole())) {
                    filteredUsers.add(user);
                }
            }
        }

        // Toggle empty-state message and list visibility
        if (filteredUsers.isEmpty()) {
            tvEmptyProfiles.setText(R.string.no_user_profiles_in_the_system);
            tvEmptyProfiles.setVisibility(View.VISIBLE);
            lvProfiles.setVisibility(View.GONE);
        } else {
            tvEmptyProfiles.setVisibility(View.GONE);
            lvProfiles.setVisibility(View.VISIBLE);
        }
        profileAdapter.notifyDataSetChanged();
    }

    /**
     * Centralized method to update the deletion mode state and UI.
     *
     * @param enabled true to enter delete mode, false to return to normal mode.
     */
    private void setDeleteMode(boolean enabled) {
        this.isDeletionModeEnabled = enabled;
        if (enabled) {
            btnEnableDeletion.setText(R.string.deletion_active);
            btnEnableDeletion.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)));
            Toast.makeText(this, R.string.click_profile_to_delete, Toast.LENGTH_SHORT).show();
        } else {
            btnEnableDeletion.setText(R.string.enable_deletion);
            btnEnableDeletion.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_blue)));
        }
    }

    /**
     * Sets up click listeners for the admin navigation elements.
     */
    private void setupNavigation() {
        // The shared admin nav defaults to the events tab, so this screen retints it.
        highlightProfilesTab();

        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(AdminBrowseProfilesActivity.this, AdminBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("role", "admin");
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, R.string.already_viewing_profiles, Toast.LENGTH_SHORT).show());
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("userId", userId);
                intent.putExtra("role", "admin");
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
            });
        }

        View btnSettings = findViewById(R.id.nav_admin_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminProfileActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("role", "admin");
                startActivity(intent);
            });
        }
    }

    /**
     * Highlights the current profiles tab without changing the shared layout defaults.
     */
    private void highlightProfilesTab() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_gray);

        ImageView homeIcon = findViewById(R.id.nav_home_icon);
        TextView homeText = findViewById(R.id.nav_home_text);
        ImageView profilesIcon = findViewById(R.id.nav_profiles_icon);
        TextView profilesText = findViewById(R.id.nav_profiles_text);
        ImageView profileIcon = findViewById(R.id.nav_settings_icon);
        TextView profileText = findViewById(R.id.nav_settings_text);

        if (homeIcon != null) {
            homeIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        }
        if (homeText != null) {
            homeText.setTextColor(inactiveColor);
        }
        if (profilesIcon != null) {
            profilesIcon.setImageTintList(ColorStateList.valueOf(activeColor));
        }
        if (profilesText != null) {
            profilesText.setTextColor(activeColor);
        }
        if (profileIcon != null) {
            profileIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        }
        if (profileText != null) {
            profileText.setTextColor(inactiveColor);
        }
    }

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {
        db.collection(FirestorePaths.USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyProfiles.setText(R.string.no_user_profiles_in_the_system);
                        tvEmptyProfiles.setVisibility(View.VISIBLE);
                        lvProfiles.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getId();
                        String username = doc.getString("username");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String userRole = doc.getString("role");
                        if (userRole == null || userRole.isEmpty()) {
                            userRole = "ENTRANT";
                        }

                        if (username == null || username.isEmpty()) {
                            username = "Unknown User";
                        }

                        if (email == null || email.isEmpty()) {
                            email = "No email";
                        }

                        if (phone == null) {
                            phone = "";
                        }

                        User user = new User(userId, username, email, phone);
                        user.setRole(userRole);
                        allUsers.add(user);
                    }

                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    tvEmptyProfiles.setText(R.string.failed_to_load_profiles);
                    tvEmptyProfiles.setVisibility(View.VISIBLE);
                    lvProfiles.setVisibility(View.GONE);
                    Toast.makeText(AdminBrowseProfilesActivity.this, R.string.error_loading_profiles, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Show AlertDialog with user's name and ask for confirmation.
     * If the user is an organizer, warn that their events will also be deleted.
     *
     * @param selectedUser the user selected for deletion in the ListView.
     */
    @VisibleForTesting
    void showDeleteConfirmationDialog(User selectedUser) {
        String message;
        if (selectedUser.isOrganizer()) {
            message = getString(R.string.confirm_delete_organizer, selectedUser.getUsername());
        } else {
            message = getString(R.string.confirm_delete_profile_message, selectedUser.getUsername());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_profile)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    deleteProfile(selectedUser);
                    setDeleteMode(false);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> setDeleteMode(false))
                .show();
    }

    /**
     * Delete profile from firebase. If the user is an organizer, also delete their events first.
     *
     * @param selectedUser the user whose profile should be deleted.
     */
    private void deleteProfile(User selectedUser) {
        if (selectedUser.isAdmin()) {
            Toast.makeText(this, "Admin accounts cannot be removed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedUser.isOrganizer()) {
            deleteOrganizerAndEvents(selectedUser);
        } else {
            deleteEntrantAndRelatedData(selectedUser);
        }
    }

    /**
     * Removes entrant-specific records before deleting the user document itself.
     * Calls deleteUserDocument directly since that method already handles
     * waitingList, inbox, and co-organizer cleanup for all user types.
     */
    private void deleteEntrantAndRelatedData(User entrant) {
        deleteUserDocument(entrant);
    }

    private void deleteWaitingListEntriesForUser(String userId, Consumer<Boolean> onComplete) {
        db.collectionGroup(FirestorePaths.WAITING_LIST)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }

                    // Collect eventIds where this user held an active spot
                    List<String> eventsNeedingPromotion = new ArrayList<>();
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(
                                doc.getString("status"));
                        if (InvitationFlowUtil.STATUS_INVITED.equals(status)
                                || InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                            // Extract eventId from path: events/{eventId}/waitingList/{userId}
                            String eventId = doc.getReference().getParent().getParent().getId();
                            eventsNeedingPromotion.add(eventId);
                        }
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                for (String eventId : eventsNeedingPromotion) {
                                    WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(
                                            db, eventId);
                                }
                                onComplete.accept(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete waiting list records for " + userId, e);
                                onComplete.accept(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query waiting list records for " + userId, e);
                    onComplete.accept(false);
                });
    }

    /**
     * Deletes all events created by the given organizer, then deletes
     * the organizer's profile.
     *
     * @param organizer the organizer whose events and profile should be removed.
     */
    private void deleteOrganizerAndEvents(User organizer) {
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", organizer.getUserId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        deleteUserDocument(organizer);
                        return;
                    }

                    List<String> eventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        eventIds.add(doc.getId());
                    }
                    deleteEventsSequentially(eventIds, 0, organizer);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_delete_organizer_events,
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Walks through the event list one-by-one, running the full 5-step cleanup
     * for each event before moving to the next.
     */
    private void deleteEventsSequentially(List<String> eventIds, int index, User organizer) {
        if (index >= eventIds.size()) {
            deleteUserDocument(organizer);
            return;
        }
        String eventId = eventIds.get(index);
        fullDeleteEvent(eventId, success -> {
            if (!success) {
                Log.e(TAG, "Failed to fully delete event " + eventId + ", continuing with next");
            }
            deleteEventsSequentially(eventIds, index + 1, organizer);
        });
    }

    /**
     * Performs the full 5-step event deletion, mirroring
     * {@link AdminEventDetailsActivity#deleteEvent()}.
     * <p>
     * Steps: collectAffectedUserIds, deleteSubCollections,
     * deleteInboxEntries, deleteEventNotifications,
     * readAndDeletePoster, deleteEventDocument
     */
    private void fullDeleteEvent(String eventId, Consumer<Boolean> onComplete) {
        // Step 1
        collectAffectedUserIds(eventId, userIds -> {
            // Step 2
            deleteSubCollections(eventId, subOk -> {
                if (!subOk) {
                    Log.e(TAG, "Failed to clean sub-collections for event " + eventId);
                    onComplete.accept(false);
                    return;
                }
                // Step 3
                deleteInboxEntriesForUsers(eventId, userIds, inboxOk -> {
                    if (!inboxOk) {
                        Log.e(TAG, "Failed to clean inbox entries for event " + eventId);
                        onComplete.accept(false);
                        return;
                    }
                    // Step 4
                    deleteEventNotifications(eventId, notifOk -> {
                        if (!notifOk) {
                            Log.e(TAG, "Failed to clean notifications for event " + eventId);
                            onComplete.accept(false);
                            return;
                        }
                        // Step 5
                        readAndDeletePoster(eventId, () ->
                                deleteEventDocument(eventId, onComplete));
                    });
                });
            });
        });
    }

    private void collectAffectedUserIds(String eventId, Consumer<Set<String>> onComplete) {
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

    private void deleteInboxEntriesForUsers(String eventId, Set<String> userIds, Consumer<Boolean> onComplete) {
        if (userIds.isEmpty()) {
            onComplete.accept(true);
            return;
        }
        AtomicInteger processed = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        int total = userIds.size();
        for (String userId : userIds) {
            db.collection(FirestorePaths.userInbox(userId))
                    .whereEqualTo("eventId", eventId).get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            if (processed.incrementAndGet() == total)
                                onComplete.accept(!hasFailure.get());
                            return;
                        }
                        AtomicInteger deleted = new AtomicInteger(0);
                        int docTotal = snap.size();
                        for (QueryDocumentSnapshot doc : snap) {
                            doc.getReference().delete().addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) hasFailure.set(true);
                                if (deleted.incrementAndGet() == docTotal && processed.incrementAndGet() == total)
                                    onComplete.accept(!hasFailure.get());
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        hasFailure.set(true);
                        if (processed.incrementAndGet() == total) onComplete.accept(false);
                    });
        }
    }

    private void deleteSubCollections(String eventId, Consumer<Boolean> onComplete) {
        AtomicInteger done = new AtomicInteger(0);
        AtomicBoolean allOk = new AtomicBoolean(true);
        Consumer<Boolean> each = ok -> {
            if (!ok) allOk.set(false);
            if (done.incrementAndGet() == 3) onComplete.accept(allOk.get());
        };
        deleteAllDocuments(db.collection(FirestorePaths.eventWaitingList(eventId)), each);
        deleteAllDocuments(db.collection(FirestorePaths.eventCoOrganizers(eventId)), each);
        deleteAllDocuments(db.collection(FirestorePaths.eventComments(eventId)), each);
    }

    private void deleteAllDocuments(CollectionReference colRef, Consumer<Boolean> onComplete) {
        colRef.get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }
                    int total = snap.size();
                    AtomicInteger completed = new AtomicInteger(0);
                    AtomicBoolean hasFailure = new AtomicBoolean(false);
                    for (QueryDocumentSnapshot doc : snap) {
                        doc.getReference().delete().addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) hasFailure.set(true);
                            if (completed.incrementAndGet() == total)
                                onComplete.accept(!hasFailure.get());
                        });
                    }
                })
                .addOnFailureListener(e -> onComplete.accept(false));
    }

    private void deleteEventNotifications(String eventId, Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.NOTIFICATIONS)
                .whereEqualTo("eventId", eventId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        onComplete.accept(true);
                        return;
                    }
                    int total = snap.size();
                    AtomicInteger completed = new AtomicInteger(0);
                    AtomicBoolean hasFailure = new AtomicBoolean(false);
                    for (QueryDocumentSnapshot notifDoc : snap) {
                        String notifId = notifDoc.getId();
                        deleteAllDocuments(
                                db.collection(FirestorePaths.notificationRecipients(notifId)),
                                recipientOk -> {
                                    if (!recipientOk) hasFailure.set(true);
                                    notifDoc.getReference().delete().addOnCompleteListener(task -> {
                                        if (!task.isSuccessful()) hasFailure.set(true);
                                        if (completed.incrementAndGet() == total)
                                            onComplete.accept(!hasFailure.get());
                                    });
                                });
                    }
                })
                .addOnFailureListener(e -> onComplete.accept(false));
    }

    private void readAndDeletePoster(String eventId, Runnable onComplete) {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String posterUri = doc.exists() ? doc.getString("posterUri") : null;
                    deletePosterFromStorage(posterUri, onComplete);
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private void deletePosterFromStorage(String posterUri, Runnable onComplete) {
        if (posterUri == null || posterUri.trim().isEmpty()) {
            onComplete.run();
            return;
        }
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(posterUri).delete()
                    .addOnCompleteListener(task -> onComplete.run());
        } catch (IllegalArgumentException e) {
            onComplete.run();
        }
    }

    private void deleteEventDocument(String eventId, Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.EVENTS).document(eventId).delete()
                .addOnSuccessListener(unused -> onComplete.accept(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete event document " + eventId, e);
                    onComplete.accept(false);
                });
    }

    /**
     * Deletes the user document from Firestore and reloads the profile list on success.
     *
     * @param selectedUser the user whose document should be deleted.
     */
    private void deleteUserDocument(User selectedUser) {
        String userId = selectedUser.getUserId();
        deleteWaitingListEntriesForUser(userId, waitingListSuccess -> {
            if (!waitingListSuccess) {
                Log.w(TAG, "Failed to clean waiting list records for " + userId + " before deleting user");
            }
            deleteAllDocuments(db.collection(FirestorePaths.userInbox(userId)), inboxSuccess -> {
                if (!inboxSuccess) {
                    Log.w(TAG, "Failed to clean inbox for " + userId + " before deleting user");
                }
                UserDeletionUtil.cleanUpCoOrganizerRecords(db, userId, () ->
                        db.collection(FirestorePaths.USERS)
                                .document(userId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show();
                                    allUsers.remove(selectedUser);
                                    applyFilter();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, R.string.failed_to_delete_profile, Toast.LENGTH_SHORT).show()));
            });
        });
    }
}
