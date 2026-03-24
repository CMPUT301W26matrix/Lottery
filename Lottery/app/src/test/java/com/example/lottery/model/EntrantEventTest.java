package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

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
     * Verifies that the parameterized constructor correctly initializes properties.
     */
    @Test
    public void testParameterizedConstructor() {
        String userId = "user123";
        String userName = "John Doe";
        String email = "john@example.com";
        String status = "waitlisted";
        Timestamp now = Timestamp.now();
        GeoPoint location = new GeoPoint(45.0, -90.0);

        EntrantEvent entrantEvent = new EntrantEvent(
                userId, userName, email, status, now, now, null, null, null, location
        );

        assertEquals(userId, entrantEvent.getUserId());
        assertEquals(userName, entrantEvent.getUserName());
        assertEquals(email, entrantEvent.getEmail());
        assertEquals(status, entrantEvent.getStatus());
        assertEquals(now, entrantEvent.getRegisteredAt());
        assertEquals(now, entrantEvent.getWaitlistedAt());
        assertEquals(location, entrantEvent.getLocation());
    }

    /**
     * Verifies that setters and getters work as intended.
     */
    @Test
    public void testSettersAndGetters() {
        EntrantEvent entrantEvent = new EntrantEvent();

        String status = "invited";
        entrantEvent.setStatus(status);
        assertEquals(status, entrantEvent.getStatus());

        Timestamp invitedAt = Timestamp.now();
        entrantEvent.setInvitedAt(invitedAt);
        assertEquals(invitedAt, entrantEvent.getInvitedAt());

        GeoPoint location = new GeoPoint(10.0, 20.0);
        entrantEvent.setLocation(location);
        assertEquals(location, entrantEvent.getLocation());
    }
}
