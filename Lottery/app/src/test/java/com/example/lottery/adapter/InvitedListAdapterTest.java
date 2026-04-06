package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.model.EntrantEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link InvitedListAdapter} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InvitedListAdapterTest {

    private InvitedListAdapter adapter;
    private List<EntrantEvent> entrantList;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrantList = new ArrayList<>();

        EntrantEvent e1 = new EntrantEvent();
        e1.setUserName("Alice Nguyen");

        EntrantEvent e2 = new EntrantEvent();
        e2.setUserName("Bob Martinez");

        entrantList.add(e1);
        entrantList.add(e2);

        adapter = new InvitedListAdapter(context, entrantList);
    }

    // US 02.06.01: Verify adapter returns correct item count for invited entrants list
    /**
     * Verifies that the adapter returns the correct number of items.
     */
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 02.06.01: Verify invited list adapter is correctly instantiated
    /**
     * Verifies that the adapter is correctly instantiated.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
