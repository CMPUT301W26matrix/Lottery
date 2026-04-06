package com.example.lottery.util;

import static com.example.lottery.util.EntrantEventFilterUtils.BROWSE_ALL;
import static com.example.lottery.util.EntrantEventFilterUtils.BROWSE_NEW;
import static com.example.lottery.util.EntrantEventFilterUtils.BROWSE_RECOMMENDED;
import static com.example.lottery.util.EntrantEventFilterUtils.CATEGORY_ALL;
import static com.example.lottery.util.EntrantEventFilterUtils.TIME_ALL_DATES;
import static com.example.lottery.util.EntrantEventFilterUtils.TIME_THIS_WEEK;
import static com.example.lottery.util.EntrantEventFilterUtils.TIME_TODAY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lottery.model.Event;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link EntrantEventFilterUtils}.
 */
public class EntrantEventFilterUtilsTest {

    private static final Date FIXED_NOW;

    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.NOVEMBER, 14, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        FIXED_NOW = cal.getTime();
    }

    private Event eventWithTitle(String title) {
        Event e = new Event();
        e.setTitle(title);
        return e;
    }

    private Event eventWithCategory(String category) {
        Event e = new Event();
        e.setCategory(category);
        return e;
    }

    private Event eventScheduledAt(Date date) {
        Event e = new Event();
        e.setScheduledDateTime(new Timestamp(date));
        return e;
    }

    private Event eventCreatedAt(Date date) {
        Event e = new Event();
        e.setCreatedAt(new Timestamp(date));
        return e;
    }

    private Event eventWithLimit(String eventId, Integer limit, Date deadline) {
        Event e = new Event();
        e.setEventId(eventId);
        e.setWaitingListLimit(limit);
        if (deadline != null) {
            e.setRegistrationDeadline(new Timestamp(deadline));
        }
        return e;
    }

    private Date offsetFromNow(long amount, TimeUnit unit) {
        return new Date(FIXED_NOW.getTime() + unit.toMillis(amount));
    }

    // US 01.01.05: A null query passes every event.
    @Test
    public void matchesSearchQuery_nullQuery_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Summer Book Club Meetup"), null));
    }

    // US 01.01.05: An empty query passes every event.
    @Test
    public void matchesSearchQuery_emptyQuery_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Summer Book Club Meetup"), ""));
    }

    // US 01.01.05: A whitespace-only query is treated as empty and passes every event.
    @Test
    public void matchesSearchQuery_whitespaceOnlyQuery_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Summer Book Club Meetup"), "   \t\n"));
    }

    // US 01.01.05: The title match is case-insensitive on the query side.
    @Test
    public void matchesSearchQuery_upperCaseQuery_matchesLowerCaseTitle() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Advanced Dance Workshop"), "DANCE"));
    }

    // US 01.01.05: The title match is case-insensitive on the title side.
    @Test
    public void matchesSearchQuery_lowerCaseQuery_matchesUpperCaseTitle() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("POTTERY CLASS"), "pottery"));
    }

    // US 01.01.05: A partial substring inside the title matches.
    @Test
    public void matchesSearchQuery_partialSubstring_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Beginner Swimming Lessons"), "swim"));
    }

    // US 01.01.05: A query that does not appear anywhere in the title is rejected.
    @Test
    public void matchesSearchQuery_nonMatchingQuery_returnsFalse() {
        assertFalse(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Pottery Class"), "dance"));
    }

    // US 01.01.05: A null event title cannot satisfy a non-empty query.
    @Test
    public void matchesSearchQuery_nullTitle_returnsFalseForNonEmptyQuery() {
        Event e = new Event();
        e.setTitle(null);
        assertFalse(EntrantEventFilterUtils.matchesSearchQuery(e, "dance"));
    }

    // US 01.01.05: Leading and trailing whitespace in the query is trimmed before matching.
    @Test
    public void matchesSearchQuery_queryTrimmedBeforeMatching_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesSearchQuery(
                eventWithTitle("Advanced Dance Workshop"), "  dance  "));
    }

    // US 01.01.06: The "All" sentinel passes every event including events without a category.
    @Test
    public void matchesCategory_allSentinel_passesEveryEvent() {
        assertTrue(EntrantEventFilterUtils.matchesCategory(
                eventWithCategory("Sports"), CATEGORY_ALL));

        Event noCategory = new Event();
        noCategory.setCategory(null);
        assertTrue(EntrantEventFilterUtils.matchesCategory(noCategory, CATEGORY_ALL));
    }

    // US 01.01.06: Category matching is case-insensitive.
    @Test
    public void matchesCategory_caseInsensitiveMatch_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.matchesCategory(
                eventWithCategory("Sports"), "sports"));
    }

    // US 01.01.06: A specific category filter excludes events in other categories.
    @Test
    public void matchesCategory_mismatchingCategory_returnsFalse() {
        assertFalse(EntrantEventFilterUtils.matchesCategory(
                eventWithCategory("Music"), "Sports"));
    }

    // US 01.01.06: An event with a null category is excluded by any specific category filter.
    @Test
    public void matchesCategory_nullEventCategory_returnsFalseForSpecificFilter() {
        Event e = new Event();
        e.setCategory(null);
        assertFalse(EntrantEventFilterUtils.matchesCategory(e, "Sports"));
    }

    // US 01.01.06: A null filter argument defaults to matching every event.
    @Test
    public void matchesCategory_nullFilter_passesEveryEvent() {
        assertTrue(EntrantEventFilterUtils.matchesCategory(
                eventWithCategory("Sports"), null));
    }

    // US 01.01.04: Past-scheduled events are excluded under every filter including "All Dates".
    @Test
    public void matchesTimeFilter_pastEventAlwaysExcluded() {
        Event past = eventScheduledAt(offsetFromNow(-1, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(past, TIME_ALL_DATES, FIXED_NOW));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(past, TIME_TODAY, FIXED_NOW));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(past, TIME_THIS_WEEK, FIXED_NOW));
    }

    // US 01.01.04: "All Dates" passes events scheduled far in the future.
    @Test
    public void matchesTimeFilter_allDatesPassesFarFutureEvent() {
        Event farFuture = eventScheduledAt(offsetFromNow(30, TimeUnit.DAYS));
        assertTrue(EntrantEventFilterUtils.matchesTimeFilter(farFuture, TIME_ALL_DATES, FIXED_NOW));
    }

    // US 01.01.04: "All Dates" also passes events with no scheduled time.
    @Test
    public void matchesTimeFilter_allDatesPassesEventWithNullSchedule() {
        Event noSchedule = new Event();
        noSchedule.setScheduledDateTime(null);
        assertTrue(EntrantEventFilterUtils.matchesTimeFilter(noSchedule, TIME_ALL_DATES, FIXED_NOW));
    }

    // US 01.01.04: Specific filters exclude events with null scheduled time.
    @Test
    public void matchesTimeFilter_specificFiltersExcludeNullSchedule() {
        Event noSchedule = new Event();
        noSchedule.setScheduledDateTime(null);
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(noSchedule, TIME_TODAY, FIXED_NOW));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(noSchedule, TIME_THIS_WEEK, FIXED_NOW));
    }

    // US 01.01.04: The "Today" filter excludes events scheduled for tomorrow.
    @Test
    public void matchesTimeFilter_todayExcludesTomorrow() {
        Event tomorrow = eventScheduledAt(offsetFromNow(1, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(tomorrow, TIME_TODAY, FIXED_NOW));
    }

    // US 01.01.04: The "Today" filter includes events scheduled later today.
    @Test
    public void matchesTimeFilter_todayIncludesLaterToday() {
        Event laterToday = eventScheduledAt(offsetFromNow(1, TimeUnit.HOURS));
        assertTrue(EntrantEventFilterUtils.matchesTimeFilter(laterToday, TIME_TODAY, FIXED_NOW));
    }

    // US 01.01.04: The "This Week" filter includes events six days out.
    @Test
    public void matchesTimeFilter_thisWeekIncludesSixDaysOut() {
        Event sixDays = eventScheduledAt(offsetFromNow(6, TimeUnit.DAYS));
        assertTrue(EntrantEventFilterUtils.matchesTimeFilter(sixDays, TIME_THIS_WEEK, FIXED_NOW));
    }

    // US 01.01.04: The "This Week" filter excludes events eight days out.
    @Test
    public void matchesTimeFilter_thisWeekExcludesEightDaysOut() {
        Event eightDays = eventScheduledAt(offsetFromNow(8, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.matchesTimeFilter(eightDays, TIME_THIS_WEEK, FIXED_NOW));
    }

    // US 01.01.04: A past registration deadline excludes the event from the spots-available filter.
    @Test
    public void hasAvailableSpots_pastDeadline_returnsFalse() {
        Event e = eventWithLimit("past-evt", 10, offsetFromNow(-1, TimeUnit.DAYS));
        Map<String, Integer> counts = new HashMap<>();
        counts.put("past-evt", 1);
        assertFalse(EntrantEventFilterUtils.hasAvailableSpots(e, counts, FIXED_NOW));
    }

    // US 01.01.04: A null waitingListLimit represents unlimited capacity and always passes.
    @Test
    public void hasAvailableSpots_nullLimit_returnsTrue() {
        Event e = eventWithLimit("unlimited-evt", null, offsetFromNow(7, TimeUnit.DAYS));
        assertTrue(EntrantEventFilterUtils.hasAvailableSpots(e, new HashMap<>(), FIXED_NOW));
    }

    // US 01.01.04: An event with one spot remaining below its limit passes.
    @Test
    public void hasAvailableSpots_oneBelowLimit_returnsTrue() {
        Event e = eventWithLimit("has-room", 10, offsetFromNow(7, TimeUnit.DAYS));
        Map<String, Integer> counts = new HashMap<>();
        counts.put("has-room", 9);
        assertTrue(EntrantEventFilterUtils.hasAvailableSpots(e, counts, FIXED_NOW));
    }

    // US 01.01.04: An event exactly at its waitlist limit is excluded.
    @Test
    public void hasAvailableSpots_exactlyAtLimit_returnsFalse() {
        Event e = eventWithLimit("full-evt", 5, offsetFromNow(7, TimeUnit.DAYS));
        Map<String, Integer> counts = new HashMap<>();
        counts.put("full-evt", 5);
        assertFalse(EntrantEventFilterUtils.hasAvailableSpots(e, counts, FIXED_NOW));
    }

    // US 01.01.04: A missing entry in the counts map is treated as unknown and excluded.
    @Test
    public void hasAvailableSpots_missingCount_returnsFalse() {
        Event e = eventWithLimit("unknown-evt", 10, offsetFromNow(7, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.hasAvailableSpots(e, new HashMap<>(), FIXED_NOW));
    }

    // US 01.01.04: A null counts map excludes any event with a finite limit for safety.
    @Test
    public void hasAvailableSpots_nullCountsMap_excludesLimitedEvent() {
        Event e = eventWithLimit("limited-evt", 10, offsetFromNow(7, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.hasAvailableSpots(e, null, FIXED_NOW));
    }

    // US 01.01.03: An event created three days ago is still within the "new" window.
    @Test
    public void isNewEvent_createdThreeDaysAgo_returnsTrue() {
        Event e = eventCreatedAt(offsetFromNow(-3, TimeUnit.DAYS));
        assertTrue(EntrantEventFilterUtils.isNewEvent(e, FIXED_NOW));
    }

    // US 01.01.03: An event created eight days ago is outside the "new" window.
    @Test
    public void isNewEvent_createdEightDaysAgo_returnsFalse() {
        Event e = eventCreatedAt(offsetFromNow(-8, TimeUnit.DAYS));
        assertFalse(EntrantEventFilterUtils.isNewEvent(e, FIXED_NOW));
    }

    // US 01.01.03: An event with no createdAt timestamp is never considered new.
    @Test
    public void isNewEvent_nullCreatedAt_returnsFalse() {
        Event e = new Event();
        assertFalse(EntrantEventFilterUtils.isNewEvent(e, FIXED_NOW));
    }

    // US 01.01.04: An event whose category matches any stored interest is recommended.
    @Test
    public void isRecommendedEvent_matchingInterest_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.isRecommendedEvent(
                eventWithCategory("Sports"), Arrays.asList("Sports", "Music")));
    }

    // US 01.01.04: Interest matching is case-insensitive and whitespace-tolerant.
    @Test
    public void isRecommendedEvent_caseInsensitiveAndTrimmedMatch_returnsTrue() {
        assertTrue(EntrantEventFilterUtils.isRecommendedEvent(
                eventWithCategory("SPORTS"), Collections.singletonList("  sports  ")));
    }

    // US 01.01.04: An empty interests list never recommends any event.
    @Test
    public void isRecommendedEvent_emptyInterestList_returnsFalse() {
        assertFalse(EntrantEventFilterUtils.isRecommendedEvent(
                eventWithCategory("Sports"), Collections.emptyList()));
    }

    // US 01.01.04: An event with a null category never matches any interest.
    @Test
    public void isRecommendedEvent_nullEventCategory_returnsFalse() {
        Event e = new Event();
        e.setCategory(null);
        assertFalse(EntrantEventFilterUtils.isRecommendedEvent(
                e, Collections.singletonList("Sports")));
    }

    // US 01.01.03: The "All" browse tab passes every event regardless of interests.
    @Test
    public void matchesBrowseTab_allPassesEveryEvent() {
        assertTrue(EntrantEventFilterUtils.matchesBrowseTab(
                eventWithTitle("Summer Book Club Meetup"), BROWSE_ALL, FIXED_NOW, Collections.emptyList()));
    }

    // US 01.01.03: The "New" browse tab dispatches to the new-event window check.
    @Test
    public void matchesBrowseTab_newDispatchesToNewWindow() {
        Event recent = eventCreatedAt(offsetFromNow(-1, TimeUnit.DAYS));
        Event older = eventCreatedAt(offsetFromNow(-30, TimeUnit.DAYS));
        assertTrue(EntrantEventFilterUtils.matchesBrowseTab(recent, BROWSE_NEW, FIXED_NOW, null));
        assertFalse(EntrantEventFilterUtils.matchesBrowseTab(older, BROWSE_NEW, FIXED_NOW, null));
    }

    // US 01.01.03: The "Recommended" browse tab dispatches to the interest match.
    @Test
    public void matchesBrowseTab_recommendedDispatchesToInterestMatch() {
        Event sports = eventWithCategory("Sports");
        assertTrue(EntrantEventFilterUtils.matchesBrowseTab(
                sports, BROWSE_RECOMMENDED, FIXED_NOW, Collections.singletonList("Sports")));
        assertFalse(EntrantEventFilterUtils.matchesBrowseTab(
                sports, BROWSE_RECOMMENDED, FIXED_NOW, Collections.emptyList()));
    }
}
