package com.example.lottery.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.MainActivity;
import com.example.lottery.R;
import com.example.lottery.entrant.EntrantMainActivity;
import com.example.lottery.entrant.EntrantProfileActivity;
import com.example.lottery.organizer.OrganizerBrowseEventsActivity;
import com.example.lottery.organizer.OrganizerProfileActivity;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.ProfileImageHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminProfileActivity displays the admin's profile and allows switching between
 * entrant and organizer roles. Now supports profile editing and avatar customization.
 */
public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvAdminName, tvAdminEmail;
    private EditText etEditName, etEditEmail;
    private Button btnSwitchToEntrant, btnSwitchToOrganizer, btnLogout, btnEditProfile, btnSaveProfile, btnCancel;
    private ImageView ivProfileImage, ivProfilePlaceholder, ivEditProfileImage, ivEditProfilePlaceholder;
    private MaterialCardView cvEditProfileImage;
    private Toolbar toolbarEdit;
    private View bottomNav, topBar;
    private LinearLayout viewContainer, editContainer;
    private FirebaseFirestore db;
    private String adminUserId;
    private String adminDeviceId;

    private boolean isEditing = false;
    private String savedImageBase64 = null;
    private String selectedImageBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        processSelectedImage(imageUri);
                    }
                }
            }
    );

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

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        adminUserId = getIntent().getStringExtra("userId");
        if (adminUserId == null) {
            adminUserId = prefs.getString("userId", null);
        }
        adminDeviceId = prefs.getString("deviceId", null);

        if (adminUserId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadAdminProfile();
        setupRoleButtons();
        setupBackPressed();
        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.SETTINGS, adminUserId);

        btnEditProfile.setOnClickListener(v -> enterEditMode());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> exitEditMode());
        toolbarEdit.setNavigationOnClickListener(v -> exitEditMode());
        cvEditProfileImage.setOnClickListener(v -> showAvatarOptions());
    }

    private void initializeViews() {
        tvAdminName = findViewById(R.id.tv_profile_name);
        tvAdminEmail = findViewById(R.id.tv_profile_email);

        etEditName = findViewById(R.id.et_edit_name);
        etEditEmail = findViewById(R.id.et_edit_email);

        ivProfileImage = findViewById(R.id.iv_profile_image);
        ivProfilePlaceholder = findViewById(R.id.iv_profile_placeholder);
        ivEditProfileImage = findViewById(R.id.iv_edit_profile_image);
        ivEditProfilePlaceholder = findViewById(R.id.iv_edit_profile_placeholder);
        cvEditProfileImage = findViewById(R.id.cv_edit_profile_image);

        btnSwitchToEntrant = findViewById(R.id.btn_switch_to_entrant);
        btnSwitchToOrganizer = findViewById(R.id.btn_switch_to_organizer);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_edit);
        btnLogout = findViewById(R.id.btn_log_out);

        toolbarEdit = findViewById(R.id.toolbar_edit_profile);
        topBar = findViewById(R.id.topBar);
        bottomNav = findViewById(R.id.bottom_nav_container);
        viewContainer = findViewById(R.id.layout_profile_view_container);
        editContainer = findViewById(R.id.layout_profile_edit_container);
    }

    private void loadAdminProfile() {
        if (adminUserId == null) return;

        db.collection(FirestorePaths.USERS).document(adminUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = null;
                    String email = null;
                    savedImageBase64 = null;

                    if (documentSnapshot.exists()) {
                        username = documentSnapshot.getString("username");
                        email = documentSnapshot.getString("email");
                        savedImageBase64 = documentSnapshot.getString("profileImageBase64");
                    }

                    // Fallback to FirebaseAuth email
                    if (email == null || email.trim().isEmpty()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) email = user.getEmail();
                    }

                    String displayName = (username != null && !username.trim().isEmpty()) ? username : "Admin";
                    String displayEmail = (email != null && !email.trim().isEmpty()) ? email : "admin@matrix.ca";

                    tvAdminName.setText(displayName);
                    tvAdminEmail.setText(displayEmail);
                    etEditName.setText(displayName);
                    etEditEmail.setText(displayEmail);

                    String avatarSeed = (username != null && !username.trim().isEmpty()) ? username :
                            (email != null && !email.trim().isEmpty()) ? email : "A";

                    ProfileImageHelper.displayProfileImage(savedImageBase64, ivProfileImage, ivProfilePlaceholder, avatarSeed);
                    ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, avatarSeed);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void enterEditMode() {
        isEditing = true;
        selectedImageBase64 = null;
        ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, getAvatarSeed());
        viewContainer.setVisibility(View.GONE);
        editContainer.setVisibility(View.VISIBLE);
        toolbarEdit.setVisibility(View.VISIBLE);
        if (topBar != null) topBar.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
    }

    private void exitEditMode() {
        isEditing = false;
        selectedImageBase64 = null;
        ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, getAvatarSeed());
        viewContainer.setVisibility(View.VISIBLE);
        editContainer.setVisibility(View.GONE);
        toolbarEdit.setVisibility(View.GONE);
        if (topBar != null) topBar.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
    }

    private String getAvatarSeed() {
        String name = etEditName.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();
        if (!name.isEmpty()) return name;
        if (!email.isEmpty()) return email;
        return "A";
    }

    private void showAvatarOptions() {
        boolean hasImage = ProfileImageHelper.hasCustomImage(selectedImageBase64, savedImageBase64);
        ProfileImageHelper.showAvatarOptions(this, hasImage,
                () -> ProfileImageHelper.openImagePicker(imagePickerLauncher),
                this::removeAvatar);
    }

    private void removeAvatar() {
        selectedImageBase64 = "";
        ProfileImageHelper.showDefaultAvatar(ivEditProfileImage, ivEditProfilePlaceholder, getAvatarSeed());
        Toast.makeText(this, "Avatar marked for removal", Toast.LENGTH_SHORT).show();
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            ProfileImageHelper.ProcessedImage result =
                    ProfileImageHelper.processSelectedImage(getContentResolver(), imageUri);
            if (result != null) {
                selectedImageBase64 = result.base64;
                ivEditProfileImage.setImageBitmap(result.bitmap);
                ivEditProfileImage.setVisibility(View.VISIBLE);
                if (ivEditProfilePlaceholder != null)
                    ivEditProfilePlaceholder.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String username = etEditName.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        if (selectedImageBase64 != null) {
            if (selectedImageBase64.isEmpty()) {
                updates.put("profileImageBase64", FieldValue.delete());
            } else {
                updates.put("profileImageBase64", selectedImageBase64);
            }
        }

        db.collection(FirestorePaths.USERS).document(adminUserId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    loadAdminProfile();
                    exitEditMode();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void setupRoleButtons() {
        btnSwitchToEntrant.setOnClickListener(v -> {
            if (adminDeviceId == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String entrantUserId = "entrant_" + adminDeviceId;
            checkAndSwitchToRole(entrantUserId, "ENTRANT");
        });

        btnSwitchToOrganizer.setOnClickListener(v -> {
            if (adminDeviceId == null) {
                Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String organizerUserId = "organizer_" + adminDeviceId;
            checkAndSwitchToRole(organizerUserId, "ORGANIZER");
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void checkAndSwitchToRole(String roleUserId, String role) {
        db.collection(FirestorePaths.USERS).document(roleUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        if (username != null && !username.trim().isEmpty() &&
                                email != null && !email.trim().isEmpty()) {
                            navigateToRoleMain(role, roleUserId);
                        } else {
                            navigateToProfileCompletion(role, roleUserId);
                        }
                    } else {
                        createRoleProfile(roleUserId, role);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check role profile", Toast.LENGTH_SHORT).show());
    }

    private void createRoleProfile(String roleUserId, String role) {
        db.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(
                    db.collection(FirestorePaths.USERS).document(roleUserId));
            if (existing.exists()) {
                return existing;
            }
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
            if ("ENTRANT".equalsIgnoreCase(role)) {
                userData.put("interests", new java.util.ArrayList<>());
            }
            transaction.set(db.collection(FirestorePaths.USERS).document(roleUserId), userData);
            return null;
        }).addOnSuccessListener(result -> {
            if (result instanceof DocumentSnapshot) {
                // Document already existed — check profile completeness
                DocumentSnapshot doc = (DocumentSnapshot) result;
                String username = doc.getString("username");
                String email = doc.getString("email");
                if (username != null && !username.trim().isEmpty() &&
                        email != null && !email.trim().isEmpty()) {
                    navigateToRoleMain(role, roleUserId);
                } else {
                    navigateToProfileCompletion(role, roleUserId);
                }
            } else {
                navigateToProfileCompletion(role, roleUserId);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to create role profile", Toast.LENGTH_SHORT).show());
    }

    private void navigateToProfileCompletion(String role, String roleUserId) {
        AdminRoleManager.setAdminRoleSession(this, adminUserId);
        Intent intent = "ENTRANT".equalsIgnoreCase(role) ?
                new Intent(this, EntrantProfileActivity.class) :
                new Intent(this, OrganizerProfileActivity.class);
        intent.putExtra("userId", roleUserId);
        intent.putExtra("forceEdit", true);
        intent.putExtra("isAdminRole", true);
        startActivity(intent);
    }

    private void navigateToRoleMain(String role, String roleUserId) {
        AdminRoleManager.setAdminRoleSession(this, adminUserId);
        Intent intent = "ENTRANT".equalsIgnoreCase(role) ?
                new Intent(this, EntrantMainActivity.class) :
                new Intent(this, OrganizerBrowseEventsActivity.class);
        intent.putExtra("userId", roleUserId);
        intent.putExtra("isAdminRole", true);
        startActivity(intent);
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditing) {
                    exitEditMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Clear admin role session to prevent stale state after re-login
        AdminRoleManager.clearAdminRoleSession(this);

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
