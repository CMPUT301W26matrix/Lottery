package com.example.lottery.util;

/**
 * Centralizes all Firestore collection names and path builders to ensure
 * consistency across the application during the refactor.
 */
public final class FirestorePaths {

    // Main Collections
    public static final String USERS = "users";
    public static final String EVENTS = "events";
    public static final String NOTIFICATIONS = "notifications";

    // Sub-collections
    public static final String INBOX = "inbox";
    public static final String WAITING_LIST = "waitingList";
    public static final String CO_ORGANIZERS = "coOrganizers";
    public static final String RECIPIENTS = "recipients";
    public static final String COMMENTS = "comments";

    private FirestorePaths() {
        // Private constructor to prevent instantiation
    }

    /**
     * Path: users/{uid}
     */
    public static String userDoc(String uid) {
        return USERS + "/" + uid;
    }

    /**
     * Path: users/{uid}/inbox
     */
    public static String userInbox(String uid) {
        return userDoc(uid) + "/" + INBOX;
    }

    /**
     * Path: events/{eventId}
     */
    public static String eventDoc(String eventId) {
        return EVENTS + "/" + eventId;
    }

    /**
     * Path: events/{eventId}/waitingList
     */
    public static String eventWaitingList(String eventId) {
        return eventDoc(eventId) + "/" + WAITING_LIST;
    }

    /**
     * Path: events/{eventId}/coOrganizers
     */
    public static String eventCoOrganizers(String eventId) {
        return eventDoc(eventId) + "/" + CO_ORGANIZERS;
    }

    /**
     * Path: events/{eventId}/comments
     */
    public static String eventComments(String eventId) {
        return eventDoc(eventId) + "/" + COMMENTS;
    }

    /**
     * Path: notifications/{notificationId}
     */
    public static String notificationDoc(String notificationId) {
        return NOTIFICATIONS + "/" + notificationId;
    }

    /**
     * Path: notifications/{notificationId}/recipients
     */
    public static String notificationRecipients(String notificationId) {
        return notificationDoc(notificationId) + "/" + RECIPIENTS;
    }
}
