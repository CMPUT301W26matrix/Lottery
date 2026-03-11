package com.example.lottery;

public class Entrant {
    private String accepted_timestamp;
    private String cancelled_timestamp;
    private String event_id;
    private String invitation_timestamp;
    private String referrer_id;
    private String register_timestamp;
    private String user_id;
    private String entrant_status;

    public String getEntrant_status() {
        return entrant_status;
    }

    public void setEntrant_status(String entrant_status) {
        this.entrant_status = entrant_status;
    }

    public Entrant() {
    }

    public Entrant(String accepted_timestamp, String cancelled_timestamp, String event_id, String invitation_timestamp, String referrer_id, String register_timestamp, String user_id, String entrant_status) {
        this.accepted_timestamp = accepted_timestamp;
        this.cancelled_timestamp = cancelled_timestamp;
        this.event_id = event_id;
        this.invitation_timestamp = invitation_timestamp;
        this.referrer_id = referrer_id;
        this.register_timestamp = register_timestamp;
        this.user_id = user_id;
        this.entrant_status = entrant_status;
    }

    public String getAccepted_timestamp() {
        return accepted_timestamp;
    }

    public void setAccepted_timestamp(String accepted_timestamp) {
        this.accepted_timestamp = accepted_timestamp;
    }

    public String getCancelled_timestamp() {
        return cancelled_timestamp;
    }

    public void setCancelled_timestamp(String cancelled_timestamp) {
        this.cancelled_timestamp = cancelled_timestamp;
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }

    public String getInvitation_timestamp() {
        return invitation_timestamp;
    }

    public void setInvitation_timestamp(String invitation_timestamp) {
        this.invitation_timestamp = invitation_timestamp;
    }

    public String getReferrer_id() {
        return referrer_id;
    }

    public void setReferrer_id(String referrer_id) {
        this.referrer_id = referrer_id;
    }

    public String getRegister_timestamp() {
        return register_timestamp;
    }

    public void setRegister_timestamp(String register_timestamp) {
        this.register_timestamp = register_timestamp;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
