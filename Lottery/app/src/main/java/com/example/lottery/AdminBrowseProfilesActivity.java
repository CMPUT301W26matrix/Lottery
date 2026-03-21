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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Allows administrators to browse all user profiles in the system.
 * Includes a "one-shot" deletion mode for safety and UX consistency.
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private ListView lvProfiles;
    private TextView tvEmptyProfiles;
    private Button btnEnableDeletion;

    private ArrayList<User> users;
    private ProfileAdapter profileAdapter;

    private FirebaseFirestore db;
    private boolean isDeletionModeEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        lvProfiles = findViewById(R.id.lvProfiles);
        tvEmptyProfiles = findViewById(R.id.tvEmptyProfiles);
        btnEnableDeletion = findViewById(R.id.btnEnableDeleteProfile);

        db = FirebaseFirestore.getInstance();

        users = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, users);
        lvProfiles.setAdapter(profileAdapter);

        setupNavigation();

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

            User selectedUser = users.get(position);
            showDeleteConfirmationDialog(selectedUser);
        });

        loadProfiles();
    }

    /**
     * Centralized method to update the deletion mode state and UI.
     * @param enabled true to enter delete mode, false to return to normal mode.
     */
    private void setDeleteMode(boolean enabled) {
        this.isDeletionModeEnabled = enabled;
        if (enabled) {
            btnEnableDeletion.setText("Deletion Active");
            btnEnableDeletion.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)));
            Toast.makeText(this, "Click a profile to delete", Toast.LENGTH_SHORT).show();
        } else {
            btnEnableDeletion.setText("Enable Deletion");
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
            btnImages.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_images_coming_soon, Toast.LENGTH_SHORT).show());
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_logs_coming_soon, Toast.LENGTH_SHORT).show());
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
    }

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {
        db.collection(FirestorePaths.USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Rebuild the list from the latest Firestore snapshot each time.
                    users.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyProfiles.setText(R.string.no_user_profiles_in_the_system);
                        tvEmptyProfiles.setVisibility(View.VISIBLE);
                        lvProfiles.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");

                        // Keep the list readable even when older documents have missing fields.
                        if (name == null || name.isEmpty()) {
                            name = "Unknown User";
                        }

                        if (email == null || email.isEmpty()) {
                            email = "No email";
                        }

                        if (phone == null) {
                            phone = "";
                        }

                        users.add(new User(userId, name, email, phone));
                    }

                    tvEmptyProfiles.setVisibility(View.GONE);
                    lvProfiles.setVisibility(View.VISIBLE);
                    profileAdapter.notifyDataSetChanged();
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
     *
     * @param selectedUser user be clicked in ListView
     */
    private void showDeleteConfirmationDialog(User selectedUser) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Delete profile for " + selectedUser.getName() + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    deleteProfile(selectedUser);
                    setDeleteMode(false);
                })
                .setNegativeButton("Cancel", (dialog, which) -> setDeleteMode(false))
                .show();
    }

    /**
     * Delete profile from firebase with query of UserId
     *
     * @param selectedUser user be clicked in ListView
     */
    private void deleteProfile(User selectedUser) {
        db.collection(FirestorePaths.USERS)
                .document(selectedUser.getUserId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    loadProfiles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
}
