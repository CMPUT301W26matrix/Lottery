package com.example.lottery.model;

import java.util.Date;

/**
 * Model class representing an Event.
 * This class encapsulates all data related to an event, including its identity,
 * scheduling, registration details, and promotional content.
 * It is designed to be compatible with Firebase Firestore serialization.
 *
 * Satisfies requirements for:
 * US 02.01.01: Event creation with promotional QR code.
 * US 02.01.04: Registration deadline management.
 */
public class Event {
    /** Unique identifier for the event, used as the Firestore document ID. */
    private String eventId;
    /** The title of the event. */
    private String title;
    /** The scheduled start date and time for the event. */
    private Date scheduledDateTime;
    /** The deadline by which entrants must register for the event. */
    private Date registrationDeadline;
    /** The maximum number of participants allowed for the event. */
    private Integer maxCapacity;
    /** A detailed description of the event. */
    private String details;
    /** URI pointing to the event's promotional poster image. */
    private String posterUri;
    /** The content encoded within the event's promotional QR code. */
    private String qrCodeContent;
    /** Unique identifier of the organizer who created the event. */
    private String organizerId;

    /**
     * Default no-argument constructor required for Firebase Firestore serialization.
     */
    public Event() {}

    /**
     * Constructs a new Event with all required fields.
     *
     * @param eventId              The unique ID of the event.
     * @param title                The title of the event.
     * @param scheduledDateTime    The date and time the event starts.
     * @param registrationDeadline The deadline for entrant registration.
     * @param maxCapacity          The maximum participant capacity.
     * @param details              The event description.
     * @param posterUri            The URI of the event poster.
     * @param qrCodeContent        The content of the promotional QR code.
     * @param organizerId          The ID of the event organizer.
     */
    public Event(String eventId, String title, Date scheduledDateTime, Date registrationDeadline,
                 Integer maxCapacity, String details, String posterUri, String qrCodeContent, String organizerId) {
        this.eventId = eventId;
        this.title = title;
        this.scheduledDateTime = scheduledDateTime;
        this.registrationDeadline = registrationDeadline;
        this.maxCapacity = maxCapacity;
        this.details = details;
        this.posterUri = posterUri;
        this.qrCodeContent = qrCodeContent;
        this.organizerId = organizerId;
    }

    /** @return The unique identifier of the event. */
    public String getEventId() { return eventId; }
    /** @param eventId The unique identifier to set for the event. */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return The title of the event. */
    public String getTitle() { return title; }
    /** @param title The title to set for the event. */
    public void setTitle(String title) { this.title = title; }

    /** @return The scheduled date and time of the event. */
    public Date getScheduledDateTime() { return scheduledDateTime; }
    /** @param scheduledDateTime The scheduled date and time to set for the event. */
    public void setScheduledDateTime(Date scheduledDateTime) { this.scheduledDateTime = scheduledDateTime; }

    /** @return The registration deadline for the event. */
    public Date getRegistrationDeadline() { return registrationDeadline; }
    /** @param registrationDeadline The registration deadline to set for the event. */
    public void setRegistrationDeadline(Date registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    /** @return The maximum participant capacity. */
    public Integer getMaxCapacity() { return maxCapacity; }
    /** @param maxCapacity The maximum participant capacity to set. */
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    /** @return The detailed description of the event. */
    public String getDetails() { return details; }
    /** @param details The detailed description to set. */
    public void setDetails(String details) { this.details = details; }

    /** @return The URI of the event poster. */
    public String getPosterUri() { return posterUri; }
    /** @param posterUri The URI of the event poster to set. */
    public void setPosterUri(String posterUri) { this.posterUri = posterUri; }

    /** @return The content of the promotional QR code. */
    public String getQrCodeContent() { return qrCodeContent; }
    /** @param qrCodeContent The QR code content to set. */
    public void setQrCodeContent(String qrCodeContent) { this.qrCodeContent = qrCodeContent; }

    /** @return The identifier of the event organizer. */
    public String getOrganizerId() { return organizerId; }
    /** @param organizerId The identifier of the event organizer to set. */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
}
