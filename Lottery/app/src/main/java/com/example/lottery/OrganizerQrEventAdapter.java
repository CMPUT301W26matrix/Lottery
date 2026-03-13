package com.example.lottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;

import java.util.List;

/**
 * Adapter for displaying events in the QR code selection list.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Inflates the layout for individual event items in the QR selection list.</li>
 *   <li>Binds event data (title and date) to the view elements.</li>
 *   <li>Handles user click interactions to trigger QR code display for a specific event.</li>
 * </ul>
 * </p>
 */
public class OrganizerQrEventAdapter extends RecyclerView.Adapter<OrganizerQrEventAdapter.ViewHolder> {

    /** The list of events to be displayed in the RecyclerView. */
    private final List<Event> events;
    /** The listener that handles click events on the event items. */
    private final OnEventClickListener listener;

    /**
     * Interface definition for a callback to be invoked when an event item is clicked.
     */
    public interface OnEventClickListener {
        /**
         * Called when an event item has been clicked.
         *
         * @param event The Event object associated with the clicked item.
         */
        void onEventClick(Event event);
    }

    /**
     * Constructs a new OrganizerQrEventAdapter.
     *
     * @param events   The list of events to display.
     * @param listener The listener to handle event clicks.
     */
    public OrganizerQrEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_qr, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvTitle.setText(event.getTitle());
        holder.tvDate.setText(event.getScheduledDateTime() != null ? event.getScheduledDateTime().toString() : "No Date");
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class for holding and recycling event item views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the event title. */
        TextView tvTitle;
        /** TextView for displaying the event date. */
        TextView tvDate;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The root view of the item layout.
         */
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
        }
    }
}
