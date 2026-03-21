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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

/**
 * EntrantRegistrationActivity handles the registration process for entrant users.
 *
 * <p>It provides two registration options:
 * <ul>
 *     <li><b>Full Registration:</b> Creates a persistent account with email and password
 *         using Firebase Authentication. User profiles are stored in Firestore.</li>
 *     <li><b>Anonymous Registration:</b> Identifies the user solely by their device using
 *         Firebase Installation ID (FID). This allows immediate app access without
 *         manual input (aligned with device-based identification requirements).</li>
 * </ul>
 * </p>
 *
 * <p>This activity handles input validation, Firebase interaction, Firestore document
 * creation, and local session management via SharedPreferences.</p>
 *
 * @see EntrantMainActivity
 * @see MainActivity
 */
public class EntrantRegistrationActivity extends AppCompatActivity {
    /**
     * Preference key for storing the user's unique ID.
     */
    private static final String KEY_USER_ID = "userId";
    /**
     * Preference key for storing the user's role (always "entrant" in this activity).
     */
    private static final String KEY_USER_ROLE = "userRole";
    /**
     * Preference key for storing the user's display name.
     */
    private static final String KEY_USER_NAME = "userName";
    /**
     * Preference key indicating if the current session is anonymous.
     */
    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    /**
     * Preference key for storing the Firebase Installation ID.
     */
    private static final String KEY_FID = "fid";

    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * UI components for user input.
     */
    private EditText entrantName, entrantEmail, entrantPassword, entrantPassword2, entrantPhone;
    /**
     * Buttons for triggering registration actions.
     */
    private Button continueButton, anonContinueButton;
    /**
     * Button to return to the previous screen.
     */
    private ImageButton backButton;
    /**
     * Firebase Auth instance for managing authenticated user accounts.
     */
    private FirebaseAuth mAuth;
    /**
     * SharedPreferences for local session persistence.
     */
    private SharedPreferences sharedPreferences;

    /**
     * Initializes the activity, sets up the layout, and configures UI components.
     *
     * @param savedInstanceState the previously saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(LotteryApplication.PREFS_NAME, MODE_PRIVATE);

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
        backButton.setOnClickListener(view -> finish());

        continueButton.setOnClickListener(view -> {
            if (validateRegistration()) {
                registerUser();
            }
        });

        // Handle anonymous registration
        anonContinueButton.setOnClickListener(view -> registerAnonymousUser());
    }

    /**
     * Performs anonymous registration by retrieving the Firebase Installation ID (FID).
     *
     * <p>If the device is already associated with an anonymous account in local preferences,
     * it navigates directly to the main screen. Otherwise, it creates a new "anon_" prefixed
     * user document in Firestore and updates local session data.</p>
     */
    private void registerAnonymousUser() {
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String fid = task.getResult();
                        String anonymousUserId = "anon_" + fid;

                        boolean isExistingAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);
                        String existingUserId = sharedPreferences.getString(KEY_USER_ID, null);
                        String existingFid = sharedPreferences.getString(KEY_FID, null);

                        if (isExistingAnonymous
                                && existingUserId != null
                                && existingFid != null
                                && existingFid.equals(fid)) {

                            anonContinueButton.setText(R.string.continue_without_registering);

                            String existingName = sharedPreferences.getString(
                                    KEY_USER_NAME,
                                    getString(R.string.unregistered_user)
                            );
                            navigateToEntrantMain(existingUserId, existingName, null, true);
                            return;
                        }

                        Map<String, Object> userData = new HashMap<>();
                        Timestamp now = Timestamp.now();

                        userData.put("userId", anonymousUserId);
                        userData.put("name", "");
                        userData.put("email", "");
                        userData.put("phone", "");
                        userData.put("role", "entrant");
                        userData.put("isAnonymous", true);
                        userData.put("deviceId", fid);
                        userData.put("createdAt", now);
                        userData.put("updatedAt", now);
                        userData.put("notificationsEnabled", true);

                        db.collection("users").document(anonymousUserId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(KEY_USER_ID, anonymousUserId);
                                    editor.putString(KEY_USER_ROLE, "entrant");
                                    editor.putString(KEY_USER_NAME, "");
                                    editor.putBoolean(KEY_IS_ANONYMOUS, true);
                                    editor.putString(KEY_FID, fid);
                                    editor.apply();

                                    Toast.makeText(this,
                                            R.string.continuing_as_anonymous_user,
                                            Toast.LENGTH_SHORT).show();

                                    navigateToEntrantMain(anonymousUserId, "", null, true);
                                })
                                .addOnFailureListener(e -> {
                                    anonContinueButton.setText(R.string.continue_without_registering);

                                    Toast.makeText(
                                            this,
                                            getString(R.string.error_failed_to_continue, e.getMessage()),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    }
                });
    }

    /**
     * Registers a new user with email and password via Firebase Authentication.
     *
     * <p>Upon successful account creation, the user's additional profile data
     * (name, email, phone) is persisted to Firestore.</p>
     */
    private void registerUser() {
        String email = entrantEmail.getText().toString().trim();
        String password = entrantPassword.getText().toString().trim();
        String name = entrantName.getText().toString().trim();
        String phone = entrantPhone.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email, phone);
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_registration_failed);

                        Toast.makeText(
                                this,
                                getString(R.string.error_prefix, errorMessage),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * Persists entrant profile data to the Firestore "users" collection.
     *
     * @param userId The unique Firebase Auth UID.
     * @param name   The user's display name.
     * @param email  The user's registered email address.
     * @param phone  The user's optional phone number.
     */
    private void saveUserToFirestore(String userId, String name, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        Timestamp now = Timestamp.now();

        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", "entrant");
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("notificationsEnabled", true);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_USER_ID, userId);
                    editor.putString(KEY_USER_ROLE, "entrant");
                    editor.putString(KEY_USER_NAME, name);
                    editor.putBoolean(KEY_IS_ANONYMOUS, false);
                    editor.apply();

                    Toast.makeText(this, R.string.registration_successful, Toast.LENGTH_SHORT).show();

                    navigateToEntrantMain(userId, name, email, false);
                })
                .addOnFailureListener(e -> {
                    continueButton.setEnabled(true);
                    Toast.makeText(
                            this,
                            getString(R.string.error_failed_to_save_profile, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * Validates user input fields for full registration.
     *
     * <p>Checks for non-empty name, valid email format, and password criteria
     * (min 8 chars, matching re-entry).</p>
     *
     * @return true if all validations pass, false otherwise.
     */
    private boolean validateRegistration() {
        String name = entrantName.getText().toString().trim();
        String email = entrantEmail.getText().toString().trim();
        String password = entrantPassword.getText().toString().trim();
        String password2 = entrantPassword2.getText().toString().trim();

        if (name.isEmpty()) {
            entrantName.setError(getString(R.string.error_name_required));
            Toast.makeText(
                    EntrantRegistrationActivity.this,
                    R.string.prompt_enter_your_name,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        if (email.isEmpty()) {
            entrantEmail.setError(getString(R.string.error_email_required));
            Toast.makeText(
                    EntrantRegistrationActivity.this,
                    R.string.prompt_enter_valid_email,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            entrantEmail.setError(getString(R.string.error_invalid_email));
            Toast.makeText(
                    EntrantRegistrationActivity.this,
                    R.string.prompt_enter_valid_email,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        if (password.isEmpty()) {
            entrantPassword.setError(getString(R.string.error_password_required));
            Toast.makeText(
                    EntrantRegistrationActivity.this,
                    R.string.prompt_enter_a_password,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        if (password.length() < 8) {
            entrantPassword.setError(getString(R.string.error_invalid_password));
            Toast.makeText(
                    EntrantRegistrationActivity.this,
                    R.string.prompt_password_min_length,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        if (!password.equals(password2)) {
            entrantPassword2.setError(getString(R.string.error_password_mismatch));
            return false;
        }

        return true;
    }

    /**
     * Navigates to the entrant's main activity after successful registration.
     *
     * @param userId      The unique ID assigned to the user.
     * @param userName    The display name of the user.
     * @param userEmail   The email address of the user (null for anonymous).
     * @param isAnonymous true if the registration was performed anonymously.
     */
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