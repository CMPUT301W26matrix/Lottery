package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing an event.
 *
 * Recommended Firestore path:
 * events/{eventId}
 *
 * This model stores:
 * - core event metadata
 * - registration window
 * - draw timing
 * - capacity and waiting list settings
 * - poster / QR assets
 * - organizer ownership
 * - timestamps for creation and last update
 *
 * Notes:
 * - Keep field names consistent with Firestore documents.
 * - Use Timestamp for consistency with Firestore and other models.
 */
public class Event {

    /**
     * Unique identifier for the event.
     * Usually also used as the Firestore document ID.
     */
    private String eventId;

    /**
     * Human-readable event title.
     */
    private String title;

    /**
     * Detailed event description.
     */
    private String details;

    /**
     * Scheduled start date/time of the event.
     */
    private Timestamp scheduledDateTime;

    /**
     * Scheduled end date/time of the event.
     */
    private Timestamp eventEndDate;

    /**
     * Registration opening time.
     */
    private Timestamp registrationStartDate;

    /**
     * Registration deadline.
     */
    private Timestamp registrationDeadline;

    /**
     * Lottery draw date/time.
     */
    private Timestamp drawDate;

    /**
     * Maximum number of accepted participants allowed.
     */
    private Integer maxCapacity;

    /**
     * Optional waiting list limit.
     * null means unlimited.
     */
    private Integer waitingListLimit;

    /**
     * Whether geolocation verification is required.
     */
    private boolean requireLocation;

    /**
     * URI or download URL of the poster image.
     */
    private String posterUri;

    /**
     * QR code payload/content for event registration or lookup.
     */
    private String qrCodeContent;

    /**
     * ID of the organizer who created/owns this event.
     */
    private String organizerId;

    /**
     * When this event document was first created.
     */
    private Timestamp createdAt;

    /**
     * When this event document was last updated.
     */
    private Timestamp updatedAt;

    /**
     * Default constructor required for Firestore.
     */
    public Event() {
    }

    /**
     * Full constructor.
     *
     * @param eventId               event ID
     * @param title                 event title
     * @param details               event description
     * @param scheduledDateTime     event start time
     * @param eventEndDate          event end time
     * @param registrationStartDate registration opening time
     * @param registrationDeadline  registration closing time
     * @param drawDate              lottery draw time
     * @param maxCapacity           max accepted participants
     * @param waitingListLimit      optional waitlist limit, null = unlimited
     * @param requireLocation       whether geolocation is required
     * @param posterUri             poster URI
     * @param qrCodeContent         QR content
     * @param organizerId           organizer user ID
     * @param createdAt             creation timestamp
     * @param updatedAt             last update timestamp
     */
    public Event(String eventId,
                 String title,
                 String details,
                 Timestamp scheduledDateTime,
                 Timestamp eventEndDate,
                 Timestamp registrationStartDate,
                 Timestamp registrationDeadline,
                 Timestamp drawDate,
                 Integer maxCapacity,
                 Integer waitingListLimit,
                 boolean requireLocation,
                 String posterUri,
                 String qrCodeContent,
                 String organizerId,
                 Timestamp createdAt,
                 Timestamp updatedAt) {
        this.eventId = eventId;
        this.title = title;
        this.details = details;
        this.scheduledDateTime = scheduledDateTime;
        this.eventEndDate = eventEndDate;
        this.registrationStartDate = registrationStartDate;
        this.registrationDeadline = registrationDeadline;
        this.drawDate = drawDate;
        this.maxCapacity = maxCapacity;
        this.waitingListLimit = waitingListLimit;
        this.requireLocation = requireLocation;
        this.posterUri = posterUri;
        this.qrCodeContent = qrCodeContent;
        this.organizerId = organizerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Convenience constructor for newly created events.
     * Automatically sets createdAt and updatedAt to now.
     */
    public Event(String eventId,
                 String title,
                 String details,
                 Timestamp scheduledDateTime,
                 Timestamp eventEndDate,
                 Timestamp registrationStartDate,
                 Timestamp registrationDeadline,
                 Timestamp drawDate,
                 Integer maxCapacity,
                 Integer waitingListLimit,
                 boolean requireLocation,
                 String posterUri,
                 String qrCodeContent,
                 String organizerId) {
        this(eventId,
                title,
                details,
                scheduledDateTime,
                eventEndDate,
                registrationStartDate,
                registrationDeadline,
                drawDate,
                maxCapacity,
                waitingListLimit,
                requireLocation,
                posterUri,
                qrCodeContent,
                organizerId,
                Timestamp.now(),
                Timestamp.now());
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
        touch();
    }

    public Timestamp getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(Timestamp scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
        touch();
    }

    public Timestamp getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Timestamp eventEndDate) {
        this.eventEndDate = eventEndDate;
        touch();
    }

    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
        touch();
    }

    public Timestamp getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(Timestamp registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
        touch();
    }

    public Timestamp getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(Timestamp drawDate) {
        this.drawDate = drawDate;
        touch();
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
        touch();
    }

    public Integer getWaitingListLimit() {
        return waitingListLimit;
    }

    public void setWaitingListLimit(Integer waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
        touch();
    }

    public boolean isRequireLocation() {
        return requireLocation;
    }

    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
        touch();
    }

    public String getPosterUri() {
        return posterUri;
    }

    public void setPosterUri(String posterUri) {
        this.posterUri = posterUri;
        touch();
    }

    public String getQrCodeContent() {
        return qrCodeContent;
    }

    public void setQrCodeContent(String qrCodeContent) {
        this.qrCodeContent = qrCodeContent;
        touch();
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
        touch();
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns true if registration has opened.
     */
    public boolean hasRegistrationStarted() {
        return registrationStartDate != null
                && registrationStartDate.toDate().before(new java.util.Date());
    }

    /**
     * Returns true if registration is still open.
     */
    public boolean isRegistrationOpen() {
        java.util.Date now = new java.util.Date();

        boolean afterStart = registrationStartDate == null || !registrationStartDate.toDate().after(now);
        boolean beforeDeadline = registrationDeadline == null || !registrationDeadline.toDate().before(now);

        return afterStart && beforeDeadline;
    }

    /**
     * Returns true if the registration deadline has passed.
     */
    public boolean isRegistrationClosed() {
        return registrationDeadline != null
                && registrationDeadline.toDate().before(new java.util.Date());
    }

    /**
     * Returns true if the draw time has passed.
     */
    public boolean isDrawTimeReached() {
        return drawDate != null
                && !drawDate.toDate().after(new java.util.Date());
    }

    /**
     * Returns true if the event has ended.
     */
    public boolean isEventEnded() {
        return eventEndDate != null
                && eventEndDate.toDate().before(new java.util.Date());
    }

    /**
     * Returns true if the waiting list has no explicit limit.
     */
    public boolean hasUnlimitedWaitingList() {
        return waitingListLimit == null;
    }

    /**
     * Returns true if the waiting list limit is enabled.
     */
    public boolean hasWaitingListLimit() {
        return waitingListLimit != null;
    }

    /**
     * Returns true if max capacity is valid and positive.
     */
    public boolean hasCapacityLimit() {
        return maxCapacity != null && maxCapacity > 0;
    }

    /**
     * Updates the updatedAt timestamp to now.
     */
    public void touch() {
        this.updatedAt = Timestamp.now();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }
}