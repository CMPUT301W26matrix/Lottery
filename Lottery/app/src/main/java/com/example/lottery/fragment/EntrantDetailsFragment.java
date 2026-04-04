package com.example.lottery.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottery.R;
import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * fragment for showing details of an entrant in an event
 */
public class EntrantDetailsFragment extends DialogFragment {
    private static final String ARG_ENTRANT = "entrant";
    private static final String ARG_REQUIRE_LOCATION = "requireLocation";
    private static final String ARG_EVENT_ID = "eventId";

    public static EntrantDetailsFragment newInstance(EntrantEvent entrant, boolean requireLocation) {
        return newInstance(entrant, requireLocation, null);
    }

    public static EntrantDetailsFragment newInstance(EntrantEvent entrant, boolean requireLocation, String eventId) {
        EntrantDetailsFragment fragment = new EntrantDetailsFragment();
        Bundle args = new Bundle();
        args.putBundle(ARG_ENTRANT, entrant.toBundle());
        args.putBoolean(ARG_REQUIRE_LOCATION, requireLocation);
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Ensure the dialog window itself is transparent so our layout's rounded corners are visible
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.entrant_details_fragment, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EntrantEvent entrant = EntrantEvent.fromBundle(requireArguments().getBundle(ARG_ENTRANT));
        boolean requireLocation = requireArguments().getBoolean(ARG_REQUIRE_LOCATION, false);
        String eventId = requireArguments().getString(ARG_EVENT_ID);

        TextView tvName = view.findViewById(R.id.details_name);
        TextView tvEmail = view.findViewById(R.id.details_email);
        TextView tvLocation = view.findViewById(R.id.details_location);
        LinearLayout llLocation = view.findViewById(R.id.details_fragment_location);
        View btnClose = view.findViewById(R.id.btn_close);
        View btnCancel = view.findViewById(R.id.btn_cancel_entrant);

        tvName.setText(entrant.getUserName() != null ? entrant.getUserName() : "Unknown");

        // Email Loading Logic
        if (entrant.getEmail() != null && !entrant.getEmail().isEmpty()) {
            tvEmail.setText(entrant.getEmail());
        } else if (entrant.getUserId() != null) {
            tvEmail.setText("Loading...");
            FirebaseFirestore.getInstance().collection(FirestorePaths.USERS)
                    .document(entrant.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded()) return;
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("email");
                            tvEmail.setText(email != null ? email : "N/A");
                        } else {
                            tvEmail.setText("N/A");
                        }
                    });
        } else {
            tvEmail.setText("N/A");
        }

        // Location Logic
        if (requireLocation) {
            llLocation.setVisibility(View.VISIBLE);
            if (entrant.getLocation() != null) {
                tvLocation.setText("(" + entrant.getLocation().getLatitude() + "," + entrant.getLocation().getLongitude() + ")");
            } else {
                tvLocation.setText("N/A");
            }
        }

        // Cancel Button Logic (US 02.06.04)
        String status = InvitationFlowUtil.normalizeEntrantStatus(entrant.getStatus());
        if (eventId != null && InvitationFlowUtil.STATUS_INVITED.equals(status)) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> showCancelConfirmation(eventId, entrant));
        }

        btnClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showCancelConfirmation(String eventId, EntrantEvent entrant) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Entrant")
                .setMessage("Are you sure you want to cancel this entrant? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelEntrant(eventId, entrant))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelEntrant(String eventId, EntrantEvent entrant) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.eventWaitingList(eventId))
                .document(entrant.getUserId())
                .update(InvitationFlowUtil.buildCancelledEntrantUpdate())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Entrant cancelled successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to cancel entrant", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
