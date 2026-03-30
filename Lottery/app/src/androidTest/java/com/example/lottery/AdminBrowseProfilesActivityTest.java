package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.content.Intent;
import android.view.View;
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

/**
 * Instrumented tests for {@link AdminBrowseProfilesActivity}.
 * Covers US 03.05.01: As an administrator, I want to be able to browse profiles.
 * Covers US 03.02.01: As an administrator, I want to be able to remove profiles.
 * Covers US 03.07.01: As an administrator I want to remove organizers that violate app policy.
 */

@RunWith(AndroidJUnit4.class)
public class AdminBrowseProfilesActivityTest {

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

    private ActivityScenario<AdminBrowseProfilesActivity> launchAdminActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AdminBrowseProfilesActivity.class
        );
        intent.putExtra("role", "admin");
        return ActivityScenario.launch(intent);
    }

    // US 03.05.01: Admin profile browser should launch and display title
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

    // US 03.05.01: Admin should see a list view of user profiles
    @Test
    public void adminBrowseProfilesActivity_displaysProfilesList() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(withId(R.id.lvProfiles))
                    .check(matches(isDisplayed()));
        }
    }

    // US 03.05.01: Empty state should inform admin there are no profiles
    @Test
    public void adminBrowseProfilesActivity_hasCorrectEmptyMessageText() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.tvEmptyProfiles))
                    .check(matches(withText("There are no user profiles in the system.")));
        }
    }

    // US 03.05.01: Empty state message view should exist in layout
    @Test
    public void adminBrowseProfilesActivity_emptyMessageViewExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.tvEmptyProfiles))
                    .check(matches(withText("There are no user profiles in the system.")));
        }
    }

    // US 03.05.01: Page title should display "Browse Profiles"
    @Test
    public void adminBrowseProfilesActivity_titleIsCorrect() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
        }
    }

    // US 03.02.01: Delete button should be visible for profile removal
    @Test
    public void adminBrowseProfilesActivity_deleteButtonExists() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.btnEnableDeleteProfile))
                    .check(matches(isDisplayed()));
        }
    }

    // US 03.05.01: Non-admin access should be denied and activity finished
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

    /**
     * Injects mixed-role users into the adapter for filter tests.
     * Directly adds to the adapter since filteredUsers is the backing list;
     * clicking a filter button will re-filter from allUsers which we also populate.
     */
    private void injectMixedRoleUsers(ActivityScenario<AdminBrowseProfilesActivity> scenario) {
        scenario.onActivity(activity -> {
            User entrant = new User("e-1", "EntrantUser", "e@test.com", "");
            entrant.setRole("ENTRANT");
            User organizer = new User("o-1", "OrganizerUser", "o@test.com", "");
            organizer.setRole("ORGANIZER");
            User entrant2 = new User("e-2", "EntrantUser2", "e2@test.com", "");
            entrant2.setRole("ENTRANT");

            // Populate allUsers so filter buttons work correctly
            activity.allUsers.clear();
            activity.allUsers.add(entrant);
            activity.allUsers.add(organizer);
            activity.allUsers.add(entrant2);

            // Populate filteredUsers (adapter backing list) to show all initially
            activity.filteredUsers.clear();
            activity.filteredUsers.addAll(activity.allUsers);

            ListView listView = activity.findViewById(R.id.lvProfiles);
            listView.setVisibility(View.VISIBLE);
            ((ProfileAdapter) listView.getAdapter()).notifyDataSetChanged();
            listView.requestLayout();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    // US 03.05.01: Filter buttons (All, Entrant, Organizer) should be visible
    @Test
    public void filterButtons_allThreeDisplayed() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.btnFilterAll)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterEntrant)).check(matches(isDisplayed()));
            onView(withId(R.id.btnFilterOrganizer)).check(matches(isDisplayed()));
        }
    }

    // US 03.05.01: Filter buttons should display correct role labels
    @Test
    public void filterButtons_showCorrectLabels() {
        try (ActivityScenario<AdminBrowseProfilesActivity> ignored = launchAdminActivity()) {
            onView(withId(R.id.btnFilterAll)).check(matches(withText("All")));
            onView(withId(R.id.btnFilterEntrant)).check(matches(withText("Entrant")));
            onView(withId(R.id.btnFilterOrganizer)).check(matches(withText("Organizer")));
        }
    }

    // US 03.05.01: Entrant filter should show only entrant profiles
    @Test
    public void filterEntrant_showsOnlyEntrants() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            injectMixedRoleUsers(scenario);

            onView(withId(R.id.btnFilterEntrant)).perform(click());
            onView(isRoot()).perform(waitFor(300));

            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.lvProfiles);
                Assert.assertEquals(2, listView.getAdapter().getCount());
            });
        }
    }

    // US 03.05.01: Organizer filter should show only organizer profiles
    @Test
    public void filterOrganizer_showsOnlyOrganizers() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            injectMixedRoleUsers(scenario);

            onView(withId(R.id.btnFilterOrganizer)).perform(click());
            onView(isRoot()).perform(waitFor(300));

            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.lvProfiles);
                Assert.assertEquals(1, listView.getAdapter().getCount());
            });
        }
    }

    // US 03.05.01: All filter should show every user profile
    @Test
    public void filterAll_showsAllUsers() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            injectMixedRoleUsers(scenario);

            // Switch to Organizer first, then back to All
            onView(withId(R.id.btnFilterOrganizer)).perform(click());
            onView(isRoot()).perform(waitFor(300));
            onView(withId(R.id.btnFilterAll)).perform(click());
            onView(isRoot()).perform(waitFor(300));

            scenario.onActivity(activity -> {
                ListView listView = activity.findViewById(R.id.lvProfiles);
                Assert.assertEquals(3, listView.getAdapter().getCount());
            });
        }
    }

    // US 03.05.01: Empty state should show when no users match the filter
    @Test
    public void filterOrganizer_emptyState_showsMessage() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            // Inject only entrants into allUsers
            scenario.onActivity(activity -> {
                User entrant = new User("e-1", "EntrantOnly", "e@test.com", "");
                entrant.setRole("ENTRANT");

                activity.allUsers.clear();
                activity.allUsers.add(entrant);
                activity.filteredUsers.clear();
                activity.filteredUsers.add(entrant);

                ListView listView = activity.findViewById(R.id.lvProfiles);
                listView.setVisibility(View.VISIBLE);
                ((ProfileAdapter) listView.getAdapter()).notifyDataSetChanged();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withId(R.id.btnFilterOrganizer)).perform(click());
            onView(isRoot()).perform(waitFor(300));

            onView(withId(R.id.tvEmptyProfiles)).check(matches(isDisplayed()));
        }
    }

    // US 03.07.01: Deleting an organizer should warn about cascading event deletion
    @Test
    public void deleteOrganizer_dialogShowsCascadeWarning() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            scenario.onActivity(activity -> {
                User organizer = new User("o-1", "BadOrganizer", "bad@test.com", "");
                organizer.setRole("ORGANIZER");

                activity.allUsers.clear();
                activity.allUsers.add(organizer);
                activity.filteredUsers.clear();
                activity.filteredUsers.add(organizer);

                ListView listView = activity.findViewById(R.id.lvProfiles);
                listView.setVisibility(View.VISIBLE);
                ((ProfileAdapter) listView.getAdapter()).notifyDataSetChanged();
                listView.requestLayout();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity ->
                    activity.showDeleteConfirmationDialog(activity.filteredUsers.get(0))
            );
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            // Verify dialog elements
            onView(withText("Delete Profile")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(containsString("All events created by this organizer will also be deleted.")))
                    .inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(containsString("BadOrganizer"))).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
        }
    }

    private void prepareSingleProfileAndClickFirstRow(ActivityScenario<AdminBrowseProfilesActivity> scenario) {
        scenario.onActivity(activity -> {
            User alice = new User("user-123", "Alice", "alice@email.com", "7801234567");

            activity.allUsers.clear();
            activity.allUsers.add(alice);
            activity.filteredUsers.clear();
            activity.filteredUsers.add(alice);

            ListView listView = activity.findViewById(R.id.lvProfiles);
            listView.setVisibility(View.VISIBLE);
            ((ProfileAdapter) listView.getAdapter()).notifyDataSetChanged();
            listView.requestLayout();
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        scenario.onActivity(activity ->
                activity.showDeleteConfirmationDialog(activity.filteredUsers.get(0))
        );

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    // US 03.02.01: Delete action should show confirmation dialog with user name
    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationDialogShows() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            prepareSingleProfileAndClickFirstRow(scenario);

            onView(withText("Delete Profile"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("Delete profile for Alice?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("Confirm"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("Cancel"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
        }
    }

    // US 03.02.01: Cancelling deletion should dismiss dialog and re-enable button
    @Test
    public void adminBrowseProfilesActivity_deleteConfirmationCancelDismissesDialog() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            prepareSingleProfileAndClickFirstRow(scenario);

            onView(withText("Delete Profile"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("Cancel"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            onView(withText("Delete Profile"))
                    .check(doesNotExist());

            onView(withId(R.id.btnEnableDeleteProfile))
                    .check(matches(isDisplayed()))
                    .check(matches(withText("Enable Deletion")));
        }
    }
}
