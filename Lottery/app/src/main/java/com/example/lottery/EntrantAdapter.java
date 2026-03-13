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

/**
 * Adapter class for connecting a list of entrants (User objects) to a ListView.
 * This adapter is primarily used in the WaitingListActivity to display entrants.
 */
public class EntrantAdapter extends ArrayAdapter<User> {

    /**
     * The current context.
     */
    private final Context context;
    /**
     * The list of entrants to display.
     */
    private final ArrayList<User> entrants;

    /**
     * Constructs a new EntrantAdapter.
     *
     * @param context  The current context.
     * @param entrants The list of entrants to display.
     */
    public EntrantAdapter(Context context, ArrayList<User> entrants) {
        super(context, 0, entrants);
        this.context = context;
        this.entrants = entrants;
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.)
     *
     * @param position    The position in the list of data that should be displayed in the list item view.
     * @param convertView The recycled view to populate.
     * @param parent      The parent ViewGroup that is used for inflation.
     * @return The View for the position in the AdapterView.
     */
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
