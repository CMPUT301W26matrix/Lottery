package com.example.lottery;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.EventValidationUtils;
import com.example.lottery.util.PosterImageLoader;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Activity for organizers to create or edit events.
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

    private String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventStartDate, eventEndDate, regStartDate, regEndDate, drawDate;
    private boolean isEditMode = false;
    private Uri selectedPosterUri = null;
    private String existingPosterUri = "";
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;

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

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = prefs.getString("userId", null);
        }

        if (userId == null) {
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

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                // Already on Create page
            });
        }
    }

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

    private void loadEventData(String existingEventId) {
        db.collection(FirestorePaths.EVENTS).document(existingEventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Event event = doc.toObject(Event.class);
            if (event == null) return;

            etEventTitle.setText(event.getTitle());
            etMaxCapacity.setText(String.valueOf(event.getCapacity()));
            etEventDetails.setText(event.getDetails());
            swRequireLocation.setChecked(event.isRequireLocation());

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
            this.eventStartDate = event.getScheduledDateTime() != null ? event.getScheduledDateTime().toDate() : null;
            this.eventEndDate = event.getEventEndDate() != null ? event.getEventEndDate().toDate() : null;
            this.regStartDate = event.getRegistrationStartDate() != null ? event.getRegistrationStartDate().toDate() : null;
            this.regEndDate = event.getRegistrationDeadline() != null ? event.getRegistrationDeadline().toDate() : null;
            this.drawDate = event.getDrawDate() != null ? event.getDrawDate().toDate() : null;

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            if (eventStartDate != null) etEventStart.setText(sdf.format(eventStartDate));
            if (eventEndDate != null) etEventEnd.setText(sdf.format(eventEndDate));
            if (regStartDate != null) etRegStart.setText(sdf.format(regStartDate));
            if (regEndDate != null) etRegEnd.setText(sdf.format(regEndDate));
            if (drawDate != null) etDrawDate.setText(sdf.format(drawDate));
        });
    }

    private void setupDialogCallback() {
        getSupportFragmentManager().setFragmentResultListener("posterRequest", this, (requestKey, bundle) -> {
            String uriString = bundle.getString("posterUri");
            if (uriString == null) return;

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
                        case "eventStart": eventStartDate = date; break;
                        case "eventEnd": eventEndDate = date; break;
                        case "regStart": regStartDate = date; break;
                        case "regEnd": regEndDate = date; break;
                        case "drawDate": drawDate = date; break;
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void validateAndSaveEvent() {
        String title = Objects.requireNonNull(etEventTitle.getText()).toString().trim();
        String capacityStr = Objects.requireNonNull(etMaxCapacity.getText()).toString().trim();
        String details = Objects.requireNonNull(etEventDetails.getText()).toString().trim();
        String waitingLimitStr = Objects.requireNonNull(etWaitingListLimit.getText()).toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventStartDate == null) {
            Toast.makeText(this, "Event date and time are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (regEndDate == null) {
            Toast.makeText(this, "Registration deadline is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!EventValidationUtils.isRegistrationDeadlineValid(regEndDate, eventStartDate)) {
            Toast.makeText(this, "Registration must end before the event starts", Toast.LENGTH_LONG).show();
            return;
        }

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

        if (isEditMode && finalWaitingListLimit != null) {
            db.collection(FirestorePaths.eventWaitingList(eventId)).get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.size() > finalWaitingListLimit) {
                            Toast.makeText(this, "New limit cannot be less than current entrants", Toast.LENGTH_LONG).show();
                        } else {
                            persistEvent(title, capacityStr, details, finalWaitingListLimit);
                        }
                    })
                    .addOnFailureListener(e -> persistEvent(title, capacityStr, details, finalWaitingListLimit));
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
        if (uri == null) return false;
        String scheme = uri.getScheme();
        return "content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme);
    }

    private void uploadPosterAndSaveEvent(String title, String capacityStr, String details, Integer waitingListLimit, Uri posterUri) {
        // Ensure user is authenticated before upload
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated, attempting anonymous sign-in before upload");
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    performUpload(title, capacityStr, details, waitingListLimit, posterUri);
                } else {
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(this, "Upload failed: Authentication error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performUpload(title, capacityStr, details, waitingListLimit, posterUri);
        }
    }

    private void performUpload(String title, String capacityStr, String details, Integer waitingListLimit, Uri posterUri) {
        StorageReference posterRef = storage.getReference().child("event_posters/" + eventId + "/" + UUID.randomUUID() + ".jpg");
        
        // Add metadata to explicitly set content type, which can help with session issues
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        posterRef.putFile(posterUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Upload failed", task.getException());
                        throw task.getException() != null ? task.getException() : new IllegalStateException("Upload failed");
                    }
                    return posterRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> saveEventToFirestore(title, capacityStr, details, waitingListLimit, downloadUri.toString()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload poster: " + e.getMessage());
                    Toast.makeText(this, "poster upload unavailable in current project plan", Toast.LENGTH_LONG).show();
                    // FIX: Proceed to save event with empty posterUri if storage fails
                    saveEventToFirestore(title, capacityStr, details, waitingListLimit, "");
                });
    }

    private void saveEventToFirestore(String title, String capacityStr, String details, Integer waitingListLimit, String posterUriToSave) {
        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        int capacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);
        boolean requireLocation = swRequireLocation.isChecked();

        Event event = new Event();
        event.setEventId(eventId);
        event.setTitle(title);
        event.setDetails(details);
        event.setOrganizerId(userId);
        event.setCapacity(capacity);
        event.setWaitingListLimit(waitingListLimit);
        event.setQrCodeContent(qrCodeContent);
        event.setScheduledDateTime(eventStartDate != null ? new Timestamp(eventStartDate) : null);
        event.setEventEndDate(eventEndDate != null ? new Timestamp(eventEndDate) : null);
        event.setRegistrationStartDate(regStartDate != null ? new Timestamp(regStartDate) : null);
        event.setRegistrationDeadline(regEndDate != null ? new Timestamp(regEndDate) : null);
        event.setDrawDate(drawDate != null ? new Timestamp(drawDate) : null);
        event.setRequireLocation(requireLocation);
        event.setPosterUri(posterUriToSave);
        event.touch();

        db.collection(FirestorePaths.EVENTS).document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    deleteReplacedPosterIfNeeded(posterUriToSave);
                    Toast.makeText(this, isEditMode ? "Event Updated Successfully!" : "Event Launched Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteReplacedPosterIfNeeded(String newPosterUri) {
        if (existingPosterUri == null || existingPosterUri.isEmpty() || existingPosterUri.equals(newPosterUri)) return;
        try {
            if (existingPosterUri.startsWith("gs://") || existingPosterUri.contains("firebasestorage.googleapis.com")) {
                FirebaseStorage.getInstance().getReferenceFromUrl(existingPosterUri).delete()
                        .addOnFailureListener(e -> {
                            // Check if it's a 404 (Object not found) and ignore it
                            if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                Log.d(TAG, "Old poster already gone, ignoring 404.");
                            } else {
                                Log.w(TAG, "Failed to delete old poster: " + e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            Log.w(TAG, "Error resolving old poster URL for deletion: " + e.getMessage());
        }
    }
}
