package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.NotificationItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsActivity
 *
 * Displays all notifications for the logged-in user.
 *
 * NEW LOGIC:
 * - Notifications are ONLY navigation triggers
 * - Clicking opens event
 * - Event screen decides UI (waitlist / accept / decline)
 */
public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    public static final String EXTRA_USER_ID = "userId";

    private final List<NotificationItem> notificationList = new ArrayList<>();

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private NotificationAdapter adapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        btnBack = findViewById(R.id.btnBack);

        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        readIntentData();

        if (userId == null) return;

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
     * Reads userId from intent
     */
    private void readIntentData() {
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Missing user info", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Loads notifications from Firestore
     */
    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    notificationList.clear();

                    querySnapshot.forEach(doc -> {

                        NotificationItem item = new NotificationItem(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("message"),
                                doc.getString("type"),
                                doc.getString("eventId"),
                                Boolean.TRUE.equals(doc.getBoolean("isRead"))
                        );

                        notificationList.add(item);
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }

    /**
     * Show empty state
     */
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvNoNotifications.setVisibility(TextView.VISIBLE);
            rvNotifications.setVisibility(RecyclerView.GONE);
        } else {
            tvNoNotifications.setVisibility(TextView.GONE);
            rvNotifications.setVisibility(RecyclerView.VISIBLE);
        }
    }

    /**
     * Click → open event
     */
    @Override
    public void onNotificationClick(NotificationItem item) {

        // mark read
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(item.getNotificationId())
                .update("isRead", true);

        item.setRead(true);
        adapter.notifyDataSetChanged();

        // open event
        if (item.getEventId() == null || item.getEventId().isEmpty()) {
            Toast.makeText(this, item.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra("eventId", item.getEventId());
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}