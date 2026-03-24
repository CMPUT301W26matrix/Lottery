package com.example.lottery.model;

/**
 * Represents a notification shown to a user in the app.
 *
 * <p>
 * This model is aligned with Firestore structure:
 *
 * users/{userId}/notifications/{notificationId}
 *
 * Fields:
 * - type
 * - eventId
 * - title
 * - message
 * - isRead
 * - createdAt
 *
 * </p>
 */
public class NotificationItem {

    private String notificationId;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private boolean isRead;

    /**
     * Required empty constructor for Firestore.
     */
    public NotificationItem() {
    }

    /**
     * Full constructor.
     *
     * @param notificationId notification id
     * @param title          title of notification
     * @param message        message content
     * @param type           notification type
     * @param eventId        related event id
     * @param isRead         read status
     */
    public NotificationItem(String notificationId, String title, String message,
                            String type, String eventId, boolean isRead) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.isRead = isRead;
    }

    /**
     * @return notification id
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return type (organizer_message / lottery_win / lottery_loss)
     */
    public String getType() {
        return type;
    }

    /**
     * @return event id linked to this notification
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return true if read
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Set read status
     */
    public void setRead(boolean read) {
        isRead = read;
    }
}