package com.example.lottery.entrant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.MainActivity;
import com.example.lottery.R;
import com.example.lottery.admin.AdminProfileActivity;
import com.example.lottery.admin.AdminSignInActivity;
import com.example.lottery.organizer.OrganizerBrowseEventsActivity;
import com.example.lottery.organizer.OrganizerProfileActivity;
import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.EntrantNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.ProfileImageHelper;
import com.example.lottery.util.UserDeletionUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntrantProfileActivity displays and manages the personal profile of an entrant user.
 * US 01.02.04: Implements profile deletion and session management.
 * US 01.04.03: Implements notification opt-out settings.
 * US 02.02.02: Implements geolocation opt-in/out settings.
 */
public class EntrantProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvNotificationBadge, tvActionsHeader;
    private EditText etName, etEmail, etPhone;
    private Button btnLogout, btnEditProfile, btnSaveProfile, btnCancel, btnDeleteProfile, btnLotteryGuidelines;
    private ImageView ivProfileImage, ivProfilePlaceholder, ivEditProfileImage, ivEditProfilePlaceholder;
    private MaterialCardView cvEditProfileImage;
    private Toolbar toolbarEdit;
    private View bottomNav, topBar;
    private LinearLayout viewContainer, editContainer;
    private ChipGroup cgEditInterests, cgDisplayInterests;
    private Chip chipAcademic, chipSocial, chipSports, chipMusic;
    private SwitchMaterial swNotifications, swGeolocation;
    private FirebaseFirestore db;
    private String userId;
    private boolean isEditing = false;
    private boolean forceEdit = false;

    // Permanent storage for the currently saved profile image in base64
    private String savedImageBase64 = null;
    // Temporary storage for the newly picked image in base64. Empty string means delete.
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
        setContentView(R.layout.activity_entrant_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "Session error: missing userId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        forceEdit = getIntent().getBooleanExtra("forceEdit", false);

        initializeViews();
        loadUserProfile();
        EntrantNavigationHelper.setup(this, EntrantNavigationHelper.EntrantTab.PROFILE, userId);
        if (forceEdit) {
            EntrantNavigationHelper.disableNavigation(this);
        }
        checkUnreadNotifications();

        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (forceEdit) {
                    Toast.makeText(EntrantProfileActivity.this, "Please complete your profile to continue", Toast.LENGTH_SHORT).show();
                } else if (isEditing) {
                    exitEditMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (forceEdit) {
            enterEditMode();
        } else {
            exitEditMode();
        }

        btnEditProfile.setOnClickListener(v -> enterEditMode());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnCancel.setOnClickListener(v -> {
            if (!forceEdit) {
                exitEditMode();
            }
        });

        toolbarEdit.setNavigationOnClickListener(v -> {
            if (!forceEdit) {
                exitEditMode();
            }
        });

        btnLogout.setOnClickListener(v -> logout());

        btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnLotteryGuidelines.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantLotteryGuidelinesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_switch_to_organizer).setOnClickListener(v -> switchToRole("ORGANIZER"));
        findViewById(R.id.btn_switch_to_admin).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminSignInActivity.class));
        });

        cvEditProfileImage.setOnClickListener(v -> showAvatarOptions());
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvActionsHeader = findViewById(R.id.tv_actions_header);

        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);

        ivProfileImage = findViewById(R.id.iv_profile_image);
        ivProfilePlaceholder = findViewById(R.id.iv_profile_placeholder);
        ivEditProfileImage = findViewById(R.id.iv_edit_profile_image);
        ivEditProfilePlaceholder = findViewById(R.id.iv_edit_profile_placeholder);
        cvEditProfileImage = findViewById(R.id.cv_edit_profile_image);

        btnLogout = findViewById(R.id.btn_log_out);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_edit);
        btnDeleteProfile = findViewById(R.id.btn_delete_profile);
        btnLotteryGuidelines = findViewById(R.id.btn_lottery_guidelines);

        toolbarEdit = findViewById(R.id.toolbar_edit_profile);
        topBar = findViewById(R.id.topBar);
        bottomNav = findViewById(R.id.bottom_nav_container);

        viewContainer = findViewById(R.id.layout_profile_view_container);
        editContainer = findViewById(R.id.layout_profile_edit_container);

        cgEditInterests = findViewById(R.id.cg_edit_interests);
        cgDisplayInterests = findViewById(R.id.cg_display_interests);
        chipAcademic = findViewById(R.id.chip_interest_academic);
        chipSocial = findViewById(R.id.chip_interest_social);
        chipSports = findViewById(R.id.chip_interest_sports);
        chipMusic = findViewById(R.id.chip_interest_music);

        swNotifications = findViewById(R.id.sw_notifications);
        swNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId != null) {
                db.collection(FirestorePaths.USERS).document(userId)
                        .update("notificationsEnabled", isChecked);
            }
        });

        swGeolocation = findViewById(R.id.sw_geolocation);
        swGeolocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId != null) {
                db.collection(FirestorePaths.USERS).document(userId)
                        .update("geolocationEnabled", isChecked);
            }
        });
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                savedImageBase64 = documentSnapshot.getString("profileImageBase64");
                Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");
                Boolean geolocationEnabled = documentSnapshot.getBoolean("geolocationEnabled");

                tvName.setText(username != null && !username.isEmpty() ? username : "Unknown");
                tvEmail.setText(email != null && !email.isEmpty() ? email : "No Email");

                if (phone != null && !phone.isEmpty()) {
                    tvPhone.setText(phone);
                    tvPhone.setVisibility(View.VISIBLE);
                } else {
                    tvPhone.setVisibility(View.GONE);
                }

                etName.setText(username != null ? username : "");
                etEmail.setText(email != null ? email : "");
                etPhone.setText(phone != null ? phone : "");

                ProfileImageHelper.displayProfileImage(savedImageBase64, ivProfileImage, ivProfilePlaceholder, username);
                ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, username);

                if (forceEdit && username != null && !username.isEmpty() && email != null && !email.isEmpty()) {
                    forceEdit = false;
                    exitEditMode();
                    EntrantNavigationHelper.setup(this, EntrantNavigationHelper.EntrantTab.PROFILE, userId);
                }

                List<String> interests = (List<String>) documentSnapshot.get("interests");
                cgDisplayInterests.removeAllViews();
                if (interests != null) {
                    chipAcademic.setChecked(interests.contains("Academic"));
                    chipSocial.setChecked(interests.contains("Social"));
                    chipSports.setChecked(interests.contains("Sports"));
                    chipMusic.setChecked(interests.contains("Music"));

                    for (String interest : interests) {
                        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
                        chip.setText(interest);
                        chip.setClickable(false);
                        chip.setCheckable(false);
                        int chipHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                        chip.setChipMinHeight(chipHeight);
                        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_light_blue)));
                        chip.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue)));
                        chip.setChipStrokeWidth(0f);
                        cgDisplayInterests.addView(chip);
                    }
                }

                if (notificationsEnabled != null) {
                    swNotifications.setChecked(notificationsEnabled);
                } else {
                    swNotifications.setChecked(true); // Default to true
                }

                if (geolocationEnabled != null) {
                    swGeolocation.setChecked(geolocationEnabled);
                } else {
                    swGeolocation.setChecked(false); // Default to false
                }
            }
        });
    }

    private void enterEditMode() {
        isEditing = true;
        selectedImageBase64 = null; // Reset temp storage
        // Ensure existing saved avatar is shown correctly when entering edit mode
        ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, etName.getText().toString());

        viewContainer.setVisibility(View.GONE);
        editContainer.setVisibility(View.VISIBLE);
        toolbarEdit.setVisibility(View.VISIBLE);
        if (topBar != null) topBar.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);

        if (forceEdit) {
            toolbarEdit.setNavigationIcon(null);
            toolbarEdit.setTitle("Create account");
            btnCancel.setVisibility(View.GONE);
        } else {
            toolbarEdit.setNavigationIcon(R.drawable.ic_back);
            toolbarEdit.setTitle("Edit Profile");
            btnCancel.setVisibility(View.VISIBLE);
        }
    }

    private void exitEditMode() {
        isEditing = false;
        selectedImageBase64 = null; // Clear temp storage
        // Revert edit preview to saved state
        ProfileImageHelper.displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, etName.getText().toString());

        viewContainer.setVisibility(View.VISIBLE);
        editContainer.setVisibility(View.GONE);
        toolbarEdit.setVisibility(View.GONE);
        if (topBar != null) topBar.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
    }

    private void showAvatarOptions() {
        boolean hasImage = ProfileImageHelper.hasCustomImage(selectedImageBase64, savedImageBase64);
        ProfileImageHelper.showAvatarOptions(this, hasImage,
                () -> ProfileImageHelper.openImagePicker(imagePickerLauncher),
                this::removeAvatar);
    }

    private void removeAvatar() {
        selectedImageBase64 = "";
        ProfileImageHelper.showDefaultAvatar(ivEditProfileImage, ivEditProfilePlaceholder, etName.getText().toString());
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
                ivEditProfilePlaceholder.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String username = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedInterests = new ArrayList<>();
        if (chipAcademic.isChecked()) selectedInterests.add("Academic");
        if (chipSocial.isChecked()) selectedInterests.add("Social");
        if (chipSports.isChecked()) selectedInterests.add("Sports");
        if (chipMusic.isChecked()) selectedInterests.add("Music");

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("interests", selectedInterests);

        // Handle avatar update or removal
        if (selectedImageBase64 != null) {
            if (selectedImageBase64.isEmpty()) {
                // If marked for removal, delete the field from Firestore
                updates.put("profileImageBase64", FieldValue.delete());
            } else {
                updates.put("profileImageBase64", selectedImageBase64);
            }
        }

        db.collection(FirestorePaths.USERS).document(userId).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    if (!AdminRoleManager.isAdminRoleSession(this)) {
                        SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
                        editor.putString("userName", username);
                        editor.apply();
                    }

                    if (forceEdit) {
                        forceEdit = false;
                        navigateToMain();
                    } else {
                        loadUserProfile();
                        exitEditMode();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_profile)
                .setMessage(R.string.delete_profile_message)
                .setPositiveButton(R.string.delete_profile, (d, which) -> deleteUserProfile())
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }

    private void deleteUserProfile() {
        if (userId == null) return;

        UserDeletionUtil.cleanUpUserRecords(db, userId, () ->
                db.collection(FirestorePaths.USERS).document(userId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                            logout();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }

    private void logout() {
        if (AdminRoleManager.isAdminRoleSession(this)) {
            String adminUserId = AdminRoleManager.getAdminUserId(this);
            AdminRoleManager.clearAdminRoleSession(this);
            // Remove stale role userName from global prefs
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().remove("userName").apply();

            Intent intent = new Intent(this, AdminProfileActivity.class);
            intent.putExtra("userId", adminUserId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void switchToRole(String role) {
        String androidId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            return;
        }
        String roleUserId = role.toLowerCase() + "_" + androidId;
        db.collection(FirestorePaths.USERS).document(roleUserId).get()
                .addOnSuccessListener(doc -> {
                    Intent intent;
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String email = doc.getString("email");
                        if (username != null && !username.trim().isEmpty()
                                && email != null && !email.trim().isEmpty()) {
                            intent = new Intent(this, OrganizerBrowseEventsActivity.class);
                        } else {
                            intent = new Intent(this, OrganizerProfileActivity.class);
                            intent.putExtra("forceEdit", true);
                        }
                    } else {
                        intent = new Intent(this, OrganizerProfileActivity.class);
                        intent.putExtra("forceEdit", true);
                    }
                    intent.putExtra("userId", roleUserId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to switch role", Toast.LENGTH_SHORT).show());
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }
}
