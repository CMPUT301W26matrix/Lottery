package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
     * 
     * @see NotificationItem#NotificationItem(String, String, String, String, String, boolean, boolean, String)
     */
    @Test
    public void testParameterizedConstructor() {
        String id = "notif123";
        String title = "Test Title";
        String message = "Test Message";
        String type = "EVENT_INVITATION";
        String eventId = "event456";
        boolean isRead = false;
        boolean actionTaken = false;
        String response = "NONE";

        NotificationItem item = new NotificationItem(id, title, message, type, eventId, isRead, actionTaken, response);

        assertEquals(id, item.getNotificationId());
        assertEquals(title, item.getTitle());
        assertEquals(message, item.getMessage());
        assertEquals(type, item.getType());
        assertEquals(eventId, item.getEventId());
        assertEquals(isRead, item.isRead());
        assertEquals(actionTaken, item.isActionTaken());
        assertEquals(response, item.getResponse());
    }

    /**
     * Verifies that setters correctly update the read and action status of the notification.
     */
    @Test
    public void testSetters() {
        NotificationItem item = new NotificationItem();

        item.setRead(true);
        assertTrue(item.isRead());

        item.setActionTaken(true);
        assertTrue(item.isActionTaken());

        String response = "ACCEPTED";
        item.setResponse(response);
        assertEquals(response, item.getResponse());
    }
}
