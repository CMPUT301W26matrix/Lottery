package com.example.lottery;

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
 * Unit tests for the {@link CancelledListAdapter} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CancelledListAdapterTest {

    private CancelledListAdapter adapter;
    private List<EntrantEvent> entrantList;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrantList = new ArrayList<>();

        EntrantEvent e = new EntrantEvent();
        e.setUserName("Cancelled User");
        entrantList.add(e);

        adapter = new CancelledListAdapter(context, entrantList);
    }

    /**
     * Verifies that the adapter returns the correct number of items.
     */
    @Test
    public void testItemCount() {
        assertEquals(1, adapter.getItemCount());
    }

    /**
     * Verifies that the adapter is correctly instantiated.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
