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
import com.example.lottery.util.EntrantNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.SessionUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a list of notifications for the currently logged-in entrant.
 * Supports Global mode (all notifications) and Event-specific mode.
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    private final List<NotificationItem> notificationList = new ArrayList<>();
    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private TextView tvTitle;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private NotificationAdapter adapter;
    private String userId;
    private String eventId;
    private String eventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_notifications);

        db = FirebaseFirestore.getInstance();

        userId = SessionUtil.resolveUserId(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        if (userId == null) {
            Toast.makeText(this, R.string.missing_user_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupHeader();
        
        btnBack.setOnClickListener(v -> finish());
        EntrantNavigationHelper.setup(this, EntrantNavigationHelper.EntrantTab.NONE, userId, true);
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        tvTitle = findViewById(R.id.tvNotificationsTitle);
        btnBack = findViewById(R.id.btnBack);

        adapter = new NotificationAdapter(notificationList, this);
        // Set mode based on whether eventId is present
        adapter.setEventSpecificMode(eventId != null);
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupHeader() {
        if (eventId != null) {
            String titleText = (eventTitle != null && !eventTitle.isEmpty()) 
                ? eventTitle + " Notifications" 
                : "Event Notifications";
            tvTitle.setText(titleText);
            tvTitle.setTextColor(getResources().getColor(R.color.primary_blue));
            btnBack.getDrawable().setTint(getResources().getColor(R.color.primary_blue));
        } else {
            tvTitle.setText(R.string.notifications_title);
            tvTitle.setTextColor(getResources().getColor(R.color.primary_blue));
            btnBack.getDrawable().setTint(getResources().getColor(R.color.primary_blue));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            loadNotifications();
        }
    }

    private void loadNotifications() {
        Query query = db.collection(FirestorePaths.userInbox(userId))
                .orderBy("createdAt", Query.Direction.DESCENDING);

        if (eventId != null) {
            query = query.whereEqualTo("eventId", eventId);
        }

        query.get()
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

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvNoNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void markNotificationRead(NotificationItem item) {
        db.collection(FirestorePaths.userInbox(userId))
                .document(item.getNotificationId())
                .update("isRead", true)
                .addOnSuccessListener(unused -> {
                    item.setRead(true);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onNotificationClick(NotificationItem item) {
        markNotificationRead(item);
        if ("event_invitation".equalsIgnoreCase(item.getType()) && item.getEventId() != null) {
            android.content.Intent intent = new android.content.Intent(this, EntrantEventDetailsActivity.class);
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, item.getEventId());
            intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(item.getTitle())
                    .setMessage(item.getMessage())
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}
