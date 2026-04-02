package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    // US 02.01.02: Private event flag defaults to false
    @Test
    public void testPrivateDefaultsToFalse() {
        Event event = new Event();
        assertFalse("New event should not be private by default", event.isPrivate());
    }

    // US 02.01.02: Private event flag can be toggled on and off
    @Test
    public void testPrivateEventStorage() {
        Event event = new Event();
        event.setPrivate(true);
        assertTrue("Event should be private after setPrivate(true)", event.isPrivate());
        event.setPrivate(false);
        assertFalse("Event should not be private after setPrivate(false)", event.isPrivate());
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

    // -------------------------------------------------------------------------
    // Default constructor defaults
    // -------------------------------------------------------------------------

    // US 02.01.01: Default constructor sets status to "open"
    @Test
    public void defaultConstructor_statusIsOpen() {
        Event event = new Event();
        assertEquals("open", event.getStatus());
    }

    // US 02.01.01: Default constructor sets category to "Other"
    @Test
    public void defaultConstructor_categoryIsOther() {
        Event event = new Event();
        assertEquals("Other", event.getCategory());
    }

    // US 02.01.01: Full constructor with null category defaults to "Other"
    @Test
    public void fullConstructor_nullCategoryDefaultsToOther() {
        Timestamp now = Timestamp.now();
        Event event = new Event("id", "t", "d", "o", 10, null,
                "qr", "open", null, null, null,
                now, now, now, now, now, false, false, now, now);
        assertEquals("Other", event.getCategory());
    }

    // -------------------------------------------------------------------------
    // Setter tests for fields only tested via constructor getter
    // -------------------------------------------------------------------------

    // US 02.01.01: eventId setter
    @Test
    public void setEventId_updatesValue() {
        Event event = new Event();
        event.setEventId("ev-new");
        assertEquals("ev-new", event.getEventId());
    }

    // US 02.01.01: title setter
    @Test
    public void setTitle_updatesValue() {
        Event event = new Event();
        event.setTitle("New Title");
        assertEquals("New Title", event.getTitle());
    }

    // US 02.01.01: details setter
    @Test
    public void setDetails_updatesValue() {
        Event event = new Event();
        event.setDetails("Some details");
        assertEquals("Some details", event.getDetails());
    }

    // US 02.01.01: organizerId setter
    @Test
    public void setOrganizerId_updatesValue() {
        Event event = new Event();
        event.setOrganizerId("org-1");
        assertEquals("org-1", event.getOrganizerId());
    }

    // US 02.01.01: capacity setter
    @Test
    public void setCapacity_updatesValue() {
        Event event = new Event();
        event.setCapacity(200);
        assertEquals(Integer.valueOf(200), event.getCapacity());
    }

    // US 02.01.01: qrCodeContent setter
    @Test
    public void setQrCodeContent_updatesValue() {
        Event event = new Event();
        event.setQrCodeContent("qr-abc");
        assertEquals("qr-abc", event.getQrCodeContent());
    }

    // US 02.01.01: status setter
    @Test
    public void setStatus_updatesValue() {
        Event event = new Event();
        event.setStatus("closed");
        assertEquals("closed", event.getStatus());
    }

    // US 02.01.01: category setter
    @Test
    public void setCategory_updatesValue() {
        Event event = new Event();
        event.setCategory("Music");
        assertEquals("Music", event.getCategory());
    }

    // US 02.01.01: scheduledDateTime setter
    @Test
    public void setScheduledDateTime_updatesValue() {
        Event event = new Event();
        Timestamp ts = Timestamp.now();
        event.setScheduledDateTime(ts);
        assertEquals(ts, event.getScheduledDateTime());
    }

    // -------------------------------------------------------------------------
    // Timestamp fields with zero prior coverage
    // -------------------------------------------------------------------------

    // US 02.01.04: eventEndDateTime getter/setter
    @Test
    public void setEventEndDateTime_updatesValue() {
        Event event = new Event();
        assertNull(event.getEventEndDateTime());
        Timestamp ts = Timestamp.now();
        event.setEventEndDateTime(ts);
        assertEquals(ts, event.getEventEndDateTime());
    }

    // US 02.01.04: registrationStart getter/setter
    @Test
    public void setRegistrationStart_updatesValue() {
        Event event = new Event();
        assertNull(event.getRegistrationStart());
        Timestamp ts = Timestamp.now();
        event.setRegistrationStart(ts);
        assertEquals(ts, event.getRegistrationStart());
    }

    // US 02.01.04: registrationDeadline getter/setter
    @Test
    public void setRegistrationDeadline_updatesValue() {
        Event event = new Event();
        assertNull(event.getRegistrationDeadline());
        Timestamp ts = Timestamp.now();
        event.setRegistrationDeadline(ts);
        assertEquals(ts, event.getRegistrationDeadline());
    }

    // US 02.05.02: drawDate getter/setter
    @Test
    public void setDrawDate_updatesValue() {
        Event event = new Event();
        assertNull(event.getDrawDate());
        Timestamp ts = Timestamp.now();
        event.setDrawDate(ts);
        assertEquals(ts, event.getDrawDate());
    }

    // US 02.01.01: createdAt getter/setter
    @Test
    public void setCreatedAt_updatesValue() {
        Event event = new Event();
        assertNull(event.getCreatedAt());
        Timestamp ts = Timestamp.now();
        event.setCreatedAt(ts);
        assertEquals(ts, event.getCreatedAt());
    }

    // US 02.01.01: updatedAt getter/setter
    @Test
    public void setUpdatedAt_updatesValue() {
        Event event = new Event();
        assertNull(event.getUpdatedAt());
        Timestamp ts = Timestamp.now();
        event.setUpdatedAt(ts);
        assertEquals(ts, event.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // touch() method
    // -------------------------------------------------------------------------

    // US 02.01.01: touch() sets updatedAt
    @Test
    public void touch_setsUpdatedAt() {
        Event event = new Event();
        assertNull(event.getUpdatedAt());
        event.touch();
        assertNotNull(event.getUpdatedAt());
    }

    // US 02.01.01: touch() sets createdAt if null
    @Test
    public void touch_setsCreatedAtIfNull() {
        Event event = new Event();
        assertNull(event.getCreatedAt());
        event.touch();
        assertNotNull(event.getCreatedAt());
        assertEquals(event.getCreatedAt(), event.getUpdatedAt());
    }

    // US 02.01.01: touch() does not overwrite existing createdAt
    @Test
    public void touch_doesNotOverwriteExistingCreatedAt() {
        Event event = new Event();
        Timestamp original = Timestamp.now();
        event.setCreatedAt(original);
        event.touch();
        assertEquals(original, event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // Full constructor — verify all 20 fields
    // -------------------------------------------------------------------------

    // US 02.01.01: Full constructor sets all timestamp and boolean fields
    @Test
    public void fullConstructor_setsAllFields() {
        Timestamp t1 = Timestamp.now();

        Event event = new Event("id1", "Title", "Details", "org1", 100, 50,
                "qr1", "open", "poster1", "Sports", "Gym",
                t1, t1, t1, t1, t1, true, true, t1, t1);

        assertEquals("id1", event.getEventId());
        assertEquals("Title", event.getTitle());
        assertEquals("Details", event.getDetails());
        assertEquals("org1", event.getOrganizerId());
        assertEquals(Integer.valueOf(100), event.getCapacity());
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());
        assertEquals("qr1", event.getQrCodeContent());
        assertEquals("open", event.getStatus());
        assertEquals("poster1", event.getPosterBase64());
        assertEquals("Sports", event.getCategory());
        assertEquals("Gym", event.getPlace());
        assertEquals(t1, event.getScheduledDateTime());
        assertEquals(t1, event.getEventEndDateTime());
        assertEquals(t1, event.getRegistrationStart());
        assertEquals(t1, event.getRegistrationDeadline());
        assertEquals(t1, event.getDrawDate());
        assertTrue(event.isRequireLocation());
        assertTrue(event.isPrivate());
        assertEquals(t1, event.getCreatedAt());
        assertEquals(t1, event.getUpdatedAt());
    }
}
