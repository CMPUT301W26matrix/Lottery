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

public class EntrantRegistrationActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private EditText entrantName, entrantEmail, entrantPassword, entrantPassword2, entrantPhone;
    private Button continueButton, anonContinueButton;
    private ImageButton backButton;

    private FirebaseAuth mAuth;

    private SharedPreferences sharedPreferences;

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";

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
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
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

                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Go to entrant main screen
                    navigateToEntrantMain(userId, name, email, false); // for now: isAnonymous is false (not yet implemented)
                })
                .addOnFailureListener(e -> {
                    // Failed to save data
                    continueButton.setEnabled(true);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(),
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
            entrantName.setError("Name is required");
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter your name", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate email
        if (email.isEmpty()) {
            entrantEmail.setError("Email is required");
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a valid email", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            entrantEmail.setError("Invalid email address");
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a valid email", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            entrantPassword.setError("Password is required");
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a password", Toast.LENGTH_LONG).show();
            return false;
        }

        if (password.length() < 8) {
            entrantPassword.setError("Invalid password");
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Password must be at least 8 characters", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password match
        if (!password.equals(password2)) {
            entrantPassword2.setError("Passwords do not match");
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
