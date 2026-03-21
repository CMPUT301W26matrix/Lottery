package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

/**
 * Data model representing an entrant in an event.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store entrant identity and status timestamps for all 4 states: waited-listed, invited, signed-up, cancelled.</li>
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
    private String status;

    private Timestamp registeredAt;
    private Timestamp waitlistedAt;
    private Timestamp invitedAt;
    private Timestamp acceptedAt;
    private Timestamp declinedAt;
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
                   String status,
                   Timestamp registeredAt,
                   Timestamp waitlistedAt,
                   Timestamp invitedAt,
                   Timestamp acceptedAt,
                   Timestamp declinedAt,
                   Timestamp cancelledAt,
                   GeoPoint location) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.registeredAt = registeredAt;
        this.waitlistedAt = waitlistedAt;
        this.invitedAt = invitedAt;
        this.acceptedAt = acceptedAt;
        this.declinedAt = declinedAt;
        this.cancelledAt = cancelledAt;
        this.location = location;
    }

    /**
     * Constructor for waitlisted entrant display.
     */
    public Entrant(String userName, String userId, Timestamp registeredAt, GeoPoint location) {
        this.userName = userName;
        this.userId = userId;
        this.registeredAt = registeredAt;
        this.waitlistedAt = registeredAt;
        this.location = location;
        this.status = "waitlisted";
    }

    /**
     * Constructor for invited entrant display.
     */
    public Entrant(Timestamp invitedAt, String userName, String userId, Timestamp registeredAt, GeoPoint location) {
        this.invitedAt = invitedAt;
        this.userName = userName;
        this.userId = userId;
        this.registeredAt = registeredAt;
        this.location = location;
        this.status = "invited";
    }

    /**
     * Constructor for accepted/signed-up entrant display.
     */
    public Entrant(String userName, String userId, Timestamp invitedAt, Timestamp registeredAt,
                   GeoPoint location, Timestamp acceptedAt) {
        this.userName = userName;
        this.userId = userId;
        this.invitedAt = invitedAt;
        this.registeredAt = registeredAt;
        this.location = location;
        this.acceptedAt = acceptedAt;
        this.status = "accepted";
    }

    /**
     * Constructor for cancelled/declined entrant display.
     */
    public Entrant(String userName, GeoPoint location, String userId,
                   Timestamp cancelledAt, Timestamp invitedAt, Timestamp registeredAt) {
        this.userName = userName;
        this.location = location;
        this.userId = userId;
        this.cancelledAt = cancelledAt;
        this.invitedAt = invitedAt;
        this.registeredAt = registeredAt;
        this.status = "cancelled";
    }

    // -------------------------------------------------------------------------
    // Preferred getters / setters
    // -------------------------------------------------------------------------

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

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    // -------------------------------------------------------------------------
    // Backward-compatible aliases for existing old code
    // -------------------------------------------------------------------------

    public String getEntrant_id() {
        return userId;
    }

    public void setEntrant_id(String entrant_id) {
        this.userId = entrant_id;
    }

    public String getEntrant_name() {
        return userName;
    }

    public void setEntrant_name(String entrant_name) {
        this.userName = entrant_name;
    }

    public Timestamp getRegistration_time() {
        return registeredAt;
    }

    public void setRegistration_time(Timestamp registration_time) {
        this.registeredAt = registration_time;
    }

    public Timestamp getInvited_time() {
        return invitedAt;
    }

    public void setInvited_time(Timestamp invited_time) {
        this.invitedAt = invited_time;
    }

    public Timestamp getSigned_up_time() {
        return acceptedAt;
    }

    public void setSigned_up_time(Timestamp signed_up_time) {
        this.acceptedAt = signed_up_time;
    }

    public Timestamp getCancelled_time() {
        return cancelledAt;
    }

    public void setCancelled_time(Timestamp cancelled_time) {
        this.cancelledAt = cancelled_time;
    }

    // -------------------------------------------------------------------------
    // Helpful status checks
    // -------------------------------------------------------------------------

    public boolean isWaitlisted() {
        return "waitlisted".equalsIgnoreCase(status);
    }

    public boolean isInvited() {
        return "invited".equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return "accepted".equalsIgnoreCase(status);
    }

    public boolean isDeclined() {
        return "declined".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }
}
