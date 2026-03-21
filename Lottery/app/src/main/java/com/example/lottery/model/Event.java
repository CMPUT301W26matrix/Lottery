package com.example.lottery.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing an event.
 *
 * Target Firestore path:
 * events/{eventId}
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
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Remaining fields kept for UI compatibility during transition
    private Timestamp scheduledDateTime;
    private Timestamp eventEndDate;
    private Timestamp registrationStartDate;
    private Timestamp registrationDeadline;
    private Timestamp drawDate;
    private boolean requireLocation;
    private String posterUri;

    /**
     * Default constructor required for Firestore.
     */
    public Event() {
        this.status = "open";
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    /** Backward compatibility alias for maxCapacity */
    public Integer getMaxCapacity() {
        return capacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.capacity = maxCapacity;
    }

    public Integer getWaitingListLimit() {
        return waitingListLimit;
    }

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

    // Compatibility Getters/Setters for UI fields

    public Timestamp getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(Timestamp scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public Timestamp getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Timestamp eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
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

    public String getPosterUri() {
        return posterUri;
    }

    public void setPosterUri(String posterUri) {
        this.posterUri = posterUri;
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
