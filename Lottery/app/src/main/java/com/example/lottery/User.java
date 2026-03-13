package com.example.lottery;

/**
 * Data model representing a user account in the lottery system.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store user profile information (username, phone, device info, role).</li>
 *   <li>Linked to Firebase Auth via userId; password is managed by Firebase Auth and never stored here.</li>
 * </ul>
 * </p>
 */
public class User {
    private String device_info;
    private String phone_number;
    private String user_id;
    private String user_role;
    private String username;

    public User(String device_info, String phone_number, String userId, String user_role, String username) {
        this.device_info = device_info;
        this.phone_number = phone_number;
        this.user_id = userId;
        this.user_role = user_role;
        this.username = username;
    }

    public User() {

    }

    public String getDevice_info() {
        return device_info;
    }

    public void setDevice_info(String device_info) {
        this.device_info = device_info;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_role() {
        return user_role;
    }

    public void setUser_role(String user_role) {
        this.user_role = user_role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

