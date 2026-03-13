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
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EntrantEventViewHolder> {

    private final List<Event> eventList;
    private final OnEventClickListener listener;
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

    @NonNull
    @Override
    public EntrantEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_entrant, parent, false);
        return new EntrantEventViewHolder(view);
    }

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

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        };

        holder.btnViewDetails.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Listener interface used to notify when an event item is clicked.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    static class EntrantEventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvEventDate;
        private final TextView tvEventDescription;
        private final Button btnViewDetails;

        EntrantEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
