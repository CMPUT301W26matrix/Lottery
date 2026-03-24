package com.example.lottery.util;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class that centralizes invitation status transitions and notification response handling.
 *
 * <p>This class ensures consistent status names and Firestore update payloads across
 * the entrant's event details and notification processing flows.</p>
 */
public final class InvitationFlowUtil {

    private InvitationFlowUtil() {
        // Utility class
    }

    // -------------------------------------------------------------------------
    // Canonical entrant status values stored in Firestore
    // -------------------------------------------------------------------------

    public static final String STATUS_WAITLISTED = "waitlisted";
    public static final String STATUS_INVITED = "invited";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_NOT_SELECTED = "not_selected";

    // -------------------------------------------------------------------------
    // Canonical notification response values
    // -------------------------------------------------------------------------

    public static final String RESPONSE_NONE = "none";
    public static final String RESPONSE_ACCEPTED = "accepted";
    public static final String RESPONSE_DECLINED = "declined";
    public static final String RESPONSE_DISMISSED = "dismissed";
    public static final String RESPONSE_CANCELLED = "cancelled";

    /**
     * Normalizes a raw entrant status string into its canonical form.
     *
     * @param rawStatus The status string from Firestore or UI.
     * @return The canonical status string (e.g., "waitlisted", "invited").
     */
    public static String normalizeEntrantStatus(String rawStatus) {
        if (rawStatus == null) return "";
        String normalized = rawStatus.trim().toLowerCase(Locale.US);

        if ("waitlisted".equalsIgnoreCase(normalized) || "waiting".equalsIgnoreCase(normalized)) {
            return STATUS_WAITLISTED;
        }
        if ("invited".equalsIgnoreCase(normalized) || "selected".equalsIgnoreCase(normalized)) {
            return STATUS_INVITED;
        }
        if ("accepted".equalsIgnoreCase(normalized)) {
            return STATUS_ACCEPTED;
        }
        if ("not_selected".equalsIgnoreCase(normalized) || "rejected_by_draw".equalsIgnoreCase(normalized)) {
            return STATUS_NOT_SELECTED;
        }
        if ("cancelled".equalsIgnoreCase(normalized) || "declined".equalsIgnoreCase(normalized) || "rejected".equalsIgnoreCase(normalized)) {
            return STATUS_CANCELLED;
        }
        return "";
    }

    /**
     * Normalizes a raw notification response string into its canonical form.
     *
     * @param rawResponse The response string from a notification dialog.
     * @return The canonical response string (e.g., "accepted", "declined").
     */
    public static String normalizeNotificationResponse(String rawResponse) {
        if (rawResponse == null) return RESPONSE_NONE;
        String normalized = rawResponse.trim().toLowerCase(Locale.US);

        if ("accepted".equals(normalized) || "accept".equals(normalized)) return RESPONSE_ACCEPTED;
        if ("declined".equals(normalized) || "decline".equals(normalized) || "rejected".equals(normalized) || "reject".equals(normalized)) return RESPONSE_DECLINED;
        if ("dismissed".equals(normalized) || "dismiss".equals(normalized)) return RESPONSE_DISMISSED;
        if ("cancelled".equals(normalized) || "canceled".equals(normalized)) return RESPONSE_CANCELLED;
        return RESPONSE_NONE;
    }

    /**
     * Builds a Firestore update payload for marking an entrant as invited.
     *
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildInvitedEntrantUpdate() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_INVITED);
        updates.put("invitedAt", now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for marking an entrant as not selected.
     *
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildNotSelectedEntrantUpdate() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_NOT_SELECTED);
        updates.put("notSelectedAt", now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for an entrant status based on a notification response.
     *
     * @param response The user's response string.
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildEntrantStatusUpdateFromResponse(String response) {
        String normalizedResponse = normalizeNotificationResponse(response);
        String status = "";
        String timeField = "";

        switch (normalizedResponse) {
            case RESPONSE_ACCEPTED: 
                status = STATUS_ACCEPTED; 
                timeField = "acceptedAt";
                break;
            case RESPONSE_DECLINED: 
            case RESPONSE_CANCELLED: 
                status = STATUS_CANCELLED; 
                timeField = "cancelledAt";
                break;
        }

        Map<String, Object> updates = new HashMap<>();
        if (status.isEmpty()) return updates;

        Timestamp now = Timestamp.now();
        updates.put("status", status);
        updates.put(timeField, now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for an entrant joining the waitlist after an invitation.
     *
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildWaitlistJoinUpdate() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_WAITLISTED);
        updates.put("waitlistedAt", now);
        updates.put("registeredAt", now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for cancelling an entrant's participation.
     *
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildCancelledEntrantUpdate() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_CANCELLED);
        updates.put("cancelledAt", now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for marking a notification as read.
     *
     * @return A map with read status updated.
     */
    public static Map<String, Object> buildReadNotificationUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
        return updates;
    }
}
