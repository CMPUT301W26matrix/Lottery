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

    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

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

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.tvType.setText(formatNotificationType(item.getType()));
        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "");

        if (item.getEventTitle() != null && !item.getEventTitle().isEmpty()) {
            holder.tvTitle.setText(item.getEventTitle() + ": " + item.getTitle());
        }

        if (!item.isRead()) {
            holder.tvNew.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            holder.tvNew.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        // Response display logic is simplified as the inbox model no longer tracks complex action state.
        // If needed, the actual participation status should be checked from the event's waiting list.
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
            case "event_invitation": return "Event Invitation";
            case "waitlist_promoted": return "Waitlist Update";
            case "draw_result": return "Draw Result";
            case "event_cancelled": return "Event Cancelled";
            case "co_organizer_assignment": return "Co-Organizer";
            default: return "General";
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvMessage, tvNew, tvResponse;

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
