package com.example.lottery;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * fragment for showing details of an entrant in an event
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>display each attribute of entrant</li>
 *   <li>implement US 02.06.01 Be able to view all chosen entrants</li>
 *   <li>implement US 02.06.04 Be able to cancel entrants that did not sign up</li>
 * </ul>
 * </p>
 */
public class EntrantDetailsFragment extends DialogFragment {
    private static final String ARG_ENTRANT = "entrant";
    private static final String ARG_REQUIRE_LOCATION = "requireLocation";
    private static final String ARG_EVENT_ID = "eventId";

    /**
     *
     * @param entrant         the entrant we will display
     * @param requireLocation whether the event requires location
     * @return initialized fragment
     */
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

    @SuppressLint("SetTextI18n")
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        EntrantEvent entrant = EntrantEvent.fromBundle(requireArguments().getBundle(ARG_ENTRANT));
        boolean requireLocation = requireArguments().getBoolean(ARG_REQUIRE_LOCATION, false);
        String eventId = requireArguments().getString(ARG_EVENT_ID);

        View view = getLayoutInflater().inflate(R.layout.entrant_details_fragment, null);
        TextView tvName = view.findViewById(R.id.details_name);
        TextView tvEmail = view.findViewById(R.id.details_email);
        TextView tvLocation = view.findViewById(R.id.details_location);

        LinearLayout llLocation = view.findViewById(R.id.details_fragment_location);

        // Unified: use getUserName()
        tvName.setText(entrant.getUserName() != null ? entrant.getUserName() : "Unknown");

        // Try to get email from EntrantEvent object
        if (entrant.getEmail() != null && !entrant.getEmail().isEmpty()) {
            tvEmail.setText(entrant.getEmail());
        } else if (entrant.getUserId() != null) {
            tvEmail.setText("Loading...");
            // Fetch email from users collection if not present in EntrantEvent
            FirebaseFirestore.getInstance().collection(FirestorePaths.USERS)
                    .document(entrant.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("email");
                            if (email != null) {
                                tvEmail.setText(email);
                                entrant.setEmail(email);
                            } else {
                                tvEmail.setText("N/A");
                            }
                        } else {
                            tvEmail.setText("N/A");
                        }
                    });
        } else {
            tvEmail.setText("N/A");
        }

        if (requireLocation) {
            llLocation.setVisibility(View.VISIBLE);
            if (entrant.getLocation() != null) {
                tvLocation.setText("(" + entrant.getLocation().getLatitude() + "," + entrant.getLocation().getLongitude() + ")");
            } else {
                tvLocation.setText("N/A");
            }
        } else {
            llLocation.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view)
                .setPositiveButton("Ok", null);

        // US 02.06.04: Adjusting visibility to match strict story scope.
        // The cancel action applies specifically to 'chosen' (invited/pending) entrants
        // who have not yet signed up (accepted).
        String status = InvitationFlowUtil.normalizeEntrantStatus(entrant.getStatus());
        boolean canCancel = eventId != null && InvitationFlowUtil.STATUS_INVITED.equals(status);

        if (canCancel) {
            builder.setNeutralButton("Cancel Entrant", (dialog, which) -> {
                showCancelConfirmation(eventId, entrant);
            });
        }

        return builder.create();
    }

    private void showCancelConfirmation(String eventId, EntrantEvent entrant) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Entrant")
                .setMessage("Are you sure you want to cancel this entrant? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cancelEntrant(eventId, entrant);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelEntrant(String eventId, EntrantEvent entrant) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.eventWaitingList(eventId))
                .document(entrant.getUserId())
                .update(InvitationFlowUtil.buildCancelledEntrantUpdate())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Entrant cancelled successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to cancel entrant", Toast.LENGTH_SHORT).show();
                });
    }
}
