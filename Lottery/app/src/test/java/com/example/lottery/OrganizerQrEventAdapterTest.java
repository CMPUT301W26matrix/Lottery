package com.example.lottery;

import static org.junit.Assert.assertEquals;

import com.example.lottery.model.Event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the OrganizerQrEventAdapter.
 * Verifies that the adapter correctly reports item counts based on the underlying data list.
 */
public class OrganizerQrEventAdapterTest {

    /**
     * Verifies that the getItemCount method returns the correct size of the event list.
     */
    @Test
    public void testItemCount() {
        List<Event> events = new ArrayList<>();
        events.add(new Event());
        events.add(new Event());
        events.add(new Event());

        OrganizerQrEventAdapter adapter = new OrganizerQrEventAdapter(events, event -> {});

        assertEquals("Adapter item count should match the list size", 3, adapter.getItemCount());
    }

    /**
     * Verifies that the adapter correctly handles an empty list.
     */
    @Test
    public void testEmptyList() {
        List<Event> events = new ArrayList<>();
        OrganizerQrEventAdapter adapter = new OrganizerQrEventAdapter(events, event -> {});

        assertEquals("Adapter item count should be 0 for an empty list", 0, adapter.getItemCount());
    }
}
