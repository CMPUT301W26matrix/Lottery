package com.example.lottery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lottery.util.EventValidationUtils;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests for EventValidationUtils.
 * Verifies business logic for event date validation (US 02.01.04).
 */
public class EventValidationUtilsTest {

    /**
     * Test Case: Deadline is strictly before event date.
     * Expected Result: true
     */
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

    /**
     * Test Case: Deadline is after event date.
     * Expected Result: false
     */
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

    /**
     * Test Case: Deadline is exactly equal to event date.
     * Expected Result: false (Strictly before required)
     */
    @Test
    public void testInvalidEqualDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, 10, 21, 10, 0);
        Date sameDate = cal.getTime();

        assertFalse("Deadline equal to event date should be invalid",
                EventValidationUtils.isRegistrationDeadlineValid(sameDate, sameDate));
    }

    /**
     * Test Case: Null values for either date.
     * Expected Result: false
     */
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
}
