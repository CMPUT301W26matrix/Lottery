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

import java.util.List;

/**
 * Adapter for invited entrants RecyclerView to display a list of entrants that have been invited to the specific event
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>bind data to invited entrants recyclerview</li>
 *   <li>render each piece of data</li>
 *   <li>handle user interaction</li>
 * </ul>
 * </p>
 */
public class InvitedListAdapter extends RecyclerView.Adapter<InvitedListAdapter.ViewHolder> {
    /**
     * data we will manipulate to display
     */
    private final List<Entrant> mData;
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
    InvitedListAdapter(Context context, List<Entrant> data) {
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
    public InvitedListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_list_item, parent, false);
        return new InvitedListAdapter.ViewHolder(view);
    }

    /**
     * binds the data to the TextView in each row
     *
     * @param holder   the view we want to set
     * @param position index to get data from the arraylist
     */
    @Override
    public void onBindViewHolder(InvitedListAdapter.ViewHolder holder, int position) {
        Entrant entrant = mData.get(position);
        holder.tvEntrantName.setText(entrant.getEntrant_name());
        holder.tvEntrantStatus.setText("");
        holder.btnViewDetails.setOnClickListener(v -> {
            EntrantDetailsFragment entrantDetailsFragment = EntrantDetailsFragment.newInstance(entrant);
            entrantDetailsFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "Entrant Details");
        });
    }

    /**
     * get total number of rows
     *
     * @return size of mData(rows number)
     */
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

