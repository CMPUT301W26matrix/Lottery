package com.example.lottery.util;

import java.util.Date;

/**
 * Utility class for validating event-related data.
 * Provides reusable logic for checking business rules defined in user stories.
 */
public class EventValidationUtils {

    /**
     * Validates if the registration deadline occurs before the event start time.
     * Required by US 02.01.04.
     *
     * @param deadline  The registration deadline date.
     * @param eventDate The scheduled start date of the event.
     * @return true if the deadline is strictly before the event date, false otherwise or if either is null.
     */
    public static boolean isRegistrationDeadlineValid(Date deadline, Date eventDate) {
        if (deadline == null || eventDate == null) {
            return false;
        }
        return deadline.before(eventDate);
    }

    /**
     * US 02.03.01 / US 02.02.02: Validates if the waiting list limit is a positive integer.
     * null is considered valid as it represents an "Unlimited" state.
     *
     * @param limit The waiting list limit to validate.
     * @return true if the limit is null or greater than zero.
     */
    public static boolean isWaitingListLimitValid(Integer limit) {
        return limit == null || limit > 0;
    }
}
