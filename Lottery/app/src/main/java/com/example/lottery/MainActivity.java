package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.admin.AdminSignInActivity;
import com.example.lottery.entrant.EntrantMainActivity;
import com.example.lottery.entrant.EntrantProfileActivity;
import com.example.lottery.organizer.OrganizerBrowseEventsActivity;
import com.example.lottery.organizer.OrganizerProfileActivity;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity serves as the entry point where users choose their role.
 * Identification is based on the device's ANDROID_ID combined with the chosen role.
 * This ensures that a single device can maintain separate profiles
 * for different roles, and the identity persists across app reinstalls.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_DEVICE_ID = "deviceId";

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

        // Clear any stale admin role session when returning to the entry screen
        com.example.lottery.util.AdminRoleManager.clearAdminRoleSession(this);

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
     * Handles login using the device's ANDROID_ID combined with the role (role_androidId).
     *
     * @param role The role chosen by the user ("ENTRANT" or "ORGANIZER").
     */
    private void handleDeviceLogin(String role) {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        if (androidId == null || androidId.isEmpty()) {
            Toast.makeText(this, "Could not get Device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // userId is role-prefixed (lowercase in ID for consistency with existing DB)
        String userId = role.toLowerCase() + "_" + androidId;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnCompleteListener(userTask -> {
            if (userTask.isSuccessful()) {
                DocumentSnapshot document = userTask.getResult();
                if (document != null && document.exists()) {
                    loginUser(document, role, androidId);
                } else {
                    createNewUser(userId, role, androidId);
                }
            } else {
                Exception ex = userTask.getException();
                String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
                Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates a new user document in Firestore with role isolation.
     */
    private void createNewUser(String userId, String role, String androidId) {
        Map<String, Object> userData = new HashMap<>();
        Timestamp now = Timestamp.now();

        userData.put("userId", userId);
        userData.put("role", role);
        userData.put("deviceId", androidId);
        userData.put("username", ""); // Fixed: unified name to username
        userData.put("email", "");
        userData.put("phone", "");
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("notificationsEnabled", true);
        userData.put("geolocationEnabled", false);

        db.collection(FirestorePaths.USERS).document(userId).set(userData).addOnSuccessListener(aVoid -> {
            saveSessionLocally(userId, role, androidId, "");
            navigateToProfileCompletion(role, userId);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Logs in an existing user and updates activity timestamp.
     */
    private void loginUser(DocumentSnapshot document, String role, String androidId) {
        String userId = document.getId();
        String username = document.getString("username"); // Fixed: unified name to username
        String email = document.getString("email");

        // Update last activity timestamp
        db.collection(FirestorePaths.USERS).document(userId)
                .update("updatedAt", Timestamp.now());

        saveSessionLocally(userId, role, androidId, username);

        if (username == null || username.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            navigateToProfileCompletion(role, userId);
        } else {
            navigateToRoleMain(role, userId, username, email);
        }
    }

    /**
     * Persists the current session details locally.
     */
    private void saveSessionLocally(String userId, String role, String androidId, String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_DEVICE_ID, androidId);
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
