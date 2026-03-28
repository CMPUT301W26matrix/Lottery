package com.example.lottery.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link ConfirmationTicketGenerator}.
 *
 * <p>These tests focus on small deterministic helper logic that supports
 * ticket generation without depending on Android PDF rendering.
 */
public class ConfirmationTicketGeneratorTest {

    /**
     * Verifies that sanitize returns the fallback when the value is null.
     */
    @Test
    public void sanitize_returnsFallbackWhenValueIsNull() {
        assertEquals("Event",
                ConfirmationTicketGenerator.sanitize(null, "Event"));
    }

    /**
     * Verifies that sanitize returns the fallback when the value is blank.
     */
    @Test
    public void sanitize_returnsFallbackWhenValueIsBlank() {
        assertEquals("Entrant",
                ConfirmationTicketGenerator.sanitize("   ", "Entrant"));
    }

    /**
     * Verifies that sanitize returns the trimmed original value when valid.
     */
    @Test
    public void sanitize_returnsTrimmedValueWhenValid() {
        assertEquals("Tech Conference",
                ConfirmationTicketGenerator.sanitize("  Tech Conference  ", "Event"));
    }

    /**
     * Verifies that buildFileName uses the expected PDF naming format.
     */
    @Test
    public void buildFileName_returnsExpectedPdfName() {
        assertEquals("ticket_event123.pdf",
                ConfirmationTicketGenerator.buildFileName("event123"));
    }

    /**
     * Verifies that buildFileName falls back safely when event ID is blank.
     */
    @Test
    public void buildFileName_usesFallbackWhenEventIdIsBlank() {
        assertEquals("ticket_N/A.pdf",
                ConfirmationTicketGenerator.buildFileName("   "));
    }
}
