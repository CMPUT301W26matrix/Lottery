package com.example.lottery;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 *
 * <p>This adapter also supports Accessibility Mode by increasing event-card
 * text sizes when enabled from the entrant profile.</p>
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EntrantEventViewHolder> {

    /** SharedPreferences file name used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Key used to store accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    private final List<Event> eventList;
    private final OnEventClickListener listener;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

    /**
     * Constructs an EntrantEventAdapter.
     *
     * @param eventList list of events to display
     * @param listener listener that handles event selection
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

        Context context = holder.itemView.getContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAccessibilityEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);

        float titleSize = isAccessibilityEnabled ? 22f : 16f;
        float dateSize = isAccessibilityEnabled ? 17f : 14f;
        float bodySize = isAccessibilityEnabled ? 18f : 14f;
        float buttonSize = isAccessibilityEnabled ? 18f : 14f;

        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);

        if (event.getScheduledDateTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }
        holder.tvEventDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize);

        holder.tvEventDescription.setText(event.getDetails());
        holder.tvEventDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodySize);

        holder.btnViewDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonSize);

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

    /**
     * ViewHolder for entrant event items.
     */
    static class EntrantEventViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvEventTitle;
        private final TextView tvEventDate;
        private final TextView tvEventDescription;
        private final TextView btnViewDetails;

        /**
         * Constructs a ViewHolder and binds UI components.
         *
         * @param itemView the event item view
         */
        EntrantEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}