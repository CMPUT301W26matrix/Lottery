package com.example.lottery;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for organizers to create new events.
 * Handles event details input, date/time selection, poster uploading,
 * and saving the event data to Firebase Firestore.
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails;
    private Button btnEventDateTime, btnRegistrationDeadline, btnUploadPoster, btnGenerateQRCode, btnCreateEvent;
    private ImageView ivPosterPreview;
    
    // Event Data Variables
    private String qrCodeContent = "";
    private String scheduledDateTime = "";
    private String registrationDeadline = "";
    private String posterUriString = "";

    // Activity Result Launcher for picking images from gallery
    private ActivityResultLauncher<String> getContentLauncher;
    
    // Firestore instance for database operations
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firestore database connection
        db = FirebaseFirestore.getInstance();

        // Initialize UI components by finding views in the layout
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        btnEventDateTime = findViewById(R.id.btnEventDateTime);
        btnRegistrationDeadline = findViewById(R.id.btnRegistrationDeadline);
        btnUploadPoster = findViewById(R.id.btnUploadPoster);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);

        /**
         * Initialize the Image Picker Launcher.
         * Sets up the callback for when a user selects an image from their device.
         */
        getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        posterUriString = uri.toString();
                        ivPosterPreview.setImageURI(uri);
                        Log.d(TAG, "Poster Image Selected: " + posterUriString);
                    }
                });

        // Set listener for uploading a poster image
        btnUploadPoster.setOnClickListener(v -> getContentLauncher.launch("image/*"));

        // Set listeners for picking date and time for the event and registration deadline
        btnEventDateTime.setOnClickListener(v -> showDateTimePicker(btnEventDateTime, true));
        btnRegistrationDeadline.setOnClickListener(v -> showDateTimePicker(btnRegistrationDeadline, false));

        /**
         * Listener for generating a unique QR code content.
         * Generates a temporary unique ID and seeds the QR content generation logic.
         */
        btnGenerateQRCode.setOnClickListener(v -> {
            String tempId = UUID.randomUUID().toString();
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(tempId);
            Log.d(TAG, "QR Code Content Generated: " + qrCodeContent);
            Toast.makeText(this, "QR Code content generated!", Toast.LENGTH_SHORT).show();
        });

        // Set listener for the final event creation action
        btnCreateEvent.setOnClickListener(v -> {
            createEvent();
        });
    }

    /**
     * Helper method to display a DatePickerDialog followed by a TimePickerDialog.
     * @param button The button that triggered the picker and where the result will be displayed.
     * @param isEventTime Boolean flag to determine if we are setting the event time or the registration deadline.
     */
    private void showDateTimePicker(Button button, boolean isEventTime) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Show Date Picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Show Time Picker after Date is selected
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view1, hourOfDay, minute1) -> {
                String formattedDateTime = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d",
                        year1, month1 + 1, dayOfMonth, hourOfDay, minute1);
                button.setText(formattedDateTime);
                
                // Store the result in the corresponding variable
                if (isEventTime) {
                    scheduledDateTime = formattedDateTime;
                } else {
                    registrationDeadline = formattedDateTime;
                }
            }, hour, minute, true);
            timePickerDialog.show();
        }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Validates input fields and creates an Event object.
     * If validation passes, the event is saved to Firebase Firestore.
     */
    private void createEvent() {
        // Read values from input fields
        String title = etEventTitle.getText().toString().trim();
        String capacityStr = etMaxCapacity.getText().toString().trim();
        String details = etEventDetails.getText().toString().trim();

        // 1. Validation Logic
        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scheduledDateTime.isEmpty()) {
            Toast.makeText(this, "Event date and time is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (registrationDeadline.isEmpty()) {
            Toast.makeText(this, "Registration deadline is required", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxCapacity;
        if (capacityStr.isEmpty()) {
            Toast.makeText(this, "Maximum capacity is required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            maxCapacity = Integer.parseInt(capacityStr);
            if (maxCapacity <= 0) {
                Toast.makeText(this, "Maximum capacity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid capacity value", Toast.LENGTH_SHORT).show();
            return;
        }

        if (posterUriString.isEmpty()) {
            Toast.makeText(this, "Event poster is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Event object creation
        // Generate a unique ID for the event
        String eventId = UUID.randomUUID().toString();
        String organizerId = "organizer_123"; // Placeholder for the current user's ID

        // Generate QR code content if not already pre-generated
        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        // Create the Event data model
        Event newEvent = new Event(
                eventId,
                title,
                scheduledDateTime,
                registrationDeadline,
                maxCapacity,
                details,
                posterUriString,
                qrCodeContent,
                organizerId
        );

        /**
         * 3. Save to Firestore.
         * Uploads the event object to the "events" collection using eventId as the document ID.
         */
        db.collection("events").document(eventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event successfully written to Firestore!");
                    Toast.makeText(CreateEventActivity.this, "Event created successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Return to the dashboard after successful creation
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(CreateEventActivity.this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }
}
