package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.anything;

import android.content.Intent;
import android.view.View;
import android.widget.ListView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import com.example.lottery.model.User;
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
                    .check(matches(withText("There are no user profiles in the system.")));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_emptyMessageViewExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
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

    @Test
    public void adminBrowseProfilesActivity_nonAdminAccessFinishesActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminBrowseProfilesActivity.class
        );

        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = ActivityScenario.launch(intent)) {
            // Activity calls finish() in onCreate() when role is missing; state becomes DESTROYED
            Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationDialogShowsSelectedUserName() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.lvProfiles);
                @SuppressWarnings("unchecked")
                ProfileAdapter adapter = (ProfileAdapter) listView.getAdapter();
                adapter.clear();
                adapter.add(new User("user-123", "Alice", "alice@email.com", "7801234567"));
                adapter.notifyDataSetChanged();
                listView.setVisibility(View.VISIBLE);
            });

            onView(withId(R.id.btnEnableDeleteProfile)).perform(click());
            onData(anything()).inAdapterView(withId(R.id.lvProfiles)).atPosition(0).perform(click());

            onView(withText("Delete Profile")).check(matches(isDisplayed()));
            onView(withText("Delete profile for Alice?")).check(matches(isDisplayed()));
            onView(withText("Confirm")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationCancelDismissesDialog() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.lvProfiles);
                @SuppressWarnings("unchecked")
                ProfileAdapter adapter = (ProfileAdapter) listView.getAdapter();
                adapter.clear();
                adapter.add(new User("user-123", "Alice", "alice@email.com", "7801234567"));
                adapter.notifyDataSetChanged();
                listView.setVisibility(View.VISIBLE);
            });

            onView(withId(R.id.btnEnableDeleteProfile)).perform(click());
            onData(anything()).inAdapterView(withId(R.id.lvProfiles)).atPosition(0).perform(click());
            onView(withText("Cancel")).perform(click());
            onView(withText("Delete Profile")).check(doesNotExist());
            onView(withId(R.id.btnEnableDeleteProfile)).check(matches(isDisplayed()));
        }
    }
}


