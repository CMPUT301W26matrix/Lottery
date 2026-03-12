package com.example.lottery;

import com.google.firebase.Timestamp;

public class Entrant {
    private Timestamp accepted_timestamp;
    private Timestamp cancelled_timestamp;
    private String event_id;
    private String user_name;

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    private Timestamp invitation_timestamp;
    private String referrer_id;
    private Timestamp register_timestamp;
    private String user_id;
    private String entrant_status;
    private com.google.firebase.firestore.GeoPoint location;

    public com.google.firebase.firestore.GeoPoint getLocation() {
        return location;
    }

    public void setLocation(com.google.firebase.firestore.GeoPoint location) {
        this.location = location;
    }

    public String getEntrant_status() {
        return entrant_status;
    }

    public void setEntrant_status(String entrant_status) {
        this.entrant_status = entrant_status;
    }

    public Entrant() {
    }

    public Entrant(Timestamp accepted_timestamp, Timestamp cancelled_timestamp, String event_id, Timestamp invitation_timestamp, String referrer_id, Timestamp register_timestamp, String user_id, String entrant_status, com.google.firebase.firestore.GeoPoint location, String user_name) {
        this.accepted_timestamp = accepted_timestamp;
        this.cancelled_timestamp = cancelled_timestamp;
        this.event_id = event_id;
        this.invitation_timestamp = invitation_timestamp;
        this.referrer_id = referrer_id;
        this.register_timestamp = register_timestamp;
        this.user_id = user_id;
        this.entrant_status = entrant_status;
        this.location = location;
        this.user_name = user_name;
    }

    public Timestamp getAccepted_timestamp() {
        return accepted_timestamp;
    }

    public void setAccepted_timestamp(Timestamp accepted_timestamp) {
        this.accepted_timestamp = accepted_timestamp;
    }

    public Timestamp getCancelled_timestamp() {
        return cancelled_timestamp;
    }

    public void setCancelled_timestamp(Timestamp cancelled_timestamp) {
        this.cancelled_timestamp = cancelled_timestamp;
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }

    public Timestamp getInvitation_timestamp() {
        return invitation_timestamp;
    }

    public void setInvitation_timestamp(Timestamp invitation_timestamp) {
        this.invitation_timestamp = invitation_timestamp;
    }

    public String getReferrer_id() {
        return referrer_id;
    }

    public void setReferrer_id(String referrer_id) {
        this.referrer_id = referrer_id;
    }

    public Timestamp getRegister_timestamp() {
        return register_timestamp;
    }

    public void setRegister_timestamp(Timestamp register_timestamp) {
        this.register_timestamp = register_timestamp;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
