package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    /**
     * Sets up the test environment before each test case.
     * Initializes a mock event list and the adapter instance.
     */
    @Before
    public void setUp() {
        eventList = new ArrayList<>();
        Event event1 = new Event();
        event1.setTitle("Test Event 1");

        Event event2 = new Event();
        event2.setTitle("Test Event 2");

        eventList.add(event1);
        eventList.add(event2);

        adapter = new EntrantEventAdapter(eventList, event -> {
            // No-op click listener for tests
        });
    }

    /**
     * Verifies that the adapter returns the correct number of items based on the provided list.
     */
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * Verifies that the adapter is correctly instantiated and not null.
     */
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
