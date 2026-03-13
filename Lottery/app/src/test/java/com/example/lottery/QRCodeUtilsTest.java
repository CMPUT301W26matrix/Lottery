package com.example.lottery;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.example.lottery.util.QRCodeUtils;

import org.junit.Test;

/**
 * Unit tests for the {@link QRCodeUtils} class.
 *
 * <p>This class validates the QR code generation logic, ensuring that:
 * <ul>
 *   <li>The generated content correctly incorporates the event ID.</li>
 *   <li>The generated content is unique even when called with the same event ID.</li>
 *   <li>Edge cases like empty or unusual event IDs are handled gracefully.</li>
 * </ul>
 * </p>
 */
public class QRCodeUtilsTest {

    /**
     * Verifies that the generated QR content includes the event ID as a prefix.
     */
    @Test
    public void testGenerateUniqueQrContentContainsEventId() {
        String eventId = "testEventId";
        String qrContent = QRCodeUtils.generateUniqueQrContent(eventId);

        assertNotNull("QR content should not be null", qrContent);
        assertTrue("QR content should start with the event ID", qrContent.startsWith(eventId));
        assertTrue("QR content should contain the separator", qrContent.contains("_"));
    }

    /**
     * Verifies that multiple calls to generate QR content with the same event ID
     * produce unique results, ensuring non-repeatability.
     */
    @Test
    public void testGenerateUniqueQrContentIsUnique() {
        String eventId = "sameId";
        String qrContent1 = QRCodeUtils.generateUniqueQrContent(eventId);
        String qrContent2 = QRCodeUtils.generateUniqueQrContent(eventId);

        assertNotEquals("Two generated QR contents for the same event ID should be unique", qrContent1, qrContent2);
    }

    /**
     * Tests behavior with an empty event ID.
     */
    @Test
    public void testGenerateUniqueQrContentWithEmptyId() {
        String eventId = "";
        String qrContent = QRCodeUtils.generateUniqueQrContent(eventId);
        
        assertNotNull(qrContent);
        assertTrue(qrContent.startsWith("_"));
    }

    /**
     * Tests behavior with unusual characters in event ID.
     */
    @Test
    public void testGenerateUniqueQrContentWithUnusualId() {
        String eventId = "!@#$%^&*()_+";
        String qrContent = QRCodeUtils.generateUniqueQrContent(eventId);
        
        assertNotNull(qrContent);
        assertTrue(qrContent.startsWith(eventId));
    }
}
