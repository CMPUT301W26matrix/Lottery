package com.example.lottery;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of events in the Organizer Dashboard.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Binds event metadata to RecyclerView items.</li>
 *   <li>Dynamically fetches and displays the current waiting list count from Firestore.</li>
 *   <li>Handles clicks on event items to navigate to detailed views.</li>
 *   <li>Visualizes event status (ACTIVE/CLOSED) based on scheduled date.</li>
 * </ul>
 * </p>
 *
 * <p>Satisfies requirement for:
 * US 02.03.01: Show the waiting list capacity.
 * </p>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /**
     * List of events to be displayed.
     */
    private final List<Event> eventList;
    /**
     * Listener for event click interactions.
     */
    private final OnEventClickListener listener;
    /**
     * Date formatter for displaying event scheduled times.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    /**
     * Firebase Firestore instance for data retrieval.
     */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Constructs a new EventAdapter.
     *
     * @param eventList The list of events to display.
     * @param listener  The listener to handle click events.
     */
    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Interface definition for a callback to be invoked when an event item is clicked.
     */
    public interface OnEventClickListener {
        /**
         * Called when an event has been clicked.
         *
         * @param event The Event object associated with the clicked item.
         */
        void onEventClick(Event event);
    }

    /**
     * ViewHolder class for holding and binding event item views.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextViews for various event details.
         */
        private final TextView tvTitle;
        private final TextView tvDate;
        private final TextView tvStatus;
        private final TextView tvCapacity;
        private final TextView tvWaiting;
        private final TextView tvSelected;

        /**
         * Constructs an EventViewHolder.
         *
         * @param itemView The root view of the item layout.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvStatus = itemView.findViewById(R.id.tvEventStatus);
            tvCapacity = itemView.findViewById(R.id.tvCapacityValue);
            tvWaiting = itemView.findViewById(R.id.tvWaitingValue);
            tvSelected = itemView.findViewById(R.id.tvSelectedValue);
        }

        /**
         * Binds event data to the view elements.
         *
         * <p>Note: This method triggers a Firestore query to fetch the current count
         * of entrants in the waiting list sub-collection.</p>
         *
         * @param event    The event data to bind.
         * @param listener The listener for click events.
         */
        public void bind(final Event event, final OnEventClickListener listener) {
            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getScheduledDateTime() != null ? dateFormat.format(event.getScheduledDateTime()) : "Date TBD");

            // 1. Clean Capacity Display: Just the Max Capacity (US 02.01.04)
            tvCapacity.setText(String.valueOf(event.getMaxCapacity()));

            // 2. Fetch and Format Waiting Column: "current / limit" (US 02.03.01)
            db.collection("events").document(event.getEventId())
                    .collection("entrants")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int currentCount = queryDocumentSnapshots.size();
                        if (event.getWaitingListLimit() != null) {
                            // Format: "current / limit"
                            tvWaiting.setText(String.format(Locale.getDefault(), "%d / %d",
                                    currentCount, event.getWaitingListLimit()));
                        } else {
                            // Format: "current" (Unlimited)
                            tvWaiting.setText(String.valueOf(currentCount));
                        }
                    })
                    .addOnFailureListener(e -> tvWaiting.setText("0"));

            tvSelected.setText("0");

            // Dynamic status based on scheduled date
            if (event.getScheduledDateTime() != null && event.getScheduledDateTime().after(new Date())) {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_blue));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.primary_light_blue)));
            } else {
                tvStatus.setText("CLOSED");
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.divider_gray)));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }
}
