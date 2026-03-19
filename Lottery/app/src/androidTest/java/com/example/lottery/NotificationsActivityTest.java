package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for {@link NotificationsActivity}.
 *
 * <p>These tests verify that the notifications screen launches
 * and displays its main static UI elements.</p>
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

    /**
     * Verifies that the notifications screen header is displayed.
     */
    @Test
    public void notificationsHeader_isDisplayed() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationsActivity.class
        );
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, "testUser");

        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }
    /**
     * Verifies that the notifications RecyclerView exists on screen.
     */
    @Test
    public void notificationsRecyclerView_isDisplayed() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationsActivity.class
        );
        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, "testUser");

        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rvNotifications)).check(matches(isDisplayed()));
        }
    }
}