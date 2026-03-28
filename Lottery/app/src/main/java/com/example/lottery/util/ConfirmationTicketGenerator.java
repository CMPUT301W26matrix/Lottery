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
     * @param context application context used for locating the output directory
     * @param eventTitle title of the event
     * @param userName name of the entrant
     * @param eventId unique event identifier
     * @return file pointing to the generated PDF
     * @throws IOException if the PDF cannot be written to storage
     */
    public static File generateTicket(Context context,
                                      String eventTitle,
                                      String userName,
                                      String eventId) throws IOException {

        String safeEventTitle = sanitize(eventTitle, "Event");
        String safeUserName = sanitize(userName, "Entrant");
        String safeEventId = sanitize(eventId, "N/A");

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
            throw new IOException("Unable to access documents directory.");
        }

        File file = new File(outputDir, buildFileName(safeEventId));

        FileOutputStream fos = new FileOutputStream(file);
        pdfDocument.writeTo(fos);
        fos.close();
        pdfDocument.close();

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
     * @param canvas canvas to draw on
     * @param eventTitle title of the event
     * @param userName name of the entrant
     * @param eventId event identifier
     */
    private static void drawTicketBody(Canvas canvas,
                                       String eventTitle,
                                       String userName,
                                       String eventId) {

        float left = SIDE_MARGIN;
        float top = 135;
        float right = PAGE_WIDTH - SIDE_MARGIN;
        float bottom = 700;

        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setAntiAlias(true);

        RectF cardRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(cardRect, 24, 24, cardPaint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#D9E2F1"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(cardRect, 24, 24, borderPaint);

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

        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E8EEF7"));
        dividerPaint.setStrokeWidth(2f);

        int contentX = SIDE_MARGIN + 28;
        int y = 195;

        canvas.drawText("EVENT", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, eventTitle, contentX, y, right - left - 56, valuePaint, 30);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("ENTRANT", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, userName, contentX, y, right - left - 56, bodyTextPaint, 28);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("EVENT ID", contentX, y, sectionTitlePaint);
        y += 34;
        y = drawWrappedText(canvas, eventId, contentX, y, right - left - 56, bodyTextPaint, 26);

        y += 25;
        canvas.drawLine(contentX, y, right - 28, y, dividerPaint);

        y += 40;
        canvas.drawText("GENERATED AT", contentX, y, sectionTitlePaint);
        y += 34;

        String timestamp = new SimpleDateFormat(
                "EEE, MMM d, yyyy  h:mm a",
                Locale.getDefault()
        ).format(new Date());

        y = drawWrappedText(canvas, timestamp, contentX, y, right - left - 56, bodyTextPaint, 26);

        y += 55;

        Paint noteTitlePaint = new Paint();
        noteTitlePaint.setColor(Color.parseColor("#1557FF"));
        noteTitlePaint.setTextSize(14f);
        noteTitlePaint.setFakeBoldText(true);
        noteTitlePaint.setAntiAlias(true);

        Paint noteBodyPaint = new Paint();
        noteBodyPaint.setColor(Color.parseColor("#4A5568"));
        noteBodyPaint.setTextSize(15f);
        noteBodyPaint.setAntiAlias(true);

        canvas.drawText("IMPORTANT", contentX, y, noteTitlePaint);
        y += 28;

        drawWrappedText(
                canvas,
                "Please keep this ticket available for confirmation or verification during event participation.",
                contentX,
                y,
                right - left - 56,
                noteBodyPaint,
                22
        );
    }

    /**
     * Draws wrapped text within a maximum width.
     *
     * @param canvas canvas to draw on
     * @param text text to render
     * @param x x-coordinate where text begins
     * @param y y-coordinate of the first line baseline
     * @param maxWidth maximum width allowed before wrapping
     * @param paint paint used for drawing text
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
            canvas.drawText("N/A", x, y, paint);
            return y;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0
                    ? word
                    : currentLine + " " + word;

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                canvas.drawText(currentLine.toString(), x, y, paint);
                y += lineSpacing;
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            canvas.drawText(currentLine.toString(), x, y, paint);
        }

        return y;
    }

    /**
     * Returns a trimmed fallback-safe value.
     *
     * @param value original value
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
        return "ticket_" + sanitize(eventId, "N/A") + ".pdf";
    }
}