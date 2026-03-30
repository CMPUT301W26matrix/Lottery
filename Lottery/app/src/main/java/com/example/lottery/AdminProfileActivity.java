package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private String adminDeviceId;

    /**
     * Initializes the activity, loads admin profile data, and sets up role switching buttons
     * and bottom navigation.
     *
     * @param savedInstanceState previously saved instance state, or null if not exist.
     */
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

        // Get device ID from shared preferences (stored during admin login)
        adminDeviceId = prefs.getString("deviceId", null);

        if (adminUserId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadAdminProfile();
        setupRoleButtons();
        setupNavigation();
    }

    /**
     * Binds UI views and hides the edit layout since admins cannot edit their profile.
     */
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

    /**
     * Loads the admin's profile data (name and email) from Firestore and updates the UI.
     * Falls back to default "Admin" values if the document does not exist or the query fails.
     */
    private void loadAdminProfile() {
        if (adminUserId == null) return;

        db.collection(FirestorePaths.USERS).document(adminUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");

                        tvAdminName.setText(username != null && !username.isEmpty() ? username : "Admin");
                        tvAdminEmail.setText(email != null && !email.isEmpty() ? email : "admin@matrix.ca");
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

    /**
     * Sets up click listeners for the "Switch to Entrant" and "Switch to Organizer" buttons,
     * as well as the logout button. Admin role profiles use an "admin_" prefix to avoid
     * collisions with regular device-bound user profiles.
     */
    private void setupRoleButtons() {
        btnSwitchToEntrant.setOnClickListener(v -> {
            if (adminDeviceId == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String entrantUserId = "admin_entrant_" + adminDeviceId;
            checkAndSwitchToRole(entrantUserId, "ENTRANT");
        });

        btnSwitchToOrganizer.setOnClickListener(v -> {
            if (adminDeviceId == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String organizerUserId = "admin_organizer_" + adminDeviceId;
            checkAndSwitchToRole(organizerUserId, "ORGANIZER");
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    /**
     * Checks if the role profile exists. If yes, switches directly to the role's main activity.
     * If no, creates a new profile for that role and then navigates to profile completion.
     *
     * @param roleUserId The user ID for the role (e.g., "entrant_" + ANDROID_ID)
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
     * Creates a new profile for the specified role using the same device ID as the admin.
     *
     * @param roleUserId The user ID for the role
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     */
    private void createRoleProfile(String roleUserId, String role) {
        Map<String, Object> userData = new HashMap<>();
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        userData.put("userId", roleUserId);
        userData.put("role", role);
        userData.put("deviceId", adminDeviceId);
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
        // Set admin role session so profile activities can return to admin
        AdminRoleManager.setAdminRoleSession(this, adminUserId);

        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantProfileActivity.class);
        } else {
            intent = new Intent(this, OrganizerProfileActivity.class);
        }
        intent.putExtra("userId", roleUserId);
        intent.putExtra("forceEdit", true);
        intent.putExtra("isAdminRole", true);
        startActivity(intent);
    }

    /**
     * Navigates directly to the main activity for the specified role.
     *
     * @param role The role name ("ENTRANT" or "ORGANIZER")
     * @param roleUserId The user ID for the role
     */
    private void navigateToRoleMain(String role, String roleUserId) {
        // Set the admin role session flag before navigating
        AdminRoleManager.setAdminRoleSession(this, adminUserId);

        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantMainActivity.class);
        } else {
            intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        }
        intent.putExtra("userId", roleUserId);
        intent.putExtra("isAdminRole", true);
        startActivity(intent);
    }

    /**
     * Clears session data, signs out from Firebase Auth, and navigates back to MainActivity.
     */
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

    /**
     * Sets up click listeners for the bottom navigation bar and highlights the settings tab.
     */
    private void setupNavigation() {
        highlightSettingsTab();

        // View Events (HOME)
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("role", "admin");
            intent.putExtra("userId", adminUserId);
            startActivity(intent);
        });

        // View Profiles
        findViewById(R.id.nav_profiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("role", "admin");
            intent.putExtra("userId", adminUserId);
            startActivity(intent);
        });

        // View Images
        findViewById(R.id.nav_images).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("role", "admin");
            intent.putExtra("userId", adminUserId);
            startActivity(intent);
        });

        // View Logs
        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseLogsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        // Settings (CURRENT SCREEN)
        findViewById(R.id.nav_admin_settings).setOnClickListener(v -> {
            // Already here — do nothing
        });
    }

    /**
     * Highlights the settings tab in the bottom navigation and dims all other tabs.
     */
    private void highlightSettingsTab() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_gray);

        ImageView homeIcon = findViewById(R.id.nav_home_icon);
        TextView homeText = findViewById(R.id.nav_home_text);
        ImageView profilesIcon = findViewById(R.id.nav_profiles_icon);
        TextView profilesText = findViewById(R.id.nav_profiles_text);
        ImageView imagesIcon = findViewById(R.id.nav_images_icon);
        TextView imagesText = findViewById(R.id.nav_images_text);
        ImageView logsIcon = findViewById(R.id.nav_logs_icon);
        TextView logsText = findViewById(R.id.nav_logs_text);
        ImageView settingsIcon = findViewById(R.id.nav_settings_icon);
        TextView settingsText = findViewById(R.id.nav_settings_text);

        if (homeIcon != null) homeIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        if (homeText != null) homeText.setTextColor(inactiveColor);
        if (profilesIcon != null) profilesIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        if (profilesText != null) profilesText.setTextColor(inactiveColor);
        if (imagesIcon != null) imagesIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        if (imagesText != null) imagesText.setTextColor(inactiveColor);
        if (logsIcon != null) logsIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        if (logsText != null) logsText.setTextColor(inactiveColor);
        if (settingsIcon != null) settingsIcon.setImageTintList(ColorStateList.valueOf(activeColor));
        if (settingsText != null) settingsText.setTextColor(activeColor);
    }
}
