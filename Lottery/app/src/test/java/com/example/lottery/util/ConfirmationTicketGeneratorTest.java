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

    // ---- sanitize() tests ----

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
     * Verifies that sanitize returns the fallback when the value is empty string.
     */
    @Test
    public void sanitize_returnsFallbackWhenValueIsEmpty() {
        assertEquals("unknown",
                ConfirmationTicketGenerator.sanitize("", "unknown"));
    }

    /**
     * Verifies that sanitize preserves a single-character valid value.
     */
    @Test
    public void sanitize_preservesSingleCharValue() {
        assertEquals("A",
                ConfirmationTicketGenerator.sanitize("A", "fallback"));
    }

    // ---- buildFileName() tests ----

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
        assertEquals("ticket_unknown.pdf",
                ConfirmationTicketGenerator.buildFileName("   "));
    }

    /**
     * Verifies that buildFileName sanitizes forward slashes in event ID.
     */
    @Test
    public void buildFileName_sanitizesSpecialCharacters() {
        assertEquals("ticket_event_123_test.pdf",
                ConfirmationTicketGenerator.buildFileName("event/123/test"));
    }

    /**
     * Verifies that buildFileName falls back when event ID is null.
     */
    @Test
    public void buildFileName_usesFallbackWhenEventIdIsNull() {
        assertEquals("ticket_unknown.pdf",
                ConfirmationTicketGenerator.buildFileName(null));
    }

    /**
     * Verifies that buildFileName sanitizes dots and other special characters.
     */
    @Test
    public void buildFileName_sanitizesDotsAndColons() {
        assertEquals("ticket_event_1_2_3.pdf",
                ConfirmationTicketGenerator.buildFileName("event.1:2@3"));
    }

    /**
     * Verifies that buildFileName preserves hyphens and underscores.
     */
    @Test
    public void buildFileName_preservesHyphensAndUnderscores() {
        assertEquals("ticket_my-event_id.pdf",
                ConfirmationTicketGenerator.buildFileName("my-event_id"));
    }

    /**
     * Verifies that buildFileName handles a UUID-style event ID correctly.
     */
    @Test
    public void buildFileName_handlesUuidEventId() {
        assertEquals("ticket_4ec82bc1-8e9b-4a7a-955a-f691409aa91b.pdf",
                ConfirmationTicketGenerator.buildFileName("4ec82bc1-8e9b-4a7a-955a-f691409aa91b"));
    }
}
