package com.example.lottery;

import android.content.Intent;
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

import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin page for browsing all notification logs sent by organizers to entrants.
 */
public class AdminBrowseLogsActivity extends AppCompatActivity {

    private static final String TAG = "AdminBrowseLogs";

    private NotificationLogAdapter adapter;
    private List<Map<String, Object>> logList;
    private TextView tvNoLogs;
    private FirebaseFirestore db;

    /**
     * Sets up the RecyclerView, adapter, and bottom navigation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_browse_logs);

        // Adjust padding for system bars like status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets in = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(in.left, in.top, in.right, in.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        RecyclerView rvLogs = findViewById(R.id.rvLogs);
        tvNoLogs = findViewById(R.id.tvNoLogs);

        logList = new ArrayList<>();
        adapter = new NotificationLogAdapter(logList);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(adapter);

        setupNavigation();
    }

    /**
     * Reloads logs each time the activity is resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
    }

    /**
     * Wires up the bottom navigation bar buttons.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                startActivity(intent);
            });
        }

        View btnProfiles = findViewById(R.id.nav_profiles);
        if (btnProfiles != null) {
            btnProfiles.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
                intent.putExtra("role", "admin"); // needed to show admin specific UI
                startActivity(intent);
            });
        }

        View btnImages = findViewById(R.id.nav_images);
        if (btnImages != null) {
            btnImages.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
                startActivity(intent);
            });
        }

        // Current tab, show toast instead of relaunching
        View btnLogs = findViewById(R.id.nav_logs);
        if (btnLogs != null) {
            btnLogs.setOnClickListener(v ->
                    Toast.makeText(this, R.string.admin_already_viewing_logs, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Fetches all notification logs from Firestore, ordered by newest first.
     */
    private void loadLogs() {
        db.collection(FirestorePaths.NOTIFICATIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::onSuccess)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notification logs", e);
                    Toast.makeText(this, R.string.failed_to_load_notifications, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles a successful Firestore query by refreshing the log list.
     *
     * @param queryDocumentSnapshots the query result
     */
    private void onSuccess(QuerySnapshot queryDocumentSnapshots) {
        logList.clear();
        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            logList.add(document.getData());
        }
        adapter.notifyDataSetChanged();
        // Show empty state text when there are no logs
        tvNoLogs.setVisibility(logList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
