package com.example.lottery.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link EventListUiStateUtil}.
 *
 * <p>Supports:
 * <b>US 01.01.03</b> – entrant can see a list of events available
 * to join the waiting list for.
 */
public class EventListUiStateUtilTest {

    @Test
    public void shouldShowEmptyState_returnsTrue_whenNoEventsExist() {
        assertTrue(EventListUiStateUtil.shouldShowEmptyState(0));
    }

    @Test
    public void shouldShowEmptyState_returnsFalse_whenEventsExist() {
        assertFalse(EventListUiStateUtil.shouldShowEmptyState(2));
    }

    @Test
    public void shouldShowEventList_returnsTrue_whenEventsExist() {
        assertTrue(EventListUiStateUtil.shouldShowEventList(2));
    }

    @Test
    public void shouldShowEventList_returnsFalse_whenNoEventsExist() {
        assertFalse(EventListUiStateUtil.shouldShowEventList(0));
    }
}