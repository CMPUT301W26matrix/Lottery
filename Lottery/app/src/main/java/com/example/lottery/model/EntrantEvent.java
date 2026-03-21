package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

/**
 * Model class representing an entrant's participation record in an event's waiting list.
 *
 * Target Firestore path:
 * events/{eventId}/waitingList/{uid}
 */
public class EntrantEvent {

    private String userId;
    private String userName;
    private String email;
    private String status; // registered, selected, accepted, declined, cancelled, not_selected
    private Timestamp joinedAt;
    private Timestamp selectedAt;
    private Timestamp respondedAt;
    private Timestamp cancelledAt;
    private Timestamp updatedAt;
    private GeoPoint location;

    /**
     * Default constructor for Firestore serialization.
     */
    public EntrantEvent() {
    }

    /**
     * Full constructor for an entrant-event relationship.
     */
    public EntrantEvent(String userId,
                        String userName,
                        String email,
                        String status,
                        Timestamp joinedAt,
                        Timestamp selectedAt,
                        Timestamp respondedAt,
                        Timestamp cancelledAt,
                        Timestamp updatedAt,
                        GeoPoint location) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.status = status;
        this.joinedAt = joinedAt;
        this.selectedAt = selectedAt;
        this.respondedAt = respondedAt;
        this.cancelledAt = cancelledAt;
        this.updatedAt = updatedAt;
        this.location = location;
    }

    /**
     * Constructor without location for backward compatibility.
     */
    public EntrantEvent(String userId,
                        String userName,
                        String email,
                        String status,
                        Timestamp joinedAt,
                        Timestamp selectedAt,
                        Timestamp respondedAt,
                        Timestamp cancelledAt,
                        Timestamp updatedAt) {
        this(userId, userName, email, status, joinedAt, selectedAt, respondedAt, cancelledAt, updatedAt, null);
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Backward compatibility alias for older code using entrantId */
    public String getEntrantId() {
        return userId;
    }

    public void setEntrantId(String entrantId) {
        this.userId = entrantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Timestamp getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Timestamp selectedAt) {
        this.selectedAt = selectedAt;
    }

    /** Backward compatibility alias for older code using invitedAt */
    public Timestamp getInvitedAt() {
        return selectedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.selectedAt = invitedAt;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    /**
     * Updates the updatedAt timestamp. Should be called explicitly before Firestore writes.
     */
    public void touch() {
        this.updatedAt = Timestamp.now();
    }
}
