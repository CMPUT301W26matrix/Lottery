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
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails;
    private Button btnEventDateTime, btnRegistrationDeadline, btnUploadPoster, btnGenerateQRCode, btnCreateEvent;
    private ImageView ivPosterPreview, ivQRCodePreview;
    private TextView tvQRCodeLabel;
    private MaterialCardView cvQRCode;
    
    // Event Data Variables - Fixed eventId to ensure consistency
    private final String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventDate;
    private Date deadlineDate;
    private String posterUriString = "";

    private ActivityResultLauncher<String> getContentLauncher;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firestore
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupImagePicker();

        // US 02.01.01: Generate unique promotional QR content tied to eventId and display it
        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());

        btnCreateEvent.setOnClickListener(v -> createEvent());

        btnEventDateTime.setOnClickListener(v -> showDateTimePicker(btnEventDateTime, true));
        btnRegistrationDeadline.setOnClickListener(v -> showDateTimePicker(btnRegistrationDeadline, false));
    }

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
        
        // QR Code Upgrade Views
        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        cvQRCode = findViewById(R.id.cvQRCode);
    }

    /**
     * US 02.01.01: Generates QR code content and renders it as a Bitmap for the UI.
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

    private void showDateTimePicker(Button button, boolean isEventTime) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, min) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, min);
                Date date = selected.getTime();
                button.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, min));
                if (isEventTime) eventDate = date; else deadlineDate = date;
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Logic for US 02.01.01 Firestore save.
     * Ensures Event object is stored in the "events" collection with eventId as the document ID.
     */
    private void createEvent() {
        String title = etEventTitle.getText().toString().trim();
        String capacityStr = etMaxCapacity.getText().toString().trim();
        String details = etEventDetails.getText().toString().trim();

        // Basic validation
        if (title.isEmpty() || eventDate == null) {
            Toast.makeText(this, "Title and Date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Auto-generate QR content if not manually generated
        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        int maxCapacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);

        // Create model instance (Shared model for US 02.01.01 & US 02.01.04)
        Event newEvent = new Event(
                eventId,
                title,
                eventDate,
                deadlineDate,
                maxCapacity,
                details,
                posterUriString,
                qrCodeContent,
                "organizer_current_user" // TODO: Get actual organizer ID
        );

        // Save to Firestore using eventId as document path
        db.collection("events").document(eventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document successfully written with ID: " + eventId);
                    Toast.makeText(this, "Event Launched Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document", e);
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }
}
