package com.example.lottery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for waited listed entrants RecyclerView to display a list of entrants that joined the specific event but not invited
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>bind data to waited listed entrants recyclerview</li>
 *   <li>render each piece of data </li>
 *   <li>handle user interaction</li>
 * </ul>
 * </p>
 */
public class WaitedListedListAdapter extends RecyclerView.Adapter<WaitedListedListAdapter.ViewHolder> {
    /**
     * data we will manipulate to display
     */
    private List<Entrant> mData;
    /**
     * context we want to interact
     */
    private Context context;
    /**
     * method for handling user click
     */
    private ItemClickListener mClickListener;

    /**
     * data is passed into the constructor
     * @param context context we want to interact
     * @param data data we will manipulate to display
     */
    WaitedListedListAdapter(Context context, List<Entrant> data) {
        this.context = context;
        this.mData = data;
    }

    /**
     * inflates the row layout from xml when needed
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
     * binds the data to the TextView in each row
     * @param holder the view we want to set
     * @param position index to get data from the arraylist
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Entrant entrant = mData.get(position);
        holder.tvEntrantName.setText(entrant.getEntrant_name());
        holder.tvEntrantStatus.setText("");
    }

    /**
     * get total number of rows
     * @return size of mData(rows number)
     */
    @Override
    public int getItemCount() {
        return mData.size();
    }


    /**
     * stores and recycles views as they are scrolled off screen
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvEntrantName;
        TextView tvEntrantStatus;

        /**
         * initialize ViewHolder
         * @param itemView the view we want to stores and recycles
         */
        ViewHolder(View itemView) {
            super(itemView);
            tvEntrantName = itemView.findViewById(R.id.tvEntrantName);
            tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
            itemView.setOnClickListener(this);
        }

        /**
         * implemented onClick for the view
         * @param view The view that was clicked.
         */
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    /**
     * convenience method for getting data at click position
     * @param id
     * @return data in the row
     */
    Entrant getItem(int id) {
        return mData.get(id);
    }

    /**
     * allows clicks events to be caught
     * @param itemClickListener the click listener we want to bind to the adapter
     */
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    /**
     *  parent activity will implement this method to respond to click events
     */
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

