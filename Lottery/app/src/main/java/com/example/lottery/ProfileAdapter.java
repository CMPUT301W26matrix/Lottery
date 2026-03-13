package com.example.lottery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lottery.model.User;

import java.util.ArrayList;

/**
 * Adapter class for displaying user profiles in a list.
 * This adapter is primarily used by administrators to browse user profiles.
 */
public class ProfileAdapter extends ArrayAdapter<User> {

    /**
     * The current context.
     */
    private final Context context;
    /**
     * The list of users to display.
     */
    private final ArrayList<User> users;

    /**
     * Creates an adapter for displaying a list of users.
     *
     * @param context the context used to inflate views
     * @param users   the users to display
     */
    public ProfileAdapter(Context context, ArrayList<User> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    /**
     * Returns a view for the user at the given position.
     *
     * @param position    the position of the user in the list
     * @param convertView the recycled view to reuse if available
     * @param parent      the parent view group
     * @return the view displaying the user information
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        User user = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false);
        }

        TextView tvProfileName = convertView.findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = convertView.findViewById(R.id.tvProfileEmail);
        TextView tvProfilePhone = convertView.findViewById(R.id.tvProfilePhone);

        if (user != null) {
            tvProfileName.setText(user.getName());
            tvProfileEmail.setText(user.getEmail());

            String phone = user.getPhoneNumber();
            if (phone == null || phone.isEmpty()) {
                tvProfilePhone.setText("No phone number");
            } else {
                tvProfilePhone.setText(phone);
            }
        }

        return convertView;
    }
}
