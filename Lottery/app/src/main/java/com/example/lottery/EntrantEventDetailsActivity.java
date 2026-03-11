package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EntrantEventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_USER_ID = "userId";

    private TextView tvEventTitle;
    private TextView tvRegistrationPeriod;
    private TextView tvWaitlistCount;
    private TextView tvNotificationBadge;
    private TextView tvEventDescription;
    private Button btnWaitlistAction;
    private ImageButton btnNotifications;
    private ImageButton btnClose;
    private ImageView ivEventPoster;

    private FirebaseFirestore db;

    private String eventId;
    private String userId;

    private boolean isInWaitlist = false;
    private int waitlistCount = 0;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_event_details);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvRegistrationPeriod = findViewById(R.id.tvRegistrationPeriod);
        tvWaitlistCount = findViewById(R.id.tvWaitlistCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvEventDescription = findViewById(R.id.tvEventDescription);
        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnClose = findViewById(R.id.btnClose);
        ivEventPoster = findViewById(R.id.ivEventPoster);

        readIntentData();
        if (eventId == null || userId == null) {
            return;
        }

        loadEventDetails();
        checkWaitlistStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        btnWaitlistAction.setOnClickListener(v -> {
            if (isInWaitlist) {
                leaveWaitlist();
            } else {
                joinWaitlist();
            }
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventId == null || userId == null) {
            return;
        }

        loadEventDetails();
        checkWaitlistStatus();
        loadWaitlistCount();
        checkUnreadNotifications();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        userId = intent.getStringExtra(EXTRA_USER_ID);

        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Missing event or user information", Toast.LENGTH_SHORT).show();
            eventId = null;
            userId = null;
            finish();
        }
    }

    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = getFirstNonEmptyString(
                            documentSnapshot.getString("title"),
                            documentSnapshot.getString("eventTitle"),
                            documentSnapshot.getString("name")
                    );

                    String details = getFirstNonEmptyString(
                            documentSnapshot.getString("details"),
                            documentSnapshot.getString("description")
                    );

                    Timestamp registrationStart = documentSnapshot.getTimestamp("registrationStartDate");
                    Timestamp registrationDeadline = documentSnapshot.getTimestamp("registrationDeadline");
                    Timestamp eventEndDate = documentSnapshot.getTimestamp("eventEndDate");
                    Timestamp drawDate = documentSnapshot.getTimestamp("drawDate");

                    String posterUri = documentSnapshot.getString("posterUri");

                    if (title == null || title.isEmpty()) {
                        title = "Event Details";
                    }

                    if (details == null || details.isEmpty()) {
                        details = "No event description available.";
                    }

                    tvEventTitle.setText(title);
                    tvEventDescription.setText(details);
                    tvRegistrationPeriod.setText(
                            buildRegistrationText(registrationStart, registrationDeadline, eventEndDate, drawDate)
                    );

                    loadPosterImage(posterUri);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show()
                );
    }

    private String buildRegistrationText(Timestamp start, Timestamp deadline, Timestamp endDate, Timestamp drawDate) {
        if (start != null && deadline != null) {
            return "Registration Period: " + dateFormat.format(start.toDate()) +
                    " to " + dateFormat.format(deadline.toDate());
        } else if (deadline != null && drawDate != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate()) +
                    " | Draw date: " + dateFormat.format(drawDate.toDate());
        } else if (deadline != null) {
            return "Registration closes: " + dateFormat.format(deadline.toDate());
        } else if (endDate != null) {
            return "Event ends: " + dateFormat.format(endDate.toDate());
        } else {
            return "Registration details unavailable";
        }
    }

    private void loadPosterImage(String posterUri) {
        if (posterUri == null || posterUri.isEmpty()) {
            ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        try {
            Uri uri = Uri.parse(posterUri);

            if ("content".equals(uri.getScheme())
                    || "file".equals(uri.getScheme())
                    || "android.resource".equals(uri.getScheme())) {
                ivEventPoster.setImageURI(uri);
            } else {
                ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private String getFirstNonEmptyString(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private void checkWaitlistStatus() {
        DocumentReference entrantRef = db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId);

        entrantRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()
                    && "waiting".equalsIgnoreCase(documentSnapshot.getString("status"))) {
                isInWaitlist = true;
                btnWaitlistAction.setText("Leave Wait List");
            } else {
                isInWaitlist = false;
                btnWaitlistAction.setText("Join Wait List");
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to check waitlist status", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadWaitlistCount() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waitlistCount = queryDocumentSnapshots.size();
                    tvWaitlistCount.setText("People in Waitlist: " + waitlistCount);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load waitlist count", Toast.LENGTH_SHORT).show()
                );
    }

    private void checkUnreadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to check notifications", Toast.LENGTH_SHORT).show()
                );
    }

    private void joinWaitlist() {
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("userId", userId);
        entrantData.put("status", "waiting");
        entrantData.put("registrationTime", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = true;
                    btnWaitlistAction.setText("Leave Wait List");
                    loadWaitlistCount();
                    Toast.makeText(this, "Joined waitlist", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show()
                );
    }

    private void leaveWaitlist() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    btnWaitlistAction.setText("Join Wait List");
                    loadWaitlistCount();
                    Toast.makeText(this, "Left waitlist", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave waitlist", Toast.LENGTH_SHORT).show()
                );
    }
}