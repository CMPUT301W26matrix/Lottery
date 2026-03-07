package com.example.lottery;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for organizers to create new events (US 02.01.01).
 * Handles event details, poster selection, and promotional QR code generation.
 * Also integrates registration deadline logic (US 02.01.04).
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components for user input and interaction
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails;
    private Button btnEventDateTime, btnRegistrationDeadline, btnUploadPoster, btnGenerateQRCode, btnCreateEvent;
    private ImageView ivPosterPreview, ivQRCodePreview;
    private TextView tvQRCodeLabel;
    private MaterialCardView cvQRCode;
    
    // Core data variables for the new event
    // Fixed eventId ensures consistency across Firestore and QR code content
    private final String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventDate;
    private Date deadlineDate;
    private String posterUriString = "";

    // Result launcher for gallery image picking
    private ActivityResultLauncher<String> getContentLauncher;
    
    // Firebase Firestore instance for database operations
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firestore with error handling for safety
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link UI components and setup initial states
        initializeViews();
        setupImagePicker();

        // Set click listeners for various UI actions
        // US 02.01.01: Manual QR generation
        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());

        // Final event creation submission
        btnCreateEvent.setOnClickListener(v -> createEvent());

        // Date and Time selection listeners
        btnEventDateTime.setOnClickListener(v -> showDateTimePicker(btnEventDateTime, true));
        btnRegistrationDeadline.setOnClickListener(v -> showDateTimePicker(btnRegistrationDeadline, false));
    }

    /**
     * Finds and assigns UI components from the layout XML.
     */
    private void initializeViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        btnEventDateTime = findViewById(R.id.btnEventDateTime);
        btnRegistrationDeadline = findViewById(R.id.btnRegistrationDeadline);
        btnUploadPoster = findViewById(R.id.btnUploadPoster);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        
        // Views related to the QR code display (added in US 02.01.01 upgrade)
        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        cvQRCode = findViewById(R.id.cvQRCode);
    }

    /**
     * Generates a unique QR content string using the eventId and a seed,
     * then renders it into a Bitmap using ZXing and displays it in the UI.
     * This fulfills the visual feedback requirement for US 02.01.01.
     */
    private void generateAndDisplayQRCode() {
        qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(qrCodeContent);
        
        if (qrBitmap != null) {
            ivQRCodePreview.setImageBitmap(qrBitmap);
            tvQRCodeLabel.setVisibility(View.VISIBLE);
            cvQRCode.setVisibility(View.VISIBLE);
            Log.d(TAG, "QR Code displayed for event: " + eventId);
            Toast.makeText(this, "QR Code Generated!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializes the Activity Result Launcher for selecting an event poster from the gallery.
     */
    private void setupImagePicker() {
        getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        posterUriString = uri.toString();
                        ivPosterPreview.setImageURI(uri);
                    }
                });
        btnUploadPoster.setOnClickListener(v -> getContentLauncher.launch("image/*"));
    }

    /**
     * Displays a DatePickerDialog followed by a TimePickerDialog to select a full timestamp.
     * 
     * @param button The button whose text will be updated with the selected timestamp.
     * @param isEventTime True if setting the event start time, false if setting the registration deadline.
     */
    private void showDateTimePicker(Button button, boolean isEventTime) {
        final Calendar calendar = Calendar.getInstance();
        
        // First, show the DatePicker
        new DatePickerDialog(this, (view, year, month, day) -> {
            // Once the date is picked, show the TimePicker
            new TimePickerDialog(this, (v, hour, min) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, min);
                Date date = selected.getTime();
                
                // Format and display the selected date/time string on the button
                button.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, min));
                
                // Store the selected date object in the appropriate class member
                if (isEventTime) {
                    eventDate = date;
                } else {
                    deadlineDate = date;
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Validates user inputs and persists the new event to Firebase Firestore.
     * Logic ensures compliance with US 02.01.01 (Event creation & QR) and US 02.01.04 (Registration deadline).
     */
    private void createEvent() {
        // Collect and trim user inputs
        String title = etEventTitle.getText().toString().trim();
        String capacityStr = etMaxCapacity.getText().toString().trim();
        String details = etEventDetails.getText().toString().trim();

        // 1. Mandatory Field Validation (US 02.01.04 Requirement)
        if (title.isEmpty() || eventDate == null || deadlineDate == null) {
            Toast.makeText(this, "All fields including dates are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Chronological Order Validation (US 02.01.04 Requirement)
        // Registration deadline MUST be strictly before the event start time.
        if (!deadlineDate.before(eventDate)) {
            Toast.makeText(this, "Registration deadline must be before the event start time", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Fallback QR Content Generation (US 02.01.01 Requirement)
        // If the user hasn't manually pre-generated a QR code, create it automatically before saving.
        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        // Parse optional maximum capacity field
        int maxCapacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);

        // 4. Data Model Creation
        // Create the Event instance containing all collected metadata.
        Event newEvent = new Event(
                eventId,
                title,
                eventDate,
                deadlineDate,
                maxCapacity,
                details,
                posterUriString,
                qrCodeContent,
                "organizer_current_user" // Placeholder: Will be replaced by actual Auth ID in future sprints
        );

        // 5. Firestore Persistence
        // Save the event document to the "events" collection using the unique eventId.
        db.collection("events").document(eventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document successfully written with ID: " + eventId);
                    Toast.makeText(this, "Event Launched Successfully!", Toast.LENGTH_SHORT).show();
                    // Finish the activity and return to the dashboard upon success
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document", e);
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }
}
