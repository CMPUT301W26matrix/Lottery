package com.example.lottery;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.Event;
import com.example.lottery.util.EventValidationUtils;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Activity for organizers to create or edit events.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Provides UI for entering event details (Title, Date, Capacity, etc.).</li>
 *   <li>Handles both creation of new events and editing of existing ones.</li>
 *   <li>Enforces business rules such as registration deadline validation
 *       and waiting list limit enforcement.</li>
 *   <li>Manages promotional QR code generation and poster selection.</li>
 * </ul>
 * </p>
 */
public class OrganizerCreateEventActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerCreateEvent";

    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails, etWaitingListLimit;
    private TextInputEditText etEventStart, etEventEnd, etRegStart, etRegEnd, etDrawDate;
    private TextInputLayout tilWaitingListLimit;
    private Button btnOpenUploadDialog, btnGenerateQRCode, btnCreateEvent;
    private ImageButton btnBack;
    private ImageView ivQRCodePreview, ivPosterPreview;
    private TextView tvQRCodeLabel, tvPosterStatus, tvHeader;
    private MaterialCardView cvQRCode;
    private SwitchMaterial swRequireLocation, swLimitWaitingList;

    // Core data variables
    /**
     * Unique identifier for the event.
     */
    private String eventId = UUID.randomUUID().toString();
    /**
     * Content encoded within the event's promotional QR code.
     */
    private String qrCodeContent = "";
    /**
     * Date objects representing various event deadlines and scheduled times.
     */
    private Date eventStartDate, eventEndDate, regStartDate, regEndDate, drawDate;
    /**
     * Flag indicating whether the activity is in edit mode for an existing event.
     */
    private boolean isEditMode = false;

    /**
     * URI of the selected poster image.
     */
    private Uri selectedPosterUri = null;
    /**
     * Current poster value loaded from Firestore. This may already be a remote download URL.
     */
    private String existingPosterUri = "";
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Firebase Storage instance for poster uploads.
     */
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_create_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        String existingEventId = getIntent().getStringExtra("eventId");
        if (existingEventId != null) {
            isEditMode = true;
            eventId = existingEventId;
            tvHeader.setText("Edit Event");
            btnCreateEvent.setText("Update Event");
            loadEventData(existingEventId);
        }

        setupDialogCallback();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnOpenUploadDialog.setOnClickListener(v -> {
            OrganizerUploadPosterDialogFragment dialog = new OrganizerUploadPosterDialogFragment();
            dialog.show(getSupportFragmentManager(), "upload_poster");
        });

        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());
        btnCreateEvent.setOnClickListener(v -> validateAndSaveEvent());

        // US 02.02.02: Handle waiting list limit toggle
        swLimitWaitingList.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilWaitingListLimit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etWaitingListLimit.setText("");
            }
        });
    }

    /**
     * Initializes UI component references and sets up click listeners for date pickers.
     */
    private void initializeViews() {
        tvHeader = findViewById(R.id.tvHeader);
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);

        etEventStart = findViewById(R.id.etEventStart);
        etEventEnd = findViewById(R.id.etEventEnd);
        etRegStart = findViewById(R.id.etRegStart);
        etRegEnd = findViewById(R.id.etRegEnd);
        etDrawDate = findViewById(R.id.etDrawDate);

        etEventStart.setOnClickListener(v -> showDateTimePicker(etEventStart, "eventStart"));
        etEventEnd.setOnClickListener(v -> showDateTimePicker(etEventEnd, "eventEnd"));
        etRegStart.setOnClickListener(v -> showDateTimePicker(etRegStart, "regStart"));
        etRegEnd.setOnClickListener(v -> showDateTimePicker(etRegEnd, "regEnd"));
        etDrawDate.setOnClickListener(v -> showDateTimePicker(etDrawDate, "drawDate"));

        btnOpenUploadDialog = findViewById(R.id.btnOpenUploadDialog);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnBack = findViewById(R.id.btnBack);

        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        tvPosterStatus = findViewById(R.id.tvPosterStatus);
        cvQRCode = findViewById(R.id.cvQRCode);

        swRequireLocation = findViewById(R.id.swRequireLocation);
        swLimitWaitingList = findViewById(R.id.swLimitWaitingList);
        tilWaitingListLimit = findViewById(R.id.tilWaitingListLimit);
        etWaitingListLimit = findViewById(R.id.etWaitingListLimit);
    }

    /**
     * Loads existing event data from Firestore and populates the UI fields.
     *
     * @param existingEventId The unique ID of the event to load.
     */
    private void loadEventData(String existingEventId) {
        db.collection("events").document(existingEventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                return;
            }

            Event event = doc.toObject(Event.class);
            if (event == null) {
                return;
            }

            etEventTitle.setText(event.getTitle());
            etMaxCapacity.setText(String.valueOf(event.getMaxCapacity()));
            etEventDetails.setText(event.getDetails());
            swRequireLocation.setChecked(event.isRequireLocation());

            // US 02.03.01: Load waiting list limit
            if (event.getWaitingListLimit() != null) {
                swLimitWaitingList.setChecked(true);
                etWaitingListLimit.setText(String.valueOf(event.getWaitingListLimit()));
                tilWaitingListLimit.setVisibility(View.VISIBLE);
            }

            String posterUri = event.getPosterUri();
            if (posterUri != null && !posterUri.isEmpty() && ivPosterPreview != null) {
                existingPosterUri = posterUri;
                selectedPosterUri = Uri.parse(posterUri);
                PosterImageLoader.load(ivPosterPreview, posterUri, R.drawable.event_placeholder);
                ivPosterPreview.setVisibility(View.VISIBLE);
                tvPosterStatus.setText("Poster selected");
                tvPosterStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
                btnOpenUploadDialog.setText(R.string.update_poster);
            }

            this.qrCodeContent = event.getQrCodeContent();
            this.eventStartDate = event.getScheduledDateTime();
            this.eventEndDate = event.getEventEndDate();
            this.regStartDate = event.getRegistrationStartDate();
            this.regEndDate = event.getRegistrationDeadline();
            this.drawDate = event.getDrawDate();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            if (eventStartDate != null) {
                etEventStart.setText(sdf.format(eventStartDate));
            }
            if (eventEndDate != null) {
                etEventEnd.setText(sdf.format(eventEndDate));
            }
            if (regStartDate != null) {
                etRegStart.setText(sdf.format(regStartDate));
            }
            if (regEndDate != null) {
                etRegEnd.setText(sdf.format(regEndDate));
            }
            if (drawDate != null) {
                etDrawDate.setText(sdf.format(drawDate));
            }

        });
    }

    /**
     * Sets up the fragment result listener for receiving poster image selection results.
     */
    private void setupDialogCallback() {
        getSupportFragmentManager().setFragmentResultListener("posterRequest", this, (requestKey, bundle) -> {
            String uriString = bundle.getString("posterUri");
            if (uriString == null) {
                return;
            }

            selectedPosterUri = Uri.parse(uriString);
            tvPosterStatus.setText("Poster selected");
            tvPosterStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            btnOpenUploadDialog.setText(R.string.update_poster);

            if (ivPosterPreview != null) {
                PosterImageLoader.load(ivPosterPreview, selectedPosterUri, R.drawable.event_placeholder);
                ivPosterPreview.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Generates a unique QR code for the event and displays it in the UI.
     */
    private void generateAndDisplayQRCode() {
        qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(qrCodeContent);
        if (qrBitmap != null) {
            ivQRCodePreview.setImageBitmap(qrBitmap);
            tvQRCodeLabel.setVisibility(View.VISIBLE);
            cvQRCode.setVisibility(View.VISIBLE);
            Toast.makeText(this, "QR Code Generated!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a date and time picker dialog and updates the provided EditText with the selection.
     *
     * @param editText  The EditText to update with the formatted date string.
     * @param fieldType The type of field being updated (e.g., "eventStart", "regEnd").
     */
    private void showDateTimePicker(final TextInputEditText editText, final String fieldType) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                new TimePickerDialog(this, (v, hour, min) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, hour, min);
                    Date date = selected.getTime();
                    String formattedDate = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d %02d:%02d",
                            month + 1,
                            day,
                            year,
                            hour,
                            min
                    );
                    editText.setText(formattedDate);
                    switch (fieldType) {
                        case "eventStart":
                            eventStartDate = date;
                            break;
                        case "eventEnd":
                            eventEndDate = date;
                            break;
                        case "regStart":
                            regStartDate = date;
                            break;
                        case "regEnd":
                            regEndDate = date;
                            break;
                        case "drawDate":
                            drawDate = date;
                            break;
                        default:
                            break;
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Validates all input fields and business rules before saving the event to Firestore.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>Title, Start Date, and Registration End are required.</li>
     *   <li>Registration must end before the event starts.</li>
     *   <li>Waiting list limit must be a positive integer.</li>
     *   <li>New limit cannot be less than the current number of entrants when editing.</li>
     * </ul>
     * </p>
     */
    private void validateAndSaveEvent() {
        String title = Objects.requireNonNull(etEventTitle.getText()).toString().trim();
        String capacityStr = Objects.requireNonNull(etMaxCapacity.getText()).toString().trim();
        String details = Objects.requireNonNull(etEventDetails.getText()).toString().trim();
        String waitingLimitStr = Objects.requireNonNull(etWaitingListLimit.getText()).toString().trim();

        // 1.1 Event must have title
        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1.2 Event must have start date and time
        if (eventStartDate == null) {
            Toast.makeText(this, "Event date and time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1.3 Event must have registration deadline
        if (regEndDate == null) {
            Toast.makeText(this, "Registration deadline is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!EventValidationUtils.isRegistrationDeadlineValid(regEndDate, eventStartDate)) {
            Toast.makeText(this, "Registration must end before the event starts", Toast.LENGTH_LONG).show();
            return;
        }

        // US 02.02.02: Validate waiting list limit
        Integer waitingListLimit = null;
        if (swLimitWaitingList.isChecked()) {
            if (waitingLimitStr.isEmpty()) {
                Toast.makeText(this, "Please enter a waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                waitingListLimit = Integer.parseInt(waitingLimitStr);
                if (!EventValidationUtils.isWaitingListLimitValid(waitingListLimit)) {
                    Toast.makeText(this, "Limit must be a positive integer (>0)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final Integer finalWaitingListLimit = waitingListLimit;

        // US 02.02.02: AC #3: Check if new limit is smaller than current entrants when editing
        if (isEditMode && finalWaitingListLimit != null) {
            db.collection("events").document(eventId).collection("entrants").get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.size() > finalWaitingListLimit) {
                            Toast.makeText(
                                    this,
                                    "New limit (" + finalWaitingListLimit + ") cannot be less than current entrants (" + snapshots.size() + ")",
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            persistEvent(title, capacityStr, details, finalWaitingListLimit);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking entrants", e);
                        persistEvent(title, capacityStr, details, finalWaitingListLimit);
                    });
        } else {
            persistEvent(title, capacityStr, details, finalWaitingListLimit);
        }
    }

    private void persistEvent(String title, String capacityStr, String details, Integer waitingListLimit) {
        btnCreateEvent.setEnabled(false);

        if (isLocalUri(selectedPosterUri)) {
            uploadPosterAndSaveEvent(title, capacityStr, details, waitingListLimit, selectedPosterUri);
            return;
        }

        String posterUriToSave = selectedPosterUri != null ? selectedPosterUri.toString() : "";
        saveEventToFirestore(title, capacityStr, details, waitingListLimit, posterUriToSave);
    }

    private boolean isLocalUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return "content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme);
    }

    private void uploadPosterAndSaveEvent(String title,
                                          String capacityStr,
                                          String details,
                                          Integer waitingListLimit,
                                          Uri posterUri) {
        StorageReference posterRef = storage.getReference()
                .child("event_posters/" + eventId + "/" + UUID.randomUUID() + ".jpg");

        posterRef.putFile(posterUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                        throw new IllegalStateException("Poster upload failed");
                    }
                    return posterRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri ->
                        saveEventToFirestore(
                                title,
                                capacityStr,
                                details,
                                waitingListLimit,
                                downloadUri.toString()
                        ))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload poster", e);
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to upload poster", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Constructs an Event object and persists it to the Firestore database.
     *
     * @param title            The title of the event.
     * @param capacityStr      The string representation of the maximum capacity.
     * @param details          The detailed description of the event.
     * @param waitingListLimit The limit for the waiting list (null for unlimited).
     */
    private void saveEventToFirestore(String title,
                                      String capacityStr,
                                      String details,
                                      Integer waitingListLimit,
                                      String posterUriToSave) {
        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        int maxCapacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);
        boolean requireLocation = swRequireLocation.isChecked();

        Event event = new Event(
                eventId,
                title,
                eventStartDate,
                eventEndDate,
                regStartDate,
                regEndDate,
                drawDate,
                maxCapacity,
                details,
                posterUriToSave,
                qrCodeContent,
                "organizer_current_user",
                requireLocation,
                waitingListLimit
        );

        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    deleteReplacedPosterIfNeeded(posterUriToSave);
                    String msg = isEditMode ? "Event Updated Successfully!" : "Event Launched Successfully!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document", e);
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteReplacedPosterIfNeeded(String newPosterUri) {
        if (existingPosterUri == null || existingPosterUri.isEmpty()) {
            return;
        }
        if (existingPosterUri.equals(newPosterUri)) {
            return;
        }

        try {
            if (existingPosterUri.startsWith("gs://")
                    || existingPosterUri.contains("firebasestorage.googleapis.com")) {
                FirebaseStorage.getInstance().getReferenceFromUrl(existingPosterUri)
                        .delete()
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to delete replaced poster", e));
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse existing poster URI for cleanup", e);
        }
    }
}
