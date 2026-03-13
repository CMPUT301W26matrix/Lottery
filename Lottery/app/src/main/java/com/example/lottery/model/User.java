package com.example.lottery.model;

/**
 * User model class representing an entrant or user in the system.
 * This class stores basic personal information including name, email, and phone number.
 */
public class User {

    private String userId;
    private String name;
    private String email;
    private String phoneNumber;

    /**
     * Default constructor required for Firebase Firestore serialization.
     */
    public User() {
    }

    /**
     * Constructs a new User with the specified details.
     *
     * @param name        The name of the user.
     * @param email       The email address of the user.
     * @param phoneNumber The phone number of the user.
     */
    public User(String name, String email, String phoneNumber) {
        this(null, name, email, phoneNumber);
    }

    public User(String userId, String name, String email, String phoneNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the ID of the user.
     *
     * @return The user's ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the name of the user.
     *
     * @return The user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email address of the user.
     *
     * @return The user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the phone number of the user.
     *
     * @return The user's phone number.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }
}
