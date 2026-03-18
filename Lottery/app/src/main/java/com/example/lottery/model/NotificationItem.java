package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing a notification shown to a user in the app.
 *
 * Recommended Firestore path:
 * users/{userId}/notifications/{notificationId}
 *
 * This model stores:
 * - notification content
 * - related event reference
 * - notification type
 * - whether it has been read
 * - whether user action is required / completed
 * - timestamps for creation and action
 */
public class NotificationItem {

    /**
     * Unique notification identifier.
     * Usually also used as the Firestore document ID.
     */
    private String notificationId;

    /**
     * Notification title shown in the UI.
     */
    private String title;

    /**
     * Notification message body shown in the UI.
     */
    private String message;

    /**
     * Type of notification.
     */
    private Type type;

    /**
     * Related event ID, if applicable.
     */
    private String eventId;

    /**
     * Whether the user has read this notification.
     */
    private boolean isRead;

    /**
     * Whether this notification requires user action.
     * Example: invitation notifications may require accept/decline.
     */
    private boolean requiresAction;

    /**
     * Whether the user has already taken the required action.
     */
    private boolean actionTaken;

    /**
     * The response the user gave, if applicable.
     */
    private Response response;

    /**
     * When the notification was created.
     */
    private Timestamp createdAt;

    /**
     * When the notification was acted on.
     */
    private Timestamp actedAt;

    /**
     * Default constructor required for Firestore.
     */
    public NotificationItem() {
        this.isRead = false;
        this.requiresAction = false;
        this.actionTaken = false;
        this.response = Response.NONE;
    }

    /**
     * Full constructor.
     *
     * @param notificationId unique notification ID
     * @param title          notification title
     * @param message        notification body
     * @param type           notification type
     * @param eventId        related event ID
     * @param isRead         whether the notification is read
     * @param requiresAction whether the notification requires user action
     * @param actionTaken    whether the action has already been taken
     * @param response       the user's response
     * @param createdAt      creation timestamp
     * @param actedAt        action timestamp
     */
    public NotificationItem(String notificationId,
                            String title,
                            String message,
                            Type type,
                            String eventId,
                            boolean isRead,
                            boolean requiresAction,
                            boolean actionTaken,
                            Response response,
                            Timestamp createdAt,
                            Timestamp actedAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.isRead = isRead;
        this.requiresAction = requiresAction;
        this.actionTaken = actionTaken;
        this.response = response;
        this.createdAt = createdAt;
        this.actedAt = actedAt;
    }

    /**
     * Convenience constructor for a newly created notification.
     * Defaults:
     * - unread
     * - no action taken
     * - response = NONE
     * - createdAt = now
     */
    public NotificationItem(String notificationId,
                            String title,
                            String message,
                            Type type,
                            String eventId,
                            boolean requiresAction) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.requiresAction = requiresAction;
        this.isRead = false;
        this.actionTaken = false;
        this.response = Response.NONE;
        this.createdAt = Timestamp.now();
        this.actedAt = null;
    }

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Backward-compatible helper if some older code still uses String type values.
     */
    public void setTypeFromString(String type) {
        if (type == null) {
            this.type = Type.GENERAL;
            return;
        }

        switch (type.trim().toLowerCase()) {
            case "event_invitation":
            case "invitation":
            case "invite":
                this.type = Type.EVENT_INVITATION;
                break;
            case "waitlist_promoted":
            case "promotion":
                this.type = Type.WAITLIST_PROMOTED;
                break;
            case "event_cancelled":
            case "cancelled":
                this.type = Type.EVENT_CANCELLED;
                break;
            case "draw_result":
                this.type = Type.DRAW_RESULT;
                break;
            default:
                this.type = Type.GENERAL;
                break;
        }
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public boolean isActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(boolean actionTaken) {
        this.actionTaken = actionTaken;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * Backward-compatible helper if some older code still uses String response values.
     */
    public void setResponseFromString(String response) {
        if (response == null) {
            this.response = Response.NONE;
            return;
        }

        switch (response.trim().toLowerCase()) {
            case "accepted":
            case "accept":
                this.response = Response.ACCEPTED;
                break;
            case "declined":
            case "decline":
                this.response = Response.DECLINED;
                break;
            case "dismissed":
            case "dismiss":
                this.response = Response.DISMISSED;
                break;
            default:
                this.response = Response.NONE;
                break;
        }
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getActedAt() {
        return actedAt;
    }

    public void setActedAt(Timestamp actedAt) {
        this.actedAt = actedAt;
    }

    /**
     * Marks this notification as read.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Marks this notification as accepted by the user.
     */
    public void markAccepted() {
        this.actionTaken = true;
        this.response = Response.ACCEPTED;
        this.actedAt = Timestamp.now();
        this.isRead = true;
    }

    /**
     * Marks this notification as declined by the user.
     */
    public void markDeclined() {
        this.actionTaken = true;
        this.response = Response.DECLINED;
        this.actedAt = Timestamp.now();
        this.isRead = true;
    }

    /**
     * Marks this notification as dismissed by the user.
     */
    public void markDismissed() {
        this.actionTaken = true;
        this.response = Response.DISMISSED;
        this.actedAt = Timestamp.now();
        this.isRead = true;
    }

    /**
     * Returns true if the notification has a related event.
     */
    public boolean hasEventReference() {
        return eventId != null && !eventId.trim().isEmpty();
    }

    /**
     * Returns true if the notification is actionable and still pending.
     */
    public boolean isPendingAction() {
        return requiresAction && !actionTaken;
    }

    /**
     * Enum for notification type.
     */
    public enum Type {
        GENERAL,
        EVENT_INVITATION,
        WAITLIST_PROMOTED,
        DRAW_RESULT,
        EVENT_CANCELLED
    }

    /**
     * Enum for user response to a notification.
     */
    public enum Response {
        NONE,
        ACCEPTED,
        DECLINED,
        DISMISSED
    }
}