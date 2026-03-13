package com.example.lottery.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Unit tests for NotificationTypeUtil.
 *
 * <p>These tests verify correct interpretation of notification types
 * related to lottery results.
 *
 * <ul>
 *   <li>US 01.04.01 – notification when selected (win)</li>
 *   <li>US 01.04.02 – notification when not selected (lose)</li>
 * </ul>
 */
public class NotificationTypeUtilTest {

    @Test
    public void isWinningNotification_returnsTrue_forWinType() {
        assertTrue(NotificationTypeUtil.isWinningNotification("win"));
    }

    @Test
    public void isWinningNotification_returnsFalse_forLoseType() {
        assertFalse(NotificationTypeUtil.isWinningNotification("lose"));
    }

    @Test
    public void isLosingNotification_returnsTrue_forLoseType() {
        assertTrue(NotificationTypeUtil.isLosingNotification("lose"));
    }

    @Test
    public void isLosingNotification_returnsFalse_forWinType() {
        assertFalse(NotificationTypeUtil.isLosingNotification("win"));
    }
}