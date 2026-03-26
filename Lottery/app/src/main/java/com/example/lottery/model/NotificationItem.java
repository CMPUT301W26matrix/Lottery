package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Model class representing a notification in a user's inbox.
 *
 * Target Firestore path:
 * users/{userId}/inbox/{notificationId}
 */
public class NotificationItem {

    private String notificationId;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private String eventTitle;
    private String senderId;
    private String senderRole;
    @PropertyName("isRead")
    private boolean isRead;
    private Timestamp createdAt;

    /**
     * Default constructor for Firestore serialization.
     */
    public NotificationItem() {
    }

    /**
     * Full constructor for a user inbox notification.
     */
    public NotificationItem(String notificationId,
                            String title,
                            String message,
                            String type,
                            String eventId,
                            String eventTitle,
                            String senderId,
                            String senderRole,
                            boolean isRead,
                            Timestamp createdAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
