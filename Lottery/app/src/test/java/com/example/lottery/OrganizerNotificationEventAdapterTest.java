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
        adapter = new OrganizerNotificationEventAdapter(eventList);
    }

    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testAdapterNotNull() {
        assertNotNull(adapter);
    }
}
