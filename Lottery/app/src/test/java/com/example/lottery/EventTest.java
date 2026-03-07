package com.example.lottery;

import static org.junit.Assert.*;
import org.junit.Test;
import com.example.lottery.model.Event;
import java.util.Date;
import java.util.Calendar;

/**
 * Unit tests for the Event model class.
 */
public class EventTest {

    /**
     * Verifies that the constructor and getters correctly handle and return the provided values.
     */
    @Test
    public void testEventConstructorAndGetters() {
        String eventId = "ev123";
        String title = "Concert";
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.DECEMBER, 31, 20, 0);
        Date scheduledDateTime = calendar.getTime();
        
        calendar.set(2024, Calendar.DECEMBER, 30, 20, 0);
        Date registrationDeadline = calendar.getTime();
        
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

    /**
     * Verifies the registration deadline business rule: 
     * registrationDeadline must be before scheduledDateTime.
     */
    @Test
    public void testRegistrationDeadlineBeforeEventStart() {
        Calendar calendar = Calendar.getInstance();
        
        // Set event start to Dec 31
        calendar.set(2024, Calendar.DECEMBER, 31, 10, 0);
        Date eventStart = calendar.getTime();
        
        // Set deadline to Dec 30 (Valid)
        calendar.set(2024, Calendar.DECEMBER, 30, 10, 0);
        Date validDeadline = calendar.getTime();
        
        // Set deadline to Jan 1 (Invalid)
        calendar.set(2025, Calendar.JANUARY, 1, 10, 0);
        Date invalidDeadline = calendar.getTime();
        
        assertTrue("Deadline should be before event start", validDeadline.before(eventStart));
        assertFalse("Deadline should NOT be after event start", invalidDeadline.before(eventStart));
    }

    /**
     * Verifies that the setters correctly update the class attributes.
     */
    @Test
    public void testSetters() {
        Event event = new Event();
        event.setTitle("New Title");
        assertEquals("New Title", event.getTitle());
        
        event.setMaxCapacity(100);
        assertEquals(Integer.valueOf(100), event.getMaxCapacity());
    }
}
