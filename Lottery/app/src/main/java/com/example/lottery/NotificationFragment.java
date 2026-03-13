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

public class NotificationFragment extends DialogFragment {
    interface NotificationListener {
        void sendNotification(String content);
    }
    private NotificationListener listener;

    public static NotificationFragment newInstance(){
        return new NotificationFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationListener){
            listener = (NotificationListener) context;
        }
        else {
            throw new RuntimeException("Implement listener");
        }
    }

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
}
