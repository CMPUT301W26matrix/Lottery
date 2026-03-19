package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for {@link NotificationSettingsActivity}.
 *
 * <p>These tests verify that the notification settings screen
 * displays the required UI elements.</p>
 */
@RunWith(AndroidJUnit4.class)
public class NotificationSettingsActivityTest {

    /**
     * Verifies that the notification switch is displayed.
     */
    @Test
    public void notificationSwitch_isDisplayed() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationSettingsActivity.class
        );
        intent.putExtra("userId", "testUser");

        try (ActivityScenario<NotificationSettingsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.switch_notifications)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the notification status text is displayed.
     */
    @Test
    public void notificationStatusText_isDisplayed() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationSettingsActivity.class
        );
        intent.putExtra("userId", "testUser");

        try (ActivityScenario<NotificationSettingsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tv_notification_status)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the notification switch can be toggled by the user.
     */
    @Test
    public void notificationSwitch_canBeClicked() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationSettingsActivity.class
        );
        intent.putExtra("userId", "testUser");

        try (ActivityScenario<NotificationSettingsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.switch_notifications)).perform(click());
        }
    }
}