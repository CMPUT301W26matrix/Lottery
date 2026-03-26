package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays the history of events an entrant has registered for.
 * Implements US 01.02.03.
 *
 * <p>This screen also supports entrant Accessibility Mode by updating
 * the toolbar title, empty-state text, bottom navigation labels, and
 * history card text sizes.</p>
 */
public class EntrantEventHistoryActivity extends AppCompatActivity {

    /** SharedPreferences file used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for entrant accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private RecyclerView rvEventHistory;
    private View emptyStateContainer;
    private ProgressBar progressBar;
    private EntrantHistoryAdapter adapter;
    private final List<EntrantHistoryAdapter.HistoryItem> historyList = new ArrayList<>();

    private FirebaseFirestore db;
    private String userId;

    private Toolbar toolbar;
    private TextView tvToolbarTitle;
    private TextView tvEmptyStateMessage;
    private View bottomNavContainer;

    /**
     * Initializes the history screen and loads registered event history.
     *
     * @param savedInstanceState previously saved state, if any
     */
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
        setupNavigation();
        loadEventHistory();
        applyAccessibilityMode();
    }

    /**
     * Reapplies accessibility mode when returning to this activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        applyAccessibilityMode();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Binds views and initializes RecyclerView.
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvEventHistory = findViewById(R.id.rvEventHistory);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        progressBar = findViewById(R.id.progressBar);
        bottomNavContainer = findViewById(R.id.bottom_nav_container);

        tvToolbarTitle = findToolbarTitle(toolbar);
        tvEmptyStateMessage = findFirstTextView(emptyStateContainer);

        adapter = new EntrantHistoryAdapter(historyList, this::openEventDetails);
        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        rvEventHistory.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Sets up bottom navigation behavior for entrant screens.
     */
    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            // Already on this screen
        });

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantQrScanActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    /**
     * Loads event history from Firestore for the current entrant.
     */
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
                        EntrantHistoryAdapter.HistoryItem item =
                                new EntrantHistoryAdapter.HistoryItem(null, status);
                        historyList.add(item);

                        regDoc.getReference().getParent().getParent().get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        Event event = eventDoc.toObject(Event.class);
                                        if (event != null) {
                                            event.setEventId(eventDoc.getId());
                                            item.event = event;

                                            if (event.getOrganizerId() != null) {
                                                db.collection(FirestorePaths.USERS)
                                                        .document(event.getOrganizerId())
                                                        .get()
                                                        .addOnSuccessListener(userDoc -> {
                                                            if (userDoc.exists()) {
                                                                item.organizerName =
                                                                        userDoc.getString("username");
                                                            }
                                                            checkAllLoaded(++loadedCount[0], totalCount);
                                                        })
                                                        .addOnFailureListener(e ->
                                                                checkAllLoaded(++loadedCount[0], totalCount));
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
                                .addOnFailureListener(e ->
                                        checkAllLoaded(++loadedCount[0], totalCount));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            "Failed to load history: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    /**
     * Checks whether all asynchronous event loads have completed.
     *
     * @param current number currently loaded
     * @param total total expected
     */
    private void checkAllLoaded(int current, int total) {
        if (current == total) {
            historyList.removeIf(item -> item.event == null);
            updateUI();
        }
    }

    /**
     * Updates the empty state and RecyclerView visibility.
     */
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

        applyAccessibilityMode();
    }

    /**
     * Opens the event details screen for the selected history item.
     *
     * @param event selected event
     */
    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    /**
     * Applies entrant accessibility mode to safe screen-level views.
     */
    private void applyAccessibilityMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);

        float toolbarTextSize = isEnabled ? 24f : 20f;
        float emptyMessageTextSize = isEnabled ? 18f : 16f;
        float navTextSize = isEnabled ? 14.5f : 12f;

        if (tvToolbarTitle != null) {
            tvToolbarTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, toolbarTextSize);
        }

        if (tvEmptyStateMessage != null) {
            tvEmptyStateMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, emptyMessageTextSize);
        }

        if (bottomNavContainer != null) {
            updateBottomNavTextRecursively(bottomNavContainer, navTextSize);
        }
    }

    /**
     * Recursively updates only bottom navigation labels.
     *
     * @param view root view
     * @param textSize desired text size in sp
     */
    private void updateBottomNavTextRecursively(View view, float textSize) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateBottomNavTextRecursively(group.getChildAt(i), textSize);
            }
        }
    }

    /**
     * Finds the first TextView inside the toolbar, used as the title.
     *
     * @param toolbar toolbar view
     * @return toolbar title TextView, or null if not found
     */
    private TextView findToolbarTitle(Toolbar toolbar) {
        if (toolbar == null) {
            return null;
        }

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            }
        }
        return null;
    }

    /**
     * Finds the first TextView inside a view hierarchy.
     *
     * @param root root view
     * @return first TextView found, or null if none exists
     */
    private TextView findFirstTextView(View root) {
        if (root == null) {
            return null;
        }

        if (root instanceof TextView) {
            return (TextView) root;
        }

        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findFirstTextView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}