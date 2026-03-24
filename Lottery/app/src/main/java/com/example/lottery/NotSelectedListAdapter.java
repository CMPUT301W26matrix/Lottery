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
 * Adapter for entrants who were not selected during the lottery draw.
 */
public class NotSelectedListAdapter extends RecyclerView.Adapter<NotSelectedListAdapter.ViewHolder> {
    private final List<EntrantEvent> mData;
    private final Context context;

    NotSelectedListAdapter(Context context, List<EntrantEvent> data) {
        this.context = context;
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EntrantEvent entrant = mData.get(position);
        holder.tvEntrantName.setText(entrant.getUserName());
        holder.tvEntrantStatus.setText("");
        holder.btnViewDetails.setOnClickListener(v -> {
            boolean requireLocation = false;
            String eventId = null;
            if (context instanceof EntrantsListActivity) {
                EntrantsListActivity activity = (EntrantsListActivity) context;
                requireLocation = activity.isRequireLocation();
                eventId = activity.getIntent().getStringExtra("eventId");
            }
            EntrantDetailsFragment entrantDetailsFragment = EntrantDetailsFragment.newInstance(entrant, requireLocation, eventId);
            entrantDetailsFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "Entrant Details");
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntrantName;
        TextView tvEntrantStatus;
        Button btnViewDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tvEntrantName = itemView.findViewById(R.id.tvEntrantName);
            tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
            btnViewDetails = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}
