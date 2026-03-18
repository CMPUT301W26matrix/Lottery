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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Displays notifications for the currently logged-in entrant.
 *
 * <p>This activity retrieves notifications from Firestore and displays them
 * in a RecyclerView. Entrants can view notification details and respond to
 * certain notification types (such as winning an event draw).</p>
 *
 * <p>If the notification represents a winning invitation, the entrant can
 * either accept or reject the invite. The response updates both the user's
 * notification document and the entrant status within the corresponding
 * event in Firestore.</p>
 *
 * <p>This activity expects the following intent extra:</p>
 * <ul>
 *     <li>{@link #EXTRA_USER_ID} – the ID of the current user</li>
 * </ul>
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    /**
     * Intent extra used to pass the user ID to this activity.
     */
    public static final String EXTRA_USER_ID = "userId";

    /**
     * List storing all notifications retrieved from Firestore.
     */
    private final List<NotificationItem> notificationList = new ArrayList<>();

    /**
     * RecyclerView used to display notifications.
     */
    private RecyclerView rvNotifications;

    /**
     * TextView shown when there are no notifications.
     */
    private TextView tvNoNotifications;

    /**
     * Button used to navigate back from the notifications screen.
     */
    private ImageButton btnBack;

    /**
     * Firestore database instance used to retrieve notifications.
     */
    private FirebaseFirestore db;

    /**
     * Adapter used to bind notification data to the RecyclerView.
     */
    private NotificationAdapter adapter;

    /**
     * ID of the currently logged-in user.
     */
    private String userId;

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

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            loadNotifications();
        }
    }

    /**
     * Reads the user ID from the intent that launched the activity.
     * If the user ID is missing, the activity closes.
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
     * Retrieves notifications for the current user from Firestore and updates the RecyclerView.
     */
    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();

                    queryDocumentSnapshots.forEach(document -> {
                        String id = document.getId();
                        String title = document.getString("title");
                        String message = document.getString("message");
                        String rawType = document.getString("type");
                        String eventId = document.getString("eventId");

                        boolean isRead = Boolean.TRUE.equals(document.getBoolean("isRead"));
                        boolean requiresAction = Boolean.TRUE.equals(document.getBoolean("requiresAction"));
                        boolean actionTaken = Boolean.TRUE.equals(document.getBoolean("actionTaken"));

                        String rawResponse = document.getString("response");

                        NotificationItem item = new NotificationItem();
                        item.setNotificationId(id);
                        item.setTitle(title);
                        item.setMessage(message);
                        item.setTypeFromString(rawType);
                        item.setEventId(eventId);
                        item.setRead(isRead);
                        item.setRequiresAction(requiresAction);
                        item.setActionTaken(actionTaken);
                        item.setResponseFromString(rawResponse);
                        item.setCreatedAt(document.getTimestamp("createdAt"));
                        item.setActedAt(document.getTimestamp("actedAt"));

                        notificationList.add(item);
                    });

                    adapter.notifyDataSetChanged();

                    if (notificationList.isEmpty()) {
                        tvNoNotifications.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        tvNoNotifications.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNoNotifications.setVisibility(View.VISIBLE);
                    tvNoNotifications.setText(R.string.failed_to_load_notifications);
                    rvNotifications.setVisibility(View.GONE);
                });
    }

    /**
     * Updates the entrant status for a specific event in Firestore based on the notification response.
     *
     * @param eventId  the event for which the status should be updated
     * @param response the notification response value
     */
    private void updateUserStatusForEvent(String eventId, String response) {
        Map<String, Object> updates =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(response);

        if (updates.isEmpty()) {
            Toast.makeText(this, R.string.failed_to_update_status, Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_status, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Marks a notification as handled.
     *
     * @param item     the notification item being updated
     * @param response the response value
     */
    private void markActionTaken(NotificationItem item, String response) {
        Map<String, Object> updates = InvitationFlowUtil.buildHandledNotificationUpdate(response);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(item.getNotificationId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    String normalizedResponse = InvitationFlowUtil.normalizeNotificationResponse(response);

                    item.setRead(true);
                    item.setActionTaken(true);
                    item.setActedAt(com.google.firebase.Timestamp.now());

                    if (InvitationFlowUtil.RESPONSE_ACCEPTED.equals(normalizedResponse)) {
                        item.setResponse(NotificationItem.Response.ACCEPTED);
                    } else if (InvitationFlowUtil.RESPONSE_DECLINED.equals(normalizedResponse)) {
                        item.setResponse(NotificationItem.Response.DECLINED);
                    } else if (InvitationFlowUtil.RESPONSE_DISMISSED.equals(normalizedResponse)) {
                        item.setResponse(NotificationItem.Response.DISMISSED);
                    } else {
                        item.setResponse(NotificationItem.Response.NONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_notification, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Marks a notification as read only.
     */
    private void markNotificationRead(NotificationItem item) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(item.getNotificationId())
                .update(InvitationFlowUtil.buildReadNotificationUpdate())
                .addOnSuccessListener(unused -> {
                    item.setRead(true);
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Handles notification item clicks.
     *
     * <p>If the notification represents an event invitation,
     * the entrant is given the option to accept or decline the invite.</p>
     *
     * @param item the clicked notification item
     */
    @Override
    public void onNotificationClick(NotificationItem item) {
        markNotificationRead(item);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle());
        builder.setMessage(item.getMessage());

        boolean isInvitation =
                item.getType() == NotificationItem.Type.EVENT_INVITATION;

        if (isInvitation && !item.isActionTaken()) {

            builder.setPositiveButton(R.string.accept_invite, (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), InvitationFlowUtil.RESPONSE_ACCEPTED);
                markActionTaken(item, InvitationFlowUtil.RESPONSE_ACCEPTED);
            });

            builder.setNegativeButton(R.string.reject, (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), InvitationFlowUtil.RESPONSE_DECLINED);
                markActionTaken(item, InvitationFlowUtil.RESPONSE_DECLINED);
            });

            builder.setNeutralButton(R.string.close, (dialog, which) -> dialog.dismiss());

        } else {
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }

        builder.show();
    }
}