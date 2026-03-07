package com.example.lottery.model;

import java.util.Date;

/**
 * Model class representing an Event.
 * Integrates requirements for US 02.01.01 and US 02.01.04.
 */
public class Event {
    private String eventId;
    private String title;
    private Date scheduledDateTime;
    private Date registrationDeadline;
    private Integer maxCapacity;
    private String details;
    private String posterUri;
    private String qrCodeContent;
    private String organizerId;

    public Event() {} // Required for Firestore serialization

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

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Date getScheduledDateTime() { return scheduledDateTime; }
    public void setScheduledDateTime(Date scheduledDateTime) { this.scheduledDateTime = scheduledDateTime; }
    public Date getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(Date registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getPosterUri() { return posterUri; }
    public void setPosterUri(String posterUri) { this.posterUri = posterUri; }
    public String getQrCodeContent() { return qrCodeContent; }
    public void setQrCodeContent(String qrCodeContent) { this.qrCodeContent = qrCodeContent; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
}
