package com.example.lottery;

import android.content.Intent;
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
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.SessionUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity that displays a list of notifications for the currently logged-in entrant.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch notifications from the user's private inbox subcollection in Firestore.</li>
 *   <li>Display notification details such as title, message, and type.</li>
 *   <li>Manage the "read/unread" status of each notification.</li>
 *   <li>Provide actionable dialogs for specific notification types (e.g., event invitations).</li>
 * </ul>
 * </p>
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    /**
     * Intent extra key used to pass the user's ID to this activity.
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

    /**
     * Initializes the activity, sets up the RecyclerView, and starts loading notifications.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. <b>Note: Otherwise it is null.</b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_notifications);

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

        readUserId();

        if (userId == null) {
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        setupNavigation();
        loadNotifications();
    }

    /**
     * Configures the click listeners for the custom bottom navigation bar.
     */
    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantMainActivity.class);
                intent.putExtra("userId", userId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                // Already here
            });
        }

        View navQrScan = findViewById(R.id.nav_qr_scan);
        if (navQrScan != null) {
            navQrScan.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantQrScanActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }

        View navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EntrantProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        }
    }

    /**
     * Refreshes the notification list whenever the activity becomes visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            loadNotifications();
        }
    }

    /**
     * Extracts the user ID from the starting Intent.
     */
    private void readUserId() {
        userId = SessionUtil.resolveUserId(this);
        if (userId == null) {
            Toast.makeText(this, R.string.missing_user_info, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Queries Firestore for notifications in the user's inbox, ordered by creation date.
     */
    private void loadNotifications() {
        db.collection(FirestorePaths.userInbox(userId))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    queryDocumentSnapshots.forEach(document -> {
                        NotificationItem item = document.toObject(NotificationItem.class);
                        if (item != null) {
                            item.setNotificationId(document.getId());
                            notificationList.add(item);
                        }
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    tvNoNotifications.setVisibility(View.VISIBLE);
                    tvNoNotifications.setText(R.string.failed_to_load_notifications);
                    rvNotifications.setVisibility(View.GONE);
                });
    }

    /**
     * Updates the UI visibility based on whether the notification list is empty.
     */
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvNoNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the entrant's status for a specific event in Firestore based on their response.
     *
     * @param eventId  The ID of the event.
     * @param response The user's response string (e.g., accepted, declined).
     */
    private void updateUserStatusForEvent(String eventId, String response) {
        Map<String, Object> updates = InvitationFlowUtil.buildEntrantStatusUpdateFromResponse(response);
        if (updates.isEmpty() || eventId == null) return;

        db.collection(FirestorePaths.eventWaitingList(eventId))
                .document(userId)
                .update(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.failed_to_update_status, Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Updates the "isRead" field of a notification document in Firestore.
     *
     * @param item The notification item to mark as read.
     */
    private void markNotificationRead(NotificationItem item) {
        db.collection(FirestorePaths.userInbox(userId))
                .document(item.getNotificationId())
                .update("isRead", true)
                .addOnSuccessListener(unused -> {
                    item.setRead(true);
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Handles clicks on notification items. Marks the item as read and displays its content in a dialog.
     * Special handling is provided for event invitations.
     *
     * @param item The clicked notification item.
     */
    @Override
    public void onNotificationClick(NotificationItem item) {
        markNotificationRead(item);

        boolean isInvitation = "event_invitation".equalsIgnoreCase(item.getType());

        if (isInvitation && item.getEventId() != null) {
            // US 01.05.06: Clicking an event_invitation should open the event in EntrantEventDetailsActivity
            Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, item.getEventId());
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        } else {
            // For other notifications, show the message in a simple dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(item.getTitle());
            builder.setMessage(item.getMessage());
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }
}
