package com.example.lottery.organizer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.R;
import com.example.lottery.fragment.OrganizerUploadPosterDialogFragment;
import com.example.lottery.model.Event;
import com.example.lottery.util.EventValidationUtils;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    // Validated in validateAndSaveEvent() — not via InputFilter to avoid truncating loadEventData().
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DETAILS_LENGTH = 500;
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails, etPlace, etWaitingListLimit;
    // Activity Result Launcher for Google Places Autocomplete
    private final ActivityResultLauncher<Intent> startAutocomplete = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (etPlace != null) {
                        etPlace.setText(place.getFormattedAddress());
                    }
                    Log.d(TAG, "Place selected: " + place.getDisplayName() + ", " + place.getFormattedAddress());
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR && result.getData() != null) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Log.e(TAG, "Autocomplete error: " + status.getStatusMessage());
                    Toast.makeText(this, "Error selecting location", Toast.LENGTH_SHORT).show();
                }
            });
    private TextInputEditText etEventStart, etEventEnd, etRegStart, etRegEnd, etDrawDate;
    private TextInputLayout tilWaitingListLimit;
    private Button btnOpenUploadDialog, btnGenerateQRCode, btnCreateEvent;
    private ImageButton btnBack;
    private ImageView ivQRCodePreview, ivPosterPreview;
    private TextView tvQRCodeLabel, tvPosterStatus, tvHeader;
    private MaterialCardView cvQRCode, cardQRCode;
    private SwitchMaterial swRequireLocation, swLimitWaitingList, swIsPrivate;

    // Core data variables
    private ChipGroup cgCategories;
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
     * URI of the selected poster image (can be a local Uri or a Base64 string parsed as Uri).
     */
    private Uri selectedPosterSource = null;
    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_create_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }

        if (userId == null) {
            Log.e(TAG, "User ID is null, finishing activity");
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
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

        swLimitWaitingList.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilWaitingListLimit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etWaitingListLimit.setText("");
            }
        });

        swIsPrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                qrCodeContent = "";
                cardQRCode.setVisibility(View.GONE);
            } else {
                cardQRCode.setVisibility(View.VISIBLE);
                btnGenerateQRCode.setVisibility(View.VISIBLE);
            }
        });

        setupNavigation();
    }

    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
                intent.putExtra("userId", userId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnNotifications = findViewById(R.id.nav_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerNotificationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        View btnQr = findViewById(R.id.nav_qr_code);
        if (btnQr != null) {
            btnQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerQrEventListActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        View btnProfile = findViewById(R.id.nav_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }
    }

    private void initializeViews() {
        tvHeader = findViewById(R.id.tvHeader);
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        etPlace = findViewById(R.id.etPlace);

        // Setup Google Places Autocomplete on etPlace click
        if (etPlace != null) {
            etPlace.setOnClickListener(v -> {
                // Request ID, DISPLAY_NAME, FORMATTED_ADDRESS, and LOCATION fields for the selected place
                // These are the new field names for SDK 5.0.0+
                List<Place.Field> fields = Arrays.asList(
                        Place.Field.ID,
                        Place.Field.DISPLAY_NAME,
                        Place.Field.FORMATTED_ADDRESS,
                        Place.Field.LOCATION);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(this);
                startAutocomplete.launch(intent);
            });
        }

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
        cardQRCode = findViewById(R.id.cardQRCode);

        swRequireLocation = findViewById(R.id.swRequireLocation);
        swLimitWaitingList = findViewById(R.id.swLimitWaitingList);
        swIsPrivate = findViewById(R.id.swIsPrivate);
        tilWaitingListLimit = findViewById(R.id.tilWaitingListLimit);
        etWaitingListLimit = findViewById(R.id.etWaitingListLimit);
        cgCategories = findViewById(R.id.cgCategories);
    }

    /**
     * Loads existing event data from Firestore and populates the UI fields.
     *
     * @param existingEventId The unique ID of the event to load.
     */
    private void loadEventData(String existingEventId) {
        db.collection(FirestorePaths.EVENTS).document(existingEventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Event event = doc.toObject(Event.class);
            if (event == null) return;

            etEventTitle.setText(event.getTitle());
            etMaxCapacity.setText(String.valueOf(event.getCapacity()));
            etEventDetails.setText(event.getDetails());
            if (event.getPlace() != null) {
                etPlace.setText(event.getPlace());
            }
            swRequireLocation.setChecked(event.isRequireLocation());
            swIsPrivate.setChecked(event.isPrivate());

            // US 02.03.01: Load waiting list limit
            if (event.getWaitingListLimit() != null) {
                swLimitWaitingList.setChecked(true);
                etWaitingListLimit.setText(String.valueOf(event.getWaitingListLimit()));
                tilWaitingListLimit.setVisibility(View.VISIBLE);
            }

            String posterBase64 = event.getPosterBase64();
            if (posterBase64 != null && posterBase64.trim().startsWith("data:image") && ivPosterPreview != null) {
                selectedPosterSource = Uri.parse(posterBase64);
                PosterImageLoader.load(ivPosterPreview, posterBase64, R.drawable.event_placeholder);
                ivPosterPreview.setVisibility(View.VISIBLE);
                tvPosterStatus.setText("Poster selected");
                tvPosterStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
                btnOpenUploadDialog.setText(R.string.update_poster);
            }

            this.qrCodeContent = event.getQrCodeContent();
            this.eventStartDate = event.getScheduledDateTime() != null ? event.getScheduledDateTime().toDate() : null;
            this.eventEndDate = event.getEventEndDateTime() != null ? event.getEventEndDateTime().toDate() : null;
            this.regStartDate = event.getRegistrationStart() != null ? event.getRegistrationStart().toDate() : null;
            this.regEndDate = event.getRegistrationDeadline() != null ? event.getRegistrationDeadline().toDate() : null;
            this.drawDate = event.getDrawDate() != null ? event.getDrawDate().toDate() : null;

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            if (eventStartDate != null) etEventStart.setText(sdf.format(eventStartDate));
            if (eventEndDate != null) etEventEnd.setText(sdf.format(eventEndDate));
            if (regStartDate != null) etRegStart.setText(sdf.format(regStartDate));
            if (regEndDate != null) etRegEnd.setText(sdf.format(regEndDate));
            if (drawDate != null) etDrawDate.setText(sdf.format(drawDate));

            // Display QR code if it exists and event is not private
            if (qrCodeContent != null && !qrCodeContent.isEmpty() && !event.isPrivate()) {
                Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(qrCodeContent);
                if (qrBitmap != null) {
                    ivQRCodePreview.setImageBitmap(qrBitmap);
                    tvQRCodeLabel.setVisibility(View.VISIBLE);
                    cvQRCode.setVisibility(View.VISIBLE);
                    btnGenerateQRCode.setVisibility(View.GONE);
                }
            } else if (event.isPrivate()) {
                cardQRCode.setVisibility(View.GONE);
            }

            // Set category
            if (event.getCategory() != null) {
                for (int i = 0; i < cgCategories.getChildCount(); i++) {
                    View child = cgCategories.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        if (chip.getText().toString().equalsIgnoreCase(event.getCategory())) {
                            chip.setChecked(true);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets up the fragment result listener for receiving poster image selection results.
     */
    private void setupDialogCallback() {
        getSupportFragmentManager().setFragmentResultListener("posterRequest", this, (requestKey, bundle) -> {
            String posterSource = bundle.getString("posterBase64");
            if (posterSource == null) return;

            selectedPosterSource = Uri.parse(posterSource);
            tvPosterStatus.setText("Poster selected");
            tvPosterStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            btnOpenUploadDialog.setText(R.string.update_poster);

            if (ivPosterPreview != null) {
                PosterImageLoader.load(ivPosterPreview, selectedPosterSource, R.drawable.event_placeholder);
                ivPosterPreview.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Generates a unique QR code for the event and displays it in the UI.
     */
    private void generateAndDisplayQRCode() {
        if (swIsPrivate.isChecked()) {
            Toast.makeText(this, "Private events do not have promotional QR codes", Toast.LENGTH_SHORT).show();
            return;
        }
        qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(qrCodeContent);
        if (qrBitmap != null) {
            ivQRCodePreview.setImageBitmap(qrBitmap);
            tvQRCodeLabel.setVisibility(View.VISIBLE);
            cvQRCode.setVisibility(View.VISIBLE);
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
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d", month + 1, day, year, hour, min);
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
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Validates all input fields and business rules before saving the event to Firestore.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>Title, Start Date, Registration End, Draw Date and Capacity are required.</li>
     *   <li>Title must be at most {@value #MAX_TITLE_LENGTH} characters; details at most {@value #MAX_DETAILS_LENGTH}.</li>
     *   <li>Registration must end before the event starts.</li>
     *   <li>Draw date must be strictly after registration end and on or before event start.</li>
     *   <li>Capacity and Waiting list limit must be positive integers.</li>
     *   <li>New limit cannot be less than the current number of entrants when editing.</li>
     * </ul>
     * </p>
     */
    private void validateAndSaveEvent() {
        Log.d(TAG, "Starting validation...");
        String title = etEventTitle.getText() != null ? etEventTitle.getText().toString().trim() : "";
        String capacityStr = etMaxCapacity.getText() != null ? etMaxCapacity.getText().toString().trim() : "";
        String details = etEventDetails.getText() != null ? etEventDetails.getText().toString().trim() : "";
        String place = etPlace.getText() != null ? etPlace.getText().toString().trim() : "";
        String waitingLimitStr = etWaitingListLimit.getText() != null ? etWaitingListLimit.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Title is empty");
            return;
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            Toast.makeText(this, "Event title must be at most " + MAX_TITLE_LENGTH + " characters", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Title too long");
            return;
        }
        if (details.length() > MAX_DETAILS_LENGTH) {
            Toast.makeText(this, "Event details must be at most " + MAX_DETAILS_LENGTH + " characters", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Details too long");
            return;
        }
        if (eventStartDate == null) {
            Toast.makeText(this, "Event date and time are required", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Event start date is null");
            return;
        }
        if (regEndDate == null) {
            Toast.makeText(this, "Registration deadline is required", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Registration deadline is null");
            return;
        }

        if (!EventValidationUtils.isRegistrationDeadlineValid(regEndDate, eventStartDate)) {
            Toast.makeText(this, "Registration must end before the event starts", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Invalid registration deadline sequence");
            return;
        }

        if (!EventValidationUtils.isEventEndDateValid(eventStartDate, eventEndDate)) {
            Toast.makeText(this, "Event end date must be after the start date", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Event end date before start date");
            return;
        }

        if (!EventValidationUtils.isRegistrationStartValid(regStartDate, regEndDate)) {
            Toast.makeText(this, "Registration start must be before registration end", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Registration start after registration end");
            return;
        }

        if (drawDate == null) {
            Toast.makeText(this, "Draw date is required", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Draw date is null");
            return;
        }
        if (!EventValidationUtils.isDrawDateValid(drawDate, regEndDate, eventStartDate)) {
            Toast.makeText(this, "Draw date must be after registration end and on or before the event start", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Invalid draw date");
            return;
        }

        // A missing/zero capacity breaks EntrantsListActivity.sampling(): getRemainingSlots()
        // collapses to 0 so maxSampleSize is 0 and no winners can ever be drawn.
        Integer parsedCapacity = parseRequiredPositiveInt(capacityStr, "Event Capacity");
        if (parsedCapacity == null) return;

        // US 02.03.01: waiting list limit is optional — null means "Unlimited"
        Integer waitingListLimit = null;
        if (swLimitWaitingList.isChecked()) {
            waitingListLimit = parseRequiredPositiveInt(waitingLimitStr, "Waiting List Limit");
            if (waitingListLimit == null) return;
        }

        final Integer finalWaitingListLimit = waitingListLimit;
        Log.d(TAG, "Validation passed, proceeding to persist event");

        // US 02.03.01: AC #3: Check if new limit is smaller than current entrants when editing
        if (isEditMode && finalWaitingListLimit != null) {
            db.collection(FirestorePaths.eventWaitingList(eventId)).get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.size() > finalWaitingListLimit) {
                            Toast.makeText(this, "New limit cannot be less than current entrants", Toast.LENGTH_LONG).show();
                        } else {
                            persistEvent(title, parsedCapacity, details, place, finalWaitingListLimit);
                        }
                    })
                    .addOnFailureListener(e -> persistEvent(title, parsedCapacity, details, place, finalWaitingListLimit));
        } else {
            persistEvent(title, parsedCapacity, details, place, finalWaitingListLimit);
        }
    }

    /**
     * Parses a required positive integer field from a raw text value, toasting the
     * appropriate error and logging a warning when validation fails. Returns null on
     * any failure so callers can early-return; the successful value is always > 0.
     */
    private Integer parseRequiredPositiveInt(String raw, String fieldLabel) {
        if (raw.isEmpty()) {
            Toast.makeText(this, fieldLabel + " is required", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: " + fieldLabel + " is empty");
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed <= 0) {
                Toast.makeText(this, fieldLabel + " must be a positive integer (>0)", Toast.LENGTH_SHORT).show();
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number for " + fieldLabel.toLowerCase(Locale.US), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Persists the event to Firestore, handling poster conversion to Base64 if needed.
     */
    private void persistEvent(String title, int capacity, String details, String place, Integer waitingListLimit) {
        Log.d(TAG, "persistEvent started: Base64 mode");
        btnCreateEvent.setEnabled(false);

        String posterData = "";
        if (selectedPosterSource != null) {
            String scheme = selectedPosterSource.getScheme();
            if ("content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme)) {
                // New local image -> convert to compressed Base64
                posterData = convertUriToBase64(selectedPosterSource);
                if (posterData.isEmpty()) {
                    // Conversion failed or image too large — abort save
                    btnCreateEvent.setEnabled(true);
                    return;
                }
            } else {
                // Existing URL or Base64 -> keep as is
                posterData = selectedPosterSource.toString();
            }
        }

        saveEventToFirestore(title, capacity, details, place, waitingListLimit, posterData);
    }

    /**
     * Converts a image Uri to a compressed Base64 string.
     *
     * @param uri The Uri of the image to convert.
     * @return A Base64 string prefixed with "data:image/jpeg;base64,".
     */
    private String convertUriToBase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            // US 02.04.01: Reasonable compression for Firestore storage
            int maxSize = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float ratio = (float) width / (float) height;
                if (ratio > 1) {
                    width = maxSize;
                    height = (int) (maxSize / ratio);
                } else {
                    height = maxSize;
                    width = (int) (maxSize * ratio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            // Firestore document limit is 1 MB; leave room for Base64 expansion and other fields
            if (bytes.length > 500_000) {
                Log.w(TAG, "Poster too large after compression (" + bytes.length + " bytes), skipping");
                Toast.makeText(this, "Image too large. Please choose a smaller image.", Toast.LENGTH_SHORT).show();
                return "";
            }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, "Base64 conversion failed", e);
            return "";
        }
    }

    /**
     * Final step to save the event object to Firestore.
     */
    private void saveEventToFirestore(String title, int capacity, String details, String place, Integer waitingListLimit, String posterBase64ToSave) {
        Log.d(TAG, "saveEventToFirestore started");

        boolean isPrivate = swIsPrivate.isChecked();

        if (isPrivate) {
            qrCodeContent = ""; // No promotional QR for private events
        } else if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        String category = "Other";
        int checkedChipId = cgCategories.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = findViewById(checkedChipId);
            if (chip != null) {
                category = chip.getText().toString();
            }
        }

        // Use LinkedHashMap to control the order of fields in Firestore console.
        // posterBase64 is put last to ensure it appears at the bottom.
        Map<String, Object> eventMap = new LinkedHashMap<>();
        eventMap.put("eventId", eventId);
        eventMap.put("title", title);
        eventMap.put("details", details);
        eventMap.put("place", place);
        eventMap.put("organizerId", userId);
        eventMap.put("capacity", capacity);
        eventMap.put("waitingListLimit", waitingListLimit);
        eventMap.put("qrCodeContent", qrCodeContent);
        eventMap.put("scheduledDateTime", eventStartDate != null ? new Timestamp(eventStartDate) : null);
        eventMap.put("eventEndDateTime", eventEndDate != null ? new Timestamp(eventEndDate) : null);
        eventMap.put("registrationStart", regStartDate != null ? new Timestamp(regStartDate) : null);
        eventMap.put("registrationDeadline", regEndDate != null ? new Timestamp(regEndDate) : null);
        eventMap.put("drawDate", drawDate != null ? new Timestamp(drawDate) : null);
        eventMap.put("requireLocation", swRequireLocation.isChecked());
        eventMap.put("private", isPrivate);
        eventMap.put("category", category);

        Timestamp now = Timestamp.now();
        eventMap.put("updatedAt", now);
        if (!isEditMode) {
            eventMap.put("status", "open");
            eventMap.put("createdAt", now);
        }

        // Put the large Base64 field at the very end
        eventMap.put("posterBase64", posterBase64ToSave);
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .set(eventMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event saved to Firestore successfully");
                    Toast.makeText(this, isEditMode ? "Event Updated Successfully!" : "Event Launched Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save event to Firestore", e);
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to save event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
