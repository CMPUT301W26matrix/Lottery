package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing an event.
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

    private String eventId;
    private String title;
    private String details;
    private String organizerId;
    private Integer capacity;
    private Integer waitingListLimit;
    private String qrCodeContent;
    private String status; // open, closed, cancelled
    private String posterUri;
    private String category; // academic, social, sports, music, other
    private Timestamp scheduledDateTime;
    private Timestamp registrationDeadline;
    private Timestamp drawDate;
    private boolean requireLocation;
    private boolean isPrivate;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor required for Firestore.
     */
    public Event() {
        this.status = "open";
        this.category = "Other";
        this.isPrivate = false;
    }

    /**
     * Full constructor for target Firestore event document.
     */
    public Event(String eventId,
                 String title,
                 String details,
                 String organizerId,
                 Integer capacity,
                 Integer waitingListLimit,
                 String qrCodeContent,
                 String status,
                 String posterUri,
                 String category,
                 Timestamp scheduledDateTime,
                 Timestamp registrationDeadline,
                 Timestamp drawDate,
                 boolean requireLocation,
                 boolean isPrivate,
                 Timestamp createdAt,
                 Timestamp updatedAt) {
        this.eventId = eventId;
        this.title = title;
        this.details = details;
        this.organizerId = organizerId;
        this.capacity = capacity;
        this.waitingListLimit = waitingListLimit;
        this.qrCodeContent = qrCodeContent;
        this.status = status;
        this.posterUri = posterUri;
        this.category = category == null ? "Other" : category;
        this.scheduledDateTime = scheduledDateTime;
        this.registrationDeadline = registrationDeadline;
        this.drawDate = drawDate;
        this.requireLocation = requireLocation;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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

    public String getDetails() {
        return details;
    }

    /**
     * @param details The detailed description to set.
     */
    public void setDetails(String details) {
        this.details = details;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * @param organizerId The identifier of the event organizer to set.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public String getQrCodeContent() {
        return qrCodeContent;
    }

    public void setQrCodeContent(String qrCodeContent) {
        this.qrCodeContent = qrCodeContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPosterUri() {
        return posterUri;
    }

    public void setPosterUri(String posterUri) {
        this.posterUri = posterUri;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Timestamp getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(Timestamp scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public Timestamp getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(Timestamp registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Timestamp getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(Timestamp drawDate) {
        this.drawDate = drawDate;
    }

    public boolean isRequireLocation() {
        return requireLocation;
    }

    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
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
     * Updates the updatedAt timestamp. Should be called explicitly before Firestore writes.
     */
    public void touch() {
        this.updatedAt = Timestamp.now();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }
}
