package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing one entrant's relationship to one event.
 *
 * Recommended Firestore path:
 * events/{eventId}/entrants/{userId}
 *
 * This model stores:
 * - who the entrant is
 * - which event they belong to
 * - their current status in the event flow
 * - timestamps for important transitions
 * - waitlist position if applicable
 *
 * Notes:
 * - relationId is kept for backward compatibility with older code,
 *   but the preferred unique identity is the Firestore document path.
 * - notificationSent is retained temporarily, but a dedicated
 *   notifications collection should be used for real notification history.
 */
public class EntrantEvent {

    /**
     * Optional legacy composite identifier.
     * Example: userId_eventId
     *
     * This is kept for compatibility with existing code,
     * but should not be treated as the primary source of identity long-term.
     */
    private String relationId;

    /**
     * The user ID of the entrant.
     */
    private String userId;

    /**
     * Optional display name cached for convenience in UI lists.
     */
    private String userName;

    /**
     * The event ID this relationship belongs to.
     */
    private String eventId;

    /**
     * Current status of this entrant in this event.
     */
    private Status status;

    /**
     * When the entrant first registered for the event.
     */
    private Timestamp registeredAt;

    /**
     * When the entrant was placed on the waitlist.
     * This may be the same as registeredAt if your flow always starts with waitlisting.
     */
    private Timestamp waitlistedAt;

    /**
     * When the entrant was invited/selected.
     */
    private Timestamp invitedAt;

    /**
     * When the entrant accepted the invitation.
     */
    private Timestamp acceptedAt;

    /**
     * When the entrant declined the invitation.
     */
    private Timestamp declinedAt;

    /**
     * When the entrant cancelled participation or was marked cancelled.
     */
    private Timestamp cancelledAt;

    /**
     * Current waitlist position, if applicable.
     * Use -1 when not on waitlist / not applicable.
     */
    private int waitlistPosition;

    /**
     * Temporary helper flag for simple notification workflows.
     * Long-term, store actual notification records under:
     * users/{userId}/notifications/{notificationId}
     */
    private boolean notificationSent;

    /**
     * Default constructor required by Firestore.
     */
    public EntrantEvent() {
        this.waitlistPosition = -1;
        this.notificationSent = false;
    }

    /**
     * Constructs a new entrant-event relationship with default WAITLISTED state.
     *
     * @param userId  the entrant's user ID
     * @param eventId the event ID
     */
    public EntrantEvent(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
        this.relationId = buildRelationId(userId, eventId);
        this.status = Status.WAITLISTED;

        Timestamp now = Timestamp.now();
        this.registeredAt = now;
        this.waitlistedAt = now;

        this.waitlistPosition = -1;
        this.notificationSent = false;
    }

    /**
     * Constructs a new entrant-event relationship with cached user name.
     *
     * @param userId   the entrant's user ID
     * @param userName the entrant's display name
     * @param eventId  the event ID
     */
    public EntrantEvent(String userId, String userName, String eventId) {
        this(userId, eventId);
        this.userName = userName;
    }

    /**
     * Utility method for building the legacy composite relation ID.
     */
    public static String buildRelationId(String userId, String eventId) {
        return userId + "_" + eventId;
    }

    public String getRelationId() {
        return relationId;
    }

    public void setRelationId(String relationId) {
        this.relationId = relationId;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Preferred setter.
     * Also refreshes relationId when possible.
     */
    public void setUserId(String userId) {
        this.userId = userId;
        refreshRelationId();
    }

    /**
     * Backward-compatible alias for older code still using entrantId naming.
     */
    public String getEntrantId() {
        return userId;
    }

    /**
     * Backward-compatible alias for older code still using entrantId naming.
     */
    public void setEntrantId(String entrantId) {
        this.userId = entrantId;
        refreshRelationId();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
        refreshRelationId();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Timestamp getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Timestamp registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Timestamp getWaitlistedAt() {
        return waitlistedAt;
    }

    public void setWaitlistedAt(Timestamp waitlistedAt) {
        this.waitlistedAt = waitlistedAt;
    }

    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Timestamp getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Timestamp getDeclinedAt() {
        return declinedAt;
    }

    public void setDeclinedAt(Timestamp declinedAt) {
        this.declinedAt = declinedAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public int getWaitlistPosition() {
        return waitlistPosition;
    }

    public void setWaitlistPosition(int waitlistPosition) {
        this.waitlistPosition = waitlistPosition;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    /**
     * Returns true if the entrant is currently on the waitlist.
     */
    public boolean isWaitlisted() {
        return status == Status.WAITLISTED;
    }

    /**
     * Returns true if the entrant has been invited.
     */
    public boolean isInvited() {
        return status == Status.INVITED;
    }

    /**
     * Returns true if the entrant accepted the invitation.
     */
    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    /**
     * Returns true if the entrant declined the invitation.
     */
    public boolean isDeclined() {
        return status == Status.DECLINED;
    }

    /**
     * Returns true if the entrant is cancelled.
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    /**
     * Marks this entrant as invited and records invitedAt.
     */
    public void markInvited() {
        this.status = Status.INVITED;
        this.invitedAt = Timestamp.now();
    }

    /**
     * Marks this entrant as accepted and records acceptedAt.
     */
    public void markAccepted() {
        this.status = Status.ACCEPTED;
        this.acceptedAt = Timestamp.now();
    }

    /**
     * Marks this entrant as declined and records declinedAt.
     */
    public void markDeclined() {
        this.status = Status.DECLINED;
        this.declinedAt = Timestamp.now();
    }

    /**
     * Marks this entrant as cancelled and records cancelledAt.
     */
    public void markCancelled() {
        this.status = Status.CANCELLED;
        this.cancelledAt = Timestamp.now();
    }

    /**
     * Refreshes the legacy relationId if both userId and eventId are present.
     */
    private void refreshRelationId() {
        if (userId != null && !userId.isEmpty() && eventId != null && !eventId.isEmpty()) {
            this.relationId = buildRelationId(userId, eventId);
        }
    }

    /**
     * Enum representing the valid event participation states.
     *
     * Keep this enum minimal and unambiguous.
     */
    public enum Status {
        WAITLISTED,
        INVITED,
        ACCEPTED,
        DECLINED,
        CANCELLED
    }
}