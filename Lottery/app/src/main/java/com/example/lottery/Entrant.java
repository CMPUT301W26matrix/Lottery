package com.example.lottery;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

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
public class Entrant {
    private String entrant_name;
    private String entrant_id;
    private Timestamp cancelled_time;
    private Timestamp invited_time;
    private Timestamp registration_time;
    private Timestamp signed_up_time;
    private com.google.firebase.firestore.GeoPoint location;

    public Entrant() {
    }

    /**
     * constructor for cancelled_collections entrants
     *
     * @param entrant_name      entrant's name
     * @param location          entrant's location
     * @param entrant_id        entrant's id
     * @param cancelled_time    entrant's cancelled time
     * @param invited_time      entrant's invited time
     * @param registration_time entrant's registration time
     */
    public Entrant(String entrant_name, GeoPoint location, String entrant_id, Timestamp cancelled_time, Timestamp invited_time, Timestamp registration_time) {
        this.entrant_name = entrant_name;
        this.location = location;
        this.entrant_id = entrant_id;
        this.cancelled_time = cancelled_time;
        this.invited_time = invited_time;
        this.registration_time = registration_time;
    }

    /**
     * constructor for signed_up_collections entrants
     *
     * @param entrant_name      entrant's name
     * @param location          entrant's location
     * @param entrant_id        entrant's id
     * @param signed_up_time    entrant's cancelled time
     * @param invited_time      entrant's invited time
     * @param registration_time entrant's registration time
     */
    public Entrant(String entrant_name, String entrant_id, Timestamp invited_time, Timestamp registration_time, GeoPoint location, Timestamp signed_up_time) {
        this.entrant_name = entrant_name;
        this.entrant_id = entrant_id;
        this.invited_time = invited_time;
        this.registration_time = registration_time;
        this.location = location;
        this.signed_up_time = signed_up_time;
    }

    /**
     * constructor for waited_listed_collections entrants
     *
     * @param entrant_name      entrant's name
     * @param location          entrant's location
     * @param entrant_id        entrant's id
     * @param registration_time entrant's registration time
     */
    public Entrant(String entrant_name, String entrant_id, Timestamp registration_time, GeoPoint location) {
        this.entrant_name = entrant_name;
        this.entrant_id = entrant_id;
        this.registration_time = registration_time;
        this.location = location;
    }

    /**
     *
     * @param invited_time      timestamp that entrants get invitation
     * @param entrant_name      entrant's name
     * @param location          entrant's location
     * @param entrant_id        entrant's id
     * @param registration_time entrant's registration time
     */
    public Entrant(Timestamp invited_time, String entrant_name, String entrant_id, Timestamp registration_time, GeoPoint location) {
        this.invited_time = invited_time;
        this.entrant_name = entrant_name;
        this.entrant_id = entrant_id;
        this.registration_time = registration_time;
        this.location = location;
    }

    public Timestamp getSigned_up_time() {
        return signed_up_time;
    }

    public void setSigned_up_time(Timestamp signed_up_time) {
        this.signed_up_time = signed_up_time;
    }

    public com.google.firebase.firestore.GeoPoint getLocation() {
        return location;
    }

    public void setLocation(com.google.firebase.firestore.GeoPoint location) {
        this.location = location;
    }

    public String getEntrant_name() {
        return entrant_name;
    }

    public void setEntrant_name(String entrant_name) {
        this.entrant_name = entrant_name;
    }

    public String getEntrant_id() {
        return entrant_id;
    }

    public void setEntrant_id(String entrant_id) {
        this.entrant_id = entrant_id;
    }

    public Timestamp getCancelled_time() {
        return cancelled_time;
    }

    public void setCancelled_time(Timestamp cancelled_time) {
        this.cancelled_time = cancelled_time;
    }

    public Timestamp getInvited_time() {
        return invited_time;
    }

    public void setInvited_time(Timestamp invited_time) {
        this.invited_time = invited_time;
    }

    public Timestamp getRegistration_time() {
        return registration_time;
    }

    public void setRegistration_time(Timestamp registration_time) {
        this.registration_time = registration_time;
    }
}
