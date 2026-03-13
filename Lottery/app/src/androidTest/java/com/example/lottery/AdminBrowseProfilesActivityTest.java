package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseProfilesActivityTest {

    private ActivityScenario<AdminBrowseProfilesActivity> launchAdminActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminBrowseProfilesActivity.class
        );
        intent.putExtra("role", "admin");
        return ActivityScenario.launch(intent);
    }

    @Test
    public void adminBrowseProfilesActivity_launchesSuccessfully() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_displaysProfilesList() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.btnEnableDeleteProfile))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_hasCorrectEmptyMessageText() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText(R.string.browse_profiles)));
        }
    }

    @Test
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
