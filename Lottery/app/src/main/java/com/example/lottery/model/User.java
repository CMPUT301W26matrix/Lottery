package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

/**
 * Model class representing a user in the system.
 *
 * Target Firestore path:
 * users/{uid}
 */
public class User {

    /**
     * Unique user identifier (Firebase Auth UID).
     */
    private String uid;

    /**
     * Optional device identifier.
     */
    private String deviceId;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * Phone number of the user.
     */
    private String phone;

    /**
     * Display name or username of the user.
     */
    private String username;

    /**
     * Role of the user in the system.
     */
    private Role role;

    /**
     * Geographic location of the user.
     */
    private GeoPoint location;

    /**
     * Whether this user has notifications enabled.
     */
    private boolean notificationsEnabled;

    /**
     * When the user record was first created.
     */
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor required for Firestore.
     */
    public User() {
        this.role = Role.ENTRANT;
        this.notificationsEnabled = true;
    }

    /**
     * Convenience constructor for basic profile information.
     */
    public User(String username, String email, String phone) {
        this();
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Convenience constructor for basic profile information with UID.
     */
    public User(String uid, String username, String email, String phone) {
        this();
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Full constructor.
     */
    public User(String uid,
                String deviceId,
                String email,
                String phone,
                String username,
                Role role,
                GeoPoint location,
                boolean notificationsEnabled,
                Timestamp createdAt,
                Timestamp updatedAt) {
        this.uid = uid;
        this.deviceId = deviceId;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.role = role == null ? Role.ENTRANT : role;
        this.location = location;
        this.notificationsEnabled = notificationsEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    /** Backward compatibility alias for older code using userId */
    public String getUserId() {
        return uid;
    }

    public void setUserId(String userId) {
        this.uid = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Backward compatibility alias for older code using phoneNumber */
    public String getPhoneNumber() {
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** Backward compatibility alias for older code using name */
    public String getName() {
        return username;
    }

    public void setName(String name) {
        this.username = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role == null ? Role.ENTRANT : role;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helpers

    public boolean isEntrant() {
        return role == Role.ENTRANT;
    }

    public boolean isOrganizer() {
        return role == Role.ORGANIZER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Updates the updatedAt timestamp. Should be called explicitly before Firestore writes.
     */
    public void touch() {
        this.updatedAt = Timestamp.now();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }

    /**
     * Enum representing valid user roles.
     */
    public enum Role {
        ENTRANT,
        ORGANIZER,
        ADMIN
    }
}
