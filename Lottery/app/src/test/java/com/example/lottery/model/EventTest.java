package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for the {@link Event} model class.
 */
public class EventTest {

    // US 03.04.01: Event model should store all fields needed for admin event browsing
    @Test
    public void testEventConstructorAndGetters() {
        String eventId = "ev123";
        String title = "Concert";
        String details = "Music concert";
        String organizerId = "org456";
        Integer capacity = 500;
        Integer waitingListLimit = 100;
        String qrCodeContent = "qr_content";
        String status = "open";
        String posterBase64 = "base64_test";
        String category = "Music";
        Timestamp now = Timestamp.now();

        // Testing the full constructor with compliant fields
        String place = "Community Centre A, Room 101";

        Event event = new Event(eventId, title, details, organizerId, capacity,
                waitingListLimit, qrCodeContent, status, posterBase64, category,
                place, now, now, now, now, now, true, false, now, now);

        assertEquals(eventId, event.getEventId());
        assertEquals(title, event.getTitle());
        assertEquals(details, event.getDetails());
        assertEquals(organizerId, event.getOrganizerId());
        assertEquals(capacity, event.getCapacity());
        assertEquals(waitingListLimit, event.getWaitingListLimit());
        assertEquals(qrCodeContent, event.getQrCodeContent());
        assertEquals(status, event.getStatus());
        assertEquals(posterBase64, event.getPosterBase64());
        assertEquals(category, event.getCategory());
        assertEquals(place, event.getPlace());
        assertEquals(now, event.getScheduledDateTime());
        assertTrue(event.isRequireLocation());
        assertFalse(event.isPrivate());
    }

    // US 03.04.01: Event should store geolocation requirement for admin review
    @Test
    public void testRequireLocationStorage() {
        Event event = new Event();
        event.setRequireLocation(true);
        assertTrue("Geolocation requirement should be true", event.isRequireLocation());
        event.setRequireLocation(false);
        assertFalse("Geolocation requirement should be false", event.isRequireLocation());
    }

    // US 03.03.01: Event should store poster Base64 for admin image management
    @Test
    public void testPosterBase64Storage() {
        Event event = new Event();
        String testBase64 = "data:image/jpeg;base64,sample";
        event.setPosterBase64(testBase64);
        assertEquals("Poster Base64 should be stored exactly as provided", testBase64, event.getPosterBase64());
    }

    // US 02.04.01: New event without poster should have null posterBase64
    @Test
    public void testPosterBase64DefaultsToNull() {
        Event event = new Event();
        assertNull("Poster Base64 should default to null", event.getPosterBase64());
    }

    // US 03.03.01: Admin clearing a poster should result in null posterBase64
    @Test
    public void testPosterBase64CanBeCleared() {
        Event event = new Event();
        event.setPosterBase64("data:image/jpeg;base64,sample");
        event.setPosterBase64(null);
        assertNull("Poster Base64 should be null after clearing", event.getPosterBase64());
    }

    // US 02.04.01: Event without uploaded poster should store empty string
    @Test
    public void testPosterBase64CanBeSetToEmpty() {
        Event event = new Event();
        event.setPosterBase64("");
        assertEquals("Poster Base64 should accept empty string", "", event.getPosterBase64());
    }

    @Test
    public void testPlaceSetterGetter() {
        Event event = new Event();
        assertNull(event.getPlace());
        event.setPlace("Local Rec Centre");
        assertEquals("Local Rec Centre", event.getPlace());
        event.setPlace(null);
        assertNull(event.getPlace());
    }

    // US 03.04.01: Event should support nullable waiting list limit for admin browsing
    @Test
    public void testWaitingListLimitSetterGetter() {
        Event event = new Event();
        event.setWaitingListLimit(50);
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());
        event.setWaitingListLimit(null);
        assertNull(event.getWaitingListLimit());
    }
}
