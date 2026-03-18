package com.example.lottery.util;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared invitation flow helpers used by entrant event details and notifications.
 *
 * This utility centralizes:
 * - canonical entrant status values used in Firestore
 * - notification response normalization
 * - Firestore update payload builders for notifications and entrant-event records
 *
 * Recommended canonical entrant statuses in Firestore:
 * - waitlisted
 * - invited
 * - accepted
 * - declined
 * - cancelled
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
    // Canonical notification response values stored in Firestore
    // -------------------------------------------------------------------------

    public static final String RESPONSE_NONE = "none";
    public static final String RESPONSE_ACCEPTED = "accepted";
    public static final String RESPONSE_DECLINED = "declined";
    public static final String RESPONSE_DISMISSED = "dismissed";
    public static final String RESPONSE_CANCELLED = "cancelled";

    /**
     * Normalizes entrant status values from Firestore into one canonical form.
     *
     * @param rawStatus stored status value
     * @return canonical status, or empty string when the value is missing/unknown
     */
    public static String normalizeEntrantStatus(String rawStatus) {
        if (rawStatus == null) {
            return "";
        }

        String normalized = rawStatus.trim().toLowerCase(Locale.US);

        if ("waiting".equals(normalized)
                || "waitlist".equals(normalized)
                || "waitlisted".equals(normalized)) {
            return STATUS_WAITLISTED;
        }

        if ("invited".equals(normalized)
                || "selected".equals(normalized)) {
            return STATUS_INVITED;
        }

        if ("accepted".equals(normalized)) {
            return STATUS_ACCEPTED;
        }

        if ("declined".equals(normalized)
                || "rejected".equals(normalized)) {
            return STATUS_DECLINED;
        }

        if ("cancelled".equals(normalized)
                || "canceled".equals(normalized)) {
            return STATUS_CANCELLED;
        }

        return "";
    }

    /**
     * Normalizes notification response values into one canonical form.
     *
     * @param rawResponse stored response value
     * @return canonical response, or RESPONSE_NONE when missing/unknown
     */
    public static String normalizeNotificationResponse(String rawResponse) {
        if (rawResponse == null) {
            return RESPONSE_NONE;
        }

        String normalized = rawResponse.trim().toLowerCase(Locale.US);

        if ("accepted".equals(normalized) || "accept".equals(normalized)) {
            return RESPONSE_ACCEPTED;
        }

        if ("declined".equals(normalized)
                || "decline".equals(normalized)
                || "rejected".equals(normalized)
                || "reject".equals(normalized)) {
            return RESPONSE_DECLINED;
        }

        if ("dismissed".equals(normalized) || "dismiss".equals(normalized)) {
            return RESPONSE_DISMISSED;
        }

        if ("cancelled".equals(normalized) || "canceled".equals(normalized)) {
            return RESPONSE_CANCELLED;
        }

        return RESPONSE_NONE;
    }

    /**
     * Maps a notification response value to the entrant status that should result from it.
     *
     * @param response notification response value
     * @return canonical entrant status, or empty string when the response does not imply a status change
     */
    public static String entrantStatusFromNotificationResponse(String response) {
        String normalizedResponse = normalizeNotificationResponse(response);

        switch (normalizedResponse) {
            case RESPONSE_ACCEPTED:
                return STATUS_ACCEPTED;
            case RESPONSE_DECLINED:
                return STATUS_DECLINED;
            case RESPONSE_CANCELLED:
                return STATUS_CANCELLED;
            default:
                return "";
        }
    }

    /**
     * Returns true if the given status represents a terminal decision.
     *
     * @param status entrant status
     * @return true if accepted / declined / cancelled
     */
    public static boolean isFinalEntrantStatus(String status) {
        String normalized = normalizeEntrantStatus(status);
        return STATUS_ACCEPTED.equals(normalized)
                || STATUS_DECLINED.equals(normalized)
                || STATUS_CANCELLED.equals(normalized);
    }

    /**
     * Returns true if the given notification response represents a handled action.
     *
     * @param response notification response
     * @return true if accepted / declined / dismissed / cancelled
     */
    public static boolean isHandledResponse(String response) {
        String normalized = normalizeNotificationResponse(response);
        return !RESPONSE_NONE.equals(normalized);
    }

    /**
     * Builds the Firestore payload used when marking a notification as read.
     *
     * @return Firestore update payload
     */
    public static Map<String, Object> buildReadNotificationUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
        return updates;
    }

    /**
     * Builds the Firestore payload used when syncing a handled notification.
     *
     * Recommended fields:
     * - isRead
     * - actionTaken
     * - response
     * - actedAt
     *
     * @param response handled response value
     * @return Firestore update payload
     */
    public static Map<String, Object> buildHandledNotificationUpdate(String response) {
        String normalizedResponse = normalizeNotificationResponse(response);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
        updates.put("actionTaken", true);
        updates.put("response", normalizedResponse);
        updates.put("actedAt", Timestamp.now());
        return updates;
    }

    /**
     * Builds the Firestore payload for marking an entrant as invited.
     *
     * @return Firestore update payload
     */
    public static Map<String, Object> buildInvitedEntrantUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_INVITED);
        updates.put("invitedAt", Timestamp.now());
        return updates;
    }

    /**
     * Builds the Firestore payload for syncing an entrant status after a notification response.
     *
     * This method updates:
     * - status
     * - acceptedAt / declinedAt / cancelledAt
     *
     * @param response notification response value
     * @return Firestore update payload
     */
    public static Map<String, Object> buildEntrantStatusUpdateFromResponse(String response) {
        String status = entrantStatusFromNotificationResponse(response);
        Map<String, Object> updates = new HashMap<>();

        if (status.isEmpty()) {
            return updates;
        }

        Timestamp now = Timestamp.now();
        updates.put("status", status);

        switch (status) {
            case STATUS_ACCEPTED:
                updates.put("acceptedAt", now);
                break;
            case STATUS_DECLINED:
                updates.put("declinedAt", now);
                break;
            case STATUS_CANCELLED:
                updates.put("cancelledAt", now);
                break;
            default:
                break;
        }

        return updates;
    }

    /**
     * Builds a Firestore payload for cancelling an entrant directly.
     *
     * @return Firestore update payload
     */
    public static Map<String, Object> buildCancelledEntrantUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_CANCELLED);
        updates.put("cancelledAt", Timestamp.now());
        return updates;
    }
}