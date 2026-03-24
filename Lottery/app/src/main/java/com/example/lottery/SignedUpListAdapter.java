package com.example.lottery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.EntrantEvent;

import java.util.List;

/**
 * Adapter for signed up entrants RecyclerView to display a list of entrants that signed up the specific event
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>bind data to signed up entrants recyclerview</li>
 *   <li>render each piece of data </li>
 *   <li>handle user interaction</li>
 *   <li>implement US 02.06.01 Be able to view all chosen entrants</li>
 * </ul>
 * </p>
 */
// Source - https://stackoverflow.com/a/40584425
// Posted by Suragch, modified by community. See post 'Timeline' for change history
// Retrieved 2026-03-10, License - CC BY-SA 4.0
public class SignedUpListAdapter extends RecyclerView.Adapter<SignedUpListAdapter.ViewHolder> {

    /**
     * data we will manipulate to display
     */
    private final List<EntrantEvent> mData;
    /**
     * context we want to interact
     */
    private final Context context;

    /**
     * data is passed into the constructor
     *
     * @param context context we want to interact
     * @param data    data we will manipulate to display
     */
    SignedUpListAdapter(Context context, List<EntrantEvent> data) {
        this.context = context;
        this.mData = data;
    }

    /**
     * inflates the row layout from xml when needed
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_list_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * get total number of rows
     *
     * @return size of mData(rows number)
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        EntrantEvent entrant = mData.get(position);
        // Unified: use getUserName() instead of getEntrant_name()
        holder.tvEntrantName.setText(entrant.getUserName());
        holder.tvEntrantStatus.setText("");
        holder.btnViewDetails.setOnClickListener(v -> {
            boolean requireLocation = false;
            if (context instanceof EntrantsListActivity) {
                requireLocation = ((EntrantsListActivity) context).isRequireLocation();
            }
            EntrantDetailsFragment entrantDetailsFragment = EntrantDetailsFragment.newInstance(entrant, requireLocation);
            entrantDetailsFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "Entrant Details");
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * stores and recycles views as they are scrolled off screen
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntrantName;
        TextView tvEntrantStatus;
        Button btnViewDetails;

        /**
         * initialize ViewHolder
         *
         * @param itemView the view we want to stores and recycles
         */
        ViewHolder(View itemView) {
            super(itemView);
            tvEntrantName = itemView.findViewById(R.id.tvEntrantName);
            tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
            btnViewDetails = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}
