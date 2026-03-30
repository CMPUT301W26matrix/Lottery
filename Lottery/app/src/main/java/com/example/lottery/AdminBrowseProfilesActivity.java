package com.example.lottery;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lottery.model.User;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.UserDeletionUtil;
import com.example.lottery.util.WaitlistPromotionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, filteredUsers);
        lvProfiles.setAdapter(profileAdapter);

        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.PROFILES, userId);
        setupFilterButtons();

        String role = getIntent().getStringExtra("role");
        if (role == null || !role.equals("admin")) {
            Toast.makeText(this, R.string.access_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnEnableDeletion.setOnClickListener(v -> setDeleteMode(true));

        lvProfiles.setOnItemClickListener((parent, view, position, id) -> {
            if (!isDeletionModeEnabled) return;
            User selectedUser = filteredUsers.get(position);
            showDeleteConfirmationDialog(selectedUser);
        });
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
        if ("ALL".equals(currentFilter)) {
            filteredUsers.addAll(allUsers);
        } else {
            for (User user : allUsers) {
                if (currentFilter.equalsIgnoreCase(user.getRole())) {
                    filteredUsers.add(user);
                }
            }
        }

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

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {
        db.collection(FirestorePaths.USERS).get().addOnSuccessListener(queryDocumentSnapshots -> {
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
                if (userRole == null || userRole.isEmpty()) userRole = "ENTRANT";
                if (username == null || username.isEmpty()) username = "Unknown User";
                if (email == null || email.isEmpty()) email = "No email";
                if (phone == null) phone = "";
                User user = new User(userId, username, email, phone);
                user.setRole(userRole);
                allUsers.add(user);
            }
            applyFilter();
        }).addOnFailureListener(e -> {
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
        String message = selectedUser.isOrganizer()
                ? getString(R.string.confirm_delete_organizer, selectedUser.getUsername())
                : getString(R.string.confirm_delete_profile_message, selectedUser.getUsername());

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
            deleteUserDocument(selectedUser);
        }
    }

    /**
     * Deletes waiting list entries for a specific user across all events.
     *
     * @param userId     The ID of the user whose entries should be removed.
     * @param onComplete Callback invoked with true on success, false otherwise.
     */
    private void deleteWaitingListEntriesForUser(String userId, Consumer<Boolean> onComplete) {
        db.collectionGroup(FirestorePaths.WAITING_LIST).whereEqualTo("userId", userId).get().addOnSuccessListener(snapshots -> {
            if (snapshots.isEmpty()) {
                onComplete.accept(true);
                return;
            }
            List<String> eventsNeedingPromotion = new ArrayList<>();
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : snapshots) {
                String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                if (InvitationFlowUtil.STATUS_INVITED.equals(status) || InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                    String eventId = doc.getReference().getParent().getParent().getId();
                    eventsNeedingPromotion.add(eventId);
                }
                batch.delete(doc.getReference());
            }
            batch.commit().addOnSuccessListener(unused -> {
                for (String eventId : eventsNeedingPromotion) WaitlistPromotionUtil.promoteOneFromWaitlistIfNeeded(db, eventId);
                onComplete.accept(true);
            }).addOnFailureListener(e -> onComplete.accept(false));
        }).addOnFailureListener(e -> onComplete.accept(false));
    }

    /**
     * Deletes all events created by the given organizer, then deletes the organizer's profile.
     *
     * @param organizer the organizer whose events and profile should be removed.
     */
    private void deleteOrganizerAndEvents(User organizer) {
        db.collection(FirestorePaths.EVENTS).whereEqualTo("organizerId", organizer.getUserId()).get().addOnSuccessListener(snapshots -> {
            if (snapshots.isEmpty()) {
                deleteUserDocument(organizer);
                return;
            }
            List<String> eventIds = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) eventIds.add(doc.getId());
            deleteEventsSequentially(eventIds, 0, organizer);
        }).addOnFailureListener(e -> Toast.makeText(this, R.string.failed_to_delete_organizer_events, Toast.LENGTH_SHORT).show());
    }

    /**
     * Walks through the event list one-by-one, running the full cleanup for each event
     * before moving to the next.
     */
    private void deleteEventsSequentially(List<String> eventIds, int index, User organizer) {
        if (index >= eventIds.size()) {
            deleteUserDocument(organizer);
            return;
        }
        fullDeleteEvent(eventIds.get(index), success -> deleteEventsSequentially(eventIds, index + 1, organizer));
    }

    /**
     * Performs the full event deletion, mirroring Administrative cleanup steps.
     *
     * @param eventId    The ID of the event to delete.
     * @param onComplete Callback invoked with the success status of the operation.
     */
    private void fullDeleteEvent(String eventId, Consumer<Boolean> onComplete) {
        collectAffectedUserIds(eventId, userIds ->
                deleteSubCollections(eventId, subOk ->
                        deleteInboxEntriesForUsers(eventId, userIds, inboxOk ->
                                deleteEventNotifications(eventId, notifOk ->
                                        deleteEventDocument(eventId, onComplete)))));
    }

    /**
     * Collects all user IDs associated with an event (participants, co-organizers, organizer).
     */
    private void collectAffectedUserIds(String eventId, Consumer<Set<String>> onComplete) {
        Set<String> userIds = new HashSet<>();
        AtomicInteger done = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        Runnable checkDone = () -> { if (done.incrementAndGet() == 2) onComplete.accept(userIds); };
        db.collection(FirestorePaths.EVENTS).document(eventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String organizerId = documentSnapshot.getString("organizerId");
                if (organizerId != null) userIds.add(organizerId);
            }
            db.collection(FirestorePaths.eventWaitingList(eventId)).get().addOnSuccessListener(snap -> {
                for (QueryDocumentSnapshot doc : snap) userIds.add(doc.getId());
                checkDone.run();
            }).addOnFailureListener(e -> { hasFailure.set(true); checkDone.run(); });
            db.collection(FirestorePaths.eventCoOrganizers(eventId)).get().addOnSuccessListener(snap -> {
                for (QueryDocumentSnapshot doc : snap) userIds.add(doc.getId());
                checkDone.run();
            }).addOnFailureListener(e -> { hasFailure.set(true); checkDone.run(); });
        });
    }

    /**
     * Deletes inbox entries for a specific event across multiple users.
     */
    private void deleteInboxEntriesForUsers(String eventId, Set<String> userIds, Consumer<Boolean> onComplete) {
        if (userIds.isEmpty()) { onComplete.accept(true); return; }
        AtomicInteger processed = new AtomicInteger(0);
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        int total = userIds.size();
        for (String userId : userIds) {
            db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("eventId", eventId).get().addOnSuccessListener(snap -> {
                if (snap.isEmpty()) { if (processed.incrementAndGet() == total) onComplete.accept(!hasFailure.get()); return; }
                AtomicInteger deleted = new AtomicInteger(0);
                int docTotal = snap.size();
                for (QueryDocumentSnapshot doc : snap) {
                    doc.getReference().delete().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) hasFailure.set(true);
                        if (deleted.incrementAndGet() == docTotal && processed.incrementAndGet() == total) onComplete.accept(!hasFailure.get());
                    });
                }
            }).addOnFailureListener(e -> { hasFailure.set(true); if (processed.incrementAndGet() == total) onComplete.accept(false); });
        }
    }

    /**
     * Deletes all sub-collections (waiting list, co-organizers, comments) for an event.
     */
    private void deleteSubCollections(String eventId, Consumer<Boolean> onComplete) {
        AtomicInteger done = new AtomicInteger(0);
        AtomicBoolean allOk = new AtomicBoolean(true);
        Consumer<Boolean> each = ok -> { if (!ok) allOk.set(false); if (done.incrementAndGet() == 3) onComplete.accept(allOk.get()); };
        deleteAllDocuments(db.collection(FirestorePaths.eventWaitingList(eventId)), each);
        deleteAllDocuments(db.collection(FirestorePaths.eventCoOrganizers(eventId)), each);
        deleteAllDocuments(db.collection(FirestorePaths.eventComments(eventId)), each);
    }

    /**
     * Deletes all documents within a given collection reference.
     */
    private void deleteAllDocuments(CollectionReference colRef, Consumer<Boolean> onComplete) {
        colRef.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) { onComplete.accept(true); return; }
            int total = snap.size();
            AtomicInteger completed = new AtomicInteger(0);
            AtomicBoolean hasFailure = new AtomicBoolean(false);
            for (QueryDocumentSnapshot doc : snap) {
                doc.getReference().delete().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) hasFailure.set(true);
                    if (completed.incrementAndGet() == total) onComplete.accept(!hasFailure.get());
                });
            }
        }).addOnFailureListener(e -> onComplete.accept(false));
    }

    /**
     * Deletes all notifications and their recipient records for a specific event.
     */
    private void deleteEventNotifications(String eventId, Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.NOTIFICATIONS).whereEqualTo("eventId", eventId).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) { onComplete.accept(true); return; }
            int total = snap.size();
            AtomicInteger completed = new AtomicInteger(0);
            AtomicBoolean hasFailure = new AtomicBoolean(false);
            for (QueryDocumentSnapshot notifDoc : snap) {
                String notifId = notifDoc.getId();
                deleteAllDocuments(db.collection(FirestorePaths.notificationRecipients(notifId)), recipientOk -> {
                    if (!recipientOk) hasFailure.set(true);
                    notifDoc.getReference().delete().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) hasFailure.set(true);
                        if (completed.incrementAndGet() == total) onComplete.accept(!hasFailure.get());
                    });
                });
            }
        }).addOnFailureListener(e -> onComplete.accept(false));
    }

    /**
     * Deletes the event document from Firestore.
     */
    private void deleteEventDocument(String eventId, Consumer<Boolean> onComplete) {
        db.collection(FirestorePaths.EVENTS).document(eventId).delete()
                .addOnSuccessListener(unused -> onComplete.accept(true))
                .addOnFailureListener(e -> onComplete.accept(false));
    }

    /**
     * Deletes the user document from Firestore and reloads the profile list on success.
     *
     * @param selectedUser the user whose document should be deleted.
     */
    private void deleteUserDocument(User selectedUser) {
        String userId = selectedUser.getUserId();
        deleteWaitingListEntriesForUser(userId, waitingListSuccess ->
                deleteAllDocuments(db.collection(FirestorePaths.userInbox(userId)), inboxSuccess ->
                        UserDeletionUtil.cleanUpCoOrganizerRecords(db, userId, () ->
                                db.collection(FirestorePaths.USERS).document(userId).delete().addOnSuccessListener(unused -> {
                                    Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show();
                                    allUsers.remove(selectedUser);
                                    applyFilter();
                                }).addOnFailureListener(e -> Toast.makeText(this, R.string.failed_to_delete_profile, Toast.LENGTH_SHORT).show()))));
    }
}
