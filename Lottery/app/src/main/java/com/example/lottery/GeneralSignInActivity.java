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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * GeneralSignInActivity handles the sign-in process for existing users.
 * Users can sign in with their email and password, and are then redirected
 * to the appropriate main activity based on their role (entrant or organizer).
 *
 * <p>This activity:
 * <ul>
 *     <li>Authenticates users with Firebase Auth using email/password</li>
 *     <li>Retrieves user role from Firestore</li>
 *     <li>Navigates to role-specific main activities (EntrantMainActivity or OrganizerBrowseEventsActivity)</li>
 *     <li>Stores session information in SharedPreferences</li>
 * </ul>
 *
 * @see EntrantMainActivity
 * @see OrganizerBrowseEventsActivity
 * @see MainActivity
 */
public class GeneralSignInActivity extends AppCompatActivity {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private EditText userEmail, userPassword;
    private Button continueButton;
    private ImageButton backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_sign_in);

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Initialize views
        userEmail = findViewById(R.id.etEmail);
        userPassword = findViewById(R.id.etPassword);
        continueButton = findViewById(R.id.btnContinue);
        backButton = findViewById(R.id.btnBack);

        // Set on click listeners for buttons
        backButton.setOnClickListener(view -> {
            finish(); // just close this activity, automatically returning to MainActivity
        });

        continueButton.setOnClickListener(view -> {
            if (validateSignIn()) {
                signInUser();
            }
        });
    }

    /**
     * Register the user with Firebase Authentication
     * This creates the login credentials
     */
    private void signInUser() {
        // Get values from input fields
        String email = userEmail.getText().toString().trim();
        String password = userPassword.getText().toString().trim();

        // Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleInFirestore(user.getUid());
                        }
                    } else {
                        // Sign in failed
                        continueButton.setText(R.string.sign_in);

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.error_sign_in_failed);
                        Toast.makeText(GeneralSignInActivity.this,
                                getString(R.string.error_prefix, errorMessage), Toast.LENGTH_LONG).show();
                    }
                });

    }

    /**
     * Save user profile data to Firestore
     * This stores additional info like name, phone, and role
     */
    private void checkUserRoleInFirestore(String userId) {

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            String name = document.getString("name");
                            String email = document.getString("email");

                            // Save user info locally
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_USER_ID, userId);
                            editor.putString(KEY_USER_NAME, name);

                            if (role != null) {
                                editor.putString(KEY_USER_ROLE, role);
                                editor.apply();

                                // Navigate based on role
                                navigateBasedOnRole(role, userId, name, email);
                            } else {
                                // Role not found - should not happen
                                Toast.makeText(this, R.string.error_user_role_not_found, Toast.LENGTH_SHORT).show();
                                continueButton.setText(R.string.sign_in);
                                mAuth.signOut();
                            }
                        } else {
                            // User document doesn't exist in Firestore
                            Toast.makeText(this, R.string.error_user_profile_not_found, Toast.LENGTH_SHORT).show();
                            continueButton.setText(R.string.sign_in);
                            mAuth.signOut();
                        }
                    } else {
                        // Failed to get user data
                        Toast.makeText(this, R.string.error_failed_to_get_user_data, Toast.LENGTH_SHORT).show();
                        continueButton.setText(R.string.sign_in);
                        mAuth.signOut();
                    }
                });
    }

    private boolean validateSignIn() {
        String email = userEmail.getText().toString().trim();
        String password = userPassword.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            userEmail.setError(getString(R.string.error_email_required));
            Toast.makeText(GeneralSignInActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            userEmail.setError(getString(R.string.error_invalid_email));
            Toast.makeText(GeneralSignInActivity.this,
                    R.string.prompt_enter_valid_email, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        if (password.isEmpty()) {
            userPassword.setError(getString(R.string.error_password_required));
            Toast.makeText(GeneralSignInActivity.this,
                    R.string.prompt_enter_your_password, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void navigateBasedOnRole(String userRole, String userId, String userName,
                                     String userEmail) {

        Intent intent = null;


        if ("entrant".equals(userRole)) {
            intent = new Intent(GeneralSignInActivity.this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userName", userName);
            intent.putExtra("userEmail", userEmail);
            intent.putExtra("isRegistered", true);
            intent.putExtra("isAnonymous", false);

        } else if ("organizer".equals(userRole)) {
            intent = new Intent(GeneralSignInActivity.this, OrganizerBrowseEventsActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userName", userName);
            intent.putExtra("userEmail", userEmail);
            intent.putExtra("isRegistered", true);

        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, check role and navigate
            checkUserRoleInFirestore(currentUser.getUid());
        }

    }


}
