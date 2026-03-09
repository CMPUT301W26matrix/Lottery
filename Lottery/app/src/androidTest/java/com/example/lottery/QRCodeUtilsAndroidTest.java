package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.util.QRCodeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QRCodeUtilsAndroidTest {

    @Test
    public void testGenerateQRCodeBitmapReturnsExpectedSize() {
        Bitmap bitmap = QRCodeUtils.generateQRCodeBitmap("test-event-qr-code-generation_seed-42");

        assertNotNull(bitmap);
        assertEquals(512, bitmap.getWidth());
        assertEquals(512, bitmap.getHeight());
    }
}
