package com.example.lottery.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared invitation flow helpers used by entrant event details and notifications.
 */
public final class InvitationFlowUtil {

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_INVITED = "invited";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";

    public static final String RESPONSE_ACCEPTED = "ACCEPTED";
    public static final String RESPONSE_REJECTED = "REJECTED";
    public static final String RESPONSE_CANCELLED = "CANCELLED";

    private InvitationFlowUtil() {
    }

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
        if (STATUS_WAITING.equals(normalized)) {
            return STATUS_WAITING;
        }

        if (STATUS_INVITED.equals(normalized)) {
            return STATUS_INVITED;
        }

        if (STATUS_ACCEPTED.equals(normalized)) {
            return STATUS_ACCEPTED;
        }

        if ("rejected".equals(normalized)
                || "cancelled".equals(normalized)
                || "canceled".equals(normalized)
                || STATUS_DECLINED.equals(normalized)) {
            return STATUS_DECLINED;
        }

        return "";
    }

    /**
     * Maps notification response values to the canonical entrant status.
     *
     * @param response notification response value
     * @return canonical entrant status, or empty string when the response is unsupported
     */
    public static String entrantStatusFromNotificationResponse(String response) {
        if (response == null) {
            return "";
        }

        String normalized = response.trim().toUpperCase(Locale.US);
        if (RESPONSE_ACCEPTED.equals(normalized)) {
            return STATUS_ACCEPTED;
        }

        if (RESPONSE_REJECTED.equals(normalized) || RESPONSE_CANCELLED.equals(normalized)) {
            return STATUS_DECLINED;
        }

        return normalizeEntrantStatus(response);
    }

    /**
     * Builds the Firestore payload used when syncing a handled win notification.
     *
     * @param response handled response value
     * @return Firestore update payload
     */
    public static Map<String, Object> buildHandledNotificationUpdate(String response) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
        updates.put("actionTaken", true);
        updates.put("response", response);
        return updates;
    }
}
