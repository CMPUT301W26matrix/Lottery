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
 * Android instrumentation tests for {@link NotificationsActivity}.
 *
 * <p>These tests verify that the notifications screen loads correctly
 * for a specific user and displays the list of notifications.
 *
 * <ul>
 *   <li><b>US 01.04.01</b> – Receive a notification when selected to participate
 *       from the waiting list (win the lottery).</li>
 *   <li><b>US 01.04.02</b> – Receive a notification when not selected to participate
 *       from the waiting list (lose the lottery).</li>
 * </ul>
 *
 * <p>The activity is launched using {@link ActivityScenario}, and Espresso
 * assertions confirm that the notifications RecyclerView is visible.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

    /**
     * Verifies that the NotificationsActivity launches successfully
     * and displays the notifications list for the specified user.
     *
     * <p>This confirms that the UI component responsible for displaying
     * notifications is present and visible.
     */
    @Test
    public void notificationsScreen_opensSuccessfully() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                NotificationsActivity.class
        );

        intent.putExtra(NotificationsActivity.EXTRA_USER_ID, "6xygP8FXpxATgAkKmj27");

        try (ActivityScenario<NotificationsActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.rvNotifications))
                    .check(matches(isDisplayed()));
        }
    }
}