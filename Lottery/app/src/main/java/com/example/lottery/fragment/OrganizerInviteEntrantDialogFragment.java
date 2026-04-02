package com.example.lottery.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.model.NotificationItem;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DialogFragment that lets an organizer search for entrants by name, email, or phone
 * and directly invite them to an event's waiting list.
 */
public class OrganizerInviteEntrantDialogFragment extends DialogFragment {

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_EVENT_TITLE = "eventTitle";
    private static final String ARG_SENDER_ID = "senderId";
    private final List<User> userList = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private String eventId;
    private String eventTitle;
    private String senderId;
    private FirebaseFirestore db;
    private UserSearchAdapter adapter;
    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private ProgressBar progressBar;
    private TextView tvNoResults;
    private Runnable pendingSearch;

    /**
     * Creates a new instance of this dialog.
     *
     * @param eventId    the event to invite entrants to
     * @param eventTitle the event title (used in the notification message)
     * @param senderId   the current organizer's user ID
     * @return a configured dialog fragment
     */
    public static OrganizerInviteEntrantDialogFragment newInstance(String eventId, String eventTitle, String senderId) {
        OrganizerInviteEntrantDialogFragment fragment = new OrganizerInviteEntrantDialogFragment();
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
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserSearchAdapter(userList, this::inviteUser);
        rvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                pendingSearch = () -> searchUsers(query);
                searchHandler.postDelayed(pendingSearch, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void searchUsers(String query) {
        if (!isAdded()) return;
        if (query.length() < 2) {
            userList.clear();
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        // Search by username, email, or phone (simplified: searching by username start)
        db.collection(FirestorePaths.USERS)
                .whereEqualTo("role", "ENTRANT")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    userList.clear();
                    String lowerQuery = query.toLowerCase();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());

                        boolean match = user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery);
                        if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery))
                            match = true;
                        if (user.getPhone() != null && user.getPhone().contains(query))
                            match = true;

                        if (match) {
                            userList.add(user);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                    tvNoResults.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    private void inviteUser(User user) {
        // 1. Add to event's waiting_list with status "invited"
        Map<String, Object> waitlistData = new HashMap<>();
        waitlistData.put("userId", user.getUserId());
        waitlistData.put("username", user.getUsername());
        waitlistData.put("status", "invited");
        waitlistData.put("invitedAt", Timestamp.now());

        db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.WAITING_LIST).document(user.getUserId())
                .set(waitlistData)
                .addOnSuccessListener(aVoid -> {
                    // 2. Send notification to user's inbox
                    sendNotification(user);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to invite user", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(User targetUser) {
        // US 01.04.03: Respect notification preference
        if (!targetUser.isNotificationsEnabled()) {
            if (isAdded())
                Toast.makeText(requireContext(), "Invitation successful (notifications opted out)", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        String notificationId = UUID.randomUUID().toString();
        NotificationItem notification = new NotificationItem(
                notificationId,
                "Event Invitation",
                "You have been invited to join the waiting list for: " + eventTitle,
                "event_invitation",
                eventId,
                eventTitle,
                senderId,
                "ORGANIZER",
                false,
                Timestamp.now()
        );

        db.collection(FirestorePaths.USERS).document(targetUser.getUserId())
                .collection(FirestorePaths.INBOX).document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded())
                        Toast.makeText(requireContext(), "Invitation sent!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(requireContext(), "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
    }

    // Simple Adapter for search results
    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<User> users;
        private final OnUserClickListener listener;

        UserSearchAdapter(List<User> users, OnUserClickListener listener) {
            this.users = users;
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
            holder.text2.setText(user.getEmail() != null ? user.getEmail() : user.getPhone());
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        interface OnUserClickListener {
            void onUserClick(User user);
        }

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