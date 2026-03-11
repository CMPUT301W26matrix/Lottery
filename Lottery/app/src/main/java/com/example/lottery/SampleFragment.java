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

public class SampleFragment extends DialogFragment {
    interface SamplingListener {
        void sampling(int size);
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
                .setTitle("City Details")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok", (dialog, which) -> {
                    String size = input.getText().toString();
                    if (!size.isEmpty()) {
                        listener.sampling(Integer.parseInt(size));
                    }
                })
                .create();
    }
}