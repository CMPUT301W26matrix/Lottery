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
    public void testWaitingListLimitValidation() {
        // Valid case: Positive integer (US 02.03.01 AC #3)
        assertTrue(EventValidationUtils.isWaitingListLimitValid(10));

        // Valid case: Null represents "Unlimited" (US 02.03.01 AC #2)
        assertTrue(EventValidationUtils.isWaitingListLimitValid(null));

        // Invalid case: Zero (Criteria says strictly positive)
        assertFalse(EventValidationUtils.isWaitingListLimitValid(0));

        // Invalid case: Negative number
        assertFalse(EventValidationUtils.isWaitingListLimitValid(-1));
    }
}
