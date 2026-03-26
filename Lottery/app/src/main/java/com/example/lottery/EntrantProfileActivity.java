package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
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
 * EntrantProfileActivity displays and manages the profile of an entrant user.
 *
 * <p>This activity supports:
 * <ul>
 *     <li>Viewing profile information</li>
 *     <li>Editing and saving profile details</li>
 *     <li>Deleting the entrant profile</li>
 *     <li>Opting in or out of notifications</li>
 *     <li>Enabling or disabling accessibility mode locally on the device</li>
 * </ul>
 *
 * <p>User stories covered:
 * <ul>
 *     <li>US 01.02.01 - Provide personal information</li>
 *     <li>US 01.02.02 - Update profile information</li>
 *     <li>US 01.02.04 - Delete profile</li>
 *     <li>US 01.04.03 - Opt out of notifications</li>
 *     <li>WOW Factor - Accessibility Mode</li>
 * </ul>
 */
public class EntrantProfileActivity extends AppCompatActivity {

    /** SharedPreferences file used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for storing whether accessibility mode is enabled. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    /** Normal text size for body text. */
    private static final float NORMAL_BODY_TEXT_SP = 14f;

    /** Accessible text size for body text. */
    private static final float ACCESSIBLE_BODY_TEXT_SP = 17f;

    /** Normal text size for card/action titles. */
    private static final float NORMAL_TITLE_TEXT_SP = 15f;

    /** Accessible text size for card/action titles. */
    private static final float ACCESSIBLE_TITLE_TEXT_SP = 18f;

    /** Normal text size for helper text. */
    private static final float NORMAL_HELPER_TEXT_SP = 12f;

    /** Accessible text size for helper text. */
    private static final float ACCESSIBLE_HELPER_TEXT_SP = 14f;

    /** Normal profile name size. */
    private static final float NORMAL_PROFILE_NAME_SP = 24f;

    /** Accessible profile name size. */
    private static final float ACCESSIBLE_PROFILE_NAME_SP = 30f;

    /** Normal minimum button height in dp. */
    private static final int NORMAL_BUTTON_HEIGHT_DP = 56;

    /** Accessible minimum button height in dp. */
    private static final int ACCESSIBLE_BUTTON_HEIGHT_DP = 66;

    /** Normal minimum input height in dp. */
    private static final int NORMAL_INPUT_HEIGHT_DP = 48;

    /** Accessible minimum input height in dp. */
    private static final int ACCESSIBLE_INPUT_HEIGHT_DP = 58;

    private TextView tvName, tvEmail, tvPhone, tvNotificationBadge, tvActionsHeader;
    private EditText etName, etEmail, etPhone;
    private Button btnLogout, btnEditSave, btnCancel, btnDeleteProfile, btnLotteryGuidelines;
    private View dividerDelete, dividerCancel, dividerGuidelines, bottomNav;
    private LinearLayout displayLayout, editLayout;
    private ChipGroup cgEditInterests, cgDisplayInterests;
    private Chip chipAcademic, chipSocial, chipSports, chipMusic;
    private SwitchMaterial swNotifications, swAccessibility;

    private FirebaseFirestore db;
    private String userId;
    private boolean isEditing = false;
    private boolean forceEdit = false;

    /**
     * Initializes the activity, binds the UI, loads profile data, and sets listeners.
     *
     * @param savedInstanceState previously saved state, if any
     */
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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }

        forceEdit = getIntent().getBooleanExtra("forceEdit", false);

        initializeViews();
        loadAccessibilityPreference();
        loadUserProfile();
        setupNavigation();
        checkUnreadNotifications();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (forceEdit) {
                    Toast.makeText(
                            EntrantProfileActivity.this,
                            "Please complete your profile to continue",
                            Toast.LENGTH_SHORT
                    ).show();
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

    /**
     * Reapplies accessibility mode when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        boolean isAccessibilityEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ACCESSIBILITY_MODE, false);
        applyAccessibilityMode(isAccessibilityEnabled);
        checkUnreadNotifications();
    }

    /**
     * Finds all required views and attaches listeners for switches.
     */
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
        swAccessibility = findViewById(R.id.sw_accessibility);

        swNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId != null) {
                db.collection(FirestorePaths.USERS)
                        .document(userId)
                        .update("notificationsEnabled", isChecked);
            }
        });

        swAccessibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAccessibilityPreference(isChecked);
            applyAccessibilityMode(isChecked);
            Toast.makeText(
                    this,
                    isChecked ? "Accessibility Mode enabled" : "Accessibility Mode disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    /**
     * Loads the entrant profile from Firestore and updates the UI.
     */
    @SuppressWarnings("unchecked")
    private void loadUserProfile() {
        if (userId == null) {
            return;
        }

        db.collection(FirestorePaths.USERS).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");

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

                if (forceEdit && username != null && !username.isEmpty()
                        && email != null && !email.isEmpty()) {
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
                    swNotifications.setChecked(true);
                }

                boolean isAccessibilityEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(KEY_ACCESSIBILITY_MODE, false);
                applyAccessibilityMode(isAccessibilityEnabled);
            }
        });
    }

    /**
     * Loads the saved accessibility mode setting from SharedPreferences,
     * updates the switch, and applies the visual changes to this entrant screen.
     */
    private void loadAccessibilityPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAccessibilityEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);
        swAccessibility.setChecked(isAccessibilityEnabled);
        applyAccessibilityMode(isAccessibilityEnabled);
    }

    /**
     * Saves the accessibility mode setting locally on the device.
     *
     * @param isEnabled true if accessibility mode is enabled, false otherwise
     */
    private void saveAccessibilityPreference(boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, isEnabled).apply();
    }

    /**
     * Applies or removes entrant-only accessibility styling on this profile screen.
     *
     * <p>This method directly styles important profile UI elements and then
     * recursively updates remaining text inside the main content area. Bottom
     * navigation text is updated separately.</p>
     *
     * @param isEnabled true to apply accessibility styling, false to restore normal styling
     */
    private void applyAccessibilityMode(boolean isEnabled) {
        if (tvName == null) {
            return;
        }

        float bodyTextSize = isEnabled ? ACCESSIBLE_BODY_TEXT_SP : NORMAL_BODY_TEXT_SP;
        float titleTextSize = isEnabled ? ACCESSIBLE_TITLE_TEXT_SP : NORMAL_TITLE_TEXT_SP;
        float helperTextSize = isEnabled ? ACCESSIBLE_HELPER_TEXT_SP : NORMAL_HELPER_TEXT_SP;
        float profileNameSize = isEnabled ? ACCESSIBLE_PROFILE_NAME_SP : NORMAL_PROFILE_NAME_SP;
        float navTextSize = isEnabled ? 14.5f : 12f;

        int buttonHeight = dpToPx(isEnabled ? ACCESSIBLE_BUTTON_HEIGHT_DP : NORMAL_BUTTON_HEIGHT_DP);
        int inputHeight = dpToPx(isEnabled ? ACCESSIBLE_INPUT_HEIGHT_DP : NORMAL_INPUT_HEIGHT_DP);

        // Direct profile header text
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, profileNameSize);
        tvEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyTextSize);
        tvPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyTextSize);
        tvActionsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, helperTextSize);

        // Edit fields
        setEditTextAccessibilityStyle(etName, bodyTextSize, inputHeight);
        setEditTextAccessibilityStyle(etEmail, bodyTextSize, inputHeight);
        setEditTextAccessibilityStyle(etPhone, bodyTextSize, inputHeight);

        // Action buttons
        setButtonAccessibilityStyle(btnLotteryGuidelines, titleTextSize, buttonHeight);
        setButtonAccessibilityStyle(btnEditSave, titleTextSize, buttonHeight);
        setButtonAccessibilityStyle(btnDeleteProfile, titleTextSize, buttonHeight);
        setButtonAccessibilityStyle(btnCancel, titleTextSize, buttonHeight);
        setButtonAccessibilityStyle(btnLogout, titleTextSize, buttonHeight);

        // Interest chips
        updateChipGroupText(cgEditInterests, isEnabled ? 15f : 13f, isEnabled ? 44 : 32);
        updateChipGroupText(cgDisplayInterests, isEnabled ? 15f : 13f, isEnabled ? 44 : 32);

        // Recursively style content areas that were previously being missed
        View scrollRoot = findViewById(R.id.profile_scroll_view);
        if (scrollRoot != null) {
            updateProfileTextRecursively(scrollRoot, titleTextSize, bodyTextSize, helperTextSize);
        }

        // Bottom navigation labels
        if (bottomNav != null) {
            updateBottomNavTextRecursively(bottomNav, navTextSize);
        }
    }

    /**
     * Recursively updates text views in the profile content area.
     *
     * @param root root view to scan
     * @param titleSize size for title-like labels
     * @param bodySize size for body text
     * @param helperSize size for helper text
     */
    private void updateProfileTextRecursively(View root,
                                              float titleSize,
                                              float bodySize,
                                              float helperSize) {
        if (root == null) {
            return;
        }

        if (root == bottomNav) {
            return;
        }

        if (root instanceof TextView && !(root instanceof Button) && !(root instanceof Chip)) {
            TextView textView = (TextView) root;
            int id = textView.getId();

            // Already styled directly
            if (id == R.id.tv_profile_name
                    || id == R.id.tv_profile_email
                    || id == R.id.tv_profile_phone
                    || id == R.id.tv_actions_header) {
                return;
            }

            CharSequence text = textView.getText();
            String value = text != null ? text.toString().trim() : "";

            if ("PREFERENCES".equalsIgnoreCase(value)
                    || "ACTIONS".equalsIgnoreCase(value)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, helperSize);
            } else if ("Enable Notifications".equals(value)
                    || "Accessibility Mode".equals(value)
                    || "Entrant".equals(value)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);
            } else if ("Receive organizer and admin notifications".equals(value)
                    || "Larger text, larger buttons, and stronger contrast".equals(value)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, helperSize);
            } else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodySize);
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateProfileTextRecursively(group.getChildAt(i), titleSize, bodySize, helperSize);
            }
        }
    }

    /**
     * Updates a whole chip group to use slightly larger accessible chip text.
     *
     * @param chipGroup the chip group to update
     * @param textSize desired text size in sp
     * @param minHeightDp desired minimum height in dp
     */
    private void updateChipGroupText(ChipGroup chipGroup, float textSize, int minHeightDp) {
        if (chipGroup == null) {
            return;
        }

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                chip.setMinHeight(dpToPx(minHeightDp));
                chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            }
        }
    }

    /**
     * Updates an EditText to use larger text and touch-friendly height.
     *
     * @param editText the EditText to update
     * @param textSizeSp desired text size in sp
     * @param minHeightPx desired minimum height in pixels
     */
    private void setEditTextAccessibilityStyle(EditText editText, float textSizeSp, int minHeightPx) {
        if (editText == null) {
            return;
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        editText.setMinHeight(minHeightPx);
    }

    /**
     * Updates a button to use larger text and larger touch height.
     *
     * @param button the button to update
     * @param textSizeSp desired text size in sp
     * @param minHeightPx desired minimum height in pixels
     */
    private void setButtonAccessibilityStyle(Button button, float textSizeSp, int minHeightPx) {
        if (button == null) {
            return;
        }

        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        button.setMinHeight(minHeightPx);

        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params != null && params.height > 0) {
            params.height = minHeightPx;
            button.setLayoutParams(params);
        }
    }

    /**
     * Recursively updates only the bottom navigation text labels.
     *
     * @param view the bottom navigation root
     * @param textSize desired text size in sp
     */
    private void updateBottomNavTextRecursively(View view, float textSize) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateBottomNavTextRecursively(group.getChildAt(i), textSize);
            }
        }
    }

    /**
     * Converts density-independent pixels (dp) into actual pixels.
     *
     * @param dp value in dp
     * @return equivalent value in pixels
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Switches the screen into edit mode.
     */
    private void enterEditMode() {
        isEditing = true;
        displayLayout.setVisibility(View.GONE);
        editLayout.setVisibility(View.VISIBLE);
        btnEditSave.setText(forceEdit ? "Complete Profile" : "Save Changes");

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

        boolean isAccessibilityEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ACCESSIBILITY_MODE, false);
        applyAccessibilityMode(isAccessibilityEnabled);
    }

    /**
     * Exits edit mode and restores the profile display layout.
     */
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

        boolean isAccessibilityEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ACCESSIBILITY_MODE, false);
        applyAccessibilityMode(isAccessibilityEnabled);
    }

    /**
     * Validates and saves profile updates to Firestore.
     */
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
        if (chipAcademic.isChecked()) {
            selectedInterests.add("Academic");
        }
        if (chipSocial.isChecked()) {
            selectedInterests.add("Social");
        }
        if (chipSports.isChecked()) {
            selectedInterests.add("Sports");
        }
        if (chipMusic.isChecked()) {
            selectedInterests.add("Music");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("interests", selectedInterests);

        db.collection(FirestorePaths.USERS).document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor =
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
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
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a confirmation dialog before deleting the profile.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Warning: This action is permanent. All your profile data will be deleted and you will be logged out.")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteUserProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Deletes the current user's profile from Firestore.
     */
    private void deleteUserProfile() {
        if (userId == null) {
            return;
        }

        db.collection(FirestorePaths.USERS).document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                    logout();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to delete profile: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

    /**
     * Logs the user out and clears local session data.
     */
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates the entrant back to the main screen.
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Sets up bottom navigation listeners for entrant screens.
     */
    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!forceEdit) {
                    navigateToMain();
                }
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

    /**
     * Checks whether the entrant has unread notifications and shows or hides the badge.
     */
    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) {
            return;
        }

        db.collection(FirestorePaths.userInbox(userId))
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        tvNotificationBadge.setVisibility(
                                querySnapshot.isEmpty() ? View.GONE : View.VISIBLE
                        ));
    }
}