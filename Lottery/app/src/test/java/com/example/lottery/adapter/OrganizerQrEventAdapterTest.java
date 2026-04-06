package com.example.lottery.adapter;

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

    // US 02.01.01: Verify adapter returns correct item count for organizer QR event list
    /**
     * Verifies that the getItemCount method returns the correct size of the event list.
     */
    @Test
    public void testItemCount() {
        List<Event> events = new ArrayList<>();
        Event annual5kRun = new Event();
        annual5kRun.setTitle("Annual Charity 5K Run");
        events.add(annual5kRun);
        Event soccerCamp = new Event();
        soccerCamp.setTitle("Kids Soccer Camp");
        events.add(soccerCamp);
        Event potteryWorkshop = new Event();
        potteryWorkshop.setTitle("Weekend Pottery Workshop");
        events.add(potteryWorkshop);

        OrganizerQrEventAdapter adapter = new OrganizerQrEventAdapter(events, event -> {
        });

        assertEquals("Adapter item count should match the list size", 3, adapter.getItemCount());
    }

    // US 02.01.01: Verify adapter handles an empty QR event list correctly
    /**
     * Verifies that the adapter correctly handles an empty list.
     */
    @Test
    public void testEmptyList() {
        List<Event> events = new ArrayList<>();
        OrganizerQrEventAdapter adapter = new OrganizerQrEventAdapter(events, event -> {
        });

        assertEquals("Adapter item count should be 0 for an empty list", 0, adapter.getItemCount());
    }
}
