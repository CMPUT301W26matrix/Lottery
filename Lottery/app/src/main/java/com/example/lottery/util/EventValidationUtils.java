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
     * Validates that the event end date is after the event start date.
     *
     * @param startDate The event start date.
     * @param endDate   The event end date.
     * @return true if endDate is strictly after startDate, or if either is null (optional fields).
     */
    public static boolean isEventEndDateValid(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.after(startDate);
    }

    /**
     * Validates that registration start is before registration end.
     *
     * @param regStart The registration start date.
     * @param regEnd   The registration end date.
     * @return true if regStart is strictly before regEnd, or if either is null (optional fields).
     */
    public static boolean isRegistrationStartValid(Date regStart, Date regEnd) {
        if (regStart == null || regEnd == null) {
            return true;
        }
        return regStart.before(regEnd);
    }

    /**
     * Validates the draw date against the rest of the event timeline. The draw
     * date is required: it must happen strictly after registration closes (so
     * registration is already closed when the draw runs) and on or before the
     * event start (so the draw does not run after attendees have already shown
     * up). Events without a draw date also disappear from the pending_draw
     * workflow in {@link com.example.lottery.adapter.EventAdapter#resolveDisplayStatus},
     * so null is explicitly rejected here.
     *
     * @param drawDate   The draw date to validate (required).
     * @param regEnd     Registration deadline.
     * @param eventStart Scheduled event start.
     * @return true only if all three dates are non-null, drawDate is strictly
     * after regEnd, and drawDate is not after eventStart.
     */
    public static boolean isDrawDateValid(Date drawDate, Date regEnd, Date eventStart) {
        if (drawDate == null || regEnd == null || eventStart == null) {
            return false;
        }
        if (!drawDate.after(regEnd)) {
            return false;
        }
        return !drawDate.after(eventStart);
    }
}
