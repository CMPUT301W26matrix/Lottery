package com.example.lottery.model;

/**
 * Represents a notification shown to a user in the app.
 */
public class NotificationItem {
    private String notificationId;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private boolean isRead;
    private boolean actionTaken;
    private String response;

    /**
     * Creates an empty notification item.
     */
    public NotificationItem() {
    }

    /**
     * Creates a notification item with all fields initialized.
     *
     * @param notificationId the notification ID
     * @param title          the notification title
     * @param message        the notification message
     * @param type           the notification type
     * @param eventId        the related event ID
     * @param isRead         whether the notification has been read
     * @param actionTaken    whether the user has taken action on the notification
     * @param response       the user's response to the notification
     */
    public NotificationItem(String notificationId, String title, String message, String type,
                            String eventId, boolean isRead, boolean actionTaken, String response) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.isRead = isRead;
        this.actionTaken = actionTaken;
        this.response = response;
    }

    /**
     * Returns the notification ID.
     *
     * @return the notification ID
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * Returns the notification title.
     *
     * @return the notification title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the notification message.
     *
     * @return the notification message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the notification type.
     *
     * @return the notification type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the related event ID.
     *
     * @return the related event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Indicates whether the notification has been read.
     *
     * @return true if the notification has been read
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Updates the read status of the notification.
     *
     * @param read the new read status
     */
    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Indicates whether action has been taken on the notification.
     *
     * @return true if action has been taken
     */
    public boolean isActionTaken() {
        return actionTaken;
    }

    /**
     * Updates whether action has been taken on the notification.
     *
     * @param actionTaken the new action state
     */
    public void setActionTaken(boolean actionTaken) {
        this.actionTaken = actionTaken;
    }

    /**
     * Returns the user's response to the notification.
     *
     * @return the user's response
     */
    public String getResponse() {
        return response;
    }

    /**
     * Updates the user's response to the notification.
     *
     * @param response the new response
     */
    public void setResponse(String response) {
        this.response = response;
    }
}
