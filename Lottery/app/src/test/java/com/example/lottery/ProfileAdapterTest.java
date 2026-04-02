package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.adapter.ProfileAdapter;
import com.example.lottery.model.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ProfileAdapterTest {

    private ProfileAdapter adapter;
    private ArrayList<User> users;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        users = new ArrayList<>();
        users.add(new User("u-1", "John Doe", "john@example.com", "1234567890"));
        users.add(new User("u-2", "Jane Smith", "jane@example.com", ""));
        users.add(new User("u-3", "No Phone", "none@example.com", null));

        adapter = new ProfileAdapter(context, users);
    }

    // US 03.05.01: Profile list should report the correct number of user profiles
    @Test
    public void testGetCount() {
        assertEquals("Adapter should have 3 items", 3, adapter.getCount());
    }

    // US 03.05.01: Profile list should return the correct user at each position
    @Test
    public void testGetItem() {
        assertEquals("First item name should match", "John Doe", adapter.getItem(0).getUsername());
        assertEquals("Second item email should match", "jane@example.com", adapter.getItem(1).getEmail());
    }

    // US 03.05.01: Profile adapter should map item IDs to positions
    @Test
    public void testGetItemId() {
        assertEquals("Item ID should match position", 0, adapter.getItemId(0));
    }

    // US 03.05.01: Profile list item should display name, email, and phone
    @Test
    public void testGetViewPopulatesData() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(0, null, parent);

        assertNotNull("View should not be null", view);

        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);

        assertEquals("Name should be John Doe", "John Doe", tvName.getText().toString());
        assertEquals("Email should be john@example.com", "john@example.com", tvEmail.getText().toString());
        assertEquals("Phone should be 1234567890", "1234567890", tvPhone.getText().toString());
    }

    // US 03.05.01: Profile list item should show placeholder for empty phone
    @Test
    public void testGetViewWithEmptyPhone() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(1, null, parent);

        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);
        assertEquals("Should display placeholder for empty phone", "No phone number", tvPhone.getText().toString());
    }

    // US 03.05.01: Profile list item should show placeholder for null phone
    @Test
    public void testGetViewWithNullPhone() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(2, null, parent);

        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);
        assertEquals("Should display placeholder for null phone", "No phone number", tvPhone.getText().toString());
    }
}
