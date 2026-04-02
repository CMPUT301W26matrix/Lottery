package com.example.lottery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays organizer notification logs in the admin log browser.
 */
public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogViewHolder> {

    private final List<Map<String, Object>> logList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> organizerNameCache = new HashMap<>();

    /**
     * @param logList raw Firestore document data for each notification log
     */
    public NotificationLogAdapter(List<Map<String, Object>> logList) {
        this.logList = logList;
    }

    /**
     * Creates a new ViewHolder for a log item.
     *
     * @param parent   the parent view group
     * @param viewType the view type
     * @return a new {@link LogViewHolder}
     */
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    /**
     * Binds log data to the given ViewHolder.
     *
     * @param holder   the ViewHolder to bind
     * @param position the position in the list
     */
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(logList.get(position));
    }

    /**
     * Returns the total number of log items.
     *
     * @return size of the log list
     */
    @Override
    public int getItemCount() {
        return logList.size();
    }

    /**
     * ViewHolder for a single notification log entry.
     * Resolves the organizer name asynchronously from Firestore.
     */
    class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvOrganizer;
        private final TextView tvMessage;
        private final TextView tvGroup;
        private final TextView tvRecipientCount;
        private final TextView tvTimestamp;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvLogEventTitle);
            tvOrganizer = itemView.findViewById(R.id.tvLogOrganizer);
            tvMessage = itemView.findViewById(R.id.tvLogMessage);
            tvGroup = itemView.findViewById(R.id.tvLogGroup);
            tvRecipientCount = itemView.findViewById(R.id.tvLogRecipientCount);
            tvTimestamp = itemView.findViewById(R.id.tvLogTimestamp);
        }

        /**
         * Populates views from a log entry. Guards against stale
         * Firestore callbacks on recycled ViewHolders.
         *
         * @param log raw Firestore document data
         */
        void bind(Map<String, Object> log) {
            String eventTitle = (String) log.get("eventTitle");
            tvEventTitle.setText(eventTitle != null ? eventTitle
                    : itemView.getContext().getString(R.string.admin_unknown_event));

            String group = (String) log.get("group");
            if (group != null && !group.isEmpty()) {
                tvGroup.setText(group.toUpperCase(Locale.getDefault()));
                tvGroup.setVisibility(View.VISIBLE);
            } else {
                tvGroup.setVisibility(View.GONE);
            }

            String message = (String) log.get("message");
            tvMessage.setText(message != null ? message : "");

            // Firestore may store the count as Long or Double, so safely coerce to long
            Object countObj = log.get("recipientCount");
            long recipientCount = countObj instanceof Number ? ((Number) countObj).longValue() : 0;
            tvRecipientCount.setText(String.valueOf(recipientCount));

            Object createdAtObj = log.get("createdAt");
            if (createdAtObj instanceof Timestamp) {
                tvTimestamp.setText(dateFormat.format(((Timestamp) createdAtObj).toDate()));
            } else {
                tvTimestamp.setText(itemView.getContext().getString(R.string.log_no_timestamp));
            }

            // Show senderId as placeholder, then resolve the display name
            String senderId = (String) log.get("senderId");
            if (senderId != null) {
                // Use cached name if available
                if (organizerNameCache.containsKey(senderId)) {
                    tvOrganizer.setText(itemView.getContext().getString(
                            R.string.admin_organizer_label, organizerNameCache.get(senderId)));
                } else {
                    tvOrganizer.setText(itemView.getContext().getString(
                            R.string.admin_organizer_label, senderId));
                    int bindPosition = getBindingAdapterPosition();
                    if (bindPosition == RecyclerView.NO_POSITION) return;
                    db.collection(FirestorePaths.USERS).document(senderId).get()
                            .addOnSuccessListener(doc -> {
                                int currentPos = getBindingAdapterPosition();
                                if (currentPos == RecyclerView.NO_POSITION
                                        || currentPos != bindPosition) return;
                                String name = doc.getString("username");
                                String displayName = name != null ? name : senderId;
                                organizerNameCache.put(senderId, displayName);
                                tvOrganizer.setText(itemView.getContext().getString(
                                        R.string.admin_organizer_label, displayName));
                            });
                }
            } else {
                tvOrganizer.setText(itemView.getContext().getString(R.string.admin_unknown_organizer));
            }
        }
    }
}
