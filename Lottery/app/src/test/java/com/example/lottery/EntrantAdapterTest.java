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
public class EntrantAdapterTest {

    private EntrantAdapter adapter;
    private ArrayList<User> entrants;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrants = new ArrayList<>();
        entrants.add(new User("Alice", "alice@example.com", "1112223333"));
        entrants.add(new User("Bob", "bob@example.com", "4445556666"));
        
        adapter = new EntrantAdapter(context, entrants);
    }

    @Test
    public void testGetCount() {
        assertEquals(2, adapter.getCount());
    }

    @Test
    public void testGetView() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(0, null, parent);
        
        assertNotNull(view);
        TextView tvName = view.findViewById(R.id.entrantName);
        assertEquals("Alice", tvName.getText().toString());
    }
}
