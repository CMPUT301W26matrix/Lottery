package com.example.lottery.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.UUID;

/**
 * Utility class for QR code related operations.
 * Provides methods to generate unique QR content strings and render them as Bitmaps.
 *
 * <p>Satisfies requirements for:
 * US 02.01.01: Event creation with promotional QR code.
 * </p>
 */
public class QRCodeUtils {

    /**
     * Generates a unique string to be used as QR code content.
     * Combines the eventId with a random UUID to ensure global uniqueness.
     *
     * @param eventId The unique ID of the event to link with the QR code.
     * @return A unique string combining eventId and a random UUID seed.
     */
    public static String generateUniqueQrContent(String eventId) {
        return eventId + "_" + UUID.randomUUID().toString();
    }

    /**
     * Generates a QR Code Bitmap from the given content string using the ZXing library.
     *
     * @param content The string content to be encoded into the QR code.
     * @return A 512x512 Bitmap of the generated QR code, or null if an error occurs.
     */
    public static Bitmap generateQRCodeBitmap(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Standard QR Code size for display
            int width = 512;
            int height = 512;
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
