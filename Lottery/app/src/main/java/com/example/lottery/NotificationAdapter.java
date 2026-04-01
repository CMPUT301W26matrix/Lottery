package com.example.lottery;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.NotificationItem;

import java.util.List;

/**
 * Adapter used to display notifications in a RecyclerView.
 *
 * <p>This adapter binds {@link NotificationItem} objects from the user's inbox.</p>
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;
    private boolean isEventSpecificMode = false;

    /**
     * Creates a new NotificationAdapter.
     *
     * @param notifications list of notifications to display
     * @param listener      click listener for notification interactions
     */
    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Sets whether the adapter is operating in event-specific mode.
     * In event-specific mode, redundant event titles are stripped from notification titles.
     *
     * @param eventSpecificMode true if in event-specific mode, false otherwise
     */
    public void setEventSpecificMode(boolean eventSpecificMode) {
        this.isEventSpecificMode = eventSpecificMode;
    }

    /**
     * Creates a new ViewHolder for notification items.
     *
     * @param parent   the parent view group
     * @param viewType the view type
     * @return a new {@link NotificationViewHolder}
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);

        String rawTitle = item.getTitle() != null ? item.getTitle() : "";
        String eventTitle = item.getEventTitle() != null ? item.getEventTitle() : "";
        
        String displayTitle = rawTitle;

        // In Global Mode (not event-specific), ensure the event title is prefixed if not already there
        if (!isEventSpecificMode && !eventTitle.isEmpty() && !rawTitle.startsWith(eventTitle)) {
            displayTitle = eventTitle + ": " + rawTitle;
        }

        // In Event-specific Mode, strip the redundant event title prefix
        if (isEventSpecificMode && !eventTitle.isEmpty()) {
            if (rawTitle.startsWith(eventTitle + ":")) {
                displayTitle = rawTitle.substring(eventTitle.length() + 1).trim();
            } else if (rawTitle.contains(eventTitle)) {
                displayTitle = rawTitle.replace(eventTitle, "").replace("for", "").replace(":", "").trim();
            }
            
            // Capitalize first letter if shortened
            if (!displayTitle.isEmpty()) {
                displayTitle = displayTitle.substring(0, 1).toUpperCase() + displayTitle.substring(1);
            }

            // Special case for invitations in event mode
            if ("You've been invited!".equalsIgnoreCase(displayTitle) || displayTitle.contains("invited")) {
                displayTitle = "You're invited";
            }
        }

        holder.tvTitle.setText(displayTitle);
        holder.tvType.setText(formatNotificationType(item.getType()));
        
        String message = item.getMessage() != null ? item.getMessage() : "";
        holder.tvMessage.setText(message);

        if (!item.isRead()) {
            holder.tvNew.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#EEF3FF"));
        } else {
            holder.tvNew.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        holder.tvResponse.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(item));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String formatNotificationType(String type) {
        if (type == null) return "General";

        switch (type.toLowerCase()) {
            case "event_invitation":
                return "Event Invitation";
            case "waitlist_promoted":
                return "Waitlist Update";
            case "draw_result":
                return "Draw Result";
            case "event_cancelled":
                return "Event Cancelled";
            case "co_organizer_assignment":
                return "Co-Organizer";
            default:
                return "General";
        }
    }

    /**
     * Callback interface invoked when a notification item is clicked.
     */
    public interface OnNotificationClickListener {
        /**
         * Called when a notification item is clicked.
         *
         * @param item the clicked notification
         */
        void onNotificationClick(NotificationItem item);
    }

    /**
     * ViewHolder that caches references to the notification item's UI components.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvMessage, tvNew, tvResponse;

        /**
         * Creates a new NotificationViewHolder.
         *
         * @param itemView the inflated item layout
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvType = itemView.findViewById(R.id.tvNotificationType);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNew = itemView.findViewById(R.id.tvNotificationNew);
            tvResponse = itemView.findViewById(R.id.tvNotificationResponse);
        }
    }
}
