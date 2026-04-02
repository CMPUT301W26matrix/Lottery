package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

/**
 * Unit tests for the {@link EntrantEvent} model class.
 *
 * <p>Verifies status helper methods, field getters/setters, and constructor
 * behavior for tracking entrant participation in events.</p>
 */
public class EntrantEventTest {

    // -------------------------------------------------------------------------
    // US 01.01.01 — Entrant joins the waiting list
    // -------------------------------------------------------------------------

    // US 01.01.01: EntrantEvent with status "waitlisted" returns isWaitlisted()=true
    @Test
    public void isWaitlisted_withWaitlistedStatus_returnsTrue() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("waitlisted");
        assertTrue("Status 'waitlisted' should make isWaitlisted() true", ee.isWaitlisted());
    }

    // US 01.01.01: isWaitlisted returns false for other statuses
    @Test
    public void isWaitlisted_withInvitedStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("invited");
        assertFalse("Status 'invited' should make isWaitlisted() false", ee.isWaitlisted());
    }

    // -------------------------------------------------------------------------
    // US 01.05.02 — Entrant accepts an invitation
    // -------------------------------------------------------------------------

    // US 01.05.02: EntrantEvent with status "accepted" returns isAccepted()=true
    @Test
    public void isAccepted_withAcceptedStatus_returnsTrue() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("accepted");
        assertTrue("Status 'accepted' should make isAccepted() true", ee.isAccepted());
    }

    // US 01.05.02: isAccepted returns false for waitlisted status
    @Test
    public void isAccepted_withWaitlistedStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("waitlisted");
        assertFalse("Status 'waitlisted' should make isAccepted() false", ee.isAccepted());
    }

    // -------------------------------------------------------------------------
    // US 01.05.03 — Entrant declines an invitation (status becomes cancelled)
    // -------------------------------------------------------------------------

    // US 01.05.03: EntrantEvent with status "cancelled" after decline returns isCancelled()=true
    @Test
    public void isCancelled_withCancelledStatus_returnsTrue() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("cancelled");
        assertTrue("Status 'cancelled' should make isCancelled() true", ee.isCancelled());
    }

    // US 01.05.03: isCancelled returns false for invited status
    @Test
    public void isCancelled_withInvitedStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("invited");
        assertFalse("Status 'invited' should make isCancelled() false", ee.isCancelled());
    }

    // -------------------------------------------------------------------------
    // US 01.05.04 — Waitlist count is tracked via timestamps
    // -------------------------------------------------------------------------

    // US 01.05.04: waitlistedAt timestamp can be stored on the entrant event
    @Test
    public void setWaitlistedAt_storesTimestamp() {
        EntrantEvent ee = new EntrantEvent();
        Timestamp now = Timestamp.now();
        ee.setWaitlistedAt(now);
        assertEquals("waitlistedAt should be the timestamp that was set", now, ee.getWaitlistedAt());
    }

    // US 01.05.04: registeredAt timestamp can be stored on the entrant event
    @Test
    public void setRegisteredAt_storesTimestamp() {
        EntrantEvent ee = new EntrantEvent();
        Timestamp now = Timestamp.now();
        ee.setRegisteredAt(now);
        assertEquals("registeredAt should be the timestamp that was set", now, ee.getRegisteredAt());
    }

    // -------------------------------------------------------------------------
    // US 02.06.01 — Organizer views invited entrants
    // -------------------------------------------------------------------------

    // US 02.06.01: isInvited returns true for "invited" status
    @Test
    public void isInvited_withInvitedStatus_returnsTrue() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("invited");
        assertTrue("Status 'invited' should make isInvited() true", ee.isInvited());
    }

    // US 02.06.01: isInvited returns false for "accepted" status
    @Test
    public void isInvited_withAcceptedStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("accepted");
        assertFalse("Status 'accepted' should make isInvited() false", ee.isInvited());
    }

    // -------------------------------------------------------------------------
    // US 02.06.02 — Organizer views cancelled entrants
    // -------------------------------------------------------------------------

    // US 02.06.02: isCancelled identifies organizer-cancelled entrant
    @Test
    public void isCancelled_identifiesCancelledEntrant() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("cancelled");
        Timestamp now = Timestamp.now();
        ee.setCancelledAt(now);

        assertTrue("isCancelled() should be true for cancelled entrant", ee.isCancelled());
        assertEquals("cancelledAt should be the timestamp that was set", now, ee.getCancelledAt());
    }

    // -------------------------------------------------------------------------
    // US 02.06.03 — Organizer views accepted/signed-up entrants
    // -------------------------------------------------------------------------

    // US 02.06.03: isAccepted identifies signed-up entrant
    @Test
    public void isAccepted_identifiesSignedUpEntrant() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("accepted");
        Timestamp now = Timestamp.now();
        ee.setAcceptedAt(now);

        assertTrue("isAccepted() should be true for signed-up entrant", ee.isAccepted());
        assertEquals("acceptedAt should be the timestamp that was set", now, ee.getAcceptedAt());
    }

    // -------------------------------------------------------------------------
    // Case-insensitive status checks
    // -------------------------------------------------------------------------

    // US 01.01.01: Waitlist helper stays case-insensitive for stored status values
    @Test
    public void isWaitlisted_caseInsensitive() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("WAITLISTED");
        assertTrue("isWaitlisted() should be case-insensitive", ee.isWaitlisted());
    }

    // US 02.06.01: Invited helper stays case-insensitive for stored status values
    @Test
    public void isInvited_caseInsensitive() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("INVITED");
        assertTrue("isInvited() should be case-insensitive", ee.isInvited());
    }

    // US 01.05.02 / US 02.06.03: Accepted helper stays case-insensitive for stored status values
    @Test
    public void isAccepted_caseInsensitive() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("ACCEPTED");
        assertTrue("isAccepted() should be case-insensitive", ee.isAccepted());
    }

    // US 01.05.03 / US 02.06.02: Cancelled helper stays case-insensitive for stored status values
    @Test
    public void isCancelled_caseInsensitive() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("CANCELLED");
        assertTrue("isCancelled() should be case-insensitive", ee.isCancelled());
    }

    // US 01.01.01: Mixed-case waitlisted values are still treated as waitlisted
    @Test
    public void isWaitlisted_mixedCase() {
        EntrantEvent ee = new EntrantEvent();
        ee.setStatus("Waitlisted");
        assertTrue("isWaitlisted() should handle mixed case", ee.isWaitlisted());
    }

    // -------------------------------------------------------------------------
    // Null status edge cases
    // -------------------------------------------------------------------------

    // US 01.01.01: Missing status should not accidentally mark the entrant as waitlisted
    @Test
    public void isWaitlisted_nullStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        // status is null by default
        assertFalse("isWaitlisted() should be false when status is null", ee.isWaitlisted());
    }

    // US 02.06.01: Missing status should not accidentally mark the entrant as invited
    @Test
    public void isInvited_nullStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        assertFalse("isInvited() should be false when status is null", ee.isInvited());
    }

    // US 01.05.02 / US 02.06.03: Missing status should not accidentally mark the entrant as accepted
    @Test
    public void isAccepted_nullStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        assertFalse("isAccepted() should be false when status is null", ee.isAccepted());
    }

    // US 01.05.03 / US 02.06.02: Missing status should not accidentally mark the entrant as cancelled
    @Test
    public void isCancelled_nullStatus_returnsFalse() {
        EntrantEvent ee = new EntrantEvent();
        assertFalse("isCancelled() should be false when status is null", ee.isCancelled());
    }

    // -------------------------------------------------------------------------
    // Field getters and setters
    // -------------------------------------------------------------------------

    // US 02.02.01 / US 02.06.01: Entrant-event records retain the entrant userId
    @Test
    public void setUserId_and_getUserId() {
        EntrantEvent ee = new EntrantEvent();
        ee.setUserId("user-42");
        assertEquals("user-42", ee.getUserId());
    }

    // US 02.02.01 / US 02.06.01: Entrant-event records retain the entrant display name
    @Test
    public void setUserName_and_getUserName() {
        EntrantEvent ee = new EntrantEvent();
        ee.setUserName("Jane Smith");
        assertEquals("Jane Smith", ee.getUserName());
    }

    // US 02.02.01 / US 02.06.01: Entrant-event records retain the entrant contact email
    @Test
    public void setEmail_and_getEmail() {
        EntrantEvent ee = new EntrantEvent();
        ee.setEmail("jane@example.com");
        assertEquals("jane@example.com", ee.getEmail());
    }

    // US 02.06.01: Invited entrants retain the invitedAt timestamp
    @Test
    public void setInvitedAt_and_getInvitedAt() {
        EntrantEvent ee = new EntrantEvent();
        Timestamp ts = Timestamp.now();
        ee.setInvitedAt(ts);
        assertEquals(ts, ee.getInvitedAt());
    }

    // US 02.02.02: Entrant-event records retain geolocation data for map viewing
    @Test
    public void setLocation_and_getLocation() {
        EntrantEvent ee = new EntrantEvent();
        GeoPoint gp = new GeoPoint(53.5461, -113.4938);
        ee.setLocation(gp);
        assertEquals(gp, ee.getLocation());
    }

    // -------------------------------------------------------------------------
    // Full constructor
    // -------------------------------------------------------------------------

    // US 01.01.01 / US 02.02.02: Full entrant-event records retain status, timestamps, and location
    @Test
    public void fullConstructor_setsAllFields() {
        Timestamp now = Timestamp.now();
        GeoPoint loc = new GeoPoint(51.05, -114.07);

        EntrantEvent ee = new EntrantEvent(
                "uid-1", "Alice", "alice@test.com", "waitlisted",
                now, now, null, null, null, loc);

        assertEquals("uid-1", ee.getUserId());
        assertEquals("Alice", ee.getUserName());
        assertEquals("alice@test.com", ee.getEmail());
        assertEquals("waitlisted", ee.getStatus());
        assertEquals(now, ee.getRegisteredAt());
        assertEquals(now, ee.getWaitlistedAt());
        assertNull(ee.getInvitedAt());
        assertNull(ee.getAcceptedAt());
        assertNull(ee.getCancelledAt());
        assertEquals(loc, ee.getLocation());
    }

    // US 01.01.01 / US 02.02.01: Default entrant-event records start empty until populated from Firestore
    @Test
    public void defaultConstructor_fieldsAreNull() {
        EntrantEvent ee = new EntrantEvent();
        assertNotNull("Default constructor should produce non-null object", ee);
        assertNull(ee.getUserId());
        assertNull(ee.getStatus());
        assertNull(ee.getRegisteredAt());
    }
}
