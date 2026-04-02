package com.example.lottery.adapter;

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
 * Unit tests for the {@link OrganizerNotificationEventAdapter} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OrganizerNotificationEventAdapterTest {

    private OrganizerNotificationEventAdapter adapter;
    private List<Event> eventList;

    @Before
    public void setUp() {
        eventList = new ArrayList<>();
        Event event1 = new Event();
        event1.setTitle("Event 1");
        Event event2 = new Event();
        event2.setTitle("Event 2");
        eventList.add(event1);
        eventList.add(event2);
        adapter = new OrganizerNotificationEventAdapter(eventList, (event, group) -> {
        });
    }

    // US 02.07.01: Verify adapter returns correct item count for organizer notification event list
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 02.07.01: Verify organizer notification event adapter is correctly instantiated
    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
