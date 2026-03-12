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

/*
 * ProfileAdapter
 * Displays user profiles in the admin browse profiles list.
 */
public class ProfileAdapter extends ArrayAdapter<User> {

    private final Context context;
    private final ArrayList<User> users;

    public ProfileAdapter(Context context, ArrayList<User> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

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