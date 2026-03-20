package com.example.lottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
 *   <li>Handles button clicks to notify specific entrant groups (Waiting, Selected, Cancelled).</li>
 * </ul>
 * </p>
 */
public class OrganizerNotificationEventAdapter extends RecyclerView.Adapter<OrganizerNotificationEventAdapter.ViewHolder> {

    /**
     * List of events to be displayed.
     */
    private final List<Event> eventList;

    /**
     * Date formatter for displaying event dates in a readable format.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * Constructs a new OrganizerNotificationEventAdapter.
     *
     * @param eventList The list of events to display.
     */
    public OrganizerNotificationEventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_notification_event, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds event data to the ViewHolder and sets up click listeners for notification buttons.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item in the event list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvEventTitle.setText(event.getTitle());

        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvEventDate.setText("No date set");
        }

        holder.btnNotifyWaiting.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Notifying Waiting List for: " + event.getTitle(), Toast.LENGTH_SHORT).show());

        holder.btnNotifySelected.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Notifying Selected Entrants for: " + event.getTitle(), Toast.LENGTH_SHORT).show());

        holder.btnNotifyCancelled.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Notifying Cancelled Entrants for: " + event.getTitle(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Returns the total number of events in the list.
     *
     * @return The size of the event list.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for caching UI component references in each list item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextViews for event title and date.
         */
        TextView tvEventTitle, tvEventDate;

        /**
         * Buttons for triggering notifications to different entrant groups.
         */
        Button btnNotifyWaiting, btnNotifySelected, btnNotifyCancelled;

        /**
         * Initializes the ViewHolder by binding UI components from the item layout.
         *
         * @param itemView The root view of the list item layout.
         */
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
