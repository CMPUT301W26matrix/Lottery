package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseProfilesActivityTest {

    private ActivityScenario<AdminBrowseProfilesActivity> launchAdminActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminBrowseProfilesActivity.class
        );

        // Change these keys only if your activity uses different extra names
        intent.putExtra("role", "administrator");
        intent.putExtra("userRole", "administrator");
        intent.putExtra("isAdmin", true);

        return ActivityScenario.launch(intent);
    }

    @Test
    public void adminBrowseProfilesActivity_launchesSuccessfully() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {

            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_displaysProfilesList() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {

            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.lvProfiles))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_hasCorrectEmptyMessageText() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {

            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.tvEmptyProfiles))
                    .check(matches(withText("There are no user profiles in the system")));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_emptyMessageViewExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
        }
    }
    public void adminBrowseProfilesActivity_titleIsCorrect() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
        }
    }
    // Verifies delete button exists (part of admin remove profile US 03.02.01)
    @Test
    public void adminBrowseProfilesActivity_deleteButtonExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.btnEnableDeleteProfile))
                    .check(matches(isDisplayed()));
        }
    }

}



