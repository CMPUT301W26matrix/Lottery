package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.init;
import static androidx.test.espresso.intent.Intents.release;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for {@link EntrantProfileActivity}.
 *
 * <p>These tests verify navigation from the entrant profile screen
 * to the edit profile screen.</p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantProfileActivityTest {

    /**
     * Initializes Espresso intents before each test.
     */
    @Before
    public void setUp() {
        init();
    }

    /**
     * Releases Espresso intents after each test.
     */
    @After
    public void tearDown() {
        release();
    }

    /**
     * Verifies that tapping the Edit Profile row opens EditProfileActivity.
     */
    @Test
    public void clickingEditProfile_opensEditProfileActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantProfileActivity.class
        );
        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EntrantProfileActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rl_edit_profile)).perform(click());
            intended(hasComponent(EditProfileActivity.class.getName()));
        }
    }
    /**
     * Verifies that tapping the Notification Settings row
     * opens NotificationSettingsActivity.
     */
    @Test
    public void clickingNotificationSettings_opensNotificationSettingsActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantProfileActivity.class
        );
        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EntrantProfileActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rl_notification_settings)).perform(click());
            intended(hasComponent(NotificationSettingsActivity.class.getName()));
        }
    }
}