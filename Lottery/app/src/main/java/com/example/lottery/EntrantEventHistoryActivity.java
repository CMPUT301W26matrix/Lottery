package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.EntrantNavigationHelper;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays the history of events an entrant has registered for.
 * Implements US 01.02.03.
 */
public class EntrantEventHistoryActivity extends AppCompatActivity {

    private final List<EntrantHistoryAdapter.HistoryItem> historyList = new ArrayList<>();
    private RecyclerView rvEventHistory;
    private View emptyStateContainer;
    private ProgressBar progressBar;
    private EntrantHistoryAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_event_history);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        EntrantNavigationHelper.setup(this, EntrantNavigationHelper.EntrantTab.MY_EVENTS, userId);
        loadEventHistory();
    }

    private void initViews() {
        rvEventHistory = findViewById(R.id.rvEventHistory);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        progressBar = findViewById(R.id.progressBar);

        adapter = new EntrantHistoryAdapter(historyList, this::openEventDetails, userId);
        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        rvEventHistory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadEventHistory() {
        progressBar.setVisibility(View.VISIBLE);

        db.collectionGroup(FirestorePaths.WAITING_LIST)
                .whereEqualTo("userId", userId)
                .orderBy("registeredAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        updateUI();
                        return;
                    }

                    final int[] loadedCount = {0};
                    int totalCount = queryDocumentSnapshots.size();

                    for (DocumentSnapshot regDoc : queryDocumentSnapshots) {
                        String status = regDoc.getString("status");
                        EntrantHistoryAdapter.HistoryItem item = new EntrantHistoryAdapter.HistoryItem(null, status);
                        historyList.add(item); // Add placeholder to maintain count

                        regDoc.getReference().getParent().getParent().get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        Event event = eventDoc.toObject(Event.class);
                                        if (event != null) {
                                            event.setEventId(eventDoc.getId());
                                            item.event = event;

                                            // Fetch Organizer Name
                                            if (event.getOrganizerId() != null) {
                                                db.collection(FirestorePaths.USERS).document(event.getOrganizerId()).get()
                                                        .addOnSuccessListener(userDoc -> {
                                                            if (userDoc.exists()) {
                                                                item.organizerName = userDoc.getString("username");
                                                            }
                                                            checkAllLoaded(++loadedCount[0], totalCount);
                                                        })
                                                        .addOnFailureListener(e -> checkAllLoaded(++loadedCount[0], totalCount));
                                            } else {
                                                checkAllLoaded(++loadedCount[0], totalCount);
                                            }
                                        } else {
                                            checkAllLoaded(++loadedCount[0], totalCount);
                                        }
                                    } else {
                                        checkAllLoaded(++loadedCount[0], totalCount);
                                    }
                                })
                                .addOnFailureListener(e -> checkAllLoaded(++loadedCount[0], totalCount));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAllLoaded(int current, int total) {
        if (current == total) {
            // Cleanup: remove items where event failed to load
            historyList.removeIf(item -> item.event == null);
            updateUI();
        }
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        if (historyList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rvEventHistory.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            rvEventHistory.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_SOURCE_TAB, "MY_EVENTS");
        startActivity(intent);
    }
}