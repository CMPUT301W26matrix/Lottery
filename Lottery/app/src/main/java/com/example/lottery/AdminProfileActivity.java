package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminProfileActivity displays the admin's profile and allows switching between
 * entrant and organizer roles. The admin cannot edit their profile.
 * When switching to a role for the first time, a profile is created for that role.
 * Subsequent switches will directly navigate to the role's main activity.
 */
public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvAdminName, tvAdminEmail, tvActionsHeader;
    private Button btnSwitchToEntrant, btnSwitchToOrganizer, btnLogout;
    private View bottomNav;
    private FirebaseFirestore db;
    private String adminUserId;
    private String adminFid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Get admin user ID from intent or shared preferences
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        adminUserId = getIntent().getStringExtra("userId");
        if (adminUserId == null) {
            adminUserId = prefs.getString("userId", null);
        }

        // Get FID from shared preferences (stored during login)
        adminFid = prefs.getString("fid", null);

        if (adminUserId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadAdminProfile();
        setupRoleButtons();
    }

    private void initializeViews() {
        tvAdminName = findViewById(R.id.tv_profile_name);
        tvAdminEmail = findViewById(R.id.tv_profile_email);
        tvActionsHeader = findViewById(R.id.tv_actions_header);
        btnSwitchToEntrant = findViewById(R.id.btn_switch_to_entrant);
        btnSwitchToOrganizer = findViewById(R.id.btn_switch_to_organizer);
        btnLogout = findViewById(R.id.btn_log_out);
        bottomNav = findViewById(R.id.bottom_nav_container);

        // Hide any edit-related views since admin cannot edit profile
        findViewById(R.id.layout_profile_edit).setVisibility(View.GONE);
    }

    private void loadAdminProfile() {
        if (adminUserId == null) return;

        db.collection(FirestorePaths.USERS).document(adminUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");

                        tvAdminName.setText(username != null && !username.isEmpty() ? username : "Admin");
                        tvAdminEmail.setText(email != null && !email.isEmpty() ? email : "admin@example.com");
                    } else {
                        // Fallback if document doesn't exist
                        tvAdminName.setText("Admin");
                        tvAdminEmail.setText("admin@example.com");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    tvAdminName.setText("Admin");
                    tvAdminEmail.setText("admin@example.com");
                });
    }

    private void setupRoleButtons() {
        btnSwitchToEntrant.setOnClickListener(v -> {
            if (adminFid == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String entrantUserId = "entrant_" + adminFid;
            checkAndSwitchToRole(entrantUserId, "ENTRANT");
        });

        btnSwitchToOrganizer.setOnClickListener(v -> {
            if (adminFid == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String organizerUserId = "organizer_" + adminFid;
            checkAndSwitchToRole(organizerUserId, "ORGANIZER");
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    /**
     * Checks if the role profile exists. If yes, switches directly to the role's main activity.
     * If no, creates a new profile for that role and then navigates to profile completion.
     *
     * @param roleUserId The user ID for the role (e.g., "entrant_" + FID)
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     */
    private void checkAndSwitchToRole(String roleUserId, String role) {
        db.collection(FirestorePaths.USERS).document(roleUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Profile exists, check if it's complete
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");

                        if (username != null && !username.trim().isEmpty() &&
                                email != null && !email.trim().isEmpty()) {
                            // Profile is complete, navigate to main activity
                            navigateToRoleMain(role, roleUserId);
                        } else {
                            // Profile exists but incomplete, go to profile completion
                            navigateToProfileCompletion(role, roleUserId);
                        }
                    } else {
                        // Profile doesn't exist, create it and then complete profile
                        createRoleProfile(roleUserId, role);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check role profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Creates a new profile for the specified role using the same FID as the admin.
     *
     * @param roleUserId The user ID for the role
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     */
    private void createRoleProfile(String roleUserId, String role) {
        Map<String, Object> userData = new HashMap<>();
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        userData.put("userId", roleUserId);
        userData.put("role", role);
        userData.put("deviceId", adminFid);
        userData.put("username", "");
        userData.put("email", "");
        userData.put("phone", "");
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("notificationsEnabled", true);

        // Add role-specific fields if needed
        if ("ENTRANT".equalsIgnoreCase(role)) {
            userData.put("interests", new java.util.ArrayList<>());
        }

        db.collection(FirestorePaths.USERS).document(roleUserId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Created " + role + " profile. Please complete it.",
                            Toast.LENGTH_SHORT).show();
                    navigateToProfileCompletion(role, roleUserId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create " + role + " profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Navigates to the profile completion activity for the specified role.
     *
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     * @param roleUserId The user ID for the role
     */
    private void navigateToProfileCompletion(String role, String roleUserId) {
        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantProfileActivity.class);
        } else {
            intent = new Intent(this, OrganizerProfileActivity.class);
        }
        intent.putExtra("userId", roleUserId);
        intent.putExtra("forceEdit", true);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates directly to the main activity for the specified role.
     *
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     * @param roleUserId The user ID for the role
     */
    private void navigateToRoleMain(String role, String roleUserId) {
        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantMainActivity.class);
        } else {
            intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        }
        intent.putExtra("userId", roleUserId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void logout() {
        // Clear shared preferences
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Sign out from Firebase Auth (if needed)
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    }
}