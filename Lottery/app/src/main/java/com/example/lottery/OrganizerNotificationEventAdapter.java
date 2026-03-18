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
 */
public class OrganizerNotificationEventAdapter extends RecyclerView.Adapter<OrganizerNotificationEventAdapter.ViewHolder> {

    private final List<Event> eventList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvEventTitle.setText(event.getTitle());
        
        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime()));
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

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvEventDate;
        Button btnNotifyWaiting, btnNotifySelected, btnNotifyCancelled;

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
