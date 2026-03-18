package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing a user in the system.
 *
 * Recommended Firestore path:
 * users/{userId}
 *
 * This model stores:
 * - user identity
 * - basic profile information
 * - user role
 * - notification preference
 * - creation/update timestamps
 */
public class User {

    /**
     * Unique user identifier.
     * Usually also used as the Firestore document ID.
     */
    private String userId;

    /**
     * Display name of the user.
     */
    private String name;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * Phone number of the user.
     * Stored as "phone" in Firestore conceptually, but we keep backward-compatible accessors too.
     */
    private String phone;

    /**
     * Role of the user in the system.
     */
    private Role role;

    /**
     * Whether this user has notifications enabled.
     */
    private boolean notificationsEnabled;

    /**
     * When the user record was first created.
     */
    private Timestamp createdAt;

    /**
     * When the user record was last updated.
     */
    private Timestamp updatedAt;

    /**
     * Default constructor required for Firestore.
     */
    public User() {
        this.role = Role.ENTRANT;
        this.notificationsEnabled = true;
    }

    /**
     * Convenience constructor for a basic entrant user.
     *
     * @param name  user name
     * @param email user email
     * @param phone user phone
     */
    public User(String name, String email, String phone) {
        this(null, name, email, phone, Role.ENTRANT, true, Timestamp.now(), Timestamp.now());
    }

    /**
     * Convenience constructor with explicit user ID.
     *
     * @param userId user ID
     * @param name   user name
     * @param email  user email
     * @param phone  user phone
     */
    public User(String userId, String name, String email, String phone) {
        this(userId, name, email, phone, Role.ENTRANT, true, Timestamp.now(), Timestamp.now());
    }

    /**
     * Full constructor.
     *
     * @param userId               user ID
     * @param name                 user display name
     * @param email                user email
     * @param phone                user phone
     * @param role                 user role
     * @param notificationsEnabled whether notifications are enabled
     * @param createdAt            creation timestamp
     * @param updatedAt            last update timestamp
     */
    public User(String userId,
                String name,
                String email,
                String phone,
                Role role,
                boolean notificationsEnabled,
                Timestamp createdAt,
                Timestamp updatedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role == null ? Role.ENTRANT : role;
        this.notificationsEnabled = notificationsEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        touch();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        touch();
    }

    /**
     * Preferred phone getter.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Preferred phone setter.
     */
    public void setPhone(String phone) {
        this.phone = phone;
        touch();
    }

    /**
     * Backward-compatible alias for older code still using phoneNumber.
     */
    public String getPhoneNumber() {
        return phone;
    }

    /**
     * Backward-compatible alias for older code still using phoneNumber.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
        touch();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role == null ? Role.ENTRANT : role;
        touch();
    }

    /**
     * Backward-compatible helper if some older code still uses String roles.
     */
    public void setRoleFromString(String role) {
        if (role == null) {
            this.role = Role.ENTRANT;
            touch();
            return;
        }

        switch (role.trim().toLowerCase()) {
            case "admin":
                this.role = Role.ADMIN;
                break;
            case "organizer":
                this.role = Role.ORGANIZER;
                break;
            case "entrant":
            default:
                this.role = Role.ENTRANT;
                break;
        }
        touch();
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
        touch();
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

    /**
     * Returns true if the user is an entrant.
     */
    public boolean isEntrant() {
        return role == Role.ENTRANT;
    }

    /**
     * Returns true if the user is an organizer.
     */
    public boolean isOrganizer() {
        return role == Role.ORGANIZER;
    }

    /**
     * Returns true if the user is an admin.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Updates the updatedAt timestamp to now.
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