package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

/**
 * Tests for {@link ConfirmationTicketGenerator}.
 *
 * <p>These tests cover deterministic helper logic plus Robolectric-side
 * text measurement behavior. Actual PDF generation remains covered by
 * instrumentation tests.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConfirmationTicketGeneratorTest {

    private Paint paint;
    private Method drawWrappedText;

    @Before
    public void setUp() throws Exception {
        paint = new Paint();
        paint.setTextSize(20f);

        drawWrappedText = ConfirmationTicketGenerator.class.getDeclaredMethod(
                "drawWrappedText", Canvas.class, String.class,
                int.class, int.class, float.class, Paint.class, int.class);
        drawWrappedText.setAccessible(true);
    }

    // ---- sanitize() tests ----

    // WOW #8: Verify sanitize returns fallback when value is null
    @Test
    public void sanitize_returnsFallbackWhenValueIsNull() {
        assertEquals("Event",
                ConfirmationTicketGenerator.sanitize(null, "Event"));
    }

    // WOW #8: Verify sanitize returns fallback when value is blank
    @Test
    public void sanitize_returnsFallbackWhenValueIsBlank() {
        assertEquals("Entrant",
                ConfirmationTicketGenerator.sanitize("   ", "Entrant"));
    }

    // WOW #8: Verify sanitize returns trimmed value when input is valid
    @Test
    public void sanitize_returnsTrimmedValueWhenValid() {
        assertEquals("Tech Conference",
                ConfirmationTicketGenerator.sanitize("  Tech Conference  ", "Event"));
    }

    // WOW #8: Verify sanitize returns fallback when value is empty string
    @Test
    public void sanitize_returnsFallbackWhenValueIsEmpty() {
        assertEquals("unknown",
                ConfirmationTicketGenerator.sanitize("", "unknown"));
    }

    // WOW #8: Verify sanitize preserves a single-character value
    @Test
    public void sanitize_preservesSingleCharValue() {
        assertEquals("A",
                ConfirmationTicketGenerator.sanitize("A", "fallback"));
    }

    // ---- buildFileName() tests ----

    // WOW #8: Verify buildFileName produces expected PDF name from event ID
    @Test
    public void buildFileName_returnsExpectedPdfName() {
        assertEquals("ticket_event123.pdf",
                ConfirmationTicketGenerator.buildFileName("event123"));
    }

    // WOW #8: Verify buildFileName uses fallback when event ID is blank
    @Test
    public void buildFileName_usesFallbackWhenEventIdIsBlank() {
        assertEquals("ticket_unknown.pdf",
                ConfirmationTicketGenerator.buildFileName("   "));
    }

    // WOW #8: Verify buildFileName sanitizes special characters in event ID
    @Test
    public void buildFileName_sanitizesSpecialCharacters() {
        assertEquals("ticket_event_123_test.pdf",
                ConfirmationTicketGenerator.buildFileName("event/123/test"));
    }

    // WOW #8: Verify buildFileName uses fallback when event ID is null
    @Test
    public void buildFileName_usesFallbackWhenEventIdIsNull() {
        assertEquals("ticket_unknown.pdf",
                ConfirmationTicketGenerator.buildFileName(null));
    }

    // WOW #8: Verify buildFileName sanitizes dots, colons, and at-signs
    @Test
    public void buildFileName_sanitizesDotsAndColons() {
        assertEquals("ticket_event_1_2_3.pdf",
                ConfirmationTicketGenerator.buildFileName("event.1:2@3"));
    }

    // WOW #8: Verify buildFileName preserves hyphens and underscores
    @Test
    public void buildFileName_preservesHyphensAndUnderscores() {
        assertEquals("ticket_my-event_id.pdf",
                ConfirmationTicketGenerator.buildFileName("my-event_id"));
    }

    // WOW #8: Verify buildFileName handles UUID-style event IDs
    @Test
    public void buildFileName_handlesUuidEventId() {
        assertEquals("ticket_4ec82bc1-8e9b-4a7a-955a-f691409aa91b.pdf",
                ConfirmationTicketGenerator.buildFileName("4ec82bc1-8e9b-4a7a-955a-f691409aa91b"));
    }

    // ---- drawWrappedText() tests ----

    // WOW #8: Verify drawWrappedText returns startY when text is null
    @Test
    public void drawWrappedText_nullCanvas_nullText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, null, 50, 100, 400f, null, 20);
        assertEquals(100, result);
    }

    // WOW #8: Verify drawWrappedText returns startY when text is blank
    @Test
    public void drawWrappedText_nullCanvas_blankText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, "   ", 50, 100, 400f, null, 20);
        assertEquals(100, result);
    }

    // WOW #8: Verify drawWrappedText returns startY when text is empty
    @Test
    public void drawWrappedText_nullCanvas_emptyText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, "", 50, 100, 400f, null, 20);
        assertEquals(100, result);
    }

    // WOW #8: Verify drawWrappedText returns Y >= startY for valid text
    @Test
    public void drawWrappedText_nullCanvas_validText_returnsYGreaterOrEqualStartY() throws Exception {
        int startY = 100;
        int result = (int) drawWrappedText.invoke(null,
                null, "Hello World", 50, startY, 400f, paint, 20);
        assertTrue(result >= startY);
    }

    // WOW #8: Verify drawWrappedText produces consistent results for same input
    @Test
    public void drawWrappedText_nullCanvas_consistentResults() throws Exception {
        String text = "Please arrive 15 minutes before the event starts";
        int y1 = (int) drawWrappedText.invoke(null,
                null, text, 50, 200, 300f, paint, 25);
        int y2 = (int) drawWrappedText.invoke(null,
                null, text, 50, 200, 300f, paint, 25);
        assertEquals(y1, y2);
    }

    // WOW #8: Verify drawWrappedText returns valid Y with different line spacing
    @Test
    public void drawWrappedText_nullCanvas_differentSpacing_returnsValidY() throws Exception {
        int startY = 100;
        int r1 = (int) drawWrappedText.invoke(null,
                null, "word1 word2 word3", 50, startY, 400f, paint, 10);
        int r2 = (int) drawWrappedText.invoke(null,
                null, "word1 word2 word3", 50, startY, 400f, paint, 30);
        assertTrue(r1 >= startY && r2 >= startY);
    }
}
