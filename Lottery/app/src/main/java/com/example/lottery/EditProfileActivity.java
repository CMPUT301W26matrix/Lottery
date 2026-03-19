package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EditProfileActivity allows an entrant to view and update
 * their personal profile information stored in Firestore.
 *
 * <p>This activity supports:
 * <ul>
 *     <li>editing name</li>
 *     <li>editing email</li>
 *     <li>editing optional phone number</li>
 *     <li>deleting the account after confirmation</li>
 * </ul>
 * </p>
 */
public class EditProfileActivity extends AppCompatActivity {

    /**
     * Firestore instance used for loading and updating profile data.
     */
    private FirebaseFirestore db;

    /**
     * Unique identifier of the current user.
     */
    private String userId;

    /**
     * EditText for the user's name.
     */
    private EditText etName;

    /**
     * EditText for the user's email.
     */
    private EditText etEmail;

    /**
     * EditText for the user's optional phone number.
     */
    private EditText etPhone;

    /**
     * Button used to save profile changes.
     */
    private Button btnSaveProfile;

    /**
     * ImageView representing the profile photo placeholder.
     */
    private ImageView ivProfileIcon;

    /**
     * TextView used to trigger account deletion.
     */
    private TextView tvDeleteAccount;

    /**
     * Initializes the activity, binds views, loads profile data,
     * and sets up click listeners.
     *
     * @param savedInstanceState previously saved activity state, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        ivProfileIcon = findViewById(R.id.iv_profile_icon);
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount);

        setupClickListeners();
        loadProfileData();
    }

    /**
     * Sets up click listeners for saving profile data,
     * deleting the account, and the profile image placeholder.
     */
    private void setupClickListeners() {
        btnSaveProfile.setOnClickListener(v -> {
            if (validateInputs()) {
                saveProfileChanges();
            }
        });

        ivProfileIcon.setOnClickListener(v ->
                Toast.makeText(this, "Profile photo upload coming later", Toast.LENGTH_SHORT).show()
        );

        tvDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    /**
     * Loads the current user's profile data from Firestore
     * and fills the form fields with existing values.
     */
    private void loadProfileData() {
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");

                        etName.setText(name != null ? name : "");
                        etEmail.setText(email != null ? email : "");
                        etPhone.setText(phone != null ? phone : "");
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Validates the entered profile information before saving.
     *
     * @return true if inputs are valid, otherwise false
     */
    private boolean validateInputs() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Saves updated name, email, and phone values to Firestore.
     */
    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);

        btnSaveProfile.setEnabled(false);

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Shows a confirmation dialog before permanently deleting the account.
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the user's Firestore profile, attempts to delete the
     * Firebase Authentication account, clears local session data,
     * and returns to the main screen.
     */
    private void deleteAccount() {
        db.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    if (currentUser != null) {
                        currentUser.delete();
                    }

                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show()
                );
    }
}