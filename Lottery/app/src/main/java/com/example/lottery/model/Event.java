package com.example.lottery.model;

import java.util.Date;

/**
 * Model class representing an Event.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Encapsulates all metadata for an event, including titles, dates, and descriptions.</li>
 *   <li>Stores references to promotional assets like poster URIs and QR code content.</li>
 *   <li>Acts as a Data Transfer Object (DTO) for Firebase Firestore serialization.</li>
 * </ul>
 * </p>
 *
 * <p>Satisfies requirements for:
 * US 02.01.01: Event creation with promotional QR code.
 * US 02.01.04: Registration deadline management.
 * US 02.04.01: Event poster support.
 * US 02.02.03: Geolocation requirement toggle.
 * US 02.02.02: Waiting List Limit.
 * </p>
 */
public class Event {
    /**
     * Unique identifier for the event, used as the Firestore document ID.
     */
    private String eventId;
    /**
     * The title of the event.
     */
    private String title;
    /**
     * The scheduled start date and time for the event.
     */
    private Date scheduledDateTime;
    /**
     * The scheduled end date and time for the event.
     */
    private Date eventEndDate;
    /**
     * The date and time when registration opens for the event.
     */
    private Date registrationStartDate;
    /**
     * The deadline by which entrants must register for the event.
     */
    private Date registrationDeadline;
    /**
     * The date and time when the lottery draw is held for the event.
     */
    private Date drawDate;
    /**
     * The maximum number of participants allowed for the event.
     */
    private Integer maxCapacity;
    /**
     * A detailed description of the event.
     */
    private String details;
    /**
     * URI or download URL pointing to the event's promotional poster image.
     */
    private String posterUri;
    /**
     * The content encoded within the event's promotional QR code.
     */
    private String qrCodeContent;
    /**
     * Unique identifier of the organizer who created the event.
     */
    private String organizerId;
    /**
     * Whether geolocation verification is required for this event.
     */
    private boolean requireLocation;
    /**
     * US 02.02.02: Optional limit for the waiting list. null means unlimited.
     */
    private Integer waitingListLimit;

    /**
     * Default no-argument constructor required for Firebase Firestore serialization.
     */
    public Event() {
    }

    /**
     * Constructs a new Event with all metadata.
     *
     * @param eventId               The unique ID of the event.
     * @param title                 The title of the event.
     * @param scheduledDateTime     The date and time the event starts.
     * @param eventEndDate          The date and time the event ends.
     * @param registrationStartDate The date and time registration opens.
     * @param registrationDeadline  The deadline for entrant registration.
     * @param drawDate              The date and time when the event draw is held.
     * @param maxCapacity           The maximum participant capacity.
     * @param details               The event description.
     * @param posterUri             The URI of the event poster.
     * @param qrCodeContent         The content of the promotional QR code.
     * @param organizerId           The ID of the event organizer.
     * @param requireLocation       Whether geolocation verification is required.
     * @param waitingListLimit      The optional limit for the waiting list.
     */
    public Event(String eventId, String title, Date scheduledDateTime, Date eventEndDate,
                 Date registrationStartDate, Date registrationDeadline, Date drawDate,
                 Integer maxCapacity, String details, String posterUri, String qrCodeContent,
                 String organizerId, boolean requireLocation, Integer waitingListLimit) {
        this.eventId = eventId;
        this.title = title;
        this.scheduledDateTime = scheduledDateTime;
        this.eventEndDate = eventEndDate;
        this.registrationStartDate = registrationStartDate;
        this.registrationDeadline = registrationDeadline;
        this.drawDate = drawDate;
        this.maxCapacity = maxCapacity;
        this.details = details;
        this.posterUri = posterUri;
        this.qrCodeContent = qrCodeContent;
        this.organizerId = organizerId;
        this.requireLocation = requireLocation;
        this.waitingListLimit = waitingListLimit;
    }

    /**
     * @return The unique identifier of the event.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @param eventId The unique identifier to set for the event.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * @return The title of the event.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title to set for the event.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The scheduled date and time of the event.
     */
    public Date getScheduledDateTime() {
        return scheduledDateTime;
    }

    /**
     * @param scheduledDateTime The scheduled date and time to set for the event.
     */
    public void setScheduledDateTime(Date scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    /**
     * @return The scheduled end date and time of the event.
     */
    public Date getEventEndDate() {
        return eventEndDate;
    }

    /**
     * @param eventEndDate The scheduled end date and time to set for the event.
     */
    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    /**
     * @return The date and time when registration opens for the event.
     */
    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * @param registrationStartDate The date and time when registration opens to set for the event.
     */
    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * @return The registration deadline for the event.
     */
    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    /**
     * @param registrationDeadline The registration deadline to set for the event.
     */
    public void setRegistrationDeadline(Date registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /**
     * @return The date and time when the lottery draw is held for the event.
     */
    public Date getDrawDate() {
        return drawDate;
    }

    /**
     * @param drawDate The date and time when the lottery draw is held to set for the event.
     */
    public void setDrawDate(Date drawDate) {
        this.drawDate = drawDate;
    }

    /**
     * @return The maximum participant capacity.
     */
    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @param maxCapacity The maximum participant capacity to set.
     */
    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * @return The detailed description of the event.
     */
    public String getDetails() {
        return details;
    }

    /**
     * @param details The detailed description to set.
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * @return The URI of the event poster.
     */
    public String getPosterUri() {
        return posterUri;
    }

    /**
     * @param posterUri The URI of the event poster to set.
     */
    public void setPosterUri(String posterUri) {
        this.posterUri = posterUri;
    }

    /**
     * @return The content of the promotional QR code.
     */
    public String getQrCodeContent() {
        return qrCodeContent;
    }

    /**
     * @param qrCodeContent The QR code content to set.
     */
    public void setQrCodeContent(String qrCodeContent) {
        this.qrCodeContent = qrCodeContent;
    }

    /**
     * @return The identifier of the event organizer.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * @param organizerId The identifier of the event organizer to set.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * @return Whether geolocation verification is required for this event.
     */
    public boolean isRequireLocation() {
        return requireLocation;
    }

    /**
     * @param requireLocation Sets whether geolocation verification is required.
     */
    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
    }

    /**
     * @return The optional limit for the waiting list. null means unlimited.
     */
    public Integer getWaitingListLimit() {
        return waitingListLimit;
    }

    /**
     * @param waitingListLimit The optional limit to set for the waiting list.
     */
    public void setWaitingListLimit(Integer waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
    }
}
