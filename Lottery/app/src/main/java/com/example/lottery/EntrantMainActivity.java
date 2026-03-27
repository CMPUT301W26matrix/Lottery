package com.example.lottery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Main activity for the entrant user role.
 * Displays a list of events with filtering and search capabilities.
 */
public class EntrantMainActivity extends AppCompatActivity {

    // Browse Tab Constants
    private static final String TAB_ALL = "All";
    private static final String TAB_NEW = "New";
    private static final String TAB_RECOMMENDED = "Recommended";

    private final List<Event> masterEventList = new ArrayList<>();
    private final List<Event> filteredEventList = new ArrayList<>();
    private final List<String> userInterests = new ArrayList<>();
    private RecyclerView rvEvents;
    private EntrantEventAdapter adapter;
    private View emptyStateContainer;
    private View tvNotificationBadge;
    private FirebaseFirestore db;
    private String userId;
    private View llSearchToggle;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private ChipGroup cgBrowseTabs, cgCategories, cgQuickFilters;
    private MaterialButton btnTimeFilter;
    private String currentBrowseTab = TAB_ALL;
    private String currentSearchQuery = "";
    private String currentCategory = TAB_ALL;
    private String currentTimeFilter = "All Dates";
    private boolean filterAvailable = false;
    private boolean filterWaitlistOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupFilters();
        setupNavigation();
        loadEvents();
        fetchUserInterests();
        checkUnreadNotifications();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        llSearchToggle = findViewById(R.id.llSearchToggle);
        tilSearch = findViewById(R.id.tilSearch);
        etSearch = findViewById(R.id.etSearch);
        cgBrowseTabs = findViewById(R.id.cgBrowseTabs);
        cgCategories = findViewById(R.id.cgCategories);
        cgQuickFilters = findViewById(R.id.cgQuickFilters);
        btnTimeFilter = findViewById(R.id.btnTimeFilter);

        adapter = new EntrantEventAdapter(filteredEventList, this::openEventDetails);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void setupFilters() {
        // Toggle search field visibility when clicking the search section
        llSearchToggle.setOnClickListener(v -> {
            if (tilSearch.getVisibility() == View.GONE) {
                tilSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                tilSearch.setVisibility(View.GONE);
                etSearch.setText(""); // Clear search when closing
                currentSearchQuery = "";
                applyFilters();
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
            }
        });

        // Wire search input changes to filtering
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cgBrowseTabs.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentBrowseTab = TAB_ALL;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipBrowseAll) {
                    currentBrowseTab = TAB_ALL;
                } else if (checkedId == R.id.chipBrowseNew) {
                    currentBrowseTab = TAB_NEW;
                } else if (checkedId == R.id.chipBrowseRecommended) {
                    currentBrowseTab = TAB_RECOMMENDED;
                }
            }
            applyFilters();
        });

        cgCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = TAB_ALL;
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                currentCategory = chip.getText().toString();
            }
            applyFilters();
        });

        cgQuickFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterAvailable = checkedIds.contains(R.id.chipAvailable);
            filterWaitlistOpen = checkedIds.contains(R.id.chipWaitlistOpen);
            applyFilters();
        });

        btnTimeFilter.setOnClickListener(v -> showTimeFilterDialog());
    }

    private void showTimeFilterDialog() {
        String[] options = {"All Dates", "Today", "This Week", "Upcoming"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Time Range")
                .setItems(options, (dialog, which) -> {
                    currentTimeFilter = options[which];
                    btnTimeFilter.setText(currentTimeFilter);
                    applyFilters();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
        fetchUserInterests();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantEventHistoryActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
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

        findViewById(R.id.ivNotificationIcon).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });
    }

    private void loadEvents() {
        db.collection(FirestorePaths.EVENTS)
                .whereEqualTo("status", "open")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterEventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        masterEventList.add(event);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fetches user interests to enable the 'Recommended' browse tab.
     */
    private void fetchUserInterests() {
        if (userId == null) return;
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    userInterests.clear();
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getInterests() != null) {
                            userInterests.addAll(user.getInterests());
                        }
                    }
                    if (TAB_RECOMMENDED.equals(currentBrowseTab)) {
                        applyFilters();
                    }
                });
    }

    private void applyFilters() {
        filteredEventList.clear();

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endOfToday = cal.getTime();

        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date endOfWeek = cal.getTime();

        for (Event event : masterEventList) {
            // 1. Browse Tab Filter (All/New/Recommended)
            if (TAB_NEW.equals(currentBrowseTab)) {
                if (!isNewEvent(event)) continue;
            } else if (TAB_RECOMMENDED.equals(currentBrowseTab)) {
                if (!isRecommendedEvent(event)) continue;
            }

            // 2. Search filter (Case-insensitive partial match on title)
            String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
            if (!currentSearchQuery.isEmpty() && !title.contains(currentSearchQuery)) continue;

            // 3. Category filter
            if (!TAB_ALL.equalsIgnoreCase(currentCategory)) {
                if (event.getCategory() == null || !event.getCategory().equalsIgnoreCase(currentCategory))
                    continue;
            }

            // 4. Time filter
            if (!"All Dates".equals(currentTimeFilter)) {
                if (event.getScheduledDateTime() == null) continue;
                Date eventDate = event.getScheduledDateTime().toDate();
                if ("Today".equals(currentTimeFilter)) {
                    if (eventDate.before(now) || eventDate.after(endOfToday)) continue;
                } else if ("This Week".equals(currentTimeFilter)) {
                    if (eventDate.before(now) || eventDate.after(endOfWeek)) continue;
                } else if ("Upcoming".equals(currentTimeFilter)) {
                    if (eventDate.before(now)) continue;
                }
            }

            // 5. Quick filter: Available (Registration still open and draw hasn't occurred)
            if (filterAvailable) {
                if (event.getRegistrationDeadline() != null && event.getRegistrationDeadline().toDate().before(now))
                    continue;
                if (event.getDrawDate() != null && event.getDrawDate().toDate().before(now))
                    continue;
                if (event.getScheduledDateTime() != null && event.getScheduledDateTime().toDate().before(now))
                    continue;
            }

            // 6. Quick filter: Waitlist Open (Waitlist capacity remains)
            if (filterWaitlistOpen) {
                if (event.getWaitingListLimit() != null && event.getWaitingListLimit() <= 0)
                    continue;
                if (event.getRegistrationDeadline() != null && event.getRegistrationDeadline().toDate().before(now))
                    continue;
            }

            filteredEventList.add(event);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredEventList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Checks if an event was created within the last 7 days.
     */
    private boolean isNewEvent(Event event) {
        if (event.getCreatedAt() == null) return false;

        long sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        long eventCreatedTime = event.getCreatedAt().toDate().getTime();

        return (currentTime - eventCreatedTime) <= sevenDaysInMillis;
    }

    /**
     * Checks if an event is recommended based on user interests.
     */
    private boolean isRecommendedEvent(Event event) {
        if (userInterests == null || userInterests.isEmpty()) return false;

        String category = event.getCategory();
        if (category == null || category.trim().isEmpty()) return false;

        category = category.trim();
        for (String interest : userInterests) {
            if (interest != null && interest.trim().equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    private void checkUnreadNotifications() {
        if (userId == null || tvNotificationBadge == null) return;
        db.collection(FirestorePaths.userInbox(userId)).whereEqualTo("isRead", false).get()
                .addOnSuccessListener(querySnapshot -> tvNotificationBadge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE));
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}
