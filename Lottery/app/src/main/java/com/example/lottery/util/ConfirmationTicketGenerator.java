package com.example.lottery.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class responsible for generating styled confirmation ticket PDFs.
 *
 * <p>This class creates a polished confirmation ticket design with:
 * <ul>
 *     <li>a colored header,</li>
 *     <li>a rounded ticket body,</li>
 *     <li>section labels and values,</li>
 *     <li>wrapped text for long fields such as event IDs,</li>
 *     <li>and a footer note.</li>
 * </ul>
 *
 * <p>The generated PDF is stored in the app's external documents directory.
 */
public final class ConfirmationTicketGenerator {

    /**
     * Page width in PDF points.
     */
    private static final int PAGE_WIDTH = 595;

    /**
     * Page height in PDF points.
     */
    private static final int PAGE_HEIGHT = 842;

    /**
     * Left and right margin.
     */
    private static final int SIDE_MARGIN = 48;

    /**
     * Top margin.
     */
    private static final int TOP_MARGIN = 50;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ConfirmationTicketGenerator() {
        // Utility class
    }

    /**
     * Generates a styled confirmation ticket PDF.
     *
     * @param context    application context used for locating the output directory
     * @param eventTitle title of the event
     * @param userName   name of the entrant
     * @param eventId    unique event identifier
     * @return file pointing to the generated PDF
     * @throws IOException if the PDF cannot be written to storage
     */
    public static File generateTicket(Context context,
                                      String eventTitle,
                                      String userName,
                                      String eventId) throws IOException {

        String safeEventTitle = sanitize(eventTitle, "Event");
        String safeUserName = sanitize(userName, "Entrant");
        String safeEventId = sanitize(eventId, "unknown");

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawBackground(canvas);
        drawHeader(canvas);
        drawTicketBody(canvas, safeEventTitle, safeUserName, safeEventId);

        pdfDocument.finishPage(page);

        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            pdfDocument.close();
            throw new IOException("Unable to access documents directory.");
        }

        File file = new File(outputDir, buildFileName(safeEventId));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            pdfDocument.writeTo(fos);
        } finally {
            pdfDocument.close();
        }

        return file;
    }

    /**
     * Draws the page background.
     *
     * @param canvas canvas to draw on
     */
    private static void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#F4F7FB"));
    }

    /**
     * Draws the top header section of the confirmation ticket.
     *
     * @param canvas canvas to draw on
     */
    private static void drawHeader(Canvas canvas) {
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1557FF"));
        headerPaint.setStyle(Paint.Style.FILL);
        headerPaint.setAntiAlias(true);

        RectF headerRect = new RectF(0, 0, PAGE_WIDTH, 165);
        canvas.drawRect(headerRect, headerPaint);

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(30f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#DCE6FF"));
        subtitlePaint.setTextSize(14f);
        subtitlePaint.setAntiAlias(true);

        canvas.drawText("CONFIRMATION TICKET", SIDE_MARGIN, 78, titlePaint);
        canvas.drawText("Official entry confirmation for your event", SIDE_MARGIN, 108, subtitlePaint);

        Paint badgePaint = new Paint();
        badgePaint.setColor(Color.WHITE);
        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setAntiAlias(true);

        RectF badgeRect = new RectF(PAGE_WIDTH - 170, 42, PAGE_WIDTH - 48, 82);
        canvas.drawRoundRect(badgeRect, 18, 18, badgePaint);

        Paint badgeTextPaint = new Paint();
        badgeTextPaint.setColor(Color.parseColor("#1557FF"));
        badgeTextPaint.setTextSize(15f);
        badgeTextPaint.setFakeBoldText(true);
        badgeTextPaint.setAntiAlias(true);

        canvas.drawText("LOTTERY APP", PAGE_WIDTH - 150, 67, badgeTextPaint);
    }

    /**
     * Draws the main ticket body containing all confirmation details.
     *
     * @param canvas     canvas to draw on
     * @param eventTitle title of the event
     * @param userName   name of the entrant
     * @param eventId    event identifier
     */
    private static void drawTicketBody(Canvas canvas,
                                       String eventTitle,
                                       String userName,
                                       String eventId) {

        float left = SIDE_MARGIN;
        float top = 135;
        float right = PAGE_WIDTH - SIDE_MARGIN;
        float contentWidth = right - left - 56;
        int contentX = SIDE_MARGIN + 28;

        Paint sectionTitlePaint = new Paint();
        sectionTitlePaint.setColor(Color.parseColor("#7A869A"));
        sectionTitlePaint.setTextSize(13f);
        sectionTitlePaint.setFakeBoldText(true);
        sectionTitlePaint.setAntiAlias(true);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#1C2333"));
        valuePaint.setTextSize(24f);
        valuePaint.setAntiAlias(true);

        Paint bodyTextPaint = new Paint();
        bodyTextPaint.setColor(Color.parseColor("#1C2333"));
        bodyTextPaint.setTextSize(20f);
        bodyTextPaint.setAntiAlias(true);

        Paint noteTitlePaint = new Paint();
        noteTitlePaint.setColor(Color.parseColor("#1557FF"));
        noteTitlePaint.setTextSize(14f);
        noteTitlePaint.setFakeBoldText(true);
        noteTitlePaint.setAntiAlias(true);

        Paint noteBodyPaint = new Paint();
        noteBodyPaint.setColor(Color.parseColor("#4A5568"));
        noteBodyPaint.setTextSize(15f);
        noteBodyPaint.setAntiAlias(true);

        String timestamp = new SimpleDateFormat(
                "EEE, MMM d, yyyy  h:mm a",
                Locale.getDefault()
        ).format(new Date());

        String noteText = "Please keep this ticket available for confirmation during event participation.";

        // Measurement pass: calculate content height without drawing
        int bottom = measureContentHeight(contentX, contentWidth,
                eventTitle, userName, eventId, timestamp, noteText,
                sectionTitlePaint, valuePaint, bodyTextPaint,
                noteTitlePaint, noteBodyPaint);
        float cardBottom = Math.min(bottom + 40, PAGE_HEIGHT - 20);

        // Draw card background
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setAntiAlias(true);

        RectF cardRect = new RectF(left, top, right, cardBottom);
        canvas.drawRoundRect(cardRect, 24, 24, cardPaint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#D9E2F1"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(cardRect, 24, 24, borderPaint);

        // Draw content
        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E8EEF7"));
        dividerPaint.setStrokeWidth(2f);

        int y = 195;

        canvas.drawText("EVENT", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, eventTitle, contentX, y, contentWidth, valuePaint, 30);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("ENTRANT", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, userName, contentX, y, contentWidth, bodyTextPaint, 28);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("EVENT ID", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, eventId, contentX, y, contentWidth, bodyTextPaint, 26);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("GENERATED AT", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, timestamp, contentX, y, contentWidth, bodyTextPaint, 26);

        y += 55;

        canvas.drawText("IMPORTANT", contentX, y, noteTitlePaint);
        y += 28;

        drawWrappedText(canvas, noteText, contentX, y, contentWidth, noteBodyPaint, 22);
    }

    /**
     * Calculates the total content height by simulating the layout without drawing.
     *
     * @param contentX          x-coordinate where content begins
     * @param contentWidth      maximum width for text wrapping
     * @param eventTitle        title of the event
     * @param userName          name of the entrant
     * @param eventId           event identifier
     * @param timestamp         formatted generation timestamp
     * @param noteText          footer note text
     * @param sectionTitlePaint paint for section labels
     * @param valuePaint        paint for the event title value
     * @param bodyTextPaint     paint for body text fields
     * @param noteTitlePaint    paint for the note title
     * @param noteBodyPaint     paint for the note body text
     * @return the y-position after all content
     */
    private static int measureContentHeight(int contentX, float contentWidth,
                                            String eventTitle, String userName,
                                            String eventId, String timestamp,
                                            String noteText,
                                            Paint sectionTitlePaint, Paint valuePaint,
                                            Paint bodyTextPaint, Paint noteTitlePaint,
                                            Paint noteBodyPaint) {
        int y = 195;

        y += 34; // EVENT label + gap
        y = drawWrappedText(null, eventTitle, contentX, y, contentWidth, valuePaint, 30);

        y += 25 + 40; // divider + gap
        y += 34; // ENTRANT label + gap
        y = drawWrappedText(null, userName, contentX, y, contentWidth, bodyTextPaint, 28);

        y += 25 + 40; // divider + gap
        y += 34; // EVENT ID label + gap
        y = drawWrappedText(null, eventId, contentX, y, contentWidth, bodyTextPaint, 26);

        y += 25 + 40; // divider + gap
        y += 34; // GENERATED AT label + gap
        y = drawWrappedText(null, timestamp, contentX, y, contentWidth, bodyTextPaint, 26);

        y += 55; // gap before IMPORTANT
        y += 28; // IMPORTANT label + gap
        y = drawWrappedText(null, noteText, contentX, y, contentWidth, noteBodyPaint, 22);

        return y;
    }

    /**
     * Draws or measures wrapped text within a maximum width.
     *
     * @param canvas      canvas to draw on, or null for measurement only
     * @param text        text to render
     * @param x           x-coordinate where text begins
     * @param y           y-coordinate of the first line baseline
     * @param maxWidth    maximum width allowed before wrapping
     * @param paint       paint used for drawing/measuring text
     * @param lineSpacing vertical spacing between wrapped lines
     * @return the y-position after the final line
     */
    private static int drawWrappedText(Canvas canvas,
                                       String text,
                                       int x,
                                       int y,
                                       float maxWidth,
                                       Paint paint,
                                       int lineSpacing) {

        if (text == null || text.trim().isEmpty()) {
            if (canvas != null) {
                canvas.drawText("N/A", x, y, paint);
            }
            return y;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Break individual word by character if it exceeds maxWidth on its own
            if (paint.measureText(word) > maxWidth) {
                if (currentLine.length() > 0) {
                    if (canvas != null) {
                        canvas.drawText(currentLine.toString(), x, y, paint);
                    }
                    y += lineSpacing;
                    currentLine = new StringBuilder();
                }
                StringBuilder charLine = new StringBuilder();
                for (int i = 0; i < word.length(); i++) {
                    String test = charLine.toString() + word.charAt(i);
                    if (paint.measureText(test) > maxWidth && charLine.length() > 0) {
                        if (canvas != null) {
                            canvas.drawText(charLine.toString(), x, y, paint);
                        }
                        y += lineSpacing;
                        charLine = new StringBuilder();
                    }
                    charLine.append(word.charAt(i));
                }
                currentLine = charLine;
                continue;
            }

            String testLine = currentLine.length() == 0
                    ? word
                    : currentLine + " " + word;

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (canvas != null) {
                    canvas.drawText(currentLine.toString(), x, y, paint);
                }
                y += lineSpacing;
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0 && canvas != null) {
            canvas.drawText(currentLine.toString(), x, y, paint);
        }

        return y;
    }

    /**
     * Returns a trimmed fallback-safe value.
     *
     * @param value    original value
     * @param fallback fallback text if original value is null or blank
     * @return sanitized text
     */
    static String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    /**
     * Builds the output PDF file name for a given event ID.
     *
     * @param eventId sanitized event ID
     * @return ticket file name ending in .pdf
     */
    static String buildFileName(String eventId) {
        String safe = sanitize(eventId, "unknown");
        safe = safe.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return "ticket_" + safe + ".pdf";
    }
}
