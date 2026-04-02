package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.MainActivity;
import com.example.lottery.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link AdminProfileActivity}.
 * Covers US 03.09.01: As an administrator, I want to be able to switch
 *     between entrant and organizer roles.
 */
@RunWith(AndroidJUnit4.class)
public class AdminProfileActivityTest {

    private static final String TEST_ADMIN_ID = "admin_test_device";

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private Intent createAdminProfileIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminProfileActivity.class);
        intent.putExtra("userId", TEST_ADMIN_ID);
        return intent;
    }

    // US 03.09.01: Admin profile should display name and email
    @Test
    public void testAdminProfileScreenIsDisplayed() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(ViewMatchers.withId(R.id.tv_profile_name)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_profile_email)).check(matches(isDisplayed()));
        }
    }

    // US 03.09.01: Admin should not be able to edit their profile
    @Test
    public void testEditModeIsHidden() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.layout_profile_edit_container))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    // US 03.09.01: Admin should see "Switch to Entrant" button
    @Test
    public void testSwitchToEntrantButtonIsDisplayed() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.btn_switch_to_entrant)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_switch_to_entrant))
                    .check(matches(withText("Switch to Entrant")));
        }
    }

    // US 03.09.01: Admin should see "Switch to Organizer" button
    @Test
    public void testSwitchToOrganizerButtonIsDisplayed() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.btn_switch_to_organizer)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_switch_to_organizer))
                    .check(matches(withText("Switch to Organizer")));
        }
    }

    // US 03.09.01: Admin should see role switching section header
    @Test
    public void testRoleSwitchingHeaderIsDisplayed() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.tv_actions_header)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_actions_header))
                    .check(matches(withText("ACTIONS")));
        }
    }

    // US 03.09.01: Admin profile should show logout button
    @Test
    public void testLogoutButtonIsDisplayed() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.btn_log_out)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_log_out)).check(matches(withText("Log Out")));
        }
    }

    // US 03.09.01: Admin bottom navigation should include settings tab
    @Test
    public void testBottomNavIsComplete() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profiles)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_images)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_logs)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_admin_settings)).check(matches(isDisplayed()));
        }
    }

    // US 03.09.01: Clicking logout should navigate back to MainActivity
    @Test
    public void testLogoutNavigatesToMainActivity() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.btn_log_out)).perform(click());
            intended(hasComponent(MainActivity.class.getName()));
        }
    }

    // US 03.09.01: Home nav should navigate to AdminBrowseEventsActivity
    @Test
    public void testHomeNavNavigatesToEventBrowser() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.nav_home)).perform(click());
            intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
        }
    }

    // US 03.09.01: Profiles nav should navigate to AdminBrowseProfilesActivity
    @Test
    public void testProfilesNavNavigatesToProfileBrowser() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.nav_profiles)).perform(click());
            intended(hasComponent(AdminBrowseProfilesActivity.class.getName()));
        }
    }

    // US 03.09.01: Images nav should navigate to AdminBrowseImagesActivity
    @Test
    public void testImagesNavNavigatesToImageBrowser() {
        try (ActivityScenario<AdminProfileActivity> ignored =
                     ActivityScenario.launch(createAdminProfileIntent())) {
            onView(withId(R.id.nav_images)).perform(click());
            intended(hasComponent(AdminBrowseImagesActivity.class.getName()));
        }
    }
}
