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
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of events in the Organizer Dashboard.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Binds event metadata to RecyclerView items.</li>
 *   <li>Dynamically fetches and displays counts from the 'waitingList' subcollection.</li>
 *   <li>Visualizes event status (OPEN/CLOSED/CANCELLED) using the document 'status' field.</li>
 * </ul>
 * </p>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final OnEventClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate, tvStatus, tvCapacity, tvWaiting, tvSelected;

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
            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getScheduledDateTime() != null ? dateFormat.format(event.getScheduledDateTime().toDate()) : "Date TBD");

            tvCapacity.setText(String.valueOf(event.getCapacity()));

            // Fetch summary counts from the waitingList subcollection
            db.collection(FirestorePaths.eventWaitingList(event.getEventId()))
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

                        // Display "current / limit" for waiting list
                        if (event.getWaitingListLimit() != null) {
                            tvWaiting.setText(String.format(Locale.getDefault(), "%d / %d",
                                    waitlisted, event.getWaitingListLimit()));
                        } else {
                            tvWaiting.setText(String.valueOf(waitlisted));
                        }

                        tvSelected.setText(String.valueOf(selected));
                    })
                    .addOnFailureListener(e -> {
                        tvWaiting.setText("0");
                        tvSelected.setText("0");
                    });

            // Set visual status based on canonical 'status' field
            updateStatusUI(event.getStatus());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }

        private void updateStatusUI(String status) {
            if (status == null) status = "open";

            switch (status.toLowerCase()) {
                case "open":
                    tvStatus.setText("OPEN");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_blue));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_light_blue)));
                    break;
                case "closed":
                    tvStatus.setText("CLOSED");
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.divider_gray)));
                    break;
                case "cancelled":
                    tvStatus.setText("CANCELLED");
                    tvStatus.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), android.R.color.white)).getDefaultColor());
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark)));
                    break;
                default:
                    tvStatus.setText(status.toUpperCase());
                    break;
            }
        }
    }
}
