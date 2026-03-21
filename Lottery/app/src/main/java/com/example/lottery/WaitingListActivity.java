package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * WaitingListActivity
 * Displays the entrants who are currently in the waitlisted status
 * for the selected event.
 */
public class WaitingListActivity extends AppCompatActivity {

    private ListView waitingListView;
    private TextView emptyMessage;
    private ImageButton backButton;

    private ArrayList<User> entrants;
    private EntrantAdapter entrantAdapter;

    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_waiting_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        waitingListView = findViewById(R.id.waitingListView);
        emptyMessage = findViewById(R.id.emptyMessage);
        backButton = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        entrants = new ArrayList<>();
        entrantAdapter = new com.example.lottery.EntrantAdapter(this, entrants);
        waitingListView.setAdapter(entrantAdapter);

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> finish());
        setupNavigation();
        loadWaitingList();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerBrowseEventsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerProfileActivity.class));
        });

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, OrganizerCreateEventActivity.class))
            );
        }
    }

    /**
     * Loads entrant IDs from events/{eventId}/waitingList subcollection.
     * Filters for status == "waitlisted".
     * Then loads user details for display.
     */
    private void loadWaitingList() {
        db.collection(FirestorePaths.eventWaitingList(eventId))
                .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITLISTED)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entrants.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    final int totalEntrants = queryDocumentSnapshots.size();
                    final int[] loadedCount = {0};

                    queryDocumentSnapshots.forEach(document -> {
                        EntrantEvent entrantRecord = document.toObject(EntrantEvent.class);
                        String uid = (entrantRecord != null) ? entrantRecord.getUserId() : document.getId();

                        if (uid == null || uid.isEmpty()) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalEntrants) updateListState();
                            return;
                        }

                        db.collection(FirestorePaths.USERS)
                                .document(uid)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (userSnapshot.exists()) {
                                        User user = userSnapshot.toObject(User.class);
                                        if (user != null) {
                                            user.setUid(userSnapshot.getId());
                                            entrants.add(user);
                                        }
                                    }

                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalEntrants) updateListState();
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalEntrants) updateListState();
                                });
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load waiting list", Toast.LENGTH_SHORT).show());
    }

    private void showEmptyState() {
        emptyMessage.setVisibility(View.VISIBLE);
        waitingListView.setVisibility(View.GONE);
    }

    private void updateListState() {
        if (entrants.isEmpty()) {
            showEmptyState();
        } else {
            emptyMessage.setVisibility(View.GONE);
            waitingListView.setVisibility(View.VISIBLE);
            entrantAdapter.notifyDataSetChanged();
        }
    }
}
