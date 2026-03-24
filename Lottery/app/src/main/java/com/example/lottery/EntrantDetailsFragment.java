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

import com.example.lottery.model.Entrant;
import com.example.lottery.model.EntrantEvent;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.lottery.util.FirestorePaths;

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
     * @param entrant the entrant we will display
     * @param requireLocation whether the event requires location
     * @return initialized fragment
     */
    public static EntrantDetailsFragment newInstance(Entrant entrant, boolean requireLocation) {
        return newInstance(entrant, requireLocation, null);
    }

    public static EntrantDetailsFragment newInstance(Entrant entrant, boolean requireLocation, String eventId) {
        EntrantDetailsFragment fragment = new EntrantDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ENTRANT, entrant);
        args.putBoolean(ARG_REQUIRE_LOCATION, requireLocation);
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Overloaded method to support EntrantEvent as well if needed
     */
    public static EntrantDetailsFragment newInstance(EntrantEvent entrantEvent, boolean requireLocation, String eventId) {
        Entrant entrant = new Entrant();
        entrant.setUserId(entrantEvent.getUserId());
        entrant.setUserName(entrantEvent.getUserName());
        entrant.setLocation(entrantEvent.getLocation());
        entrant.setEmail(entrantEvent.getEmail());
        entrant.setStatus(entrantEvent.getStatus());
        
        return newInstance(entrant, requireLocation, eventId);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Entrant entrant = (Entrant) requireArguments().getSerializable(ARG_ENTRANT);
        boolean requireLocation = requireArguments().getBoolean(ARG_REQUIRE_LOCATION, false);
        String eventId = requireArguments().getString(ARG_EVENT_ID);
        
        View view = getLayoutInflater().inflate(R.layout.entrant_details_fragment, null);
        TextView tvName = view.findViewById(R.id.details_name);
        TextView tvEmail = view.findViewById(R.id.details_email);
        TextView tvLocation = view.findViewById(R.id.details_location);
        
        LinearLayout llLocation = view.findViewById(R.id.details_fragment_location);

        tvName.setText(entrant.getUserName() != null ? entrant.getUserName() : "Unknown");
        
        if (entrant.getEmail() != null && !entrant.getEmail().isEmpty()) {
            tvEmail.setText(entrant.getEmail());
        } else if (entrant.getUserId() != null) {
            tvEmail.setText("Loading...");
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

    private void showCancelConfirmation(String eventId, Entrant entrant) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Entrant")
                .setMessage("Are you sure you want to cancel this entrant? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cancelEntrant(eventId, entrant);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelEntrant(String eventId, Entrant entrant) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.eventWaitingList(eventId))
                .document(entrant.getUserId())
                .update(InvitationFlowUtil.buildCancelledEntrantUpdate())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Entrant cancelled successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to cancel entrant", Toast.LENGTH_SHORT).show();
                });
    }
}
