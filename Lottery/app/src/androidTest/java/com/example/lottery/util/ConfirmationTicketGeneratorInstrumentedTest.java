package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Android instrumentation tests for {@link ConfirmationTicketGenerator}.
 *
 * <p>These tests run on a real device/emulator where PdfDocument, Canvas,
 * and Paint.measureText work correctly, verifying actual PDF generation
 * including dynamic card height and character-level text wrapping.
 */
public class ConfirmationTicketGeneratorInstrumentedTest {

    private Context context;
    private final List<File> generatedFiles = new ArrayList<>();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        for (File f : generatedFiles) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    private File generate(String title, String user, String eventId) throws IOException {
        File f = ConfirmationTicketGenerator.generateTicket(context, title, user, eventId);
        generatedFiles.add(f);
        return f;
    }

    /**
     * Verifies that generateTicket produces a non-empty PDF file.
     */
    @Test
    public void generateTicket_createsNonEmptyPdf() throws IOException {
        File pdf = generate("Spring Community BBQ", "Alice Nguyen", "spring_bbq_2025");

        assertNotNull(pdf);
        assertTrue("PDF file should exist", pdf.exists());
        assertTrue("PDF file should not be empty", pdf.length() > 0);
        assertTrue("File name should end with .pdf", pdf.getName().endsWith(".pdf"));
    }

    /**
     * Verifies that the file name is derived from the event ID.
     */
    @Test
    public void generateTicket_usesCorrectFileName() throws IOException {
        File pdf = generate("My Event", "Alice", "abc-123");
        assertEquals("ticket_abc-123.pdf", pdf.getName());
    }

    /**
     * Verifies that null inputs produce a valid PDF with fallback file name.
     */
    @Test
    public void generateTicket_handlesNullInputs() throws IOException {
        File pdf = generate(null, null, null);

        assertNotNull(pdf);
        assertTrue(pdf.exists());
        assertEquals("ticket_unknown.pdf", pdf.getName());
    }

    /**
     * Verifies that a very long event title does not crash PDF generation
     * (exercises dynamic card height and text wrapping).
     */
    @Test
    public void generateTicket_handlesVeryLongTitle() throws IOException {
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longTitle.append("LongEventTitle ");
        }

        File pdf = generate(longTitle.toString(), "User", "event1");

        assertNotNull(pdf);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
    }

    /**
     * Verifies that a long event ID without spaces does not crash
     * (exercises character-level wrapping in drawWrappedText).
     */
    @Test
    public void generateTicket_handlesLongEventIdWithoutSpaces() throws IOException {
        String longId = "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz";

        File pdf = generate("Event", "User", longId);

        assertNotNull(pdf);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
    }

    /**
     * Verifies that a UUID-style event ID produces the expected file name.
     */
    @Test
    public void generateTicket_handlesUuidEventId() throws IOException {
        String uuid = "4ec82bc1-8e9b-4a7a-955a-f691409aa91b";
        File pdf = generate("Trip", "youhao", uuid);

        assertEquals("ticket_4ec82bc1-8e9b-4a7a-955a-f691409aa91b.pdf", pdf.getName());
    }

    /**
     * Verifies that special characters in event ID are sanitized in the file name.
     */
    @Test
    public void generateTicket_sanitizesSpecialCharsInFileName() throws IOException {
        File pdf = generate("Event", "User", "test/event:id@2026");

        assertEquals("ticket_test_event_id_2026.pdf", pdf.getName());
    }

    /**
     * Verifies that generating a ticket twice for the same event overwrites the file
     * (no duplicates created).
     */
    @Test
    public void generateTicket_overwritesExistingFile() throws IOException {
        File pdf1 = generate("Event v1", "User", "same-event");
        long size1 = pdf1.length();

        File pdf2 = generate("Event v2 with a much longer title", "User", "same-event");

        assertEquals("Should use same path", pdf1.getAbsolutePath(), pdf2.getAbsolutePath());
        assertTrue("File should still exist", pdf2.exists());
    }
}
