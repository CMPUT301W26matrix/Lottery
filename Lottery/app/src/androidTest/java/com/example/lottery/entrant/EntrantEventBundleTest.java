package com.example.lottery.entrant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.model.EntrantEvent;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link EntrantEvent} Bundle serialization.
 * These require Android runtime because Bundle is an Android framework class.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventBundleTest {

    // toBundle packs all string fields
    @Test
    public void toBundle_packsStringFields() {
        EntrantEvent ee = new EntrantEvent();
        ee.setUserId("uid-1");
        ee.setUserName("Alice");
        ee.setEmail("alice@test.com");
        ee.setStatus("invited");

        Bundle b = ee.toBundle();
        assertEquals("uid-1", b.getString("userId"));
        assertEquals("Alice", b.getString("userName"));
        assertEquals("alice@test.com", b.getString("email"));
        assertEquals("invited", b.getString("status"));
    }

    // toBundle packs timestamp fields as longs
    @Test
    public void toBundle_packsTimestamps() {
        EntrantEvent ee = new EntrantEvent();
        Timestamp now = Timestamp.now();
        ee.setRegisteredAt(now);
        ee.setWaitlistedAt(now);
        ee.setInvitedAt(now);
        ee.setAcceptedAt(now);
        ee.setCancelledAt(now);

        Bundle b = ee.toBundle();
        assertEquals(now.toDate().getTime(), b.getLong("registeredAt"));
        assertEquals(now.toDate().getTime(), b.getLong("waitlistedAt"));
        assertEquals(now.toDate().getTime(), b.getLong("invitedAt"));
        assertEquals(now.toDate().getTime(), b.getLong("acceptedAt"));
        assertEquals(now.toDate().getTime(), b.getLong("cancelledAt"));
    }

    // toBundle packs GeoPoint location
    @Test
    public void toBundle_packsLocation() {
        EntrantEvent ee = new EntrantEvent();
        GeoPoint gp = new GeoPoint(53.5461, -113.4938);
        ee.setLocation(gp);

        Bundle b = ee.toBundle();
        assertTrue(b.getBoolean("hasLocation"));
        assertEquals(53.5461, b.getDouble("lat"), 0.0001);
        assertEquals(-113.4938, b.getDouble("lng"), 0.0001);
    }

    // toBundle omits null timestamps and location
    @Test
    public void toBundle_omitsNullFields() {
        EntrantEvent ee = new EntrantEvent();
        ee.setUserId("uid-1");

        Bundle b = ee.toBundle();
        assertFalse(b.containsKey("registeredAt"));
        assertFalse(b.containsKey("invitedAt"));
        assertFalse(b.containsKey("hasLocation"));
    }

    // fromBundle reconstructs string fields
    @Test
    public void fromBundle_reconstructsStringFields() {
        Bundle b = new Bundle();
        b.putString("userId", "uid-2");
        b.putString("userName", "Bob");
        b.putString("email", "bob@test.com");
        b.putString("status", "accepted");

        EntrantEvent ee = EntrantEvent.fromBundle(b);
        assertEquals("uid-2", ee.getUserId());
        assertEquals("Bob", ee.getUserName());
        assertEquals("bob@test.com", ee.getEmail());
        assertEquals("accepted", ee.getStatus());
    }

    // fromBundle reconstructs timestamps from longs
    @Test
    public void fromBundle_reconstructsTimestamps() {
        Timestamp now = Timestamp.now();
        long millis = now.toDate().getTime();

        Bundle b = new Bundle();
        b.putLong("registeredAt", millis);
        b.putLong("invitedAt", millis);

        EntrantEvent ee = EntrantEvent.fromBundle(b);
        assertNotNull(ee.getRegisteredAt());
        assertNotNull(ee.getInvitedAt());
        assertEquals(millis, ee.getRegisteredAt().toDate().getTime());
    }

    // fromBundle reconstructs GeoPoint location
    @Test
    public void fromBundle_reconstructsLocation() {
        Bundle b = new Bundle();
        b.putBoolean("hasLocation", true);
        b.putDouble("lat", 51.05);
        b.putDouble("lng", -114.07);

        EntrantEvent ee = EntrantEvent.fromBundle(b);
        assertNotNull(ee.getLocation());
        assertEquals(51.05, ee.getLocation().getLatitude(), 0.0001);
        assertEquals(-114.07, ee.getLocation().getLongitude(), 0.0001);
    }

    // fromBundle with null returns empty EntrantEvent
    @Test
    public void fromBundle_nullReturnsEmptyObject() {
        EntrantEvent ee = EntrantEvent.fromBundle(null);
        assertNotNull(ee);
        assertNull(ee.getUserId());
        assertNull(ee.getStatus());
    }

    // round-trip toBundle → fromBundle preserves all data
    @Test
    public void roundTrip_preservesAllFields() {
        Timestamp now = Timestamp.now();
        GeoPoint loc = new GeoPoint(53.5, -113.5);

        EntrantEvent original = new EntrantEvent(
                "uid-rt", "RoundTrip", "rt@test.com", "waitlisted",
                now, now, null, null, null, loc);

        Bundle b = original.toBundle();
        EntrantEvent restored = EntrantEvent.fromBundle(b);

        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getUserName(), restored.getUserName());
        assertEquals(original.getEmail(), restored.getEmail());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getRegisteredAt().toDate().getTime(),
                restored.getRegisteredAt().toDate().getTime());
        assertNotNull(restored.getLocation());
        assertEquals(original.getLocation().getLatitude(),
                restored.getLocation().getLatitude(), 0.0001);
    }
}
