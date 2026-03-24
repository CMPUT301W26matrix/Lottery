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
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying organizer event cards in the dashboard.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Bind event title, date, status, and capacity to each card</li>
 *     <li>Load the current waiting count from Firestore</li>
 *     <li>Display ACTIVE / CLOSED state based on scheduled date</li>
 *     <li>Handle click events for opening organizer event details</li>
 * </ul>
 *
 * <p>Firestore source of truth for waiting count:</p>
 * <pre>
 * events/{eventId}/entrant_events/{userId}
 * </pre>
 *
 * <p>Waiting entrants are counted using:</p>
 * <pre>
 * status = InvitationFlowUtil.STATUS_WAITING
 * </pre>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /**
     * List of events displayed in the organizer dashboard.
     */
    private final List<Event> eventList;

    /**
     * Click listener for event cards.
     */
    private final OnEventClickListener listener;

    /**
     * Formatter for displaying scheduled event date/time.
     */
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

    /**
     * Firestore instance used for loading waiting counts.
     */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creates a new adapter.
     *
     * @param eventList list of events to display
     * @param listener  click listener for event cards
     */
    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Inflates a single event card row.
     *
     * @param parent   parent view group
     * @param viewType row view type
     * @return inflated view holder
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data to the row.
     *
     * @param holder   row holder
     * @param position row position
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    /**
     * Returns number of event cards.
     *
     * @return event count
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Callback for organizer event card click.
     */
    public interface OnEventClickListener {

        /**
         * Called when an event card is tapped.
         *
         * @param event selected event
         */
        void onEventClick(Event event);
    }

    /**
     * ViewHolder for organizer event card.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        /**
         * Event title text view.
         */
        private final TextView tvTitle;

        /**
         * Event date text view.
         */
        private final TextView tvDate;

        /**
         * Event status badge text view.
         */
        private final TextView tvStatus;

        /**
         * Capacity value text view.
         */
        private final TextView tvCapacity;

        /**
         * Waiting count text view.
         */
        private final TextView tvWaiting;

        /**
         * Selected count text view.
         */
        private final TextView tvSelected;

        /**
         * Creates holder and binds row views.
         *
         * @param itemView inflated row view
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
         * Binds one event card.
         *
         * @param event    event data
         * @param listener click listener
         */
        public void bind(final Event event, final OnEventClickListener listener) {
            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getScheduledDateTime() != null
                    ? dateFormat.format(event.getScheduledDateTime())
                    : "Date TBD");

            tvCapacity.setText(String.valueOf(event.getMaxCapacity()));

            loadWaitingCount(event);

            tvSelected.setText("0");

            if (event.getScheduledDateTime() != null
                    && event.getScheduledDateTime().after(new Date())) {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.primary_blue
                ));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.primary_light_blue)
                ));
            } else {
                tvStatus.setText("CLOSED");
                tvStatus.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.text_secondary
                ));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.divider_gray)
                ));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }

        /**
         * Loads waiting count for an event from entrant_events.
         *
         * @param event event whose waiting count should be displayed
         */
        private void loadWaitingCount(Event event) {
            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                tvWaiting.setText("0");
                return;
            }

            db.collection("events")
                    .document(event.getEventId())
                    .collection("entrant_events")
                    .whereEqualTo("status", InvitationFlowUtil.STATUS_WAITING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int currentCount = queryDocumentSnapshots.size();

                        if (event.getWaitingListLimit() != null) {
                            tvWaiting.setText(String.format(
                                    Locale.getDefault(),
                                    "%d / %d",
                                    currentCount,
                                    event.getWaitingListLimit()
                            ));
                        } else {
                            tvWaiting.setText(String.valueOf(currentCount));
                        }
                    })
                    .addOnFailureListener(e -> tvWaiting.setText("0"));
        }
    }
}