package com.example.lottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
 * RecyclerView adapter used to display events to entrants on the
 * {@link EntrantMainActivity} screen.
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EntrantEventViewHolder> {

    private final List<Event> eventList;
    private final OnEventClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private final String userId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Constructs an EntrantEventAdapter.
     *
     * @param eventList list of events to display
     * @param listener  listener that handles event selection
     * @param userId    current user ID to check waitlist status
     */
    public EntrantEventAdapter(List<Event> eventList, OnEventClickListener listener, String userId) {
        this.eventList = eventList;
        this.listener = listener;
        this.userId = userId;
    }

    @NonNull
    @Override
    public EntrantEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_home, parent, false);
        return new EntrantEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantEventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvEventTitle.setText(event.getTitle());

        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        String details = event.getDetails();
        if (details != null && !details.trim().isEmpty()) {
            holder.tvEventDescription.setVisibility(View.VISIBLE);
            holder.tvEventDescription.setText(details);
        } else {
            holder.tvEventDescription.setVisibility(View.GONE);
        }

        // Handle Quick Action Button (Waitlist Join/Leave and Status display)
        updateWaitlistButton(holder, event);

        View.OnClickListener detailClickListener = v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        };

        holder.itemView.setOnClickListener(detailClickListener);
        holder.btnViewDetail.setOnClickListener(detailClickListener);
    }

    private void updateWaitlistButton(EntrantEventViewHolder holder, Event event) {
        if (userId == null) {
            holder.btnWaitlistAction.setVisibility(View.GONE);
            return;
        }

        // Reset button state
        holder.btnWaitlistAction.setEnabled(true);
        holder.btnWaitlistAction.setAlpha(1.0f);
        holder.btnWaitlistAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));

        db.collection(FirestorePaths.eventWaitingList(event.getEventId()))
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        
                        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
                            holder.btnWaitlistAction.setText(R.string.leave_waitlist);
                            holder.btnWaitlistAction.setVisibility(View.VISIBLE);
                            holder.btnWaitlistAction.setOnClickListener(v -> leaveWaitlist(event, holder));
                        } else if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
                            holder.btnWaitlistAction.setText("Selected");
                            holder.btnWaitlistAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                            holder.btnWaitlistAction.setOnClickListener(v -> openEventDetails(event));
                        } else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                            holder.btnWaitlistAction.setText("Confirmed");
                            holder.btnWaitlistAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                            holder.btnWaitlistAction.setOnClickListener(v -> openEventDetails(event));
                        } else if (InvitationFlowUtil.STATUS_CANCELLED.equals(status)) {
                            holder.btnWaitlistAction.setText("Declined");
                            holder.btnWaitlistAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_gray));
                            holder.btnWaitlistAction.setOnClickListener(v -> openEventDetails(event));
                        } else if (InvitationFlowUtil.STATUS_NOT_SELECTED.equals(status)) {
                            holder.btnWaitlistAction.setText("Not Selected");
                            holder.btnWaitlistAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_gray));
                            holder.btnWaitlistAction.setOnClickListener(v -> openEventDetails(event));
                        } else {
                            holder.btnWaitlistAction.setVisibility(View.GONE);
                        }
                    } else {
                        // Not in list at all - check if registration is still open
                        if (isRegistrationOpen(event)) {
                            holder.btnWaitlistAction.setText(R.string.join_waitlist);
                            holder.btnWaitlistAction.setVisibility(View.VISIBLE);
                            holder.btnWaitlistAction.setOnClickListener(v -> joinWaitlist(event, holder));
                        } else {
                            holder.btnWaitlistAction.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> holder.btnWaitlistAction.setVisibility(View.GONE));
    }

    private boolean isRegistrationOpen(Event event) {
        if (event.getRegistrationDeadline() == null) return true;
        return event.getRegistrationDeadline().toDate().after(new java.util.Date());
    }

    private void joinWaitlist(Event event, EntrantEventViewHolder holder) {
        if (listener != null) {
            Toast.makeText(holder.itemView.getContext(), R.string.opening_details_to_join, Toast.LENGTH_SHORT).show();
            listener.onEventClick(event);
        }
    }

    private void leaveWaitlist(Event event, EntrantEventViewHolder holder) {
        db.collection(FirestorePaths.eventWaitingList(event.getEventId()))
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(holder.itemView.getContext(), R.string.left_waitlist, Toast.LENGTH_SHORT).show();
                    // Update specific item UI instead of full list reload if possible
                    updateWaitlistButton(holder, event);
                });
    }

    private void openEventDetails(Event event) {
        if (listener != null) {
            listener.onEventClick(event);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    static class EntrantEventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvEventDate;
        private final TextView tvEventDescription;
        private final Button btnViewDetail;
        private final Button btnWaitlistAction;

        EntrantEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            btnWaitlistAction = itemView.findViewById(R.id.btnWaitlistAction);
        }
    }
}
