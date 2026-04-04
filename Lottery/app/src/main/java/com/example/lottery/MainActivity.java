package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_DEVICE_ID = "deviceId";
    private static final String KEY_USER_NAME = "userName";

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
     * Ensures the user is authenticated with Firebase Auth.
     */
    private void ensureAuthenticated() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously();
        }
    }

    /**
     * Handles login using the device's ANDROID_ID combined with the role.
     */
    private void handleDeviceLogin(String role) {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        if (androidId == null || androidId.isEmpty()) {
            Toast.makeText(this, "Could not get Device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = role.toLowerCase() + "_" + androidId;

        db.collection(FirestorePaths.USERS).document(userId).get().addOnCompleteListener(userTask -> {
            if (userTask.isSuccessful()) {
                DocumentSnapshot document = userTask.getResult();
                if (document != null && document.exists()) {
                    loginUser(document, role, androidId);
                } else {
                    // Document missing (might have been deleted manually)
                    Log.w(TAG, "Account document missing for ID: " + userId);
                    handleAccountNotFound();
                    createNewUser(userId, role, androidId);
                }
            } else {
                Exception ex = userTask.getException();
                String errorMsg = (ex != null) ? ex.getMessage() : "Unknown";
                Log.e(TAG, "Login check failed: " + errorMsg, ex);

                // If PERMISSION_DENIED happens, it usually means the document was deleted 
                // and security rules are blocking the read, or session is invalid.
                if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                    Log.w(TAG, "Permission denied for stale/deleted account, recreating profile");
                    clearSession();
                    Toast.makeText(this, "Old account was removed. Creating a new profile.", Toast.LENGTH_LONG).show();
                    createNewUser(userId, role, androidId);
                } else {
                    Toast.makeText(this, "Login failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Clears the current local session data.
     */
    private void clearSession() {
        Log.d(TAG, "Clearing local session");
        sharedPreferences.edit().clear().apply();
    }

    /**
     * Helper to handle cases where the account is verified as missing from Firestore.
     */
    private void handleAccountNotFound() {
        String savedUserId = sharedPreferences.getString(KEY_USER_ID, "");
        if (!savedUserId.isEmpty()) {
            Log.i(TAG, "Cleaning up stale session for deleted account");
            clearSession();
            Toast.makeText(this, "Account no longer exists. Creating a new profile.", Toast.LENGTH_SHORT).show();
        }
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
        userData.put("username", ""); 
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
        String username = document.getString("username"); 
        String email = document.getString("email");

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
        editor.putString(KEY_USER_NAME, username);
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
