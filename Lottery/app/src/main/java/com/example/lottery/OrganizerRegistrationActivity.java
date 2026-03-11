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

import java.util.HashMap;
import java.util.Map;

/**
 * OrganizerRegistrationActivity handles the registration process for organizer users.
 * Organizers must register with email and password through Firebase Authentication,
 * and their profile information is stored in Firestore with the role "organizer".
 *
 * <p>This activity validates user input, creates user accounts in Firebase Auth,
 * stores organizer profiles in Firestore, and saves session information in SharedPreferences.</p>
 *
 * @see OrganizerBrowseEventsActivity
 * @see MainActivity
 */
public class OrganizerRegistrationActivity extends AppCompatActivity {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private FirebaseFirestore db;
    private EditText organizerName, organizerEmail, organizerPassword, organizerPassword2, organizerPhone;
    private Button continueButton;
    private ImageButton backButton;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_registration);

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Initialize button views
        backButton = findViewById(R.id.btnBack);
        continueButton = findViewById(R.id.btnContinue);
        // Initialize text editor views
        organizerName = findViewById(R.id.etName);
        organizerEmail = findViewById(R.id.etEmail);
        organizerPhone = findViewById(R.id.etPhoneNumber);
        organizerPassword = findViewById(R.id.etPassword);
        organizerPassword2 = findViewById(R.id.etReEnterPassword);

        // Set on click listeners for buttons
        backButton.setOnClickListener(view -> {
            finish(); // just close this activity, automatically returning to MainActivity
        });

        continueButton.setOnClickListener(view -> {
            if (validateRegistration()) {
                registerUser();
            }
        });
    }

    /**
     * Register the user with Firebase Authentication
     * This creates the login credentials
     */
    private void registerUser() {
        // Get values from input fields
        String email = organizerEmail.getText().toString().trim();
        String password = organizerPassword.getText().toString().trim();
        String name = organizerName.getText().toString().trim();
        String phone = organizerPhone.getText().toString().trim();

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
        userData.put("role", "organizer");      // User role
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
                    editor.putString(KEY_USER_ROLE, "organizer");
                    editor.apply();

                    Toast.makeText(this, R.string.registration_successful, Toast.LENGTH_SHORT).show();

                    // Go to organizer main screen
                    navigateToOrganizerMain(userId, name, email); // for now: isAnonymous is false (not yet implemented)
                })
                .addOnFailureListener(e -> {
                    // Failed to save data
                    continueButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_failed_to_save_profile, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateRegistration() {
        String name = organizerName.getText().toString().trim();
        String email = organizerEmail.getText().toString().trim();
        String password = organizerPassword.getText().toString().trim();
        String password2 = organizerPassword2.getText().toString().trim();

        // Validate name
        if (name.isEmpty()) {
            organizerName.setError(getString(R.string.error_name_required));
            Toast.makeText(OrganizerRegistrationActivity.this,
                    R.string.prompt_enter_your_name, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate email
        if (email.isEmpty()) {
            organizerEmail.setError(getString(R.string.error_email_required));
            Toast.makeText(OrganizerRegistrationActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            organizerEmail.setError(getString(R.string.error_invalid_email));
            Toast.makeText(OrganizerRegistrationActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            organizerPassword.setError(getString(R.string.error_password_required));
            Toast.makeText(OrganizerRegistrationActivity.this,
                    R.string.prompt_enter_a_password, Toast.LENGTH_LONG).show();
            return false;
        }

        if (password.length() < 8) {
            organizerPassword.setError(getString(R.string.error_invalid_password));
            Toast.makeText(OrganizerRegistrationActivity.this,
                    R.string.prompt_password_min_length, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password match
        if (!password.equals(password2)) {
            organizerPassword2.setError(getString(R.string.error_password_mismatch));
            return false;
        }

        // Phone number is optional - no validation needed
        return true;
    }

    private void navigateToOrganizerMain(String userId, String userName,
                                         String userEmail) {
        Intent intent = new Intent(OrganizerRegistrationActivity.this, OrganizerBrowseEventsActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("isRegistered", true);

        if (userEmail != null) {
            intent.putExtra("userEmail", userEmail);
        }

        startActivity(intent);
        finish();

    }
}
