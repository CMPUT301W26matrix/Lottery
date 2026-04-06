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
 * Unit tests for the {@link SignedUpListAdapter} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SignedUpListAdapterTest {

    private SignedUpListAdapter adapter;
    private List<EntrantEvent> entrantList;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        entrantList = new ArrayList<>();

        EntrantEvent e = new EntrantEvent();
        e.setUserName("Sarah Thompson");
        entrantList.add(e);

        adapter = new SignedUpListAdapter(context, entrantList);
    }

    // US 02.06.03: Verify adapter returns correct item count for signed-up entrants list
    /**
     * Verifies that the adapter returns the correct number of items.
     */
    @Test
    public void testItemCount() {
        assertEquals(1, adapter.getItemCount());
    }

    // US 02.06.03: Verify signed-up list adapter is correctly instantiated
    /**
     * Verifies that the adapter is correctly instantiated.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
