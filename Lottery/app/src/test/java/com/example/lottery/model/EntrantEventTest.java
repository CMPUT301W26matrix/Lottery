package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for the {@link EntrantEvent} model class.
 * 
 * <p>Ensures that the entrant-event relationship object correctly manages
 * IDs, statuses, and timestamps used for tracking participation.</p>
 */
public class EntrantEventTest {

    /**
     * Verifies that the default constructor creates a non-null object for Firestore deserialization.
     */
    @Test
    public void testDefaultConstructor() {
        EntrantEvent entrantEvent = new EntrantEvent();
        assertNotNull(entrantEvent);
    }

    /**
     * Verifies that the parameterized constructor correctly initializes 
     * the entrant ID, event ID, and the composite relation ID.
     */
    @Test
    public void testParameterizedConstructor() {
        String entrantId = "entrant123";
        String eventId = "event456";
        EntrantEvent entrantEvent = new EntrantEvent(entrantId, eventId);

        assertEquals(entrantId, entrantEvent.getEntrantId());
        assertEquals(eventId, entrantEvent.getEventId());
        assertEquals(entrantId + "_" + eventId, entrantEvent.getRelationId());
        assertEquals(EntrantEvent.Status.WAITLISTED, entrantEvent.getStatus());
        assertNotNull(entrantEvent.getJoinedAt());
    }

    /**
     * Verifies that setters and getters for status, timestamps, and position work as intended.
     */
    @Test
    public void testSettersAndGetters() {
        EntrantEvent entrantEvent = new EntrantEvent();
        
        entrantEvent.setStatus(EntrantEvent.Status.INVITED);
        assertEquals(EntrantEvent.Status.INVITED, entrantEvent.getStatus());

        Timestamp invitedAt = Timestamp.now();
        entrantEvent.setInvitedAt(invitedAt);
        assertEquals(invitedAt, entrantEvent.getInvitedAt());

        entrantEvent.setWaitlistPosition(5);
        assertEquals(5, entrantEvent.getWaitlistPosition());

        entrantEvent.setNotificationSent(true);
        assertTrue(entrantEvent.isNotificationSent());
    }
}
