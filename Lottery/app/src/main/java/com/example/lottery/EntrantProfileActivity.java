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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EntrantProfileActivity displays and manages the personal profile of an entrant user.
 * Unified to handle both profile viewing and mandatory profile completion.
 */
public class EntrantProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvNotificationBadge;
    private EditText etName, etEmail, etPhone;
    private Button btnLogout, btnEditSave, btnCancel;
    private LinearLayout displayLayout, editLayout;
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

        if (forceEdit) {
            enterEditMode();
            Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_LONG).show();
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
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        
        etName = findViewById(R.id.et_edit_name); 
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);

        btnLogout = findViewById(R.id.btn_log_out);
        btnEditSave = findViewById(R.id.btn_edit_save); 
        btnCancel = findViewById(R.id.btn_cancel_edit);
        displayLayout = findViewById(R.id.layout_profile_display);
        editLayout = findViewById(R.id.layout_profile_edit);
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Fixed: unified field name to username
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
                
                // If we were forced to edit but data is now present, we can exit edit mode
                if (forceEdit && username != null && !username.isEmpty() && email != null && !email.isEmpty()) {
                    forceEdit = false;
                    exitEditMode();
                }
            }
        });
    }

    private void enterEditMode() {
        isEditing = true;
        if (displayLayout != null) displayLayout.setVisibility(View.GONE);
        if (editLayout != null) editLayout.setVisibility(View.VISIBLE);
        if (btnEditSave != null) btnEditSave.setText("Save");
        if (btnCancel != null) btnCancel.setVisibility(View.VISIBLE);
    }

    private void exitEditMode() {
        isEditing = false;
        if (displayLayout != null) displayLayout.setVisibility(View.VISIBLE);
        if (editLayout != null) editLayout.setVisibility(View.GONE);
        if (btnEditSave != null) btnEditSave.setText("Edit Profile");
        if (btnCancel != null) btnCancel.setVisibility(View.GONE);
    }

    private void saveProfile() {
        String username = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username); // Fixed: unified field name to username
        updates.put("email", email);
        updates.put("phone", phone); // Optional

        db.collection(FirestorePaths.USERS).document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    
                    // Update local prefs
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
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    navigateToMain();
                }
            });
        }
        
        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, NotificationsActivity.class);
                    intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
                    startActivity(intent);
                    finish();
                }
            });
        }

        View navQrScan = findViewById(R.id.nav_qr_scan);
        if (navQrScan != null) {
            navQrScan.setOnClickListener(v -> {
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, EntrantQrScanActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                }
            });
        }

        View navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                // Already here
            });
        }
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }
    
    @Override
    public void onBackPressed() {
        if (forceEdit) {
            Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}
