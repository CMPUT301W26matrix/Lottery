package com.example.lottery;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottery.model.Entrant;
import com.example.lottery.model.EntrantEvent;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.lottery.util.FirestorePaths;

/**
 * fragment for showing details of an entrant in an event
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>display each attribute of entrant</li>
 *   <li>implement US 02.06.01 Be able to view all chosen entrants</li>
 *   <li>it's a general template which can display info of entrant in 4 different status</li>
 * </ul>
 * </p>
 */
public class EntrantDetailsFragment extends DialogFragment {
    private static final String ARG_ENTRANT = "entrant";
    private static final String ARG_REQUIRE_LOCATION = "requireLocation";

    /**
     *
     * @param entrant the entrant we will display
     * @param requireLocation whether the event requires location
     * @return initialized fragment
     */
    public static EntrantDetailsFragment newInstance(Entrant entrant, boolean requireLocation) {
        EntrantDetailsFragment fragment = new EntrantDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ENTRANT, entrant);
        args.putBoolean(ARG_REQUIRE_LOCATION, requireLocation);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Overloaded method to support EntrantEvent as well if needed, 
     * but internally we should probably unify on Entrant model for details view.
     */
    public static EntrantDetailsFragment newInstance(EntrantEvent entrantEvent, boolean requireLocation) {
        Entrant entrant = new Entrant();
        entrant.setEntrant_id(entrantEvent.getUserId());
        entrant.setEntrant_name(entrantEvent.getUserName());
        entrant.setLocation(entrantEvent.getLocation());
        // Map other fields as necessary
        return newInstance(entrant, requireLocation);
    }

    /**
     * set up component for rendering the entrant details fragment
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return Dialog a built fragment
     */
    @SuppressLint("SetTextI18n")
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Entrant entrant = (Entrant) requireArguments().getSerializable(ARG_ENTRANT);
        boolean requireLocation = requireArguments().getBoolean(ARG_REQUIRE_LOCATION, false);
        
        View view = getLayoutInflater().inflate(R.layout.entrant_details_fragment, null);
        TextView tvName = view.findViewById(R.id.details_name);
        TextView tvEmail = view.findViewById(R.id.details_email);
        TextView tvLocation = view.findViewById(R.id.details_location);
        
        LinearLayout llLocation = view.findViewById(R.id.details_fragment_location);

        tvName.setText(entrant.getEntrant_name());
        
        // Try to get email from Entrant object
        if (entrant.getEmail() != null && !entrant.getEmail().isEmpty()) {
            tvEmail.setText(entrant.getEmail());
        } else {
            tvEmail.setText("Loading...");
            // Fetch email from users collection if not present in Entrant
            FirebaseFirestore.getInstance().collection(FirestorePaths.USERS)
                    .document(entrant.getEntrant_id())
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
        return builder
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok", null).create();
    }
}
