package com.example.lottery.util;

import java.util.UUID;

/**
 * Utility class for QR code related operations.
 */
public class QRCodeUtils {

    /**
     * Generates a unique string to be used as QR code content.
     * 
     * @param eventId The unique ID of the event.
     * @return A unique string combining eventId and a random UUID.
     */
    public static String generateUniqueQrContent(String eventId) {
        return eventId + "_" + UUID.randomUUID().toString();
    }
}
