package com.example.lottery;
/**
 * Activity to display the details of a specific event and handle registration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch the event record from Firestore using the supplied event ID.</li>
 *   <li>Render the poster, title, schedule, deadline, and description.</li>
 *   <li>Surface organizer-configured requirements such as geolocation.</li>
 *   <li>Enforce US 02.03.01: Disables registration when waiting list is full.</li>
 *   <li>Writes registration data to Firestore 'entrants' sub-collection (US 02.01.01).</li>
 *   <li>Keep the custom bottom navigation active on the details screen.</li>
 * </ul>
 * </p>
 */
public class User {
    private String device_info;
    private String password;
    private String phone_number;
    private String user_id;
    private String user_role;
    private String username;

    public User(String device_info, String password, String phone_number, String userId, String user_role, String username) {
        this.device_info = device_info;
        this.password = password;
        this.phone_number = phone_number;
        this.user_id = user_id;
        this.user_role = user_role;
        this.username = username;
    }

    public User(){

    }

    public String getDevice_info() {
        return device_info;
    }

    public void setDevice_info(String device_info) {
        this.device_info = device_info;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

