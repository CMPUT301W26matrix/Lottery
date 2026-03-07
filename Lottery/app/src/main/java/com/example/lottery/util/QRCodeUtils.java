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

    /**
     * Generates a QR Code Bitmap from the given content string.
     * 
     * @param content The string to encode in the QR code.
     * @return A Bitmap of the QR code, or null if generation fails.
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
