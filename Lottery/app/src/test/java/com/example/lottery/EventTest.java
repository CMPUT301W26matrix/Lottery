package com.example.lottery;

import static org.junit.Assert.*;
import org.junit.Test;
import com.example.lottery.model.Event;

/**
 * Unit tests for the Event model class.
 */
public class EventTest {

    @Test
    public void testEventConstructorAndGetters() {
        String eventId = "ev123";
        String title = "Concert";
        String scheduledDateTime = "2024-12-31 20:00";
        String registrationDeadline = "2024-12-30 20:00";
        Integer maxCapacity = 500;
        String details = "Music concert";
        String posterUri = "uri://poster";
        String qrCodeContent = "qr_content";
        String organizerId = "org456";

        Event event = new Event(eventId, title, scheduledDateTime, registrationDeadline, 
                                maxCapacity, details, posterUri, qrCodeContent, organizerId);

        assertEquals(eventId, event.getEventId());
        assertEquals(title, event.getTitle());
        assertEquals(scheduledDateTime, event.getScheduledDateTime());
        assertEquals(registrationDeadline, event.getRegistrationDeadline());
        assertEquals(maxCapacity, event.getMaxCapacity());
        assertEquals(details, event.getDetails());
        assertEquals(posterUri, event.getPosterUri());
        assertEquals(qrCodeContent, event.getQrCodeContent());
        assertEquals(organizerId, event.getOrganizerId());
    }

    @Test
    public void testSetters() {
        Event event = new Event();
        event.setTitle("New Title");
        assertEquals("New Title", event.getTitle());
        
        event.setMaxCapacity(100);
        assertEquals(Integer.valueOf(100), event.getMaxCapacity());
    }
}
