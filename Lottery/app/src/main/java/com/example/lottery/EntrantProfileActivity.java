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
 * EntrantProfileActivity displays and manages the personal profile of an entrant user.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Displays the entrant's name and email retrieved from Firestore.</li>
 *   <li>Provides a logout mechanism that clears Firebase authentication and local preferences.</li>
 *   <li>Handles navigation to the entrant's home screen and other feature placeholders.</li>
 *   <li>Serves as a hub for entrant settings such as profile editing and notification preferences.</li>
 * </ul>
 * </p>
 */
public class EntrantProfileActivity extends AppCompatActivity {

    /**
     * TextView for displaying the entrant's name.
     */
    private TextView tvName;
    /**
     * TextView for displaying the entrant's email.
     */
    private TextView tvEmail;
    /**
     * Button used to trigger the logout process.
     */
    private Button btnLogout;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Firebase Auth instance for handling user sessions.
     */
    private FirebaseAuth mAuth;
    /**
     * The unique identifier for the current user.
     */
    private String userId;

    /**
     * Initializes the activity, sets up the layout, and configures UI components.
     *
     * @param savedInstanceState the previously saved state of the activity.
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

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.rl_edit_profile).setOnClickListener(v -> {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rl_notification_settings).setOnClickListener(v -> {
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Fetches user profile data (name and email) from the Firestore "users" collection.
     * Updates the UI if the document exists.
     */
    private void loadUserProfile() {
        if (userId == null) return;

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvName.setText(documentSnapshot.getString("name"));
                tvEmail.setText(documentSnapshot.getString("email"));
            }
        });
    }

    /**
     * Configures click listeners for the navigation bar elements.
     * Manages transitions between the profile, home screen, and other features.
     */
    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v ->
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_qr_scan).setOnClickListener(v ->
                Toast.makeText(this, "QR Scan coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            // Already here
        });
    }
}
