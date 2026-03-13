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
 * fragment for organizer sent notification to specific group of entrants(depending on status like waitedlisted signedup invited cancelled)
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>ask organizer to input a sequence of words and then call sendNotificaiton in the EntrantsListActivity to send notification</li>
 *   <li>not be implemented yet since no one takes it</li>
 * </ul>
 * </p>
 */

/**
 *
 */
public class NotificationFragment extends DialogFragment {
    private NotificationListener listener;

    /**
     * initialize and return a new notification fragment
     *
     * @return a new notification fragment
     */
    public static NotificationFragment newInstance() {
        return new NotificationFragment();
    }

    /**
     * check if the caller implemented the communication protocol
     *
     * @param context caller's context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationListener) {
            listener = (NotificationListener) context;
        } else {
            throw new RuntimeException("Implement listener");
        }
    }

    /**
     * set up the dialog
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return a built dialog which can sent the notification content to caller by the protocol
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.notification_fragment, null);
        EditText input = view.findViewById(R.id.input_sampling_size);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("")
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
     * a protocol interface for caller to implement
     */
    interface NotificationListener {
        void sendNotification(String content);
    }
}
