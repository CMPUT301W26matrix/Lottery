package com.example.lottery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Logic tests for event validation business rules.
 */
public class EventValidationUtilsTest {

    @Test
    public void testValidDeadlineBeforeEventDate() {
        Calendar cal = Calendar.getInstance();

        cal.set(2024, 10, 20, 10, 0);
        Date deadline = cal.getTime();

        cal.set(2024, 10, 21, 10, 0);
        Date eventDate = cal.getTime();

        assertTrue("Deadline before event date should be valid",
                EventValidationUtils.isRegistrationDeadlineValid(deadline, eventDate));
    }

    @Test
    public void testInvalidDeadlineAfterEventDate() {
        Calendar cal = Calendar.getInstance();

        cal.set(2024, 10, 22, 10, 0);
        Date deadline = cal.getTime();

        cal.set(2024, 10, 21, 10, 0);
        Date eventDate = cal.getTime();

        assertFalse("Deadline after event date should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(deadline, eventDate));
    }

    @Test
    public void testInvalidEqualDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, 10, 21, 10, 0);
        Date sameDate = cal.getTime();

        assertFalse("Deadline equal to event date should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(sameDate, sameDate));
    }

    @Test
    public void testNullHandling() {
        Date someDate = new Date();

        assertFalse("Null deadline should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(null, someDate));
        assertFalse("Null event date should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(someDate, null));
        assertFalse("Both null should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(null, null));
    }

    @Test
    public void testRegistrationDeadlineMillisecondPrecision() {
        long now = System.currentTimeMillis();
        Date eventDate = new Date(now);
        Date deadline = new Date(now - 1); // Exactly 1ms before

        assertTrue("1ms before should be valid",
                EventValidationUtils.isRegistrationDeadlineValid(deadline, eventDate));

        Date exactlySame = new Date(now);
        assertFalse("Exactly same millisecond should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(exactlySame, eventDate));
    }

    @Test
    public void testDifferentYearsValidation() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, 11, 31, 23, 59);
        Date deadline = cal.getTime();

        cal.set(2024, 0, 1, 0, 1);
        Date eventDate = cal.getTime();

        assertTrue("Deadline in previous year should be valid",
                EventValidationUtils.isRegistrationDeadlineValid(deadline, eventDate));
    }

    // US 02.01.01 / US 02.01.04 / US 02.05.02: Draw date must sit strictly after
    // registration end (so registration is closed when the draw runs) and on or before
    // event start (so the draw happens before attendees show up).
    @Test
    public void testDrawDateValidationHappyPath() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, 3, 5, 10, 0);
        Date regEnd = cal.getTime();
        cal.set(2026, 3, 7, 10, 0);
        Date draw = cal.getTime();
        cal.set(2026, 3, 9, 10, 0);
        Date eventStart = cal.getTime();

        assertTrue("Draw strictly between regEnd and eventStart should be valid",
                EventValidationUtils.isDrawDateValid(draw, regEnd, eventStart));
    }

    @Test
    public void testDrawDateValidationDrawOnEventStartAllowed() {
        // Boundary: draw date equal to event start is allowed (draw happens right
        // before the event begins).
        Calendar cal = Calendar.getInstance();
        cal.set(2026, 3, 5, 10, 0);
        Date regEnd = cal.getTime();
        cal.set(2026, 3, 9, 10, 0);
        Date same = cal.getTime();

        assertTrue("Draw equal to event start should be valid",
                EventValidationUtils.isDrawDateValid(same, regEnd, same));
    }

    @Test
    public void testDrawDateValidationRejectsDrawBeforeOrAtRegEnd() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, 3, 5, 10, 0);
        Date regEnd = cal.getTime();
        cal.set(2026, 3, 9, 10, 0);
        Date eventStart = cal.getTime();

        // Draw before registration ends — registration still open when we draw, invalid.
        cal.set(2026, 3, 4, 10, 0);
        Date drawTooEarly = cal.getTime();
        assertFalse("Draw before regEnd should be invalid",
                EventValidationUtils.isDrawDateValid(drawTooEarly, regEnd, eventStart));

        // Draw exactly at registration end — the spec wants strict "after", so invalid.
        assertFalse("Draw equal to regEnd should be invalid",
                EventValidationUtils.isDrawDateValid(regEnd, regEnd, eventStart));
    }

    @Test
    public void testDrawDateValidationRejectsDrawAfterEventStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, 3, 5, 10, 0);
        Date regEnd = cal.getTime();
        cal.set(2026, 3, 9, 10, 0);
        Date eventStart = cal.getTime();
        // Draw after event already started — nonsense, invalid.
        cal.set(2026, 3, 10, 10, 0);
        Date drawTooLate = cal.getTime();

        assertFalse("Draw after event start should be invalid",
                EventValidationUtils.isDrawDateValid(drawTooLate, regEnd, eventStart));
    }

    // US 02.01.01 / US 02.05.02: Draw date is required — without it, EventAdapter's
    // pending_draw classification can never fire, so the event disappears from the
    // organizer's "needs sampling" workflow.
    @Test
    public void testDrawDateValidationNullsRejected() {
        Date d = new Date();
        assertFalse(EventValidationUtils.isDrawDateValid(null, d, d));
        assertFalse(EventValidationUtils.isDrawDateValid(d, null, d));
        assertFalse(EventValidationUtils.isDrawDateValid(d, d, null));
        assertFalse(EventValidationUtils.isDrawDateValid(null, null, null));
    }
}
