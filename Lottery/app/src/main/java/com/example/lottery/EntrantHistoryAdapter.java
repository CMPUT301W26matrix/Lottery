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
import com.example.lottery.util.InvitationFlowUtil;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying the entrant's event registration history.
 *
 * <p>This adapter also supports entrant Accessibility Mode by increasing
 * event-card text sizes when the setting is enabled.</p>
 */
public class EntrantHistoryAdapter extends RecyclerView.Adapter<EntrantHistoryAdapter.ViewHolder> {

    /** SharedPreferences file used across the app. */
    private static final String PREFS_NAME = "AppPrefs";

    /** Preference key for entrant accessibility mode. */
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    /**
     * Model representing a single history entry.
     */
    public static class HistoryItem {
        public Event event;
        public String status;
        public String organizerName;

        /**
         * Creates a history item.
         *
         * @param event event data
         * @param status entrant status for that event
         */
        public HistoryItem(Event event, String status) {
            this.event = event;
            this.status = status;
        }
    }

    private final List<HistoryItem> historyItems;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    /**
     * Click listener for history items.
     */
    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    /**
     * Creates the history adapter.
     *
     * @param historyItems list of history items to display
     * @param listener click listener for opening event details
     */
    public EntrantHistoryAdapter(List<HistoryItem> historyItems, OnItemClickListener listener) {
        this.historyItems = historyItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        Event event = item.event;

        Context context = holder.itemView.getContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAccessibilityEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);

        float titleSize = isAccessibilityEnabled ? 22f : 16f;
        float dateSize = isAccessibilityEnabled ? 17f : 14f;
        float organizerSize = isAccessibilityEnabled ? 15f : 12f;
        float bodySize = isAccessibilityEnabled ? 18f : 14f;
        float buttonSize = isAccessibilityEnabled ? 18f : 14f;
        float statusSize = isAccessibilityEnabled ? 13f : 11f;

        holder.tvTitle.setText(event.getTitle());
        holder.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);

        if (event.getScheduledDateTime() != null) {
            holder.tvDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
        } else {
            holder.tvDate.setText("Date TBD");
        }
        holder.tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize);

        if (item.organizerName != null && !item.organizerName.isEmpty()) {
            holder.tvOrganizer.setVisibility(View.VISIBLE);
            holder.tvOrganizer.setText("By: " + item.organizerName);
            holder.tvOrganizer.setTextSize(TypedValue.COMPLEX_UNIT_SP, organizerSize);
        } else {
            holder.tvOrganizer.setVisibility(View.GONE);
        }

        holder.tvDescription.setText(event.getDetails());
        holder.tvDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodySize);

        holder.tvStatus.setVisibility(View.VISIBLE);
        String status = InvitationFlowUtil.normalizeEntrantStatus(item.status);

        String displayStatus = status.toUpperCase();
        if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
            displayStatus = "SELECTED";
        }
        if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
            displayStatus = "CONFIRMED";
        }
        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
            displayStatus = "WAITING LIST";
        }

        holder.tvStatus.setText(displayStatus);
        holder.tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, statusSize);

        if ("SELECTED".equals(displayStatus)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge);
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onItemClick(event);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.btnViewDetails.setOnClickListener(clickListener);
        holder.btnViewDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonSize);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    /**
     * ViewHolder for history cards.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDate;
        TextView tvDescription;
        TextView tvStatus;
        TextView tvOrganizer;
        TextView btnViewDetails;

        /**
         * Creates the ViewHolder and binds card views.
         *
         * @param view item view
         */
        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvEventTitle);
            tvDate = view.findViewById(R.id.tvEventDate);
            tvOrganizer = view.findViewById(R.id.tvRegistrationPeriod);
            tvDescription = view.findViewById(R.id.tvEventDescription);
            tvStatus = view.findViewById(R.id.tvEventStatus);
            btnViewDetails = view.findViewById(R.id.btnViewDetails);
        }
    }
}