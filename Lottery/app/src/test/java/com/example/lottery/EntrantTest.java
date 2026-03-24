package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

/**
 * Unit tests for the {@link Entrant} data model.
 */
public class EntrantTest {

    /**
     * Verifies the default constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Entrant entrant = new Entrant();
        assertNotNull(entrant);
        assertNull(entrant.getUserId());
    }

    /**
     * Verifies the full constructor and getters.
     */
    @Test
    public void testFullConstructor() {
        String userId = "user123";
        String userName = "John Doe";
        String email = "john@example.com";
        String status = "waitlisted";
        Timestamp registeredAt = Timestamp.now();
        Timestamp waitlistedAt = Timestamp.now();
        Timestamp invitedAt = null;
        Timestamp acceptedAt = null;
        Timestamp cancelledAt = null;
        GeoPoint location = new GeoPoint(45.0, -90.0);

        Entrant entrant = new Entrant(userId, userName, email, status, registeredAt,
                waitlistedAt, invitedAt, acceptedAt, cancelledAt, location);

        assertEquals(userId, entrant.getUserId());
        assertEquals(userName, entrant.getUserName());
        assertEquals(email, entrant.getEmail());
        assertEquals(status, entrant.getStatus());
        assertEquals(registeredAt, entrant.getRegisteredAt());
        assertEquals(waitlistedAt, entrant.getWaitlistedAt());
        assertEquals(invitedAt, entrant.getInvitedAt());
        assertEquals(acceptedAt, entrant.getAcceptedAt());
        assertEquals(cancelledAt, entrant.getCancelledAt());
        assertEquals(location, entrant.getLocation());
        assertTrue(entrant.isWaitlisted());
    }

    /**
     * Verifies setters and individual property updates.
     */
    @Test
    public void testSettersAndGetters() {
        Entrant entrant = new Entrant();

        String userId = "user456";
        entrant.setUserId(userId);
        assertEquals(userId, entrant.getUserId());

        String userName = "Jane Smith";
        entrant.setUserName(userName);
        assertEquals(userName, entrant.getUserName());

        String email = "jane@example.com";
        entrant.setEmail(email);
        assertEquals(email, entrant.getEmail());

        String status = "invited";
        entrant.setStatus(status);
        assertEquals(status, entrant.getStatus());
        assertTrue(entrant.isInvited());

        Timestamp registeredAt = Timestamp.now();
        entrant.setRegisteredAt(registeredAt);
        assertEquals(registeredAt, entrant.getRegisteredAt());

        Timestamp cancelledAt = Timestamp.now();
        entrant.setCancelledAt(cancelledAt);
        assertEquals(cancelledAt, entrant.getCancelledAt());

        GeoPoint location = new GeoPoint(10.0, 20.0);
        entrant.setLocation(location);
        assertEquals(location, entrant.getLocation());
    }
}
