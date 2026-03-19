package com.example.lottery;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.NotificationItem;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays notifications for the currently logged-in entrant.
 *
 * <p>This activity always loads and shows notifications that already exist
 * in Firestore for the current user. Notification opt-out should be enforced
 * at notification creation time by organizers/admins, not by hiding previously
 * stored notifications here.</p>
 *
 * <p>If a notification represents a winning invitation and still requires
 * a response, the entrant can accept or reject it from this screen.</p>
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    /**
     * Intent extra used to pass the current user ID into this activity.
     */
    public static final String EXTRA_USER_ID = "userId";

    /**
     * In-memory list of notifications displayed in the RecyclerView.
     */
    private final List<NotificationItem> notificationList = new ArrayList<>();

    /**
     * RecyclerView used to display the notifications list.
     */
    private RecyclerView rvNotifications;

    /**
     * Empty-state message shown when no notifications exist.
     */
    private TextView tvNoNotifications;

    /**
     * Back button used to leave the notifications screen.
     */
    private ImageButton btnBack;

    /**
     * Firestore database instance.
     */
    private FirebaseFirestore db;

    /**
     * RecyclerView adapter for notifications.
     */
    private NotificationAdapter adapter;

    /**
     * ID of the currently logged-in user.
     */
    private String userId;

    /**
     * Initializes the activity, binds the UI, reads the current user ID,
     * and loads all stored notifications for that user.
     *
     * @param savedInstanceState previously saved activity state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        btnBack = findViewById(R.id.btnBack);

        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        readIntentData();
        if (userId == null) {
            return;
        }

        btnBack.setOnClickListener(v -> finish());

        loadNotifications();
    }

    /**
     * Reloads notifications whenever the activity becomes visible again.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            loadNotifications();
        }
    }

    /**
     * Reads the current user ID from the launching intent.
     * If the value is missing, the activity closes.
     */
    private void readIntentData() {
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, R.string.missing_user_info, Toast.LENGTH_SHORT).show();
            userId = null;
            finish();
        }
    }

    /**
     * Loads all existing notifications from the current user's
     * {@code users/{userId}/notifications} Firestore subcollection.
     *
     * <p>This screen intentionally does not check the user's
     * {@code notificationsEnabled} toggle. Old notifications remain visible.
     * The toggle should only block creation of new notifications.</p>
     */
    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();

                    queryDocumentSnapshots.forEach(document -> {
                        String id = document.getId();
                        String title = document.getString("title");
                        String message = document.getString("message");
                        String type = document.getString("type");
                        String eventId = document.getString("eventId");

                        boolean isRead = Boolean.TRUE.equals(document.getBoolean("isRead"));
                        boolean actionTaken = Boolean.TRUE.equals(document.getBoolean("actionTaken"));
                        String response = document.getString("response");

                        NotificationItem item = new NotificationItem(
                                id,
                                title != null ? title : "Notification",
                                message != null ? message : "",
                                type != null ? type : "",
                                eventId != null ? eventId : "",
                                isRead,
                                actionTaken,
                                response
                        );

                        notificationList.add(item);
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    tvNoNotifications.setVisibility(View.VISIBLE);
                    tvNoNotifications.setText(R.string.failed_to_load_notifications);
                    rvNotifications.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the empty-state message depending on whether any notifications exist.
     */
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvNoNotifications.setText("No notifications yet");
            tvNoNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the entrant's event status based on a notification response.
     *
     * @param eventId  the related event ID
     * @param response the response string selected by the entrant
     */
    private void updateUserStatusForEvent(String eventId, String response) {
        String normalizedStatus = InvitationFlowUtil.entrantStatusFromNotificationResponse(response);
        if (normalizedStatus.isEmpty()) {
            Toast.makeText(this, R.string.failed_to_update_status, Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update("status", normalizedStatus)
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_status, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Marks a notification as actioned and stores the entrant's response.
     *
     * @param item     the notification being updated
     * @param response the entrant's response
     */
    private void markActionTaken(NotificationItem item, String response) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(item.getNotificationId())
                .update(
                        "actionTaken", true,
                        "response", response
                )
                .addOnSuccessListener(unused -> {
                    item.setActionTaken(true);
                    item.setResponse(response);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_notification, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Handles clicks on a notification item.
     *
     * <p>The notification is marked as read. If it is a win notification
     * and still needs user action, the entrant may accept or reject it.
     * Otherwise, the notification is shown in a simple dialog.</p>
     *
     * @param item the clicked notification
     */
    @Override
    public void onNotificationClick(NotificationItem item) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(item.getNotificationId())
                .update("isRead", true);

        item.setRead(true);
        adapter.notifyDataSetChanged();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle());
        builder.setMessage(item.getMessage());

        if ("win".equalsIgnoreCase(item.getType()) && !item.isActionTaken()) {
            builder.setPositiveButton(R.string.accept_invite, (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), InvitationFlowUtil.RESPONSE_ACCEPTED);
                markActionTaken(item, InvitationFlowUtil.RESPONSE_ACCEPTED);
            });

            builder.setNegativeButton(R.string.reject, (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), InvitationFlowUtil.RESPONSE_REJECTED);
                markActionTaken(item, InvitationFlowUtil.RESPONSE_REJECTED);
            });

            builder.setNeutralButton(R.string.close, (dialog, which) -> dialog.dismiss());
        } else {
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }

        builder.show();
    }
}