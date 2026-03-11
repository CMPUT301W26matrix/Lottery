package com.example.lottery;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lottery.model.User;

import java.util.ArrayList;

/*
 * EntrantAdapter
 * This adapter connects the list of entrants (User objects)
 * to the ListView in the WaitingListActivity.
 * It controls how each row (item_entrant.xml) is displayed.
 */
public class EntrantAdapter extends ArrayAdapter<User> {

    private Context context;
    private ArrayList<User> entrants;

    // Constructor: receives the list of entrants
    public EntrantAdapter(Context context, ArrayList<User> entrants) {
        super(context, 0, entrants);
        this.context = context;
        this.entrants = entrants;
    }

    // getView() runs for every row in the ListView
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Get the entrant at this position
        User entrant = getItem(position);

        // Inflate the layout if it doesn't exist yet
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_entrant, parent, false);
        }

        // Find views inside item_entrant.xml
        TextView entrantName = convertView.findViewById(R.id.entrantName);
        Button viewDetailsButton = convertView.findViewById(R.id.viewDetailsButton);

        // Set the entrant name
        if (entrant != null) {
            entrantName.setText(entrant.getName());

            // When "View Details" button is clicked
            viewDetailsButton.setOnClickListener(v -> {

                // Build a string with entrant information
                String details =
                        "Name: " + entrant.getName() + "\n" +
                                "Email: " + entrant.getEmail() + "\n" +
                                "Phone: " + entrant.getPhoneNumber();

                // Show entrant information in a popup dialog
                new AlertDialog.Builder(context)
                        .setTitle("Entrant Details")
                        .setMessage(details)
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        return convertView;
    }
}