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
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_CANCELLED = "cancelled";

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

        if ("waiting".equals(normalized) || "waitlist".equals(normalized) || "waitlisted".equals(normalized)) {
            return STATUS_WAITLISTED;
        }
        if ("invited".equals(normalized) || "selected".equals(normalized)) {
            return STATUS_INVITED;
        }
        if ("accepted".equals(normalized)) {
            return STATUS_ACCEPTED;
        }
        if ("declined".equals(normalized) || "rejected".equals(normalized)) {
            return STATUS_DECLINED;
        }
        if ("cancelled".equals(normalized) || "canceled".equals(normalized)) {
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
     * Builds a Firestore update payload for marking an entrant as invited/selected.
     *
     * @return A map containing status and timestamp updates.
     */
    public static Map<String, Object> buildInvitedEntrantUpdate() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_INVITED);
        updates.put("selectedAt", now);
        updates.put("updatedAt", now);
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

        switch (normalizedResponse) {
            case RESPONSE_ACCEPTED: status = STATUS_ACCEPTED; break;
            case RESPONSE_DECLINED: status = STATUS_DECLINED; break;
            case RESPONSE_CANCELLED: status = STATUS_CANCELLED; break;
        }

        Map<String, Object> updates = new HashMap<>();
        if (status.isEmpty()) return updates;

        Timestamp now = Timestamp.now();
        updates.put("status", status);
        updates.put("respondedAt", now);
        updates.put("updatedAt", now);
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
        updates.put("updatedAt", now);
        return updates;
    }

    /**
     * Builds a Firestore update payload for marking a notification as handled.
     *
     * @param response The user's response to the notification.
     * @return A map with read status updated.
     */
    public static Map<String, Object> buildHandledNotificationUpdate(String response) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
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
