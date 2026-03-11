package com.example.lottery;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lottery.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/*
 * WaitingListActivity
 * Displays the real entrants who joined the waiting list
 * for the selected event.
 */
public class WaitingListActivity extends AppCompatActivity {

    private ListView waitingListView;
    private TextView emptyMessage;

    private ArrayList<User> entrants;
    private com.example.lottery.EntrantAdapter entrantAdapter;

    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list);

        waitingListView = findViewById(R.id.waitingListView);
        emptyMessage = findViewById(R.id.emptyMessage);

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

        loadWaitingList();
    }

    /*
     * Loads entrant IDs from:
     * events/{eventId}/entrants
     *
     * Then loads each user's personal info from:
     * users
     */
    private void loadWaitingList() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
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
                        String entrantId = document.getString("entrantId");

                        if (entrantId == null || entrantId.isEmpty()) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalEntrants) {
                                updateListState();
                            }
                            return;
                        }

                        /*
                         * If your users collection stores entrantId inside a field
                         * like "user_id", use whereEqualTo("user_id", entrantId).
                         */
                        db.collection("users")
                                .whereEqualTo("user_id", entrantId)
                                .get()
                                .addOnSuccessListener(userSnapshots -> {
                                    if (!userSnapshots.isEmpty()) {
                                        String username = userSnapshots.getDocuments().get(0).getString("username");
                                        String email = userSnapshots.getDocuments().get(0).getString("email");
                                        String phone = userSnapshots.getDocuments().get(0).getString("phone_number");

                                        if (username == null) username = "Unknown User";
                                        if (email == null) email = "No email";
                                        if (phone == null) phone = "No phone";

                                        entrants.add(new User(username, email, phone));
                                    }

                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalEntrants) {
                                        updateListState();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalEntrants) {
                                        updateListState();
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load waiting list", Toast.LENGTH_SHORT).show();
                });
    }

    /*
     * Shows the empty message if no entrants exist.
     */
    private void showEmptyState() {
        emptyMessage.setVisibility(View.VISIBLE);
        waitingListView.setVisibility(View.GONE);
    }

    /*
     * Updates the screen after Firestore data finishes loading.
     */
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