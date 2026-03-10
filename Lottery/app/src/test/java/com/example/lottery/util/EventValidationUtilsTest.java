package com.example.lottery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Logic tests for event validation business rules.
 */
public class EventValidationUtilsTest {

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
