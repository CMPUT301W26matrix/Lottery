package com.example.lottery;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * NotificationSettingsActivity allows an entrant to manage
 * their notification preference in the app.
 *
 * <p>This activity:
 * <ul>
 *     <li>Loads the current notification preference from Firestore</li>
 *     <li>Displays the preference using a switch</li>
 *     <li>Updates Firestore when the switch is changed</li>
 *     <li>Keeps the preference persistent across app restarts</li>
 * </ul>
 * </p>
 */
public class NotificationSettingsActivity extends AppCompatActivity {

    /**
     * Firestore instance used to load and update notification settings.
     */
    private FirebaseFirestore db;

    /**
     * Unique identifier of the currently signed-in user.
     */
    private String userId;

    /**
     * Switch used to enable or disable notifications.
     */
    private Switch switchNotifications;

    /**
     * TextView used as a small status label below the switch.
     */
    private TextView tvNotificationStatus;

    /**
     * Tracks whether the switch has finished initial Firestore loading.
     * This prevents the toggle listener from firing immediately on startup.
     */
    private boolean isSwitchInitialized = false;

    /**
     * Initializes the activity, binds views, loads the user's current
     * notification preference, and sets up listeners.
     *
     * @param savedInstanceState previously saved activity state, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        switchNotifications = findViewById(R.id.switch_notifications);
        tvNotificationStatus = findViewById(R.id.tv_notification_status);

        loadNotificationPreference();
        setupListeners();
    }

    /**
     * Loads the user's notification preference from Firestore
     * and updates the switch UI accordingly.
     */
    private void loadNotificationPreference() {
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");
                        boolean isEnabled = notificationsEnabled == null || notificationsEnabled;

                        isSwitchInitialized = false;
                        switchNotifications.setChecked(isEnabled);
                        updateStatusText(isEnabled);
                        isSwitchInitialized = true;
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notification settings", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Sets up the toggle listener for the notification switch.
     * When the user changes the switch, the new preference is saved to Firestore.
     */
    private void setupListeners() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isSwitchInitialized) {
                return;
            }

            updateStatusText(isChecked);
            saveNotificationPreference(isChecked);

            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "You have opted out of notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Saves the user's notification preference to Firestore.
     *
     * @param isEnabled true if notifications should be enabled, false otherwise
     */
    private void saveNotificationPreference(boolean isEnabled) {
        db.collection("users")
                .document(userId)
                .update("notificationsEnabled", isEnabled)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save notification setting", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Updates the small status text below the switch to reflect
     * the currently selected notification state.
     *
     * @param isEnabled true if notifications are enabled, false otherwise
     */
    private void updateStatusText(boolean isEnabled) {
        if (isEnabled) {
            tvNotificationStatus.setText("You will receive notifications from organizers and admins.");
        } else {
            tvNotificationStatus.setText("You have opted out of notifications.");
        }
    }
}