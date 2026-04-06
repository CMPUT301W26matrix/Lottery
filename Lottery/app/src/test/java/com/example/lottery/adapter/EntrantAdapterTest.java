package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;
import com.example.lottery.model.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EntrantAdapterTest {

    private EntrantAdapter adapter;
    private ArrayList<User> entrants;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrants = new ArrayList<>();
        entrants.add(new User("e-1", "Alice Nguyen", "alice.nguyen@gmail.com", "7801112233"));
        entrants.add(new User("e-2", "Bob Martinez", "bob.martinez@gmail.com", "7804445566"));

        adapter = new EntrantAdapter(context, entrants);
    }

    // US 02.06.01: Verify adapter returns correct count for entrants list
    @Test
    public void testGetCount() {
        assertEquals(2, adapter.getCount());
    }

    // US 02.06.01: Verify adapter renders entrant name correctly in list view
    @Test
    public void testGetView() {
        ViewGroup parent = new FrameLayout(context);
        View view = adapter.getView(0, null, parent);

        assertNotNull(view);
        TextView tvName = view.findViewById(R.id.entrantName);
        assertEquals("Alice Nguyen", tvName.getText().toString());
    }
}
