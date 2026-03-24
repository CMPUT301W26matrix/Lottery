package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity class representing the profile screen for an organizer.
 * Unified to handle both profile viewing, editing, and mandatory profile completion.
 */
public class OrganizerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvActionsHeader;
    private EditText etName, etEmail, etPhone;
    private Button btnLogout, btnEditSave, btnCancel, btnDeleteProfile, btnLotteryGuidelines;
    private View dividerDelete, dividerCancel, dividerGuidelines, bottomNav;
    private LinearLayout displayLayout, editLayout;
    private FirebaseFirestore db;
    private String userId;
    private boolean isEditing = false;
    private boolean forceEdit = false;

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

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }

        forceEdit = getIntent().getBooleanExtra("forceEdit", false);

        initializeViews();
        loadUserProfile();
        setupNavigation();
        setupBackPressed();

        if (forceEdit) {
            enterEditMode();
            Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_LONG).show();
        }else {
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
            if (forceEdit) {
                Toast.makeText(this, "Profile completion is required", Toast.LENGTH_SHORT).show();
            } else {
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
            Intent intent = new Intent(this, OrganizerLotteryGuidelinesActivity.class);
            startActivity(intent);
        });
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
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");

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

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("email", email);
        updates.put("phone", phone);

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

        db.collection(FirestorePaths.USERS).document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                    logout();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    navigateToMain();
                }
            });
        }

        View btnNotifications = findViewById(R.id.nav_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                if (!forceEdit) {
                    Intent intent = new Intent(this, OrganizerNotificationsActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                }
            });
        }

        View btnQr = findViewById(R.id.nav_qr_code);
        if (btnQr != null) {
            btnQr.setOnClickListener(v -> {
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, OrganizerQrEventListActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                }
            });
        }

        View btnProfile = findViewById(R.id.nav_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                // Already here
            });
        }

        View navCreate = findViewById(R.id.nav_create_container);
        if (navCreate != null) {
            navCreate.setOnClickListener(v -> {
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, OrganizerCreateEventActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                }
            });
        }

        updateNavigationSelection();
    }

    private void updateNavigationSelection() {
        // Reset home selection
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            if (navHome instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) navHome;
                if (ll.getChildCount() >= 2) {
                    View iv = ll.getChildAt(0);
                    View tv = ll.getChildAt(1);
                    if (iv instanceof ImageView) ((ImageView) iv).setColorFilter(getResources().getColor(R.color.text_gray));
                    if (tv instanceof TextView) ((TextView) tv).setTextColor(getResources().getColor(R.color.text_gray));
                }
            }
        }

        // Highlight profile selection
        View navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) {
            if (navProfile instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) navProfile;
                if (ll.getChildCount() >= 2) {
                    View iv = ll.getChildAt(0);
                    View tv = ll.getChildAt(1);
                    if (iv instanceof ImageView) ((ImageView) iv).setColorFilter(getResources().getColor(R.color.primary_blue));
                    if (tv instanceof TextView) ((TextView) tv).setTextColor(getResources().getColor(R.color.primary_blue));
                }
            }
        }
    }
}
