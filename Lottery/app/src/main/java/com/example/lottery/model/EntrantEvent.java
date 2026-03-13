package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing the relationship between an entrant and an event.
 * Stores the status of the entrant in the event (e.g., waitlisted, invited).
 */
public class EntrantEvent {

    /**
     * Enum representing the possible statuses of an entrant for an event.
     */
    public enum Status {
        WAITLISTED,
        INVITED,
        ACCEPTED,
        DECLINED,
        SELECTED,
        CANCELLED
    }

    private String relationId;           // Composite ID: entrantId_eventId
    private String entrantId;
    private String eventId;
    private Status status;
    private Timestamp joinedAt;           // When entrant joined waiting list
    private Timestamp invitedAt;           // When entrant was invited (won lottery)
    private Timestamp respondedAt;         // When entrant accepted/declined
    private int waitlistPosition;          // Position in waiting list (if applicable)
    private boolean notificationSent;      // Whether notification was sent

    /**
     * Default constructor for Firestore.
     */
    public EntrantEvent() {
        // Default constructor for Firestore
    }

    /**
     * Constructs a new EntrantEvent relationship.
     *
     * @param entrantId The ID of the entrant.
     * @param eventId   The ID of the event.
     */
    public EntrantEvent(String entrantId, String eventId) {
        this.relationId = entrantId + "_" + eventId;
        this.entrantId = entrantId;
        this.eventId = eventId;
        this.status = Status.WAITLISTED;
        this.joinedAt = Timestamp.now();
        this.notificationSent = false;
    }

    // Getters and Setters
    public String getRelationId() { return relationId; }
    public void setRelationId(String relationId) { this.relationId = relationId; }

    public String getEntrantId() { return entrantId; }
    public void setEntrantId(String entrantId) { this.entrantId = entrantId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public Timestamp getInvitedAt() { return invitedAt; }
    public void setInvitedAt(Timestamp invitedAt) { this.invitedAt = invitedAt; }

    public Timestamp getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Timestamp respondedAt) { this.respondedAt = respondedAt; }

    public int getWaitlistPosition() { return waitlistPosition; }
    public void setWaitlistPosition(int waitlistPosition) { this.waitlistPosition = waitlistPosition; }

    public boolean isNotificationSent() { return notificationSent; }
    public void setNotificationSent(boolean notificationSent) { this.notificationSent = notificationSent; }
}
