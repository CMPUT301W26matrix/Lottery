package com.example.lottery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lottery.R;
import com.example.lottery.model.User;

import java.util.ArrayList;

/**
 * Adapter for displaying a list of entrants (Users) in a ListView.
 * Used primarily in WaitingListActivity.
 */
public class EntrantAdapter extends ArrayAdapter<User> {

    private final Context context;
    private final ArrayList<User> entrants;

    public EntrantAdapter(@NonNull Context context, ArrayList<User> entrants) {
        super(context, 0, entrants);
        this.context = context;
        this.entrants = entrants;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_entrant, parent, false);
        }

        User currentUser = entrants.get(position);

        TextView name = listItem.findViewById(R.id.entrantName);
        if (currentUser != null) {
            // Fixed: use getUsername() only as per specification
            name.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "Unknown");
        }

        return listItem;
    }
}
