package com.example.lottery;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.PosterImageLoader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminImageDetailsActivity displays details of a specific event's poster image
 * and provides functionality for an administrator to remove the image.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Displays the event poster, title, organizer name, and event details.</li>
 *   <li>Allows the admin to delete the event's poster image from Firestore.</li>
 *   <li>Handles navigation back to the image browser after deletion.</li>
 * </ul>
 * </p>
 */
public class AdminImageDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminImageDetails";
    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvOrganizerName, tvEventDetails;
    private Button btnDeleteImage;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private String eventId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_image_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");
        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        btnBack = findViewById(R.id.btnBack);

        if (eventId == null) {
            Toast.makeText(this, "Error: Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnDeleteImage.setOnClickListener(v -> showDeleteConfirmation());

        loadEventDetails();
        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.IMAGES, userId);
    }

    /**
     * Fetches event data from Firestore using the provided eventId.
     * Updates the UI with the event title, organizer ID, poster image, and details.
     */
    private void loadEventDetails() {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String organizerId = documentSnapshot.getString("organizerId");
                        String details = documentSnapshot.getString("description");
                        String posterBase64 = documentSnapshot.getString("posterBase64");

                        tvEventTitle.setText(title);
                        tvOrganizerName.setText(getString(R.string.admin_organizer_label, organizerId));
                        tvEventDetails.setText(details);

                        // Use PosterImageLoader instead of ProfileImageHelper to support full-size posters
                        PosterImageLoader.load(ivEventPoster, posterBase64, R.drawable.event_placeholder);
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a confirmation dialog before deleting the image.
     */
    private void showDeleteConfirmation() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_image)
                .setMessage(R.string.confirm_delete_image)
                .setPositiveButton(R.string.confirm, (d, which) -> deleteImage())
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();

        // Make the Delete button visually distinct (red) to match Organizer/Entrant style
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        // Soften title size slightly to match unified style (18sp)
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }

    /**
     * Deletes the event's poster image from Firestore by setting posterBase64 to an empty string.
     * Navigates back to the image browser upon successful deletion.
     */
    private void deleteImage() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("posterBase64", ""); // Clearing the poster

        db.collection(FirestorePaths.EVENTS).document(eventId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting image", e);
                    Toast.makeText(this, R.string.failed_to_delete_image, Toast.LENGTH_SHORT).show();
                });
    }
}
