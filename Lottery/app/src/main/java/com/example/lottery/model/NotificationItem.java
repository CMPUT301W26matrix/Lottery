package com.example.lottery.model;

public class NotificationItem {
    private String notificationId;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private boolean isRead;
    private boolean actionTaken;
    private String response;

    public NotificationItem() {}

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

    public String getNotificationId() {
        return notificationId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isActionTaken() {
        return actionTaken;
    }

    public String getResponse() {
        return response;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setActionTaken(boolean actionTaken) {
        this.actionTaken = actionTaken;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}