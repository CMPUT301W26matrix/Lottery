package com.example.lottery.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottery.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    static final String ARG_DEFAULT_SIZE = "defaultSize";
    private SamplingListener listener;

    /**
     * initialize a new fragment
     *
     * @return initialized SampleFragment
     */
    public static SampleFragment newInstance() {
        return new SampleFragment();
    }

    /**
     * Creates a new fragment with a pre-filled default sample size.
     *
     * @param defaultSize the maximum number of entrants that can be sampled
     * @return configured SampleFragment
     */
    public static SampleFragment newInstance(long defaultSize) {
        SampleFragment fragment = new SampleFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DEFAULT_SIZE, defaultSize);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * check whether implemented the communication channel or not
     *
     * @param context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SamplingListener) {
            listener = (SamplingListener) context;
        } else {
            throw new RuntimeException("Implement listener");
        }
    }

    /**
     * assemble the fragment
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     *                           get text from user
     * @return a built fragment which can prompt user to enter a number for sampling
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.sample_fragment, null);
        EditText input = view.findViewById(R.id.input_sampling_size);

        if (getArguments() != null && getArguments().containsKey(ARG_DEFAULT_SIZE)) {
            long defaultSize = getArguments().getLong(ARG_DEFAULT_SIZE);
            input.setText(String.valueOf(defaultSize));
            input.selectAll();
        }

        return new MaterialAlertDialogBuilder(requireContext())
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

    /**
     * interface for the activity who want to use the sample fragment to implement, like a communication channel
     */
    public interface SamplingListener {
        void sampling(String size);
    }
}
