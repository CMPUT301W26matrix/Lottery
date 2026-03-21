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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity class representing the profile screen for an organizer.
 * Unified to handle both profile viewing and mandatory profile completion.
 */
public class OrganizerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone;
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
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");

                tvName.setText(name != null && !name.isEmpty() ? name : "Unknown");
                tvEmail.setText(email != null && !email.isEmpty() ? email : "No Email");
                
                if (phone != null && !phone.isEmpty()) {
                    tvPhone.setText(phone);
                    tvPhone.setVisibility(View.VISIBLE);
                } else {
                    tvPhone.setVisibility(View.GONE);
                }

                etName.setText(name != null ? name : "");
                etEmail.setText(email != null ? email : "");
                etPhone.setText(phone != null ? phone : "");
                
                if (forceEdit && name != null && !name.isEmpty() && email != null && !email.isEmpty()) {
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
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone); // Optional

        db.collection(FirestorePaths.USERS).document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    
                    SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
                    editor.putString("userName", name);
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
                if (forceEdit) {
                    Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
                } else {
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
            // Finding subviews by type/index since they don't have IDs in organizer layout
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
    
    @Override
    public void onBackPressed() {
        if (forceEdit) {
            Toast.makeText(this, "Please complete your profile to continue", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}
