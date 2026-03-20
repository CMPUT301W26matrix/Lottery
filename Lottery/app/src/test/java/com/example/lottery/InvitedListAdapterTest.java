package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

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
    private List<Entrant> entrantList;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrantList = new ArrayList<>();
        
        Entrant e1 = new Entrant();
        e1.setEntrant_name("Alice");
        
        Entrant e2 = new Entrant();
        e2.setEntrant_name("Bob");
        
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
