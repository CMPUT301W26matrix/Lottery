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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of events in the Organizer Dashboard.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

    /**
     * Interface for handling event item clicks.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

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

    class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvDate, tvStatus, tvCapacity, tvWaiting, tvSelected;

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
            tvDate.setText(event.getScheduledDateTime() != null ? dateFormat.format(event.getScheduledDateTime()) : "Date TBD");
            tvCapacity.setText(String.valueOf(event.getMaxCapacity()));
            
            // Placeholder values for lottery counts
            tvWaiting.setText("0");
            tvSelected.setText("0");
            
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
