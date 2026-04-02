package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.adapter.InvitedListAdapter;
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
        e1.setUserName("Alice");

        EntrantEvent e2 = new EntrantEvent();
        e2.setUserName("Bob");

        entrantList.add(e1);
        entrantList.add(e2);

        adapter = new InvitedListAdapter(context, entrantList);
    }

    /**
     * Verifies that the adapter returns the correct number of items.
     */
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * Verifies that the adapter is correctly instantiated.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
