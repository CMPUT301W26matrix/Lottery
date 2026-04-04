package com.example.lottery.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.example.lottery.organizer.EntrantsListActivity;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
 *   <li>Provides navigation to the entrants management list.</li>
 * </ul>
 * </p>
 */
public class OrganizerNotificationEventAdapter extends RecyclerView.Adapter<OrganizerNotificationEventAdapter.ViewHolder> {

    private final List<Event> eventList;
    private final OnNotificationGroupClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        // Fetch and display counts for different entrant groups
        loadEntrantCounts(event.getEventId(), holder.tvEventCounts);

        holder.btnViewList.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EntrantsListActivity.class);
            intent.putExtra("eventId", event.getEventId());
            v.getContext().startActivity(intent);
        });

        holder.btnNotifyWaiting.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(event, "waitlisted");
        });

        holder.btnNotifyMore.setOnClickListener(v -> {
            String[] options = {"Notify Invited", "Notify Accepted", "Notify Cancelled"};
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("Select Group to Notify")
                    .setItems(options, (dialog, which) -> {
                        if (listener != null) {
                            switch (which) {
                                case 0: listener.onGroupClick(event, "invited"); break;
                                case 1: listener.onGroupClick(event, "accepted"); break;
                                case 2: listener.onGroupClick(event, "cancelled"); break;
                            }
                        }
                    })
                    .show();
        });
    }

    private void loadEntrantCounts(String eventId, TextView tvCounts) {
        if (eventId == null) return;
        
        db.collection(FirestorePaths.eventWaitingList(eventId))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int waiting = 0, invited = 0, accepted = 0, cancelled = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                        switch (status) {
                            case InvitationFlowUtil.STATUS_WAITLISTED: waiting++; break;
                            case InvitationFlowUtil.STATUS_INVITED: invited++; break;
                            case InvitationFlowUtil.STATUS_ACCEPTED: accepted++; break;
                            case InvitationFlowUtil.STATUS_CANCELLED: cancelled++; break;
                        }
                    }
                    String summary = String.format(Locale.getDefault(),
                            "Waiting %d • Invited %d • Accepted %d • Cancelled %d",
                            waiting, invited, accepted, cancelled);
                    tvCounts.setText(summary);
                })
                .addOnFailureListener(e -> tvCounts.setText("Counts unavailable"));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Listener interface for handling clicks on notification group buttons.
     */
    public interface OnNotificationGroupClickListener {
        /**
         * Called when a specific group (Waitlist, Invited, Accepted, or Cancelled) is clicked for an event.
         *
         * @param event The event associated with the notification.
         * @param group The group identifier (e.g., "waitlisted", "invited", "accepted", "cancelled").
         */
        void onGroupClick(Event event, String group);
    }

    /**
     * ViewHolder class for caching UI component references in each list item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvEventDate, tvEventCounts;
        Button btnNotifyWaiting, btnNotifyMore, btnViewList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventCounts = itemView.findViewById(R.id.tvEventCounts);
            btnNotifyWaiting = itemView.findViewById(R.id.btnNotifyWaiting);
            btnNotifyMore = itemView.findViewById(R.id.btnNotifyMore);
            btnViewList = itemView.findViewById(R.id.btnViewList);
        }
    }
}
