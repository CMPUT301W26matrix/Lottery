package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.OrganizerNavigationHelper;
import com.example.lottery.util.ProfileImageHelper;
import com.example.lottery.util.UserDeletionUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity class representing the profile screen for an organizer.
 * Unified to handle both profile viewing, editing, and mandatory profile completion.
 */
public class OrganizerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvActionsHeader;
    private EditText etName, etEmail, etPhone;
    private Button btnLogout, btnEditProfile, btnSaveProfile, btnCancel, btnDeleteProfile, btnLotteryGuidelines;
    private ImageView ivProfileImage, ivProfilePlaceholder, ivEditProfileImage, ivEditProfilePlaceholder;
    private MaterialCardView cvEditProfileImage;
    private Toolbar toolbarEdit;
    private View bottomNav, topBar;
    private LinearLayout viewContainer, editContainer;
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

    /**
     * Initializes the activity, sets up the layout, and configures UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_profile);

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
        OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.PROFILE, userId);
        if (forceEdit) {
            OrganizerNavigationHelper.disableNavigation(this);
        }
        setupBackPressed();

        if (forceEdit) {
            enterEditMode();
            Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_LONG).show();
        } else {
            exitEditMode();
        }

        btnEditProfile.setOnClickListener(v -> enterEditMode());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnCancel.setOnClickListener(v -> {
            if (forceEdit) {
                Toast.makeText(this, "Profile completion is required", Toast.LENGTH_SHORT).show();
            } else {
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
            Intent intent = new Intent(this, OrganizerLotteryGuidelinesActivity.class);
            startActivity(intent);
        });

        cvEditProfileImage.setOnClickListener(v -> showAvatarOptions());
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (forceEdit) {
                    Toast.makeText(OrganizerProfileActivity.this, "Please complete your profile to continue", Toast.LENGTH_SHORT).show();
                } else if (isEditing) {
                    exitEditMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPhone = findViewById(R.id.tv_profile_phone);
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
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                savedImageBase64 = documentSnapshot.getString("profileImageBase64");

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
                    OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.PROFILE, userId);
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        updates.put("phone", phone);

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

                    SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
                    editor.putString("userName", username);
                    editor.apply();

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

        // Make the Delete button visually distinct (red)
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        // Soften title size slightly (Material default is usually 20sp)
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
        // Check if this is an admin role session
        if (AdminRoleManager.isAdminRoleSession(this)) {
            String adminUserId = AdminRoleManager.getAdminUserId(this);
            // Clear the admin role session
            AdminRoleManager.clearAdminRoleSession(this);

            // Navigate back to admin profile
            Intent intent = new Intent(this, AdminProfileActivity.class);
            intent.putExtra("userId", adminUserId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // Regular logout
            FirebaseAuth.getInstance().signOut();
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

}
