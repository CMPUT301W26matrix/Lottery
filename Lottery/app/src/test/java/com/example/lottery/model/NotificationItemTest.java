package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for the {@link NotificationItem} model class.
 *
 * <p>Ensures that the notification model correctly stores information such as
 * the title, message, and action status needed for the entrant notification flow.</p>
 */
public class NotificationItemTest {

    /**
     * Verifies that the default constructor creates a non-null object for Firestore.
     */
    @Test
    public void testDefaultConstructor() {
        NotificationItem item = new NotificationItem();
        assertNotNull(item);
    }

    /**
     * Verifies that the parameterized constructor correctly initializes all notification fields.
     */
    @Test
    public void testParameterizedConstructor() {
        String id = "notif123";
        String title = "Test Title";
        String message = "Test Message";
        String type = "EVENT_INVITATION";
        String eventId = "event456";
        String eventTitle = "Event Title";
        String senderId = "sender123";
        String senderRole = "organizer";
        boolean isRead = false;
        Timestamp createdAt = Timestamp.now();

        NotificationItem item = new NotificationItem(id, title, message, type, eventId, eventTitle, senderId, senderRole, isRead, createdAt);

        assertEquals(id, item.getNotificationId());
        assertEquals(title, item.getTitle());
        assertEquals(message, item.getMessage());
        assertEquals(type, item.getType());
        assertEquals(eventId, item.getEventId());
        assertEquals(eventTitle, item.getEventTitle());
        assertEquals(senderId, item.getSenderId());
        assertEquals(senderRole, item.getSenderRole());
        assertEquals(isRead, item.isRead());
        assertEquals(createdAt, item.getCreatedAt());
    }

    /**
     * Verifies that setters correctly update properties.
     */
    @Test
    public void testSetters() {
        NotificationItem item = new NotificationItem();

        item.setRead(true);
        assertTrue(item.isRead());

        String newTitle = "New Title";
        item.setTitle(newTitle);
        assertEquals(newTitle, item.getTitle());
    }
}
