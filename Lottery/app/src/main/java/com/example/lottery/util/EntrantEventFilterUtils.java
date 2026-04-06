package com.example.lottery.util;

import com.example.lottery.model.Event;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Pure filter predicates for the entrant Explore event list.
 * Extracted from {@link com.example.lottery.entrant.EntrantMainActivity} so the
 * filtering rules can be unit tested without launching an Activity.
 */
public final class EntrantEventFilterUtils {

    public static final String BROWSE_ALL = "All";
    public static final String BROWSE_NEW = "New";
    public static final String BROWSE_RECOMMENDED = "Recommended";

    public static final String TIME_ALL_DATES = "All Dates";
    public static final String TIME_TODAY = "Today";
    public static final String TIME_THIS_WEEK = "This Week";
    public static final String TIME_THIS_MONTH = "Next 30 Days";

    public static final String CATEGORY_ALL = "All";

    private static final long NEW_EVENT_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private EntrantEventFilterUtils() {
    }

    /**
     * Case-insensitive substring match of the event title against the query.
     */
    public static boolean matchesSearchQuery(Event event, String rawQuery) {
        if (rawQuery == null) {
            return true;
        }
        String normalized = rawQuery.toLowerCase().trim();
        if (normalized.isEmpty()) {
            return true;
        }
        if (event == null || event.getTitle() == null) {
            return false;
        }
        return event.getTitle().toLowerCase().contains(normalized);
    }

    /**
     * True if the event category matches the selected filter.
     */
    public static boolean matchesCategory(Event event, String category) {
        if (category == null || CATEGORY_ALL.equalsIgnoreCase(category)) {
            return true;
        }
        if (event == null || event.getCategory() == null) {
            return false;
        }
        return event.getCategory().equalsIgnoreCase(category);
    }

    /**
     * True if the event's scheduled time falls within the selected time window.
     */
    public static boolean matchesTimeFilter(Event event, String timeFilter, Date now) {
        if (event == null || now == null) {
            return false;
        }
        Timestamp scheduled = event.getScheduledDateTime();

        // Past-scheduled events are always excluded regardless of selected filter.
        if (scheduled != null && scheduled.toDate().before(now)) {
            return false;
        }
        if (timeFilter == null || TIME_ALL_DATES.equals(timeFilter)) {
            return true;
        }
        // Specific filters need a concrete scheduled date.
        if (scheduled == null) {
            return false;
        }
        Date eventDate = scheduled.toDate();
        if (TIME_TODAY.equals(timeFilter)) {
            return !eventDate.after(endOfDay(now));
        }
        if (TIME_THIS_WEEK.equals(timeFilter)) {
            return !eventDate.after(endOfWindow(now, 7));
        }
        if (TIME_THIS_MONTH.equals(timeFilter)) {
            return !eventDate.after(endOfWindow(now, 30));
        }
        // Unknown filter values default to pass.
        return true;
    }

    /**
     * True if the event has room on its waiting list and registration is still open.
     */
    public static boolean hasAvailableSpots(Event event, Map<String, Integer> waitlistCounts, Date now) {
        if (event == null || now == null) {
            return false;
        }
        if (event.getRegistrationDeadline() != null
                && event.getRegistrationDeadline().toDate().before(now)) {
            return false;
        }
        Integer limit = event.getWaitingListLimit();
        if (limit == null) {
            return true;
        }
        if (waitlistCounts == null || !waitlistCounts.containsKey(event.getEventId())) {
            return false;
        }
        Integer count = waitlistCounts.get(event.getEventId());
        return count != null && count < limit;
    }

    /**
     * True if the event was created within the last seven days.
     */
    public static boolean isNewEvent(Event event, Date now) {
        if (event == null || now == null || event.getCreatedAt() == null) {
            return false;
        }
        long ageMillis = now.getTime() - event.getCreatedAt().toDate().getTime();
        return ageMillis <= NEW_EVENT_WINDOW_MILLIS;
    }

    /**
     * True if the event's category matches any stored user interest.
     */
    public static boolean isRecommendedEvent(Event event, List<String> interests) {
        if (event == null || interests == null || interests.isEmpty()) {
            return false;
        }
        String category = event.getCategory();
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        String trimmedCategory = category.trim();
        for (String interest : interests) {
            if (interest != null && interest.trim().equalsIgnoreCase(trimmedCategory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches to the predicate for the selected browse tab.
     */
    public static boolean matchesBrowseTab(Event event, String browseTab, Date now, List<String> interests) {
        if (browseTab == null || BROWSE_ALL.equals(browseTab)) {
            return true;
        }
        if (BROWSE_NEW.equals(browseTab)) {
            return isNewEvent(event, now);
        }
        if (BROWSE_RECOMMENDED.equals(browseTab)) {
            return isRecommendedEvent(event, interests);
        }
        return true;
    }

    private static Date endOfDay(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    private static Date endOfWindow(Date now, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }
}
