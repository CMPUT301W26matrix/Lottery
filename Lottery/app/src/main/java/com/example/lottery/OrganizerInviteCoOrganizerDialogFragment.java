package com.example.lottery;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.NotificationItem;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DialogFragment that lets an organizer manage (view, remove, and add) co-organizers
 * for a specific event.
 */
public class OrganizerInviteCoOrganizerDialogFragment extends DialogFragment {

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_EVENT_TITLE = "eventTitle";
    private static final String ARG_SENDER_ID = "senderId";

    private final List<User> searchResults = new ArrayList<>();
    private final List<User> currentCoOrganizers = new ArrayList<>();
    private final Set<String> currentCoOrganizerIds = new HashSet<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    private String eventId;
    private String eventTitle;
    private String senderId;
    private FirebaseFirestore db;

    private UserSearchAdapter searchAdapter;
    private CoOrganizerAdapter currentAdapter;

    private TextInputEditText etSearch;
    private RecyclerView rvResults, rvCurrent;
    private ProgressBar progressBar;
    private TextView tvNoResults;
    private Runnable pendingSearch;

    public static OrganizerInviteCoOrganizerDialogFragment newInstance(String eventId, String eventTitle, String senderId) {
        OrganizerInviteCoOrganizerDialogFragment fragment = new OrganizerInviteCoOrganizerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        args.putString(ARG_SENDER_ID, senderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventTitle = getArguments().getString(ARG_EVENT_TITLE);
            senderId = getArguments().getString(ARG_SENDER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_search, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        rvResults = view.findViewById(R.id.rvResults);
        rvCurrent = view.findViewById(R.id.rvCurrentCoOrganizers);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        // Setup Current Co-Organizers List
        rvCurrent.setLayoutManager(new LinearLayoutManager(getContext()));
        currentAdapter = new CoOrganizerAdapter(currentCoOrganizers, this::showRemoveConfirmation);
        rvCurrent.setAdapter(currentAdapter);

        // Setup Search Results List
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new UserSearchAdapter(searchResults, currentCoOrganizerIds, this::assignCoOrganizer);
        rvResults.setAdapter(searchAdapter);

        fetchCoOrganizers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                pendingSearch = () -> searchUsers(query);
                searchHandler.postDelayed(pendingSearch, 300);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void fetchCoOrganizers() {
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.CO_ORGANIZERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    currentCoOrganizers.clear();
                    currentCoOrganizerIds.clear();
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ids.add(doc.getId());
                        currentCoOrganizerIds.add(doc.getId());
                    }
                    
                    if (ids.isEmpty()) {
                        currentAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Fetch user details for these IDs
                    db.collection(FirestorePaths.USERS)
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                                    if (currentCoOrganizerIds.contains(userDoc.getId())) {
                                        User u = userDoc.toObject(User.class);
                                        u.setUserId(userDoc.getId());
                                        currentCoOrganizers.add(u);
                                    }
                                }
                                currentAdapter.notifyDataSetChanged();
                                searchAdapter.notifyDataSetChanged();
                            });
                });
    }

    private void searchUsers(String query) {
        if (!isAdded()) return;
        if (query.length() < 2) {
            searchResults.clear();
            searchAdapter.notifyDataSetChanged();
            tvNoResults.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        db.collection(FirestorePaths.USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    searchResults.clear();
                    String lowerQuery = query.toLowerCase();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());

                        if (user.getUserId().equals(senderId)) continue;
                        if (currentCoOrganizerIds.contains(user.getUserId())) continue;

                        boolean match = (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery))
                                || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery));

                        if (match) searchResults.add(user);
                    }

                    progressBar.setVisibility(View.GONE);
                    searchAdapter.notifyDataSetChanged();
                    tvNoResults.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void assignCoOrganizer(User user) {
        WriteBatch batch = db.batch();
        DocumentReference coOrgRef = db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.CO_ORGANIZERS).document(user.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("assignedAt", Timestamp.now());
        batch.set(coOrgRef, data);

        // Remove from waitlist if exists
        DocumentReference waitlistRef = db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.WAITING_LIST).document(user.getUserId());
        batch.delete(waitlistRef);

        batch.commit().addOnSuccessListener(aVoid -> {
            fetchCoOrganizers();
            sendNotification(user, true);
            etSearch.setText("");
        });
    }

    private void showRemoveConfirmation(User user) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Co-Organizer")
                .setMessage("Are you sure you want to remove " + user.getUsername() + " as a co-organizer from this event?")
                .setPositiveButton("Remove", (d, which) -> removeCoOrganizer(user))
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        // Style the positive button as destructive
        Button removeBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (removeBtn != null) {
            removeBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
        }
    }

    private void removeCoOrganizer(User user) {
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.CO_ORGANIZERS).document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    fetchCoOrganizers();
                    sendNotification(user, false);
                });
    }

    private void sendNotification(User targetUser, boolean isAdded) {
        String notificationId = UUID.randomUUID().toString();
        String title = isAdded ? "Co-Organizer Assignment" : "Co-Organizer Removal";
        String message = isAdded 
                ? "You have been assigned as a co-organizer for: " + eventTitle
                : "You are no longer a co-organizer for: " + eventTitle;

        NotificationItem notification = new NotificationItem(
                notificationId, title, message, "co_organizer_update",
                eventId, eventTitle, senderId, "ORGANIZER", false, Timestamp.now()
        );

        db.collection(FirestorePaths.USERS).document(targetUser.getUserId())
                .collection(FirestorePaths.INBOX).document(notificationId)
                .set(notification);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // Set the window background to transparent to allow layout's rounded corners to show
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    // --- Adapters ---

    private static class CoOrganizerAdapter extends RecyclerView.Adapter<CoOrganizerAdapter.ViewHolder> {
        private final List<User> users;
        private final OnRemoveListener listener;

        CoOrganizerAdapter(List<User> users, OnRemoveListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_current_co_organizer, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvUsername.setText(user.getUsername());
            holder.tvEmail.setText(user.getEmail());
            holder.btnRemove.setOnClickListener(v -> listener.onRemove(user));
        }

        @Override
        public int getItemCount() { return users.size(); }

        interface OnRemoveListener { void onRemove(User user); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUsername, tvEmail;
            ImageButton btnRemove;
            ViewHolder(View v) {
                super(v);
                tvUsername = v.findViewById(R.id.tvUsername);
                tvEmail = v.findViewById(R.id.tvEmail);
                btnRemove = v.findViewById(R.id.btnRemove);
            }
        }
    }

    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<User> users;
        private final Set<String> existingIds;
        private final OnUserClickListener listener;

        UserSearchAdapter(List<User> users, Set<String> existingIds, OnUserClickListener listener) {
            this.users = users;
            this.existingIds = existingIds;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.text1.setText(user.getUsername());
            holder.text2.setText(user.getEmail());
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() { return users.size(); }

        interface OnUserClickListener { void onUserClick(User user); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
