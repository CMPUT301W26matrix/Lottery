package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for the {@link QRCodeUtils} class.
 *
 * <p>These tests use Robolectric to verify QR code bitmap generation,
 * which requires Android graphics APIs.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QRCodeUtilsBitmapTest {

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
