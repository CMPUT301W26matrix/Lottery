package com.example.lottery.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link NotificationPreferenceUtil}.
 */
public class NotificationPreferenceUtilTest {

    /**
     * Verifies that null defaults to enabled.
     */
    @Test
    public void shouldReceiveNewNotifications_null_returnsTrue() {
        assertTrue(NotificationPreferenceUtil.shouldReceiveNewNotifications(null));
    }

    /**
     * Verifies that true allows notifications.
     */
    @Test
    public void shouldReceiveNewNotifications_true_returnsTrue() {
        assertTrue(NotificationPreferenceUtil.shouldReceiveNewNotifications(true));
    }

    /**
     * Verifies that false blocks notifications.
     */
    @Test
    public void shouldReceiveNewNotifications_false_returnsFalse() {
        assertFalse(NotificationPreferenceUtil.shouldReceiveNewNotifications(false));
    }

    /**
     * Verifies that null maps to an enabled switch state.
     */
    @Test
    public void getSwitchState_null_returnsTrue() {
        assertTrue(NotificationPreferenceUtil.getSwitchState(null));
    }

    /**
     * Verifies the enabled status text.
     */
    @Test
    public void getStatusText_enabled_returnsExpectedMessage() {
        assertEquals(
                "You will receive notifications from organizers and admins.",
                NotificationPreferenceUtil.getStatusText(true)
        );
    }

    /**
     * Verifies the disabled status text.
     */
    @Test
    public void getStatusText_disabled_returnsExpectedMessage() {
        assertEquals(
                "You have opted out of notifications.",
                NotificationPreferenceUtil.getStatusText(false)
        );
    }
}