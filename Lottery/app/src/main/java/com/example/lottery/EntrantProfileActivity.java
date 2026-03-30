package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.UserDeletionUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private Button btnLogout, btnEditSave, btnCancel, btnDeleteProfile, btnLotteryGuidelines;
    private View dividerDelete, dividerCancel, dividerGuidelines, bottomNav;
    private LinearLayout displayLayout, editLayout;
    private ChipGroup cgEditInterests, cgDisplayInterests;
    private Chip chipAcademic, chipSocial, chipSports, chipMusic;
    private SwitchMaterial swNotifications, swGeolocation;
    private FirebaseFirestore db;
    private String userId;
    private boolean isEditing = false;
    private boolean forceEdit = false;

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

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }

        forceEdit = getIntent().getBooleanExtra("forceEdit", false);

        initializeViews();
        loadUserProfile();
        setupNavigation();
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

        btnEditSave.setOnClickListener(v -> {
            if (isEditing) {
                saveProfile();
            } else {
                enterEditMode();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (!forceEdit) {
                exitEditMode();
            }
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnLotteryGuidelines.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantLotteryGuidelinesActivity.class);
            startActivity(intent);
        });
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

        btnLogout = findViewById(R.id.btn_log_out);
        btnEditSave = findViewById(R.id.btn_edit_save);
        btnCancel = findViewById(R.id.btn_cancel_edit);
        btnDeleteProfile = findViewById(R.id.btn_delete_profile);
        btnLotteryGuidelines = findViewById(R.id.btn_lottery_guidelines);

        dividerDelete = findViewById(R.id.divider_delete);
        dividerCancel = findViewById(R.id.divider_cancel);
        dividerGuidelines = findViewById(R.id.divider_guidelines);
        bottomNav = findViewById(R.id.bottom_nav_container);

        displayLayout = findViewById(R.id.layout_profile_display);
        editLayout = findViewById(R.id.layout_profile_edit);

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

                if (forceEdit && username != null && !username.isEmpty() && email != null && !email.isEmpty()) {
                    forceEdit = false;
                    exitEditMode();
                }

                List<String> interests = (List<String>) documentSnapshot.get("interests");
                cgDisplayInterests.removeAllViews();
                if (interests != null) {
                    chipAcademic.setChecked(interests.contains("Academic"));
                    chipSocial.setChecked(interests.contains("Social"));
                    chipSports.setChecked(interests.contains("Sports"));
                    chipMusic.setChecked(interests.contains("Music"));

                    for (String interest : interests) {
                        Chip chip = new Chip(this);
                        chip.setText(interest);
                        chip.setClickable(false);
                        chip.setCheckable(false);
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
        displayLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.VISIBLE);
        btnEditSave.setText(forceEdit ? "Complete Profile" : "Save Changes");

        // Hide options when editing
        btnDeleteProfile.setVisibility(View.GONE);
        dividerDelete.setVisibility(View.GONE);
        btnLotteryGuidelines.setVisibility(View.GONE);
        dividerGuidelines.setVisibility(View.GONE);

        if (forceEdit) {
            btnCancel.setVisibility(View.GONE);
            dividerCancel.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            bottomNav.setVisibility(View.GONE);
            tvActionsHeader.setVisibility(View.GONE);
        } else {
            btnCancel.setVisibility(View.VISIBLE);
            dividerCancel.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            bottomNav.setVisibility(View.VISIBLE);
            tvActionsHeader.setVisibility(View.VISIBLE);
        }
    }

    private void exitEditMode() {
        isEditing = false;
        displayLayout.setVisibility(View.VISIBLE);
        editLayout.setVisibility(View.GONE);
        btnEditSave.setText("Edit Profile");

        btnCancel.setVisibility(View.GONE);
        dividerCancel.setVisibility(View.GONE);
        btnDeleteProfile.setVisibility(View.VISIBLE);
        dividerDelete.setVisibility(View.VISIBLE);
        btnLotteryGuidelines.setVisibility(View.VISIBLE);
        dividerGuidelines.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
        tvActionsHeader.setVisibility(View.VISIBLE);
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

        db.collection(FirestorePaths.USERS).document(userId).update(updates)
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
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Warning: This action is permanent. All your profile data will be deleted and you will be logged out.")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteUserProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteUserProfile() {
        if (userId == null) return;

        UserDeletionUtil.cleanUpCoOrganizerRecords(db, userId, () ->
                db.collection(FirestorePaths.USERS).document(userId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                            logout();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!forceEdit) navigateToMain();
            });
        }

        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                if (!forceEdit) {
                    Intent intent = new Intent(this, EntrantEventHistoryActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                }
            });
        }

        View navQrScan = findViewById(R.id.nav_qr_scan);
        if (navQrScan != null) {
            navQrScan.setOnClickListener(v -> {
                if (!forceEdit) {
                    Intent intent = new Intent(this, EntrantQrScanActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }
}
