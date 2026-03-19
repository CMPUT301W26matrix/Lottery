package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * EntrantProfileActivity displays and manages the personal profile
 * of an entrant user.
 *
 * <p>Key responsibilities:
 * <ul>
 *     <li>Displays the entrant's name and email retrieved from Firestore.</li>
 *     <li>Provides a logout mechanism that clears Firebase authentication
 *     and local preferences.</li>
 *     <li>Allows navigation to the edit profile screen.</li>
 *     <li>Allows navigation to the notification settings screen.</li>
 *     <li>Handles bottom navigation between home, notifications,
 *     QR scan, and profile.</li>
 * </ul>
 * </p>
 */
public class EntrantProfileActivity extends AppCompatActivity {

    /**
     * TextView used to display the entrant's name.
     */
    private TextView tvName;

    /**
     * TextView used to display the entrant's email.
     */
    private TextView tvEmail;

    /**
     * Button used to trigger logout.
     */
    private Button btnLogout;

    /**
     * Firestore database instance for profile data retrieval.
     */
    private FirebaseFirestore db;

    /**
     * FirebaseAuth instance for authentication and logout.
     */
    private FirebaseAuth mAuth;

    /**
     * Unique identifier of the currently signed-in user.
     */
    private String userId;

    /**
     * Initializes the activity, binds views, loads user profile data,
     * and configures click listeners and bottom navigation.
     *
     * @param savedInstanceState previously saved activity state, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = getIntent().getStringExtra("userId");

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        btnLogout = findViewById(R.id.btn_log_out);

        loadUserProfile();
        setupNavigation();
        setupClickListeners();
    }

    /**
     * Reloads profile information whenever the activity becomes visible again.
     * This ensures updated values appear after returning from EditProfileActivity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    /**
     * Sets up click listeners for logout, edit profile,
     * and notification settings.
     */
    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(EntrantProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.rl_edit_profile).setOnClickListener(v -> {
            Intent intent = new Intent(EntrantProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.rl_notification_settings).setOnClickListener(v -> {
            Intent intent = new Intent(EntrantProfileActivity.this, NotificationSettingsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    /**
     * Fetches the entrant's profile data from the Firestore {@code users}
     * collection and updates the displayed name and email.
     */
    private void loadUserProfile() {
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        tvName.setText(name != null ? name : "");
                        tvEmail.setText(email != null ? email : "");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Configures click listeners for the bottom navigation bar.
     */
    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(EntrantProfileActivity.this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            Intent intent = new Intent(EntrantProfileActivity.this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        findViewById(R.id.nav_qr_scan).setOnClickListener(v ->
                Toast.makeText(EntrantProfileActivity.this,
                        "QR Scan coming soon",
                        Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            // Already on profile screen
        });
    }
}