package com.example.lottery.model;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for the {@link Event} model class.
 *
 * <p>This class focuses on verifying the data integrity and behavior of the Event object,
 * especially concerning the waiting list limit feature.</p>
 *
 * <p>Satisfies testing requirements for:
 * US 02.03.01: Store and retrieve waiting list capacity.
 * </p>
 */
public class EventTest {

    /**
     * Verifies that the {@link Event#setWaitingListLimit(Integer)} and
     * {@link Event#getWaitingListLimit()} methods correctly handle both
     * specific integer values and the 'null' state for an unlimited list.
     */
    @Test
    public void testWaitingListLimitSetterGetter() {
        Event event = new Event();

        // Test setting a specific limit (US 02.03.01)
        event.setWaitingListLimit(50);
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());

        // Test setting null (Unlimited state)
        event.setWaitingListLimit(null);
        assertNull(event.getWaitingListLimit());
    }

    /**
     * Verifies that the {@link Event} constructor correctly initializes the
     * {@code waitingListLimit} field when a value is provided.
     */
    @Test
    public void testEventConstructorWithLimit() {
        // Verify constructor correctly maps the waiting list limit
        Event event = new Event("1", "Title", null, null, 10, "Details", null, null, "Org1", false, 100);
        assertEquals(Integer.valueOf(100), event.getWaitingListLimit());
    }
}