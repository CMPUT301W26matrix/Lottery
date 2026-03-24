package com.example.lottery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * DialogFragment used by Organizers to input a notification message.
 *
 * <p>This fragment handles the UI for composing a notification. The actual sending
 * logic is handled by the listener (e.g., {@link EntrantsListActivity} or
 * {@link OrganizerNotificationsActivity}) which will create entries in the global
 * 'notifications' collection and individual user 'inbox' subcollections.</p>
 */
public class NotificationFragment extends DialogFragment {

    private NotificationListener listener;

    /**
     * Initializes and returns a new NotificationFragment.
     *
     * @return a new notification fragment
     */
    public static NotificationFragment newInstance() {
        return new NotificationFragment();
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
        } else if (getParentFragment() instanceof NotificationListener) {
            listener = (NotificationListener) getParentFragment();
        } else {
            throw new RuntimeException(context.toString() + " or parent fragment must implement NotificationListener");
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Compose Notification")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (dialog, which) -> {
                    String content = input.getText().toString();
                    if (!content.isEmpty()) {
                        listener.sendNotification(content);
                    }
                })
                .create();
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