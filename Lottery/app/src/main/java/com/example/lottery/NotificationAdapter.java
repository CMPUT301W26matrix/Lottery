package com.example.lottery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.TypedValue;
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
 * <p>This adapter binds {@link NotificationItem} objects from the user's inbox
 * and supports entrant Accessibility Mode by increasing card text sizes when enabled.</p>
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    /** SharedPreferences file used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for entrant accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;

    /**
     * Creates a new NotificationAdapter.
     *
     * @param notifications list of notifications to display
     * @param listener click listener for notification interactions
     */
    public NotificationAdapter(List<NotificationItem> notifications,
                               OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder for notification items.
     *
     * @param parent the parent view group
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

        Context context = holder.itemView.getContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAccessibilityEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);

        float titleSize = isAccessibilityEnabled ? 19f : 16f;
        float typeSize = isAccessibilityEnabled ? 15f : 12f;
        float messageSize = isAccessibilityEnabled ? 17f : 14f;
        float badgeSize = isAccessibilityEnabled ? 13f : 11f;

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.tvType.setText(formatNotificationType(item.getType()));
        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "");

        if (item.getEventTitle() != null && !item.getEventTitle().isEmpty()) {
            holder.tvTitle.setText(item.getEventTitle() + ": " + item.getTitle());
        }

        holder.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);
        holder.tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, typeSize);
        holder.tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, messageSize);
        holder.tvNew.setTextSize(TypedValue.COMPLEX_UNIT_SP, badgeSize);
        holder.tvResponse.setTextSize(TypedValue.COMPLEX_UNIT_SP, badgeSize);

        if (!item.isRead()) {
            holder.tvNew.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"));
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

    /**
     * Converts internal notification types into user-friendly labels.
     *
     * @param type internal notification type
     * @return display label
     */
    private String formatNotificationType(String type) {
        if (type == null) {
            return "General";
        }

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
     * Listener for notification clicks.
     */
    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    /**
     * ViewHolder for notification cards.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvType;
        TextView tvMessage;
        TextView tvNew;
        TextView tvResponse;

        /**
         * Creates a ViewHolder and binds item views.
         *
         * @param itemView notification card view
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