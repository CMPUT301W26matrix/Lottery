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

        // Handle Quick Action Button (Waitlist Join/Leave)
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

        // Fetch user status for this specific event
        db.collection(FirestorePaths.eventWaitingList(event.getEventId()))
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
                            holder.btnWaitlistAction.setText("Leave Waitlist");
                            holder.btnWaitlistAction.setVisibility(View.VISIBLE);
                            holder.btnWaitlistAction.setOnClickListener(v -> leaveWaitlist(event, holder));
                        } else {
                            // If in any other state (invited, accepted, cancelled), hide quick action
                            // and force user to use the Detail page for safety.
                            holder.btnWaitlistAction.setVisibility(View.GONE);
                        }
                    } else {
                        // Not in list at all - check if registration is still open
                        if (isRegistrationOpen(event)) {
                            holder.btnWaitlistAction.setText("Join Waitlist");
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
        // Since joining requires location check and other validations, 
        // we delegate to the Detail page to ensure consistent behavior.
        if (listener != null) {
            Toast.makeText(holder.itemView.getContext(), "Opening details to join...", Toast.LENGTH_SHORT).show();
            listener.onEventClick(event);
        }
    }

    private void leaveWaitlist(Event event, EntrantEventViewHolder holder) {
        db.collection(FirestorePaths.eventWaitingList(event.getEventId()))
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(holder.itemView.getContext(), "Left waitlist", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                });
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
