package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.model.User;

import org.hamcrest.Matcher;
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
        intent.putExtra("role", "admin");
        return ActivityScenario.launch(intent);
    }

    private static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    @Test
    public void adminBrowseProfilesActivity_launchesSuccessfully() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(isDisplayed()));
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
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
            onView(withId(R.id.tvEmptyProfiles))
                    .check(matches(withText("There are no user profiles in the system.")));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_emptyMessageViewExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvEmptyProfiles))
                    .check(matches(withText("There are no user profiles in the system.")));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_titleIsCorrect() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
        }
    }

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
            Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    private void prepareSingleProfileAndClickFirstRow(ActivityScenario<AdminBrowseProfilesActivity> scenario) {
        scenario.onActivity(activity -> {
            ListView listView = activity.findViewById(R.id.lvProfiles);
            Button enableDeleteButton = activity.findViewById(R.id.btnEnableDeleteProfile);

            @SuppressWarnings("unchecked")
            ProfileAdapter adapter = (ProfileAdapter) listView.getAdapter();

            adapter.clear();
            adapter.add(new User("user-123", "Alice", "alice@email.com", "7801234567"));
            adapter.notifyDataSetChanged();

            listView.setVisibility(View.VISIBLE);
            listView.requestLayout();
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        scenario.onActivity(activity -> {
            ListView listView = activity.findViewById(R.id.lvProfiles);
            Button enableDeleteButton = activity.findViewById(R.id.btnEnableDeleteProfile);

            enableDeleteButton.performClick();

            View firstVisibleChild = listView.getChildAt(0);
            Assert.assertNotNull("First visible list item should not be null", firstVisibleChild);

            listView.performItemClick(
                    firstVisibleChild,
                    0,
                    listView.getAdapter().getItemId(0)
            );
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationDialogShows() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            prepareSingleProfileAndClickFirstRow(scenario);
            onView(isRoot()).perform(waitFor(500));

            onView(withText("Delete Profile"))
                    .check(matches(isDisplayed()));
            onView(withText("Delete profile for Alice?"))
                    .check(matches(isDisplayed()));
            onView(withText("Confirm"))
                    .check(matches(isDisplayed()));
            onView(withText("Cancel"))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationCancelDismissesDialog() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            prepareSingleProfileAndClickFirstRow(scenario);
            onView(isRoot()).perform(waitFor(500));

            onView(withText("Delete Profile"))
                    .check(matches(isDisplayed()));
            onView(withText("Cancel"))
                    .check(matches(isDisplayed()));

            onView(withText("Cancel")).perform(click());
            onView(isRoot()).perform(waitFor(300));

            onView(withText("Delete Profile"))
                    .check(doesNotExist());

            onView(withId(R.id.btnEnableDeleteProfile))
                    .check(matches(isDisplayed()))
                    .check(matches(withText("Enable Deletion")));
        }
    }
}