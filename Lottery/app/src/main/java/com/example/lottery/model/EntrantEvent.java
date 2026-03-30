package com.example.lottery.model;

import android.os.Bundle;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

/**
 * Model class representing an entrant's participation record in an event's waiting list.
 * <p>
 * Target Firestore path:
 * events/{eventId}/waitingList/{userId}
 */
public class EntrantEvent {

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

    /**
     * Reconstructs an EntrantEvent from a Bundle created by {@link #toBundle()}.
     */
    public static EntrantEvent fromBundle(Bundle b) {
        if (b == null) return new EntrantEvent();
        EntrantEvent e = new EntrantEvent();
        e.setUserId(b.getString("userId"));
        e.setUserName(b.getString("userName"));
        e.setEmail(b.getString("email"));
        e.setStatus(b.getString("status"));
        if (b.containsKey("registeredAt"))
            e.setRegisteredAt(new Timestamp(new java.util.Date(b.getLong("registeredAt"))));
        if (b.containsKey("waitlistedAt"))
            e.setWaitlistedAt(new Timestamp(new java.util.Date(b.getLong("waitlistedAt"))));
        if (b.containsKey("invitedAt"))
            e.setInvitedAt(new Timestamp(new java.util.Date(b.getLong("invitedAt"))));
        if (b.containsKey("acceptedAt"))
            e.setAcceptedAt(new Timestamp(new java.util.Date(b.getLong("acceptedAt"))));
        if (b.containsKey("cancelledAt"))
            e.setCancelledAt(new Timestamp(new java.util.Date(b.getLong("cancelledAt"))));
        if (b.getBoolean("hasLocation", false)) {
            e.setLocation(new GeoPoint(b.getDouble("lat"), b.getDouble("lng")));
        }
        return e;
    }

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

    // Helpful status checks

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

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

    /**
     * Packs this object into a Bundle for safe Fragment argument passing.
     */
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("userId", userId);
        b.putString("userName", userName);
        b.putString("email", email);
        b.putString("status", status);
        if (registeredAt != null) b.putLong("registeredAt", registeredAt.toDate().getTime());
        if (waitlistedAt != null) b.putLong("waitlistedAt", waitlistedAt.toDate().getTime());
        if (invitedAt != null) b.putLong("invitedAt", invitedAt.toDate().getTime());
        if (acceptedAt != null) b.putLong("acceptedAt", acceptedAt.toDate().getTime());
        if (cancelledAt != null) b.putLong("cancelledAt", cancelledAt.toDate().getTime());
        if (location != null) {
            b.putDouble("lat", location.getLatitude());
            b.putDouble("lng", location.getLongitude());
            b.putBoolean("hasLocation", true);
        }
        return b;
    }
}
