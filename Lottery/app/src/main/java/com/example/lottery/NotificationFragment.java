package com.example.lottery;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * DialogFragment used by Organizers to input a notification message.
 *
 * <p>This fragment handles the UI for composing a notification. The actual sending
 * logic is handled by the listener (e.g., {@link EntrantsListActivity} or
 * {@link OrganizerNotificationsActivity}) which will create entries in the global
 * 'notifications' collection and individual user 'inbox' subcollections.</p>
 */
public class NotificationFragment extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private NotificationListener listener;

    /**
     * Initializes and returns a new NotificationFragment with a default title.
     *
     * @return a new notification fragment
     */
    public static NotificationFragment newInstance() {
        return newInstance("Compose Notification");
    }

    /**
     * Initializes and returns a new NotificationFragment with a specific title.
     *
     * @param title The title to display on the dialog.
     * @return a new notification fragment
     */
    public static NotificationFragment newInstance(String title) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Attaches the fragment to its context and ensures it implements the {@link NotificationListener}.
     *
     * @param context The context to which the fragment is being attached.
     * @throws RuntimeException if the context or parent fragment does not implement {@link NotificationListener}.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationListener) {
            listener = (NotificationListener) context;
        } else {
            if (getParentFragment() instanceof NotificationListener) {
                listener = (NotificationListener) getParentFragment();
            } else {
                throw new RuntimeException(context + " or parent fragment must implement NotificationListener");
            }
        }
    }

    /**
     * Creates and returns the notification composition dialog.
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return A built {@link AlertDialog} for notification input.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.notification_fragment, null);
        EditText input = view.findViewById(R.id.etNotificationContent);
        TextView tvMainTitle = view.findViewById(R.id.tvDialogMainTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvDialogSubtitle);

        if (getArguments() != null) {
            String fullTitle = getArguments().getString(ARG_TITLE, "Compose Notification");
            if (fullTitle.contains("\n")) {
                String[] parts = fullTitle.split("\n", 2);
                tvMainTitle.setText(parts[0]);
                tvSubtitle.setText(parts[1]);
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvMainTitle.setText(fullTitle);
                tvSubtitle.setVisibility(View.GONE);
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (d, which) -> {
                    String content = input.getText().toString();
                    if (!content.isEmpty()) {
                        listener.sendNotification(content);
                    }
                })
                .create();

        // Final UI Polish for buttons
        dialog.setOnShowListener(d -> {
            Button sendBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (sendBtn != null) {
                sendBtn.setTypeface(null, Typeface.BOLD);
                sendBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
            }
            
            Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (cancelBtn != null) {
                // Set to a normal text color instead of faded text_secondary
                cancelBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        });

        return dialog;
    }

    /**
     * Interface for components that handle the actual notification sending logic.
     */
    public interface NotificationListener {
        /**
         * Called when the organizer clicks "Send" with the provided message content.
         *
         * @param content the notification message body
         */
        void sendNotification(String content);
    }
}
