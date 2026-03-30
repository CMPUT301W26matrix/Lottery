package com.example.lottery;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.Event;
import com.example.lottery.util.AdminNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.PosterImageLoader;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AdminImageDetailsActivity displays a full-size poster preview for administrators.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Fetches the event record from Firestore using the supplied event ID.</li>
 *   <li>Renders the poster (Base64) at full width, event title, organizer name, and description.</li>
 *   <li>Provides a delete button for administrators to clear poster data from Firestore.</li>
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
    static Event testEvent;

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

        btnDeleteImage.setOnClickListener(v -> showDeleteConfirmationDialog());

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

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        }
    }

    /**
     * Launches a confirmation dialog before deleting the poster image.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.confirm_deletion).setMessage(R.string.confirm_delete_image)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteImage())
                .setNegativeButton(R.string.cancel, null).show();
    }

    /**
     * Clears the poster data directly from the Firestore event document.
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
        btnDeleteImage.setEnabled(currentPosterBase64 != null && !currentPosterBase64.isEmpty());

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
