package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests for the {@link Event} model class.
 */
public class EventTest {

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
        Timestamp now = Timestamp.now();

        Event event = new Event(eventId, title, details, organizerId, capacity,
                waitingListLimit, qrCodeContent, status, now, now);

        assertEquals(eventId, event.getEventId());
        assertEquals(title, event.getTitle());
        assertEquals(details, event.getDetails());
        assertEquals(organizerId, event.getOrganizerId());
        assertEquals(capacity, event.getCapacity());
        assertEquals(waitingListLimit, event.getWaitingListLimit());
        assertEquals(qrCodeContent, event.getQrCodeContent());
        assertEquals(status, event.getStatus());
    }

    @Test
    public void testRequireLocationStorage() {
        Event event = new Event();
        event.setRequireLocation(true);
        assertTrue("Geolocation requirement should be true", event.isRequireLocation());
        event.setRequireLocation(false);
        assertFalse("Geolocation requirement should be false", event.isRequireLocation());
    }

    @Test
    public void testPosterUriStorage() {
        Event event = new Event();
        String testUri = "content://com.android.providers.media.documents/document/image%3A123";
        event.setPosterUri(testUri);
        assertEquals("Poster URI should be stored exactly as provided", testUri, event.getPosterUri());
    }

    @Test
    public void testWaitingListLimitSetterGetter() {
        Event event = new Event();
        event.setWaitingListLimit(50);
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());
        event.setWaitingListLimit(null);
        assertNull(event.getWaitingListLimit());
    }
}
