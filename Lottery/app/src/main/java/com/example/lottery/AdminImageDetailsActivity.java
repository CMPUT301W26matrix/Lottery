package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.Event;
import com.example.lottery.util.PosterImageLoader;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * AdminImageDetailsActivity displays a full-size poster preview for administrators.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Fetches the event record from Firestore using the supplied event ID.</li>
 *   <li>Renders the poster at full width, event title, organizer name, and description.</li>
 *   <li>Looks up the organizer name from the users collection via organizerId.</li>
 *   <li>Provides a delete button (to be repurposed for image-only deletion later).</li>
 *   <li>Keeps the custom admin bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class AdminImageDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminImageDetails";
    private static final String EXTRA_EVENT_ID = "eventId";

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
     * Button for deleting the event (to be repurposed for image deletion).
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
     * The current poster URI, saved for deletion from Firebase Storage.
     */
    private String currentPosterUri;

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

        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvEventDetails = findViewById(R.id.tvEventDetails);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);

        btnDeleteImage.setOnClickListener(v -> showDeleteConfirmationDialog());

        setupNavigation();

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
     * Sets up click listeners for the admin bottom navigation bar.
     */
    private void setupNavigation() {
        View btnEvents = findViewById(R.id.nav_home);
        if (btnEvents != null) {
            btnEvents.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
                intent.putExtra("role", "admin");
                startActivity(intent);
                finish();
            });
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseLogsActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Launches a confirmation dialog before deleting the event.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Do you want to delete this poster image?")
                .setPositiveButton("Delete", (dialog, which) -> DeleteImage())
                .setNegativeButton("Cancel", null).show();
    }

    /**
     * Deletes the poster image from Firebase Storage and clears the posterUri in Firestore.
     */
    private void DeleteImage() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteImage.setEnabled(false);

        // Delete file from Firebase Storage if it's a Firebase URL
        if (currentPosterUri != null
                && (currentPosterUri.contains("firebasestorage.googleapis.com")
                || currentPosterUri.startsWith("gs://"))) {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(currentPosterUri)
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete storage file", e));
        }

        // Clear posterUri in Firestore
        db.collection("events").document(eventId)
                .update("posterUri", null)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear posterUri", e);
                    btnDeleteImage.setEnabled(true);
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the event details from Firestore.
     */
    private void fetchEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Event event = documentSnapshot.toObject(Event.class);
                    if (event == null) {
                        Toast.makeText(this, R.string.failed_to_load_event_details, Toast.LENGTH_SHORT).show();
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
        currentPosterUri = event.getPosterUri();
        PosterImageLoader.load(ivEventPoster, currentPosterUri, R.drawable.event_placeholder);

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
        db.collection("users").document(organizerId).get()
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
