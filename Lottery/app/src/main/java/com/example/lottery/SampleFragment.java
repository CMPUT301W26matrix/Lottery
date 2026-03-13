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
import java.util.Objects;
/**
 * fragment for user to enter the number of invitations they want to send
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>ask user to input a number and then call sampling in the EntrantsListActivity to sample entrants</li>
 *   <li>implement US 02.05.02 Be able to sample number of attendees to register for the event</li>
 *   <li>Keep the custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class SampleFragment extends DialogFragment {
    interface SamplingListener {
        void sampling(String size);
    }
    private SamplingListener listener;

    public static SampleFragment newInstance(){
        return new SampleFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SamplingListener){
            listener = (SamplingListener) context;
        }
        else {
            throw new RuntimeException("Implement listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.sample_fragment, null);
        EditText input = view.findViewById(R.id.input_sampling_size);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok", (dialog, which) -> {
                    String size = input.getText().toString();
                    if (!size.isEmpty()) {
                        listener.sampling(size);
                    }
                })
                .create();
    }
}