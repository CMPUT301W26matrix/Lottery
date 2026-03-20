package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        assertNull(entrant.getEntrant_id());
    }

    /**
     * Verifies the wait-listed constructor and getters.
     */
    @Test
    public void testWaitListedConstructor() {
        String name = "John Doe";
        String id = "user123";
        Timestamp regTime = Timestamp.now();
        GeoPoint location = new GeoPoint(45.0, -90.0);

        Entrant entrant = new Entrant(name, id, regTime, location);

        assertEquals(name, entrant.getEntrant_name());
        assertEquals(id, entrant.getEntrant_id());
        assertEquals(regTime, entrant.getRegistration_time());
        assertEquals(location, entrant.getLocation());
    }

    /**
     * Verifies the invited constructor and getters.
     */
    @Test
    public void testInvitedConstructor() {
        Timestamp invitedTime = Timestamp.now();
        String name = "Jane Smith";
        String id = "user456";
        Timestamp regTime = Timestamp.now();
        GeoPoint location = new GeoPoint(50.0, -100.0);

        Entrant entrant = new Entrant(invitedTime, name, id, regTime, location);

        assertEquals(invitedTime, entrant.getInvited_time());
        assertEquals(name, entrant.getEntrant_name());
        assertEquals(id, entrant.getEntrant_id());
        assertEquals(regTime, entrant.getRegistration_time());
        assertEquals(location, entrant.getLocation());
    }

    /**
     * Verifies setters and individual property updates.
     */
    @Test
    public void testSettersAndGetters() {
        Entrant entrant = new Entrant();

        String name = "New Name";
        entrant.setEntrant_name(name);
        assertEquals(name, entrant.getEntrant_name());

        Timestamp cancelledTime = Timestamp.now();
        entrant.setCancelled_time(cancelledTime);
        assertEquals(cancelledTime, entrant.getCancelled_time());

        Timestamp signedUpTime = Timestamp.now();
        entrant.setSigned_up_time(signedUpTime);
        assertEquals(signedUpTime, entrant.getSigned_up_time());

        GeoPoint location = new GeoPoint(10.0, 20.0);
        entrant.setLocation(location);
        assertEquals(location, entrant.getLocation());
    }
}
