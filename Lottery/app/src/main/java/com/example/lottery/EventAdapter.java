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
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for displaying a list of events in the Organizer Dashboard.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Binds event metadata to RecyclerView items.</li>
 *   <li>Fetches and caches waitingList counts per event (one query per eventId).</li>
 *   <li>Visualizes event status (OPEN/CLOSED/PENDING) derived from event dates.</li>
 * </ul>
 * </p>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final OnEventClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, int[]> countsCache = new HashMap<>();

    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Derives display status from event dates per the project spec.
     */
    static String resolveDisplayStatus(Event event) {
        if (event == null) return "closed";

        Date now = new Date();

        if (event.getRegistrationDeadline() != null
                && event.getDrawDate() != null
                && event.getRegistrationDeadline().toDate().before(now)
                && event.getDrawDate().toDate().after(now)) {
            return "pending";
        }

        if (event.getScheduledDateTime() != null
                && event.getScheduledDateTime().toDate().after(now)) {
            return "open";
        }

        return "closed";
    }

    /**
     * Clears the cached waitingList counts so the next bind triggers a fresh fetch.
     */
    public void clearCountsCache() {
        countsCache.clear();
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

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate, tvStatus, tvCapacity, tvWaiting, tvSelected;
        private String boundEventId;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvStatus = itemView.findViewById(R.id.tvEventStatus);
            tvCapacity = itemView.findViewById(R.id.tvCapacityValue);
            tvWaiting = itemView.findViewById(R.id.tvWaitingValue);
            tvSelected = itemView.findViewById(R.id.tvSelectedValue);
        }

        public void bind(final Event event, final OnEventClickListener listener) {
            String eventId = event.getEventId();
            boundEventId = eventId;

            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getScheduledDateTime() != null ? dateFormat.format(event.getScheduledDateTime().toDate()) : "Date TBD");
            tvCapacity.setText(String.valueOf(event.getCapacity()));

            // Bind counts from cache or fetch once
            if (countsCache.containsKey(eventId)) {
                bindCounts(event, countsCache.get(eventId));
            } else {
                tvWaiting.setText("-");
                tvSelected.setText("-");
                fetchCounts(event);
            }

            updateStatusUI(resolveDisplayStatus(event));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }

        private void fetchCounts(Event event) {
            String eventId = event.getEventId();
            db.collection(FirestorePaths.eventWaitingList(eventId))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int waitlisted = 0;
                        int selected = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                            if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
                                waitlisted++;
                            } else if (InvitationFlowUtil.STATUS_INVITED.equals(status)
                                    || InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                                selected++;
                            }
                        }

                        int[] counts = {waitlisted, selected};
                        countsCache.put(eventId, counts);

                        // Only write UI if this holder still shows the same event
                        if (eventId.equals(boundEventId)) {
                            bindCounts(event, counts);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (eventId.equals(boundEventId)) {
                            tvWaiting.setText("0");
                            tvSelected.setText("0");
                        }
                    });
        }

        private void bindCounts(Event event, int[] counts) {
            int waitlisted = counts[0];
            int selected = counts[1];

            if (event.getWaitingListLimit() != null) {
                tvWaiting.setText(String.format(Locale.getDefault(), "%d / %d",
                        waitlisted, event.getWaitingListLimit()));
            } else {
                tvWaiting.setText(String.valueOf(waitlisted));
            }
            tvSelected.setText(String.valueOf(selected));
        }

        private void updateStatusUI(String status) {
            if (status == null) status = "closed";

            switch (status) {
                case "open":
                    tvStatus.setText("OPEN");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_blue));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_light_blue)));
                    break;
                case "pending":
                    tvStatus.setText("PENDING");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF3E0));
                    break;
                case "closed":
                    tvStatus.setText("CLOSED");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.divider_gray)));
                    break;
                default:
                    tvStatus.setText(status.toUpperCase());
                    break;
            }
        }
    }
}
