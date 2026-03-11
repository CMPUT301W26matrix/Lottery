package com.example.lottery.model;

/**
 * User model
 * Represents an entrant/user in the system.
 * This class stores basic personal information.
 */
public class User {

    private String name;
    private String email;
    private String phoneNumber;

    // Empty constructor required for Firestore
    public User() {
    }

    public User(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
