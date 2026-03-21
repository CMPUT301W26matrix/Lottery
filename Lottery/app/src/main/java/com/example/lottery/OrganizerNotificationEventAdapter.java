package com.example.lottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying events in the Organizer Notifications screen.
 * Each item allows the organizer to send notifications to different groups of entrants.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Binds event data to the list items in the notification screen.</li>
 *   <li>Delegates button clicks to a listener to handle notification composition.</li>
 * </ul>
 * </p>
 */
public class OrganizerNotificationEventAdapter extends RecyclerView.Adapter<OrganizerNotificationEventAdapter.ViewHolder> {

    /**
     * Listener interface for handling clicks on notification group buttons.
     */
    public interface OnNotificationGroupClickListener {
        /**
         * Called when a specific group (Waitlist, Selected, or Cancelled) is clicked for an event.
         *
         * @param event The event associated with the notification.
         * @param group The group identifier (e.g., "waitlisted", "selected", "cancelled").
         */
        void onGroupClick(Event event, String group);
    }

    private final List<Event> eventList;
    private final OnNotificationGroupClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * Constructs a new OrganizerNotificationEventAdapter.
     *
     * @param eventList The list of events to display.
     * @param listener  The listener to handle group click events.
     */
    public OrganizerNotificationEventAdapter(List<Event> eventList, OnNotificationGroupClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_notification_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvEventTitle.setText(event.getTitle());

        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvEventDate.setText("No date set");
        }

        // Map UI buttons to backend status groups
        holder.btnNotifyWaiting.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(event, "waitlisted");
        });

        holder.btnNotifySelected.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(event, "selected");
        });

        holder.btnNotifyCancelled.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(event, "cancelled");
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for caching UI component references in each list item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvEventDate;
        Button btnNotifyWaiting, btnNotifySelected, btnNotifyCancelled;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            btnNotifyWaiting = itemView.findViewById(R.id.btnNotifyWaiting);
            btnNotifySelected = itemView.findViewById(R.id.btnNotifySelected);
            btnNotifyCancelled = itemView.findViewById(R.id.btnNotifyCancelled);
        }
    }
}
