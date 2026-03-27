package com.example.lottery;

import android.app.Dialog;
import android.os.Bundle;
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

import com.example.lottery.model.NotificationItem;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
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

public class OrganizerInviteCoOrganizerDialogFragment extends DialogFragment {

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_EVENT_TITLE = "eventTitle";
    private static final String ARG_SENDER_ID = "senderId";
    private final List<User> userList = new ArrayList<>();
    private final Set<String> existingCoOrganizerIds = new HashSet<>();
    private String eventId;
    private String eventTitle;
    private String senderId;
    private FirebaseFirestore db;
    private UserSearchAdapter adapter;

    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private ProgressBar progressBar;
    private TextView tvNoResults;

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
        fetchExistingCoOrganizers();
    }

    private void fetchExistingCoOrganizers() {
        db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.CO_ORGANIZERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    existingCoOrganizerIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        existingCoOrganizerIds.add(doc.getId());
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_search, container, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Assign Co-Organizer");
        }

        etSearch = view.findViewById(R.id.etSearch);
        rvResults = view.findViewById(R.id.rvResults);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserSearchAdapter(userList, existingCoOrganizerIds, this::assignCoOrganizer);
        rvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().trim());
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
        if (query.length() < 2) {
            userList.clear();
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    String lowerQuery = query.toLowerCase();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());

                        // Don't show the current organizer in search
                        if (user.getUserId().equals(senderId)) continue;

                        boolean match = user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery);
                        if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery))
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
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Search failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void assignCoOrganizer(User user) {
        if (existingCoOrganizerIds.contains(user.getUserId())) {
            Toast.makeText(getContext(), "Already a co-organizer", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();

        // 1. Add to event's coOrganizers collection
        DocumentReference coOrgRef = db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.CO_ORGANIZERS).document(user.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("assignedAt", Timestamp.now());
        batch.set(coOrgRef, data);

        // 2. Remove from waitlist if they are there (US Requirement)
        DocumentReference waitlistRef = db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.WAITING_LIST).document(user.getUserId());
        batch.delete(waitlistRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    existingCoOrganizerIds.add(user.getUserId());
                    sendNotification(user);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to assign co-organizer", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(User targetUser) {
        String notificationId = UUID.randomUUID().toString();
        NotificationItem notification = new NotificationItem(
                notificationId,
                "Co-Organizer Assignment",
                "You have been assigned as a co-organizer for: " + eventTitle,
                "co_organizer_assignment",
                eventId,
                eventTitle,
                senderId,
                "ORGANIZER",
                false,
                Timestamp.now()
        );

        db.collection("users").document(targetUser.getUserId())
                .collection("inbox").document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), targetUser.getUsername() + " is now a co-organizer!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Assigned, but notification failed", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<User> users;
        private final Set<String> existingCoOrganizerIds;
        private final OnUserClickListener listener;

        UserSearchAdapter(List<User> users, Set<String> existingCoOrganizerIds, OnUserClickListener listener) {
            this.users = users;
            this.existingCoOrganizerIds = existingCoOrganizerIds;
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
            boolean isAlreadyCoOrg = existingCoOrganizerIds.contains(user.getUserId());

            holder.text1.setText(user.getUsername() + (isAlreadyCoOrg ? " (Already Co-Organizer)" : ""));
            holder.text2.setText(user.getEmail() != null ? user.getEmail() : user.getPhone());

            holder.itemView.setEnabled(!isAlreadyCoOrg);
            holder.itemView.setAlpha(isAlreadyCoOrg ? 0.5f : 1.0f);

            holder.itemView.setOnClickListener(v -> {
                if (!isAlreadyCoOrg) {
                    listener.onUserClick(user);
                }
            });
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
