package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity serves as the entry point where users choose their role.
 * Identification is based on the device's unique ID (FID) combined with the chosen role (role_FID).
 * This ensures that a single device can maintain separate profiles for different roles.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_FID = "fid";

    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        Button entrantButton = findViewById(R.id.entrant_login_button);
        Button organizerButton = findViewById(R.id.organizer_login_button);
        Button adminButton = findViewById(R.id.admin_login_button);

        entrantButton.setOnClickListener(v -> handleDeviceLogin("ENTRANT"));
        organizerButton.setOnClickListener(v -> handleDeviceLogin("ORGANIZER"));
        adminButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminSignInActivity.class);
            startActivity(intent);
        });

        ensureAuthenticated();
    }

    /**
     * Ensures the user is authenticated with Firebase Auth to avoid Storage/Firestore permission issues.
     */
    private void ensureAuthenticated() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously();
        }
    }

    /**
     * Handles login using the device's unique ID combined with the role (role_FID).
     *
     * @param role The role chosen by the user ("ENTRANT" or "ORGANIZER").
     */
    private void handleDeviceLogin(String role) {
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String fid = task.getResult();
                // userId is role-prefixed (lowercase in ID for consistency with existing DB)
                String userId = role.toLowerCase() + "_" + fid;

                db.collection(FirestorePaths.USERS).document(userId).get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        DocumentSnapshot document = userTask.getResult();
                        if (document != null && document.exists()) {
                            loginUser(document, role, fid);
                        } else {
                            createNewUser(userId, role, fid);
                        }
                    } else {
                        Exception ex = userTask.getException();
                        String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Could not get Device ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates a new user document in Firestore with role isolation.
     */
    private void createNewUser(String userId, String role, String fid) {
        Map<String, Object> userData = new HashMap<>();
        Timestamp now = Timestamp.now();

        userData.put("userId", userId);
        userData.put("role", role);
        userData.put("deviceId", fid);
        userData.put("username", ""); // Fixed: unified name to username
        userData.put("email", "");
        userData.put("phone", "");
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("notificationsEnabled", true);

        db.collection(FirestorePaths.USERS).document(userId).set(userData).addOnSuccessListener(aVoid -> {
            saveSessionLocally(userId, role, fid, "");
            navigateToProfileCompletion(role, userId);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Logs in an existing user and updates activity timestamp.
     */
    private void loginUser(DocumentSnapshot document, String role, String fid) {
        String userId = document.getId();
        String username = document.getString("username"); // Fixed: unified name to username
        String email = document.getString("email");

        // Update last activity timestamp
        db.collection(FirestorePaths.USERS).document(userId)
                .update("updatedAt", Timestamp.now());

        saveSessionLocally(userId, role, fid, username);

        if (username == null || username.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            navigateToProfileCompletion(role, userId);
        } else {
            navigateToRoleMain(role, userId, username, email);
        }
    }

    /**
     * Persists the current session details locally.
     */
    private void saveSessionLocally(String userId, String role, String fid, String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_FID, fid);
        editor.putString("userName", username); // Key userName is used in SP throughout the app
        editor.apply();
    }

    private void navigateToProfileCompletion(String role, String userId) {
        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantProfileActivity.class);
        } else {
            intent = new Intent(this, OrganizerProfileActivity.class);
        }
        intent.putExtra("userId", userId);
        intent.putExtra("forceEdit", true);
        startActivity(intent);
        finish();
    }

    private void navigateToRoleMain(String role, String userId, String username, String email) {
        Intent intent;
        if ("ENTRANT".equalsIgnoreCase(role)) {
            intent = new Intent(this, EntrantMainActivity.class);
        } else {
            intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        }
        intent.putExtra("userId", userId);
        intent.putExtra("userName", username);
        intent.putExtra("userEmail", email);
        startActivity(intent);
        finish();
    }
}
