package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

/**
 * EntrantRegistrationActivity handles the registration process for entrant users.
 * It provides two registration options:
 * <ul>
 *     <li>Full registration with email/password using Firebase Authentication</li>
 *     <li>Anonymous registration using Firebase Installation ID (FID) for users who want to use the app without creating an account (should be modified to identify users by device)</li>
 * </ul>
 *
 * <p>This activity validates user input, creates user accounts in Firebase Auth,
 * stores user profiles in Firestore, and saves session information in SharedPreferences.</p>
 *
 * @see EntrantMainActivity
 * @see MainActivity
 */
public class EntrantRegistrationActivity extends AppCompatActivity {
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    private static final String KEY_FID = "fid"; // Store FID instead of custom deviceId
    private FirebaseFirestore db;
    private EditText entrantName, entrantEmail, entrantPassword, entrantPassword2, entrantPhone;
    private Button continueButton, anonContinueButton;
    private ImageButton backButton;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_registration);

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Initialize button views
        backButton = findViewById(R.id.btnBack);
        continueButton = findViewById(R.id.btnContinue);
        anonContinueButton = findViewById(R.id.btnContinueWithoutRegistering);
        // Initialize text editor views
        entrantName = findViewById(R.id.etName);
        entrantEmail = findViewById(R.id.etEmail);
        entrantPhone = findViewById(R.id.etPhoneNumber);
        entrantPassword = findViewById(R.id.etPassword);
        entrantPassword2 = findViewById(R.id.etReEnterPassword);

        // Set on click listeners for buttons
        backButton.setOnClickListener(view -> {
            finish(); // Just close this activity, automatically returning to MainActivity
        });

        continueButton.setOnClickListener(view -> {
            if (validateRegistration()) {
                registerUser();
            }
        });

        // Handle anonymous registration
        anonContinueButton.setOnClickListener(view -> {
            registerAnonymousUser();
        });
    }

    private void registerAnonymousUser() {
        // Get Firebase Installation ID (FID) to identify anonymous users
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String fid = task.getResult(); // Get the FID

                        // Create anonymous user ID using FID
                        String anonymousUserId = "anon_" + fid;

                        // Check if this device has already registered anonymously
                        boolean isExistingAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);

                        String existingUserId = sharedPreferences.getString(KEY_USER_ID, null);
                        String existingFid = sharedPreferences.getString(KEY_FID, null);

                        if (isExistingAnonymous && existingUserId != null && existingFid != null && existingFid.equals(fid)) {
                            // Device already has an anonymous account with same FID
                            anonContinueButton.setText(R.string.continue_without_registering);

                            String existingName = sharedPreferences.getString(KEY_USER_NAME, getString(R.string.unregistered_user));
                            navigateToEntrantMain(existingUserId, existingName, null, true);
                            return;
                        }

                        // Create user data for Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", anonymousUserId);
                        userData.put("name", ""); // No email for anonymous
                        userData.put("email", ""); // No email for anonymous
                        userData.put("phone", ""); // No email for anonymous
                        userData.put("role", "entrant");
                        userData.put("isAnonymous", true);
                        userData.put("deviceId", fid);

                        // Add timestamps
                        userData.put("createdAt", Timestamp.now());
                        userData.put("notificationsEnabled", true);

                        // Save to FireStore
                        db.collection("users").document(anonymousUserId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(KEY_USER_ID, anonymousUserId);
                                    editor.putString(KEY_USER_ROLE, "entrant");
                                    editor.putString(KEY_USER_NAME, null);
                                    editor.putBoolean(KEY_IS_ANONYMOUS, true);
                                    editor.putString(KEY_FID, fid);
                                    editor.apply();

                                    Toast.makeText(this, R.string.continuing_as_anonymous_user, Toast.LENGTH_SHORT).show();

                                    // Go to entrant main screen
                                    navigateToEntrantMain(anonymousUserId,
                                            null,
                                            null, true);
                                })
                                .addOnFailureListener(e -> {
                                    anonContinueButton.setText(R.string.continue_without_registering);

                                    Toast.makeText(this, getString(R.string.error_failed_to_continue, e.getMessage()),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                });

    }


    /**
     * Register the user with Firebase Authentication
     * This creates the login credentials
     */
    private void registerUser() {
        // Get values from input fields
        String email = entrantEmail.getText().toString().trim();
        String password = entrantPassword.getText().toString().trim();
        String name = entrantName.getText().toString().trim();
        String phone = entrantPhone.getText().toString().trim();

        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User account created
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save additional user data to Firestore
                            saveUserToFirestore(user.getUid(), name, email, phone);
                        }
                    } else {
                        // Failed to create account
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.error_registration_failed);
                        Toast.makeText(this, getString(R.string.error_prefix, errorMessage), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Save user profile data to Firestore
     * This stores additional info like name, phone, and role
     */
    private void saveUserToFirestore(String userId, String name, String email, String phone) {
        // Create a Map (like a dictionary) of user data to store
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);            // Link to Auth UID
        userData.put("name", name);                // User's full name
        userData.put("email", email);              // User's email
        userData.put("phone", phone);              // Optional phone number
        userData.put("role", "entrant");        // User role
        userData.put("createdAt", Timestamp.now()); // Timestamp
        userData.put("notificationsEnabled", true); // Default setting

        // Save to Firestore in the "users" collection
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Data saved successfully

                    // Save user info locally so we know they're logged in next time
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_USER_ID, userId);
                    editor.putString(KEY_USER_ROLE, "entrant");
                    editor.apply();

                    Toast.makeText(this, R.string.registration_successful, Toast.LENGTH_SHORT).show();

                    // Go to entrant main screen
                    navigateToEntrantMain(userId, name, email, false);
                })
                .addOnFailureListener(e -> {
                    // Failed to save data
                    continueButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_failed_to_save_profile, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateRegistration() {
        String name = entrantName.getText().toString().trim();
        String email = entrantEmail.getText().toString().trim();
        String password = entrantPassword.getText().toString().trim();
        String password2 = entrantPassword2.getText().toString().trim();

        // Validate name
        if (name.isEmpty()) {
            entrantName.setError(getString(R.string.error_name_required));
            Toast.makeText(EntrantRegistrationActivity.this,
                    R.string.prompt_enter_your_name, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate email
        if (email.isEmpty()) {
            entrantEmail.setError(getString(R.string.error_email_required));
            Toast.makeText(EntrantRegistrationActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            entrantEmail.setError(getString(R.string.error_invalid_email));
            Toast.makeText(EntrantRegistrationActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            entrantPassword.setError(getString(R.string.error_password_required));
            Toast.makeText(EntrantRegistrationActivity.this,
                    R.string.prompt_enter_a_password, Toast.LENGTH_LONG).show();
            return false;
        }

        if (password.length() < 8) {
            entrantPassword.setError(getString(R.string.error_invalid_password));
            Toast.makeText(EntrantRegistrationActivity.this,
                    R.string.prompt_password_min_length, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password match
        if (!password.equals(password2)) {
            entrantPassword2.setError(getString(R.string.error_password_mismatch));
            return false;
        }

        // Phone number is optional - no validation needed
        return true;
    }


    private void navigateToEntrantMain(String userId, String userName,
                                       String userEmail, boolean isAnonymous) {
        Intent intent = new Intent(EntrantRegistrationActivity.this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("isRegistered", !isAnonymous);
        intent.putExtra("isAnonymous", isAnonymous);

        if (userEmail != null) {
            intent.putExtra("userEmail", userEmail);
        }

        startActivity(intent);
        finish();
    }
}
