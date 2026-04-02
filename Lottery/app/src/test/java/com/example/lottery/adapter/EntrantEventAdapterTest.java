package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;
import com.example.lottery.model.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link EntrantEventAdapter} class.
 *
 * <p>Validates the basic functionality of the adapter used for the entrant's event list,
 * including item count consistency and successful instantiation.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EntrantEventAdapterTest {

    /**
     * The adapter instance under test.
     */
    private EntrantEventAdapter adapter;

    /**
     * Mock list of events to be used in the adapter.
     */
    private List<Event> eventList;
    private String userId;
    private Context context;

    /**
     * Sets up the test environment before each test case.
     * Initializes a mock event list and the adapter instance.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);
        eventList = new ArrayList<>();
        Event event1 = new Event();
        event1.setTitle("Test Event 1");

        Event event2 = new Event();
        event2.setTitle("Test Event 2");

        eventList.add(event1);
        eventList.add(event2);

        userId = "entrant_c1ijFznXQYSp_Cev3cUuNs";

        adapter = new EntrantEventAdapter(eventList, event -> {
            // No-op click listener for tests
        }, "test-user-id");
    }

    // US 01.02.03: Verify adapter returns correct item count for entrant event list
    /**
     * Verifies that the adapter returns the correct number of items based on the provided list.
     */
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 01.02.03: Verify entrant event adapter is correctly instantiated
    /**
     * Verifies that the adapter is correctly instantiated and not null.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }

    // US 01.02.03: Verify event item layout contains a View Detail button for navigation
    /**
     * Verifies that the item_event_explore layout contains a View Detail button.
     */
    @Test
    public void testViewDetailButtonExists() {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_event_explore, null);
        View btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
        assertNotNull("btnViewDetail should exist in item_event_explore layout", btnViewDetail);
    }

    // US 01.02.03: Verify clicking an event item triggers the event click listener
    /**
     * Verifies that the whole card is still clickable via itemView click listener.
     */
    @Test
    public void testItemViewClickable() {
        final boolean[] clicked = {false};
        EntrantEventAdapter clickAdapter = new EntrantEventAdapter(eventList, event -> clicked[0] = true, "test-user-id");

        EntrantEventAdapter.EntrantEventViewHolder holder =
                clickAdapter.onCreateViewHolder(
                        new android.widget.FrameLayout(context), 0);
        clickAdapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();
        assertEquals("Clicking itemView should trigger event click", true, clicked[0]);
    }
}
