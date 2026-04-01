package com.example.lottery;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.model.NotificationItem;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.example.lottery.util.OrganizerNavigationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity that allows organizers to manage and send notifications to different groups of entrants.
 */
public class OrganizerNotificationsActivity extends AppCompatActivity
        implements NotificationFragment.NotificationListener,
        OrganizerNotificationEventAdapter.OnNotificationGroupClickListener {

    private static final String TAG = "OrganizerNotifications";
    private RecyclerView rvEvents;
    private OrganizerNotificationEventAdapter adapter;
    private List<Event> eventList;
    private TextView tvNoEvents;
    private FirebaseFirestore db;
    private String userId;

    private Event selectedEventForNotification;
    private String selectedGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_notifications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            userId = prefs.getString("userId", null);
        }

        if (userId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvEvents = findViewById(R.id.rvOrganizerEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);

        eventList = new ArrayList<>();
        adapter = new OrganizerNotificationEventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.NOTIFICATIONS, userId);
        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        if (userId == null) return;

        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onGroupClick(Event event, String group) {
        this.selectedEventForNotification = event;
        this.selectedGroup = group;

        String displayGroupName;
        switch (group.toLowerCase()) {
            case "waitlisted":
                displayGroupName = "Notify Waiting List";
                break;
            case "invited":
                displayGroupName = "Notify Invited Entrants";
                break;
            case "accepted":
                displayGroupName = "Notify Accepted Entrants";
                break;
            case "cancelled":
                displayGroupName = "Notify Cancelled Entrants";
                break;
            case "not_selected":
                displayGroupName = "Notify Not Selected Entrants";
                break;
            default:
                displayGroupName = "Notify Group: " + group;
                break;
        }

        // Create a 2-line title: Line 1 Group Action, Line 2 Event Title
        String dialogTitle = displayGroupName + "\n" + event.getTitle();

        NotificationFragment fragment = NotificationFragment.newInstance(dialogTitle);
        fragment.show(getSupportFragmentManager(), "compose_notification");
    }

    @Override
    public void sendNotification(String message) {
        if (selectedEventForNotification == null || selectedGroup == null || userId == null) return;

        String notificationId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.now();

        Map<String, Object> globalNotif = new HashMap<>();
        globalNotif.put("notificationId", notificationId);
        globalNotif.put("title", "Update for " + selectedEventForNotification.getTitle());
        globalNotif.put("message", message);
        globalNotif.put("type", "general");
        globalNotif.put("eventId", selectedEventForNotification.getEventId());
        globalNotif.put("eventTitle", selectedEventForNotification.getTitle());
        globalNotif.put("group", selectedGroup);
        globalNotif.put("senderId", userId);
        globalNotif.put("senderRole", "organizer");
        globalNotif.put("createdAt", now);
        globalNotif.put("recipientCount", 0);

        db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId)
                .set(globalNotif)
                .addOnSuccessListener(aVoid -> fetchRecipientsAndSend(notificationId, message, globalNotif));
    }

    private void fetchRecipientsAndSend(String notificationId, String message, Map<String, Object> globalNotif) {
        String targetStatus;
        switch (selectedGroup.toLowerCase()) {
            case "invited":
                targetStatus = InvitationFlowUtil.STATUS_INVITED;
                break;
            case "accepted":
                targetStatus = InvitationFlowUtil.STATUS_ACCEPTED;
                break;
            case "cancelled":
                targetStatus = InvitationFlowUtil.STATUS_CANCELLED;
                break;
            case "not_selected":
                targetStatus = InvitationFlowUtil.STATUS_NOT_SELECTED;
                break;
            case "waitlisted":
            default:
                targetStatus = InvitationFlowUtil.STATUS_WAITLISTED;
                break;
        }

        db.collection(FirestorePaths.eventWaitingList(selectedEventForNotification.getEventId()))
                .whereEqualTo("status", targetStatus)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    int totalPotentialRecipients = querySnapshot.size();
                    
                    if (totalPotentialRecipients == 0) {
                        Toast.makeText(this, "No recipients found in group", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AtomicInteger processedCount = new AtomicInteger(0);
                    AtomicInteger actualSentCount = new AtomicInteger(0);

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String recipientUid = doc.getId();

                        db.collection(FirestorePaths.USERS).document(recipientUid).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot userDoc = task.getResult();
                                Boolean notifPref = userDoc.getBoolean("notificationsEnabled");
                                boolean enabled = notifPref == null || notifPref;

                                if (enabled) {
                                    DocumentReference recipientRef = db.collection(FirestorePaths.notificationRecipients(notificationId))
                                            .document(recipientUid);
                                    Map<String, Object> recData = new HashMap<>();
                                    recData.put("userId", recipientUid);
                                    recData.put("delivered", true);
                                    recData.put("isRead", false);
                                    recData.put("statusAtSendTime", targetStatus);
                                    recData.put("deliveredAt", Timestamp.now());
                                    batch.set(recipientRef, recData);

                                    NotificationItem inboxItem = new NotificationItem(
                                            notificationId,
                                            (String) globalNotif.get("title"),
                                            message,
                                            "general",
                                            selectedEventForNotification.getEventId(),
                                            selectedEventForNotification.getTitle(),
                                            (String) globalNotif.get("senderId"),
                                            "organizer",
                                            false,
                                            (Timestamp) globalNotif.get("createdAt")
                                    );
                                    batch.set(db.collection(FirestorePaths.userInbox(recipientUid)).document(notificationId), inboxItem);
                                    actualSentCount.incrementAndGet();
                                }
                            }

                            if (processedCount.incrementAndGet() == totalPotentialRecipients) {
                                if (actualSentCount.get() > 0) {
                                    batch.update(db.collection(FirestorePaths.NOTIFICATIONS).document(notificationId), "recipientCount", actualSentCount.get());
                                    batch.commit().addOnSuccessListener(unused ->
                                            Toast.makeText(this, "Notification sent to " + actualSentCount.get() + " recipients", Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(this, "All entrants in this group have notifications disabled", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching recipients", e));
    }
}
