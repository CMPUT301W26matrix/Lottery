package com.example.lottery;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

/**
 * Allows administrators to browse all user profiles in the system.
 * Supports filtering by role with three options All / Entrant / Organizer.
 * Includes a "one-shot" deletion mode for safety and UX consistency.
 * When deleting an organizer, their associated events are also removed.
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private ListView lvProfiles;
    private TextView tvEmptyProfiles;
    private Button btnEnableDeletion;

    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterEntrant;
    private MaterialButton btnFilterOrganizer;

    @VisibleForTesting
    ArrayList<User> allUsers;
    @VisibleForTesting
    ArrayList<User> filteredUsers;
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
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Click a profile to delete", Toast.LENGTH_SHORT).show();
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
                intent.putExtra("role", "admin");
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v ->
                    Toast.makeText(this, "Already viewing profiles", Toast.LENGTH_SHORT).show());
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            });
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_logs_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnProfile = findViewById(R.id.nav_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminProfileActivity.class);
                intent.putExtra("userId", userId);
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
                    Toast.makeText(AdminBrowseProfilesActivity.this, "Error loading profiles", Toast.LENGTH_SHORT).show();
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
            message = "Delete organizer \"" + selectedUser.getUsername()
                    + "\"? All events created by this organizer will also be deleted.";
        } else {
            message = "Delete profile for " + selectedUser.getUsername() + "?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    deleteProfile(selectedUser);
                    setDeleteMode(false);
                })
                .setNegativeButton("Cancel", (dialog, which) -> setDeleteMode(false))
                .show();
    }

    /**
     * Delete profile from firebase. If the user is an organizer, also delete their events first.
     *
     * @param selectedUser the user whose profile should be deleted.
     */
    private void deleteProfile(User selectedUser) {
        if (selectedUser.isOrganizer()) {
            deleteOrganizerAndEvents(selectedUser);
        } else {
            deleteUserDocument(selectedUser);
        }
    }

    /**
     * Deletes all events created by the given organizer, then deletes the organizer's profile.
     *
     * @param organizer the organizer whose events and profile should be removed.
     */
    private void deleteOrganizerAndEvents(User organizer) {
        // Query all events owned by this organizer and delete them
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", organizer.getUserId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        deleteUserDocument(organizer);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> deleteUserDocument(organizer))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete organizer's events",
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete organizer's events",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes the user document from Firestore and reloads the profile list on success.
     *
     * @param selectedUser the user whose document should be deleted.
     */
    private void deleteUserDocument(User selectedUser) {
        db.collection(FirestorePaths.USERS)
                .document(selectedUser.getUserId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    allUsers.remove(selectedUser);
                    applyFilter();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
}