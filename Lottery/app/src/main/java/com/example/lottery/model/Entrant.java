package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

/**
 * Data model representing an entrant in an event.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store entrant identity and status timestamps for all 4 states: waitlisted, invited, accepted, cancelled.</li>
 *   <li>Store the entrant's geolocation at the time of registration.</li>
 * </ul>
 * </p>
 */
public class Entrant implements Serializable {

    /**
     * Preferred unified fields.
     */
    private String userId;
    private String userName;
    private String email;
    private String status; // waitlisted / invited / accepted / cancelled

    private Timestamp registeredAt;
    private Timestamp waitlistedAt;
    private Timestamp invitedAt;
    private Timestamp acceptedAt;
    private Timestamp cancelledAt;

    private GeoPoint location;

    /**
     * Default constructor required for Firestore / serialization.
     */
    public Entrant() {
    }

    /**
     * Full constructor using the new unified structure.
     */
    public Entrant(String userId,
                   String userName,
                   String email,
                   String status,
                   Timestamp registeredAt,
                   Timestamp waitlistedAt,
                   Timestamp invitedAt,
                   Timestamp acceptedAt,
                   Timestamp cancelledAt,
                   GeoPoint location) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.status = status;
        this.registeredAt = registeredAt;
        this.waitlistedAt = waitlistedAt;
        this.invitedAt = invitedAt;
        this.acceptedAt = acceptedAt;
        this.cancelledAt = cancelledAt;
        this.location = location;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    // Helpful status checks

    public boolean isWaitlisted() {
        return "waitlisted".equalsIgnoreCase(status);
    }

    public boolean isInvited() {
        return "invited".equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return "accepted".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }
}
