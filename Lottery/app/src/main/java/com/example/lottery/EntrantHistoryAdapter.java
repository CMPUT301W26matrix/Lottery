package com.example.lottery;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying the entrant's event registration history.
 */
public class EntrantHistoryAdapter extends RecyclerView.Adapter<EntrantHistoryAdapter.ViewHolder> {

    private final List<HistoryItem> historyItems;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final String userId;

    /**
     * Creates a new EntrantHistoryAdapter.
     *
     * @param historyItems list of history items to display
     * @param listener     click listener for item interactions
     * @param userId       the current user's ID
     */
    public EntrantHistoryAdapter(List<HistoryItem> historyItems, OnItemClickListener listener, String userId) {
        this.historyItems = historyItems;
        this.listener = listener;
        this.userId = userId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        Event event = item.event;

        holder.tvTitle.setText(event.getTitle());
        if (event.getScheduledDateTime() != null) {
            holder.tvDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvDate.setText("Date TBD");
        }

        // Show Organizer Name if available
        if (item.organizerName != null && !item.organizerName.isEmpty()) {
            holder.tvOrganizer.setVisibility(View.VISIBLE);
            holder.tvOrganizer.setText("By: " + item.organizerName);
        } else {
            holder.tvOrganizer.setVisibility(View.GONE);
        }

        // Show description only if available (Max 2 lines as per UI polish)
        String details = event.getDetails();
        if (details != null && !details.trim().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(details);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Set status badge
        holder.tvStatus.setVisibility(View.VISIBLE);
        String status = InvitationFlowUtil.normalizeEntrantStatus(item.status);

        // Map internal status to user-friendly labels
        String displayStatus = status.toUpperCase();
        if (InvitationFlowUtil.STATUS_INVITED.equals(status)) displayStatus = "SELECTED";
        if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) displayStatus = "CONFIRMED";
        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) displayStatus = "WAITING LIST";

        holder.tvStatus.setText(displayStatus);

        // Style status badge
        if ("SELECTED".equals(displayStatus)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge); // Assuming this is a standout color
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(event));
        holder.btnViewDetails.setOnClickListener(v -> listener.onItemClick(event));

        // Notification Icon Click - Event Specific Mode
        holder.ivNotificationIcon.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NotificationsActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("eventTitle", event.getTitle());
            v.getContext().startActivity(intent);
        });

        // Check for unread notifications for this user and event
        checkUnreadNotifications(holder.tvNotificationBadge, event.getEventId());
    }

    private void checkUnreadNotifications(View badge, String eventId) {
        if (userId == null || eventId == null) return;
        FirebaseFirestore.getInstance().collection(FirestorePaths.userInbox(userId))
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    badge.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    /**
     * Listener interface for handling clicks on history items.
     */
    public interface OnItemClickListener {
        /**
         * Called when an entrant taps a history item.
         *
         * @param event the event associated with the clicked item
         */
        void onItemClick(Event event);
    }

    /**
     * Data class that pairs an {@link Event} with the entrant's registration status and
     * the organizer's display name.
     */
    public static class HistoryItem {
        public Event event;
        public String status;
        public String organizerName;

        /**
         * Creates a new HistoryItem.
         *
         * @param event  the event
         * @param status the entrant's status for this event
         */
        public HistoryItem(Event event, String status) {
            this.event = event;
            this.status = status;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDescription, tvStatus, tvOrganizer, btnViewDetails, tvNotificationBadge;
        View ivNotificationIcon;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvEventTitle);
            tvDate = view.findViewById(R.id.tvEventDate);
            tvOrganizer = view.findViewById(R.id.tvRegistrationPeriod); // Reusing this view for Organizer Name
            tvDescription = view.findViewById(R.id.tvEventDescription);
            tvStatus = view.findViewById(R.id.tvEventStatus);
            btnViewDetails = view.findViewById(R.id.btnViewDetails);
            ivNotificationIcon = view.findViewById(R.id.ivNotificationIcon);
            tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        }
    }
}
