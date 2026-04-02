package com.example.lottery.admin;

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
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.R;
import com.example.lottery.model.Event;
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
    private static final String EXTRA_EVENT_ID = "eventId";

    /**
     * Inject a test Event to bypass Firestore while still exercising updateUi().
     */
    @VisibleForTesting
    public static Event testEvent;

    /**
     * ImageView used for displaying the full-size event poster.
     */
    private ImageView ivEventPoster;
    /**
     * TextView for rendering the event title.
     */
    private TextView tvEventTitle;
    /**
     * TextView for displaying the organizer name.
     */
    private TextView tvOrganizerName;
    /**
     * TextView for displaying the event description.
     */
    private TextView tvEventDetails;
    /**
     * Button for deleting the poster image.
     */
    private Button btnDeleteImage;
    /**
     * ImageButton for returning to previous page.
     */
    private ImageButton btnBack;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Identifier of the event currently being displayed.
     */
    private String eventId;
    /**
     * The current poster Base64, saved for deletion from Firestore.
     */
    private String currentPosterBase64;
    /**
     * The userId of admin for jump to other pages.
     */
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

        // Get userId from intent or shared preferences
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
        }

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnDeleteImage.setOnClickListener(v -> showDeleteConfirmation());

        AdminNavigationHelper.setup(this, AdminNavigationHelper.AdminTab.IMAGES, userId, true);
        // Logs and Settings should keep image detail alive for Back navigation
        AdminNavigationHelper.overrideTabWithoutFinish(this, AdminNavigationHelper.AdminTab.LOGS, userId);
        AdminNavigationHelper.overrideTabWithoutFinish(this, AdminNavigationHelper.AdminTab.SETTINGS, userId);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchEventDetails();
    }

    /**
     * Fetches the latest event details from Firestore whenever the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        }
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
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.error_event_id_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteImage.setEnabled(false);
        clearPosterBase64InFirestore();
    }

    /**
     * Clears the posterBase64 field in Firestore.
     */
    private void clearPosterBase64InFirestore() {
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .update("posterBase64", null)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear posterBase64", e);
                    btnDeleteImage.setEnabled(true);
                    Toast.makeText(this, R.string.failed_to_delete_image, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the event details from Firestore.
     */
    private void fetchEventDetails() {
        if (testEvent != null) {
            updateUi(testEvent);
            return;
        }
        db.collection(FirestorePaths.EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Event event = documentSnapshot.toObject(Event.class);
                    if (event == null) {
                        Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    updateUi(event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event details", e);
                    Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI components with the provided event data.
     *
     * @param event The event data to display.
     */
    private void updateUi(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());
        currentPosterBase64 = event.getPosterBase64();
        PosterImageLoader.load(ivEventPoster, currentPosterBase64, R.drawable.event_placeholder);
        
        // Anti-zero protection: disable button if image is already empty
        btnDeleteImage.setEnabled(currentPosterBase64 != null && !currentPosterBase64.trim().isEmpty());

        if (event.getOrganizerId() != null) {
            fetchOrganizerName(event.getOrganizerId());
        } else {
            tvOrganizerName.setText(getString(R.string.admin_organizer_label,
                    getString(R.string.admin_unknown_organizer)));
        }
    }

    /**
     * Fetches the organizer name from the users' collection.
     *
     * @param organizerId The ID of the organizer to look up.
     */
    private void fetchOrganizerName(String organizerId) {
        db.collection(FirestorePaths.USERS).document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("username");
                    tvOrganizerName.setText(getString(R.string.admin_organizer_label,
                            name != null ? name : getString(R.string.admin_unknown_organizer)));
                })
                .addOnFailureListener(e ->
                        tvOrganizerName.setText(getString(R.string.admin_organizer_label,
                                getString(R.string.admin_unknown_organizer))));
    }
}
