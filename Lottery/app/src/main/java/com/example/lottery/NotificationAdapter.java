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
 * <p>This adapter binds {@link NotificationItem} objects to the
 * notification item layout and handles click interactions for
 * each notification.</p>
 *
 * <p>Unread notifications are visually highlighted and a "NEW"
 * badge is displayed. If the notification has already been acted
 * upon (e.g., accepted or rejected), the user's response is shown.</p>
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    /**
     * List of notifications to display.
     */
    private final List<NotificationItem> notifications;
    /**
     * Listener used to handle click events on notifications.
     */
    private final OnNotificationClickListener listener;

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

    /**
     * Binds notification data to the ViewHolder.
     *
     * @param holder   the ViewHolder representing the item
     * @param position position of the notification in the list
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {

        NotificationItem item = notifications.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvType.setText(item.getType());
        holder.tvMessage.setText(item.getMessage());

        // Highlight unread notifications
        if (!item.isRead()) {
            holder.tvNew.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            holder.tvNew.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        // Show response if the user has already acted on the notification
        if (item.isActionTaken() && item.getResponse() != null && !item.getResponse().isEmpty()) {
            holder.tvResponse.setVisibility(View.VISIBLE);
            holder.tvResponse.setText(holder.itemView.getContext()
                    .getString(R.string.notification_response, item.getResponse()));
        } else {
            holder.tvResponse.setVisibility(View.GONE);
        }

        // Handle click event
        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(item));
    }

    /**
     * Returns the total number of notifications.
     *
     * @return number of notifications in the list
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * Listener interface used to handle notification click events.
     */
    public interface OnNotificationClickListener {

        /**
         * Called when a notification item is clicked.
         *
         * @param item the notification that was selected
         */
        void onNotificationClick(NotificationItem item);
    }

    /**
     * ViewHolder representing a single notification item in the RecyclerView.
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        /**
         * Displays the notification title.
         */
        TextView tvTitle;

        /**
         * Displays the notification type.
         */
        TextView tvType;

        /**
         * Displays the notification message.
         */
        TextView tvMessage;

        /**
         * Badge indicating that the notification is new/unread.
         */
        TextView tvNew;

        /**
         * Displays the user's response to the notification (if any).
         */
        TextView tvResponse;

        /**
         * Constructs a ViewHolder and binds UI elements.
         *
         * @param itemView the notification item view
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