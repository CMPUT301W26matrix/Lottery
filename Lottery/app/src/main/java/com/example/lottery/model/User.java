package com.example.lottery.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

/**
 * Model class representing a user in the system.
 * <p>
 * Target Firestore path:
 * users/{userId}
 */
public class User {

    /**
     * Unique user identifier.
     */
    private String userId;

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
    private String role;

    /**
     * Geographic location of the user.
     */
    private GeoPoint location;

    /**
     * Whether this user has geolocation enabled.
     */
    private boolean geolocationEnabled;

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
     * List of user interests for event recommendations.
     */
    private List<String> interests;

    /**
     * Base64 encoded profile image.
     */
    private String profileImageBase64;

    /**
     * Default constructor required for Firestore.
     */
    public User() {
        this.role = "ENTRANT";
        this.notificationsEnabled = true;
    }

    /**
     * Convenience constructor for basic profile information with userId.
     */
    public User(String userId, String username, String email, String phone) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Full constructor.
     */
    public User(String userId,
                String deviceId,
                String email,
                String phone,
                String username,
                String role,
                GeoPoint location,
                boolean notificationsEnabled,
                Timestamp createdAt,
                Timestamp updatedAt,
                List<String> interests) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.role = role == null ? "ENTRANT" : role;
        this.location = location;
        this.notificationsEnabled = notificationsEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.interests = interests;
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the email address of the user.
     *
     * @return The user's email.
     */
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? "ENTRANT" : role;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public boolean isGeolocationEnabled() {
        return geolocationEnabled;
    }

    public void setGeolocationEnabled(boolean geolocationEnabled) {
        this.geolocationEnabled = geolocationEnabled;
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

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    // Helpers

    public boolean isEntrant() {
        return "ENTRANT".equalsIgnoreCase(role);
    }

    public boolean isOrganizer() {
        return "ORGANIZER".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
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
}
