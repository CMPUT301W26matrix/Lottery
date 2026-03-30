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
 * Robolectric tests for {@link ConfirmationTicketGenerator} drawWrappedText logic.
 *
 * <p>Note: Robolectric's Paint.measureText shadow returns 0 for all strings,
 * so these tests verify the null/empty/blank handling and the measurement-mode
 * (canvas=null) contract rather than actual pixel-based wrapping. Actual PDF
 * generation and visual wrapping are verified in Android instrumentation tests.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ConfirmationTicketGeneratorRobolectricTest {

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

    // ---- drawWrappedText measurement mode (canvas=null) ----

    /**
     * Null text should return the starting y without advancing.
     */
    @Test
    public void drawWrappedText_nullCanvas_nullText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, null, 50, 100, 400f, paint, 20);
        assertEquals(100, result);
    }

    /**
     * Blank text should return the starting y without advancing.
     */
    @Test
    public void drawWrappedText_nullCanvas_blankText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, "   ", 50, 100, 400f, paint, 20);
        assertEquals(100, result);
    }

    /**
     * Empty string should return the starting y without advancing.
     */
    @Test
    public void drawWrappedText_nullCanvas_emptyText_returnsStartY() throws Exception {
        int result = (int) drawWrappedText.invoke(null,
                null, "", 50, 100, 400f, paint, 20);
        assertEquals(100, result);
    }

    /**
     * Valid single-line text should return y >= startY in measurement mode.
     */
    @Test
    public void drawWrappedText_nullCanvas_validText_returnsYGreaterOrEqualStartY() throws Exception {
        int startY = 100;
        int result = (int) drawWrappedText.invoke(null,
                null, "Hello World", 50, startY, 400f, paint, 20);
        assertTrue("Should return y >= startY for valid text", result >= startY);
    }

    /**
     * Measurement mode and drawing mode (with a null canvas for both since
     * Robolectric PdfDocument is limited) should be consistent for same input.
     */
    @Test
    public void drawWrappedText_nullCanvas_consistentResults() throws Exception {
        String text = "Test text for consistency check";
        int y1 = (int) drawWrappedText.invoke(null,
                null, text, 50, 200, 300f, paint, 25);
        int y2 = (int) drawWrappedText.invoke(null,
                null, text, 50, 200, 300f, paint, 25);
        assertEquals("Same input should return same y", y1, y2);
    }

    /**
     * Different line spacing should produce different y results for multi-word text.
     * (With Robolectric's measureText returning 0, all words fit on one line,
     * but the contract should still hold: y >= startY.)
     */
    @Test
    public void drawWrappedText_nullCanvas_differentSpacing_returnsValidY() throws Exception {
        int startY = 100;
        int r1 = (int) drawWrappedText.invoke(null,
                null, "word1 word2 word3", 50, startY, 400f, paint, 10);
        int r2 = (int) drawWrappedText.invoke(null,
                null, "word1 word2 word3", 50, startY, 400f, paint, 30);
        assertTrue("Both should return y >= startY", r1 >= startY && r2 >= startY);
    }
}
