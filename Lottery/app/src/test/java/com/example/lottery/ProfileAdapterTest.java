package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

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
        users.add(new User("John Doe", "john@example.com", "1234567890"));
        users.add(new User("Jane Smith", "jane@example.com", ""));
        users.add(new User("No Phone", "none@example.com", null));
        
        adapter = new ProfileAdapter(context, users);
    }

    @Test
    public void testGetCount() {
        assertEquals("Adapter should have 3 items", 3, adapter.getCount());
    }

    @Test
    public void testGetItem() {
        assertEquals("First item name should match", "John Doe", adapter.getItem(0).getName());
        assertEquals("Second item email should match", "jane@example.com", adapter.getItem(1).getEmail());
    }

    @Test
    public void testGetItemId() {
        assertEquals("Item ID should match position", 0, adapter.getItemId(0));
    }

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

    @Test
    public void testGetViewWithEmptyPhone() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(1, null, parent);
        
        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);
        assertEquals("Should display placeholder for empty phone", "No phone number", tvPhone.getText().toString());
    }

    @Test
    public void testGetViewWithNullPhone() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(2, null, parent);
        
        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);
        assertEquals("Should display placeholder for null phone", "No phone number", tvPhone.getText().toString());
    }
}
