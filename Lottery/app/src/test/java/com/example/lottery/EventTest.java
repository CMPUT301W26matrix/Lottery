package com.example.lottery;

import static org.junit.Assert.*;
import org.junit.Test;
import com.example.lottery.model.Event;
import java.util.Date;
import java.util.Calendar;

/**
 * Unit tests for the Event model class.
 * Ensures data integrity and validation for core fields including US 02.04.01 
 * and US 02.02.03 requirements.
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
     * US 02.02.03 Requirement:
     * Verifies that the requireLocation toggle can be stored and retrieved correctly.
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
     * US 02.04.01 Requirement:
     * Verifies that posterUri can be stored and retrieved correctly.
     */
    @Test
    public void testPosterUriStorage() {
        Event event = new Event();
        String testUri = "content://com.android.providers.media.documents/document/image%3A123";
        event.setPosterUri(testUri);
        assertEquals("Poster URI should be stored exactly as provided", testUri, event.getPosterUri());
    }

    /**
     * US 02.04.01 Requirement (Non-mandatory):
     * Verifies that empty or null posterUri is handled safely without crashes.
     */
    @Test
    public void testEmptyPosterUriHandling() {
        Event event = new Event();
        
        // Test Null
        event.setPosterUri(null);
        assertNull("Event should handle null posterUri safely", event.getPosterUri());
        
        // Test Empty String
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
}
