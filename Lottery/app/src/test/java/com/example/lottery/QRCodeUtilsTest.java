package com.example.lottery;

import static org.junit.Assert.*;
import org.junit.Test;
import com.example.lottery.util.QRCodeUtils;

/**
 * Unit tests for the QRCodeUtils class.
 */
public class QRCodeUtilsTest {

    @Test
    public void testGenerateUniqueQrContentContainsEventId() {
        String eventId = "testEventId";
        String qrContent = QRCodeUtils.generateUniqueQrContent(eventId);
        
        assertNotNull(qrContent);
        assertTrue(qrContent.startsWith(eventId));
    }

    @Test
    public void testGenerateUniqueQrContentIsUnique() {
        String eventId = "sameId";
        String qrContent1 = QRCodeUtils.generateUniqueQrContent(eventId);
        String qrContent2 = QRCodeUtils.generateUniqueQrContent(eventId);
        
        assertNotEquals(qrContent1, qrContent2);
    }
}
