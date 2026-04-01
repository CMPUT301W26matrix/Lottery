package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.AvatarUtils;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private View bottomNav;
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

                    displayProfileImage(savedImageBase64, ivProfileImage, ivProfilePlaceholder, avatarSeed);
                    displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, avatarSeed);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void displayProfileImage(String base64String, ImageView imageView, ImageView placeholderView, String seed) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    imageView.setImageBitmap(decodedByte);
                    imageView.setVisibility(View.VISIBLE);
                    if (placeholderView != null) placeholderView.setVisibility(View.GONE);
                } else {
                    showDefaultPlaceholder(imageView, placeholderView, seed);
                }
            } catch (Exception e) {
                showDefaultPlaceholder(imageView, placeholderView, seed);
            }
        } else {
            showDefaultPlaceholder(imageView, placeholderView, seed);
        }
    }

    private void showDefaultPlaceholder(ImageView imageView, ImageView placeholderView, String seed) {
        Bitmap defaultAvatar = AvatarUtils.generateDefaultAvatar(seed != null ? seed : "A", 200);
        imageView.setImageBitmap(defaultAvatar);
        imageView.setVisibility(View.VISIBLE);
        if (placeholderView != null) placeholderView.setVisibility(View.GONE);
    }

    private void enterEditMode() {
        isEditing = true;
        selectedImageBase64 = null;
        displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, getAvatarSeed());
        viewContainer.setVisibility(View.GONE);
        editContainer.setVisibility(View.VISIBLE);
        toolbarEdit.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.GONE);
    }

    private void exitEditMode() {
        isEditing = false;
        selectedImageBase64 = null;
        displayProfileImage(savedImageBase64, ivEditProfileImage, ivEditProfilePlaceholder, getAvatarSeed());
        viewContainer.setVisibility(View.VISIBLE);
        editContainer.setVisibility(View.GONE);
        toolbarEdit.setVisibility(View.GONE);
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
        boolean hasImage = (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) || 
                          (selectedImageBase64 == null && savedImageBase64 != null && !savedImageBase64.isEmpty());

        if (!hasImage) {
            openImagePicker();
        } else {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.layout_avatar_options_sheet, null);

            view.findViewById(R.id.ll_change_photo).setOnClickListener(v -> {
                openImagePicker();
                bottomSheetDialog.dismiss();
            });

            view.findViewById(R.id.ll_remove_photo).setOnClickListener(v -> {
                removeAvatar();
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.setContentView(view);
            bottomSheetDialog.show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void removeAvatar() {
        selectedImageBase64 = "";
        Bitmap defaultAvatar = AvatarUtils.generateDefaultAvatar(getAvatarSeed(), 200);
        ivEditProfileImage.setImageBitmap(defaultAvatar);
        ivEditProfileImage.setVisibility(View.VISIBLE);
        if (ivEditProfilePlaceholder != null) ivEditProfilePlaceholder.setVisibility(View.GONE);
        Toast.makeText(this, "Avatar marked for removal", Toast.LENGTH_SHORT).show();
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) return;

            int size = 200;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, size, size, true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

            ivEditProfileImage.setImageBitmap(scaledBitmap);
            ivEditProfileImage.setVisibility(View.VISIBLE);
            if (ivEditProfilePlaceholder != null) ivEditProfilePlaceholder.setVisibility(View.GONE);
        } catch (Exception e) {
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
        db.collection(FirestorePaths.USERS).document(roleUserId).set(userData)
                .addOnSuccessListener(aVoid -> navigateToProfileCompletion(role, roleUserId))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create role profile", Toast.LENGTH_SHORT).show());
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
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
