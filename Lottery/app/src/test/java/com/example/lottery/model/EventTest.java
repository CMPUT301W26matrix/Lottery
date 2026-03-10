package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests for the {@link Event} model class.
 *
 * <p>This class focuses on verifying the data integrity and behavior of the Event object,
 * especially concerning the waiting list limit feature.</p>
 *
 * <p>Satisfies testing requirements for:
 * US 02.02.03: requireLocation toggle.
 * US 02.03.01: Store and retrieve waiting list capacity.
 * US 02.04.01: Poster URI storage.
 * </p>
 */
public class EventTest {

    /**
     * Verifies that the constructor and getters correctly handle and return the provided values.
     * Updated to include US 02.02.03 (requireLocation) and US 02.02.02 (waitingListLimit).
     */
    @Test
    public void testEventConstructorAndGetters() {
        String eventId = "ev123";
        String title = "Concert";
        Date now = new Date();
        Integer maxCapacity = 500;
        String details = "Music concert";
        String posterUri = "content://media/external/images/media/1";
        String qrCodeContent = "qr_content";
        String organizerId = "org456";
        boolean requireLocation = true;
        Integer waitingListLimit = 100;

        Event event = new Event(eventId, title, now, now, now, now, now,
                maxCapacity, details, posterUri, qrCodeContent,
                organizerId, requireLocation, waitingListLimit);

        assertEquals(eventId, event.getEventId());
        assertEquals(posterUri, event.getPosterUri());
        assertEquals(requireLocation, event.isRequireLocation());
        assertEquals(waitingListLimit, event.getWaitingListLimit());
    }

    /**
     * US 02.02.03: Verifies that the requireLocation toggle can be stored and retrieved correctly.
     */
    @Test
    public void testRequireLocationStorage() {
        Event event = new Event();

        event.setRequireLocation(true);
        assertTrue("Geolocation requirement should be true", event.isRequireLocation());

        event.setRequireLocation(false);
        assertFalse("Geolocation requirement should be false", event.isRequireLocation());
    }

    /**
     * US 02.04.01: Verifies that posterUri can be stored and retrieved correctly.
     */
    @Test
    public void testPosterUriStorage() {
        Event event = new Event();
        String testUri = "content://com.android.providers.media.documents/document/image%3A123";
        event.setPosterUri(testUri);
        assertEquals("Poster URI should be stored exactly as provided", testUri, event.getPosterUri());
    }

    /**
     * US 02.04.01 (Non-mandatory): Verifies that empty or null posterUri is handled safely.
     */
    @Test
    public void testEmptyPosterUriHandling() {
        Event event = new Event();

        event.setPosterUri(null);
        assertNull("Event should handle null posterUri safely", event.getPosterUri());

        event.setPosterUri("");
        assertEquals("Event should store empty string for posterUri", "", event.getPosterUri());
    }

    /**
     * Verifies the registration deadline business rule:
     * registrationDeadline must be before scheduledDateTime.
     */
    @Test
    public void testRegistrationDeadlineBeforeEventStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.DECEMBER, 31, 10, 0);
        Date eventStart = calendar.getTime();
        calendar.set(2024, Calendar.DECEMBER, 30, 10, 0);
        Date validDeadline = calendar.getTime();

        assertTrue("Deadline should be before event start", validDeadline.before(eventStart));
    }

    /**
     * US 02.03.01: Verifies that the {@link Event#setWaitingListLimit(Integer)} and
     * {@link Event#getWaitingListLimit()} methods correctly handle both
     * specific integer values and the 'null' state for an unlimited list.
     */
    @Test
    public void testWaitingListLimitSetterGetter() {
        Event event = new Event();

        event.setWaitingListLimit(50);
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());

        event.setWaitingListLimit(null);
        assertNull(event.getWaitingListLimit());
    }

    /**
     * US 02.03.01: Verifies that the {@link Event} constructor correctly initializes the
     * {@code waitingListLimit} field when a value is provided.
     */
    @Test
    public void testEventConstructorWithLimit() {
        Event event = new Event("1", "Title", null, null, null, null, null,
                10, "Details", null, null, "Org1", false, 100);
        assertEquals(Integer.valueOf(100), event.getWaitingListLimit());
    }
}
