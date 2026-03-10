package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EntrantRegistrationActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private EditText entrantName, entrantEmail, entrantPassword, entrantPassword2, entrantPhone;
    private Button continueButton, anonContinueButton;
    private ImageButton backButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_registration);

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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
            Intent intent = new Intent(EntrantRegistrationActivity.this, MainActivity.class);
            startActivity(intent);
        });

        continueButton.setOnClickListener(view -> {
            if (validateRegistration()) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        final String email = entrantEmail.getText().toString().trim();
        final String password = entrantPassword.getText().toString().trim();
        final String name = entrantName.getText().toString().trim();
        final String phone = entrantPhone.getText().toString().trim();

        // Disable button to prevent multiple clicks
        continueButton.setEnabled(false);
        Toast.makeText(EntrantRegistrationActivity.this,
                "Creating account...", Toast.LENGTH_LONG).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registration successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserToFirestore(user.getUid(), name, email, phone);
                            }
                        } else {
                            // Registration failed
                            continueButton.setEnabled(true);
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Registration failed";
                            Toast.makeText(EntrantRegistrationActivity.this,
                                    "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Saves user data to Firestore after successful authentication.
     * Creates a user document in the "users" collection with the user's UID.
     * Stores the user's role as "entrant" for role-based access control.
     *
     * @param userId The Firebase Authentication UID of the user
     * @param name The user's full name
     * @param email The user's email address
     * @param phone The user's phone number (may be empty)
     */
    private void saveUserToFirestore(final String userId, final String name,
                                     final String email, final String phone) {
        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", "entrant");
        user.put("createdAt", System.currentTimeMillis());
        user.put("userId", userId);
        user.put("hasAcceptedInvitation", false); // For US 01.05.02
        user.put("notificationsEnabled", true); // For US 01.04.03

        // Save to Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Store user info locally
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KEY_USER_ID, userId);
                        editor.putBoolean(KEY_IS_ANONYMOUS, false);
                        editor.apply();

                        showToast("Registration successful!");

                        // Navigate to main screen
                        navigateToEntrantMain(userId, name, email, false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnContinue.setEnabled(true);
                        showToast("Failed to save user data: " + e.getMessage());
                    }
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
            //etName.requestFocus();
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter your name", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate email
        if (email.isEmpty()) {
            entrantEmail.setError("Email is required");
            //etEmail.requestFocus();
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a valid email", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            entrantEmail.setError("Invalid email address");
            //entrantEmail.requestFocus();
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a valid email", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            entrantPassword.setError("Password is required");
            //etPassword.requestFocus();
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Please enter a password", Toast.LENGTH_LONG).show();
            return false;
        }

        if (password.length() < 8) {
            entrantPassword.setError("Invalid password");
            //entrantPassword.requestFocus();
            Toast.makeText(EntrantRegistrationActivity.this,
                    "Password must be at least 8 characters", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password match
        if (!password.equals(password2)) {
            entrantPassword2.setError("Passwords do not match");
            //entrantPassword2.requestFocus();
            return false;
        }

        // Phone number is optional - no validation needed
        return true;
    }
}
