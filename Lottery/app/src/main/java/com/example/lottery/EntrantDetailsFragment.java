package com.example.lottery;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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
    private Entrant entrant;

    /**
     *
     * @param entrant the entrant we will display
     * @return initialized fragment
     */
    public static EntrantDetailsFragment newInstance(Entrant entrant){
        EntrantDetailsFragment entrantDetailsFragment = new EntrantDetailsFragment();
        entrantDetailsFragment.entrant = entrant;
        return entrantDetailsFragment;
    }

    /**
     * set up component for rendering the entrant details fragment
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this is a freshly created Fragment.
     *
     * @return Dialog a built fragment
     */
    @SuppressLint("SetTextI18n")
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.entrant_details_fragment, null);
        TextView tvName = view.findViewById(R.id.details_name);
        TextView tvId = view.findViewById(R.id.details_id);
        TextView tvLocation = view.findViewById(R.id.details_location);
        TextView tvRegistrationTime = view.findViewById(R.id.details_registration_time);
        TextView tvInvitedTime = view.findViewById(R.id.details_invited_time);
        TextView tvCancelledTime = view.findViewById(R.id.details_cancelled_time);
        TextView tvSignedUpTime = view.findViewById(R.id.details_signed_up_time);
        LinearLayout llInvitedTime = view.findViewById(R.id.details_fragment_invited_time);
        LinearLayout llCancelledTime = view.findViewById(R.id.details_fragment_cancelled_time);
        LinearLayout llSignedUpTime = view.findViewById(R.id.details_fragment_signed_up_time);

        tvName.setText(entrant.getEntrant_name());
        tvId.setText(entrant.getEntrant_id());
        tvLocation.setText("("+entrant.getLocation().getLatitude()+","+entrant.getLocation().getLongitude()+")");
        tvRegistrationTime.setText(entrant.getRegistration_time().toDate().toString());
        if(entrant.getInvited_time()!=null){
            tvInvitedTime.setText(entrant.getInvited_time().toDate().toString());
            llInvitedTime.setVisibility(View.VISIBLE);
        }
        if(entrant.getCancelled_time()!=null){
            tvCancelledTime.setText(entrant.getCancelled_time().toDate().toString());
            llCancelledTime.setVisibility(View.VISIBLE);
        }
        if(entrant.getSigned_up_time()!=null){
            tvSignedUpTime.setText(entrant.getSigned_up_time().toDate().toString());
            llSignedUpTime.setVisibility(View.VISIBLE);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok",null).create();
    }
}
