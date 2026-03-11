package com.example.lottery;

public class Organizer extends User{
    public Organizer(String device_info, String password, String phone_number, String userId, String username) {
        super(device_info, password, phone_number, userId, "organizer", username);
    }
}
