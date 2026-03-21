package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity serves as the entry point where users choose their role.
 * Identification is based on the device's unique ID (FID).
 * All users are unified under a single device-based ID.
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
        
        // Hide legacy sign-in components if they exist
        View signInPrompt = findViewById(R.id.tvSignInHint);
        View signInButton = findViewById(R.id.btnSignIn);
        if (signInPrompt != null) signInPrompt.setVisibility(View.GONE);
        if (signInButton != null) signInButton.setVisibility(View.GONE);

        entrantButton.setOnClickListener(v -> handleDeviceLogin("entrant"));
        organizerButton.setOnClickListener(v -> handleDeviceLogin("organizer"));
        adminButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminSignInActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Handles login using the device's unique ID.
     * If the user doesn't exist, a new profile is created.
     * @param role The role chosen by the user ("entrant" or "organizer").
     */
    private void handleDeviceLogin(String role) {
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String fid = task.getResult();
                // Unified userId: just use the device ID (FID)
                String userId = fid;
                
                db.collection(FirestorePaths.USERS).document(userId).get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        DocumentSnapshot document = userTask.getResult();
                        if (document != null && document.exists()) {
                            loginUser(document, role, fid);
                        } else {
                            createNewUser(userId, role, fid);
                        }
                    } else {
                        Toast.makeText(this, "Login failed: " + userTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Could not get Device ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates a new user document in Firestore.
     * New users are directed to complete their profile.
     */
    private void createNewUser(String userId, String role, String fid) {
        Map<String, Object> userData = new HashMap<>();
        Timestamp now = Timestamp.now();

        userData.put("userId", userId);
        userData.put("deviceId", fid);
        userData.put("role", role);
        userData.put("name", ""); 
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
     * Logs in an existing user.
     * If profile information is missing, directs them to profile completion.
     */
    private void loginUser(DocumentSnapshot document, String role, String fid) {
        String userId = document.getId();
        String name = document.getString("name");
        String email = document.getString("email");
        
        // Update role if it changed (optional, but keep consistent with selection)
        db.collection(FirestorePaths.USERS).document(userId).update("role", role);
        
        saveSessionLocally(userId, role, fid, name);

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            navigateToProfileCompletion(role, userId);
        } else {
            navigateToRoleMain(role, userId, name, email);
        }
    }

    private void saveSessionLocally(String userId, String role, String fid, String name) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_FID, fid);
        editor.putString("userName", name);
        editor.apply();
    }

    private void navigateToProfileCompletion(String role, String userId) {
        Intent intent;
        if ("entrant".equals(role)) {
            intent = new Intent(this, EntrantProfileActivity.class);
        } else {
            intent = new Intent(this, OrganizerProfileActivity.class);
        }
        intent.putExtra("userId", userId);
        intent.putExtra("forceEdit", true); 
        startActivity(intent);
        finish();
    }

    private void navigateToRoleMain(String role, String userId, String name, String email) {
        Intent intent;
        if ("entrant".equals(role)) {
            intent = new Intent(this, EntrantMainActivity.class);
        } else {
            intent = new Intent(this, OrganizerBrowseEventsActivity.class);
        }
        intent.putExtra("userId", userId);
        intent.putExtra("userName", name);
        intent.putExtra("userEmail", email);
        startActivity(intent);
        finish();
    }
}
