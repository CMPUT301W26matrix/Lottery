package com.example.lottery.organizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.adapter.EventAdapter;
import com.example.lottery.model.Event;
import com.example.lottery.util.AdminRoleManager;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.OrganizerNavigationHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerBrowseEventsActivity serves as the organizer event browser, displaying a summary of published events.
 *
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Displays a list of events created by the organizer.</li>
 *   <li>Provides a summary of event statuses (Active, Closed, etc.).</li>
 *   <li>Handles navigation to the event creation screen and event detail screens.</li>
 *   <li>Fetches event data from Firestore on creation and resume.</li>
 * </ul>
 */
public class OrganizerBrowseEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "OrganizerBrowseEvents";

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private final List<Event> masterEventList = new ArrayList<>();
    private final List<Event> filteredEventList = new ArrayList<>();
    
    private TextView tvNoEvents, tvCurrentFilter;
    private TextView tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;
    private MaterialCardView cardActive, cardPending, cardClosed, cardAll, cvActionRequired;
    private TextView tvActionMessage;
    
    private View llSearchToggle;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;

    private FirebaseFirestore db;
    private String userId;
    private boolean isAdminRole = false;
    private String adminUserId;
    
    private String currentStatusFilter = "all"; // all, active, pending, finished
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_browse_events);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
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

        isAdminRole = getIntent().getBooleanExtra("isAdminRole", false);
        if (isAdminRole) {
            adminUserId = AdminRoleManager.getAdminUserId(this);
        }

        if (userId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupInteractions();
        
        OrganizerNavigationHelper.setup(this, OrganizerNavigationHelper.OrganizerTab.HOME, userId);
        loadOrganizerEvents();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvCurrentFilter = findViewById(R.id.tvCurrentFilter);
        
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        
        cardActive = findViewById(R.id.cardActive);
        cardPending = findViewById(R.id.cardPending);
        cardClosed = findViewById(R.id.cardClosed);
        cardAll = findViewById(R.id.cardAll);
        
        cvActionRequired = findViewById(R.id.cvActionRequired);
        tvActionMessage = findViewById(R.id.tvActionMessage);
        
        llSearchToggle = findViewById(R.id.llSearchToggle);
        tilSearch = findViewById(R.id.tilSearch);
        etSearch = findViewById(R.id.etSearch);

        adapter = new EventAdapter(filteredEventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void setupInteractions() {
        llSearchToggle.setOnClickListener(v -> {
            if (tilSearch.getVisibility() == View.GONE) {
                tilSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            } else {
                tilSearch.setVisibility(View.GONE);
                etSearch.setText("");
                currentSearchQuery = "";
                applyFilters();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        cardActive.setOnClickListener(v -> setFilter("active", getString(R.string.filter_active_label)));
        cardPending.setOnClickListener(v -> setFilter("pending", getString(R.string.filter_pending_label)));
        cardClosed.setOnClickListener(v -> setFilter("finished", getString(R.string.filter_finished_label)));
        cardAll.setOnClickListener(v -> setFilter("all", getString(R.string.filter_all_label)));
    }

    private void setFilter(String status, String label) {
        currentStatusFilter = status;
        tvCurrentFilter.setText(label);
        applyFilters();
    }

    private void applyFilters() {
        filteredEventList.clear();
        for (Event event : masterEventList) {
            String displayStatus = EventAdapter.resolveDisplayStatus(event);
            
            if (!currentStatusFilter.equals("all")) {
                boolean match = false;
                if (currentStatusFilter.equals("active")) {
                    match = "open".equals(displayStatus) || "ongoing".equals(displayStatus);
                } else if (currentStatusFilter.equals("pending")) {
                    match = "pending_draw".equals(displayStatus);
                } else if (currentStatusFilter.equals("finished")) {
                    match = "finished".equals(displayStatus) || "closed".equals(displayStatus) || "private".equals(displayStatus);
                }
                if (!match) continue;
            }
            
            if (!currentSearchQuery.isEmpty()) {
                String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
                if (!title.contains(currentSearchQuery)) continue;
            }
            
            filteredEventList.add(event);
        }
        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(filteredEventList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadOrganizerEvents() {
        if (userId == null) return;

        adapter.clearCountsCache();
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("organizerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterEventList.clear();
                    int active = 0;
                    int closed = 0;
                    int pending = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            masterEventList.add(event);

                            String displayStatus = EventAdapter.resolveDisplayStatus(event);
                            switch (displayStatus) {
                                case "open":
                                case "ongoing":
                                    active++;
                                    break;
                                case "pending_draw":
                                    pending++;
                                    break;
                                default:
                                    closed++;
                                    break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document", e);
                        }
                    }

                    updateSummaryStats(active, closed, pending, masterEventList.size());
                    updateUrgentTasks(pending);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase error", e);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUrgentTasks(int pendingDraws) {
        if (pendingDraws > 0) {
            cvActionRequired.setVisibility(View.VISIBLE);
            tvActionMessage.setText(getString(R.string.pending_draws_message, pendingDraws));
            cvActionRequired.setOnClickListener(v -> setFilter("pending", getString(R.string.filter_pending_label)));
        } else {
            cvActionRequired.setVisibility(View.GONE);
        }
    }

    private void updateSummaryStats(int active, int closed, int pending, int total) {
        tvActiveCount.setText(String.valueOf(active));
        tvClosedCount.setText(String.valueOf(closed));
        tvPendingCount.setText(String.valueOf(pending));
        tvTotalCount.setText(String.valueOf(total));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        intent.putExtra("userId", userId);
        intent.putExtra("isAdminRole", isAdminRole);
        if (isAdminRole) {
            intent.putExtra("adminUserId", adminUserId);
        }
        startActivity(intent);
    }
}
