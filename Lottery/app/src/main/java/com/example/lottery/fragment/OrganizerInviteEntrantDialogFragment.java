package com.example.lottery.fragment;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.dialog_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvSearchLabel = view.findViewById(R.id.tvSearchLabel);
        View coOrganizerSection = view.findViewById(R.id.layout_co_organizer_section);
        
        // Customize for Invite Entrant mode
        tvTitle.setText("Invite Entrants");
        tvSearchLabel.setText("Search for Entrants");
        if (coOrganizerSection != null) {
            coOrganizerSection.setVisibility(View.GONE);
        }

        etSearch = view.findViewById(R.id.etSearch);
        rvResults = view.findViewById(R.id.rvResults);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserSearchAdapter(userList, this::inviteUser);
        rvResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                pendingSearch = () -> searchUsers(query);
                searchHandler.postDelayed(pendingSearch, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
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

                        boolean match = (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery))
                                || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery))
                                || (user.getPhone() != null && user.getPhone().contains(query));

                        if (match) userList.add(user);
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
        Map<String, Object> waitlistData = new HashMap<>();
        waitlistData.put("userId", user.getUserId());
        waitlistData.put("username", user.getUsername());
        waitlistData.put("status", "invited");
        waitlistData.put("invitedAt", Timestamp.now());

        db.collection(FirestorePaths.EVENTS).document(eventId)
                .collection(FirestorePaths.WAITING_LIST).document(user.getUserId())
                .set(waitlistData)
                .addOnSuccessListener(aVoid -> sendNotification(user))
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to invite user", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(User targetUser) {
        if (!targetUser.isNotificationsEnabled()) {
            if (isAdded()) Toast.makeText(requireContext(), "Invitation successful (notifications opted out)", Toast.LENGTH_SHORT).show();
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
                    if (isAdded()) Toast.makeText(requireContext(), "Invitation sent!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
    }

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
            holder.text1.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            holder.text2.setText(user.getEmail() != null ? user.getEmail() : user.getPhone());
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
