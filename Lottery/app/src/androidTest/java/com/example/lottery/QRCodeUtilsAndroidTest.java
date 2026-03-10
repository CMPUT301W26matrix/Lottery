package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.util.QRCodeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the {@link QRCodeUtils} class.
 *
 * <p>These tests run on an Android device or emulator to verify QR code
 * bitmap generation, which requires Android graphics APIs.</p>
 */
@RunWith(AndroidJUnit4.class)
public class QRCodeUtilsAndroidTest {

    /**
     * Verifies that the {@link QRCodeUtils#generateQRCodeBitmap(String)} method
     * produces a bitmap of the expected dimensions (512x512).
     */
    @Test
    public void testGenerateQRCodeBitmapReturnsExpectedSize() {
        Bitmap bitmap = QRCodeUtils.generateQRCodeBitmap("test-event-qr-code-generation_seed-42");

        assertNotNull("The generated bitmap should not be null", bitmap);
        assertEquals("The bitmap width should be 512 pixels", 512, bitmap.getWidth());
        assertEquals("The bitmap height should be 512 pixels", 512, bitmap.getHeight());
    }
}
