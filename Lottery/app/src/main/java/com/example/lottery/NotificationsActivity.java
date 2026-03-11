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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    public static final String EXTRA_USER_ID = "userId";

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private NotificationAdapter adapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();

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

    private void readIntentData() {
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Missing user information", Toast.LENGTH_SHORT).show();
            userId = null;
            finish();
        }
    }

    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
                                title,
                                message,
                                type,
                                eventId,
                                isRead,
                                actionTaken,
                                response
                        );

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
                    tvNoNotifications.setText("Failed to load notifications");
                    rvNotifications.setVisibility(View.GONE);
                });
    }

    private void updateUserStatusForEvent(String eventId, String status) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update("status", status)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
                );
    }

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
                        Toast.makeText(this, "Failed to update notification", Toast.LENGTH_SHORT).show()
                );
    }

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

            builder.setPositiveButton("Accept Invite", (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), "ACCEPTED");
                markActionTaken(item, "ACCEPTED");
            });

            builder.setNegativeButton("Reject", (dialog, which) -> {
                updateUserStatusForEvent(item.getEventId(), "REJECTED");
                markActionTaken(item, "REJECTED");
            });

            builder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());

        } else {
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }

        builder.show();
    }
}