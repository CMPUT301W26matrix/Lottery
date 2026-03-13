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
 * RecyclerView adapter used to display events to entrants on the
 * {@link EntrantMainActivity} screen.
 *
 * <p>This adapter binds {@link Event} objects retrieved from Firestore to the
 * entrant event card layout ({@code item_event_entrant.xml}). Each item shows:
 *
 * <ul>
 *   <li>Event title</li>
 *   <li>Scheduled event date</li>
 *   <li>Short event description</li>
 *   <li>A button to view full event details</li>
 * </ul>
 *
 * <p>When an event item or its "View Details" button is clicked, the adapter
 * notifies a listener so the application can open
 * {@link EntrantEventDetailsActivity}.
 *
 * <p>This adapter supports the entrant workflow where users browse available
 * events and select one to join or leave the waiting list.
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EntrantEventViewHolder> {

    /**
     * Listener interface used to notify when an event item is clicked.
     */
    public interface OnEventClickListener {

        /**
         * Called when an event item is selected.
         *
         * @param event the selected {@link Event}
         */
        void onEventClick(Event event);
    }

    /**
     * List of events displayed in the RecyclerView.
     */
    private final List<Event> eventList;

    /**
     * Listener for event click interactions.
     */
    private final OnEventClickListener listener;

    /**
     * Formatter used to display event dates.
     */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

    /**
     * Constructs an EntrantEventAdapter.
     *
     * @param eventList list of events to display
     * @param listener  listener that handles event selection
     */
    public EntrantEventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder for an event item.
     *
     * @param parent   parent view group
     * @param viewType type of view
     * @return a new {@link EntrantEventViewHolder}
     */
    @NonNull
    @Override
    public EntrantEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_entrant, parent, false);

        return new EntrantEventViewHolder(view);
    }

    /**
     * Binds event data to the views inside the ViewHolder.
     *
     * @param holder   the ViewHolder
     * @param position the position of the event in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantEventViewHolder holder, int position) {

        Event event = eventList.get(position);

        holder.tvEventTitle.setText(event.getTitle());

        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        holder.tvEventDescription.setText(event.getDetails());

        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    /**
     * Returns the number of events in the list.
     *
     * @return total number of events
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class that stores references to the views used
     * in each event item.
     */
    static class EntrantEventViewHolder extends RecyclerView.ViewHolder {

        /** Event title text view */
        TextView tvEventTitle;

        /** Event date text view */
        TextView tvEventDate;

        /** Event description preview */
        TextView tvEventDescription;

        /** Button used to open the event details screen */
        Button btnViewDetails;

        /**
         * Constructs the ViewHolder and initializes the view references.
         *
         * @param itemView the layout view for an event item
         */
        public EntrantEventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}