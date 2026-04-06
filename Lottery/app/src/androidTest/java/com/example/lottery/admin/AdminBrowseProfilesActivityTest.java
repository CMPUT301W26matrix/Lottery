package com.example.lottery.admin;

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
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.adapter.ProfileAdapter;
import com.example.lottery.model.User;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented tests for {@link AdminBrowseProfilesActivity}.
 * Covers US 03.05.01: As an administrator, I want to be able to browse profiles.
 * Covers US 03.02.01: As an administrator, I want to be able to remove profiles.
 * Covers US 03.07.01: As an administrator I want to remove organizers that violate app policy.
 */

@RunWith(AndroidJUnit4.class)
public class AdminBrowseProfilesActivityTest {
    private static final long FIRESTORE_TIMEOUT_SECONDS = 15;

    private static final String TEST_ENTRANT_ID = "entrant_liam_chen";
    private static final String TEST_ORGANIZER_ID = "organizer_coach_sophia_liu";
    private static final String TEST_EVENT_ID = "adult_swimming_course_saturday";
    private static final String TEST_EVENT_COMMENT_ID = "comment_swim_schedule";
    private static final String BROWSE_FIRESTORE_ENTRANT_ID = "admin_profile_entrant_liam_chen";
    private static final String BROWSE_FIRESTORE_ORGANIZER_ID = "admin_profile_organizer_sophia_liu";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    private void seedUser(String userId, String username, String email, String role, String phone)
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("email", email);
        user.put("role", role);
        user.put("phone", phone);
        user.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.USERS).document(userId).set(user),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void seedInboxEntry(String userId, String inboxId, String eventId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> inboxEntry = new HashMap<>();
        inboxEntry.put("eventId", eventId);
        inboxEntry.put("message", "Administrator review notice");
        inboxEntry.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.userInbox(userId)).document(inboxId).set(inboxEntry),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private String uniqueId(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private void ensureFirestoreNetworkEnabled()
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.enableNetwork(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void deleteAllDocuments(String collectionPath)
            throws InterruptedException, ExecutionException, TimeoutException {
        QuerySnapshot snapshots = Tasks.await(
                db.collection(collectionPath).get(),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
        if (snapshots.isEmpty()) return;
        WriteBatch batch = db.batch();
        for (DocumentSnapshot document : snapshots) {
            batch.delete(document.getReference());
        }
        Tasks.await(batch.commit(), FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void deleteDocument(String collectionPath, String documentId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(
                db.collection(collectionPath).document(documentId).delete(),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void deleteAllDocumentsQuietly(String collectionPath) {
        try {
            deleteAllDocuments(collectionPath);
        } catch (Exception ignored) {
        }
    }

    private void deleteDocumentQuietly(String collectionPath, String documentId) {
        try {
            deleteDocument(collectionPath, documentId);
        } catch (Exception ignored) {
        }
    }

    private boolean documentExists(String collectionPath, String documentId)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Tasks.await(
                db.collection(collectionPath).document(documentId).get(),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        ).exists();
    }

    private int countDocuments(String collectionPath)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Tasks.await(
                db.collection(collectionPath).get(),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        ).size();
    }

    private void waitForDocumentDeletion(String collectionPath, String documentId) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            if (!documentExists(collectionPath, documentId)) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.assertFalse("Document should be deleted: " + collectionPath + "/" + documentId,
                documentExists(collectionPath, documentId));
    }

    private void waitForEmptyCollection(String collectionPath) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            if (countDocuments(collectionPath) == 0) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.assertEquals("Collection should be empty: " + collectionPath, 0, countDocuments(collectionPath));
    }

    private User waitForLoadedUser(ActivityScenario<AdminBrowseProfilesActivity> scenario, String expectedUserId)
            throws InterruptedException {
        User[] found = new User[1];
        for (int attempt = 0; attempt < 40; attempt++) {
            scenario.onActivity(activity -> {
                for (User user : activity.allUsers) {
                    if (expectedUserId.equals(user.getUserId())) {
                        found[0] = user;
                        break;
                    }
                }
            });
            if (found[0] != null) {
                return found[0];
            }
            Thread.sleep(500);
        }
        Assert.fail("Expected profile " + expectedUserId + " to load from Firestore");
        return null;
    }

    private void waitForUsersLoaded(ActivityScenario<AdminBrowseProfilesActivity> scenario,
                                    String firstUserId,
                                    String secondUserId) throws InterruptedException {
        boolean[] loaded = {false};
        for (int attempt = 0; attempt < 40; attempt++) {
            scenario.onActivity(activity -> {
                boolean firstFound = false;
                boolean secondFound = false;
                for (User user : activity.allUsers) {
                    if (firstUserId.equals(user.getUserId())) firstFound = true;
                    if (secondUserId.equals(user.getUserId())) secondFound = true;
                }
                loaded[0] = firstFound && secondFound;
            });
            if (loaded[0]) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.assertTrue("Expected seeded Firestore profiles to load", loaded[0]);
    }

    private void cleanBrowseFirestoreUsers()
            throws InterruptedException, ExecutionException, TimeoutException {
        deleteDocument(FirestorePaths.USERS, BROWSE_FIRESTORE_ENTRANT_ID);
        deleteDocument(FirestorePaths.USERS, BROWSE_FIRESTORE_ORGANIZER_ID);
    }

    private void cleanProfileDeletionScenario(String entrantId,
                                              String organizerId,
                                              String eventId) {
        deleteAllDocumentsQuietly(FirestorePaths.userInbox(entrantId));
        deleteAllDocumentsQuietly(FirestorePaths.userInbox(organizerId));
        deleteAllDocumentsQuietly(FirestorePaths.eventComments(eventId));
        deleteAllDocumentsQuietly(FirestorePaths.eventCoOrganizers(eventId));
        deleteAllDocumentsQuietly(FirestorePaths.eventWaitingList(eventId));
        deleteDocumentQuietly(FirestorePaths.EVENTS, eventId);
        deleteDocumentQuietly(FirestorePaths.USERS, entrantId);
        deleteDocumentQuietly(FirestorePaths.USERS, organizerId);
    }

    // US 03.05.01: Admin profile browser should launch and display title
    @Test
    public void adminBrowseProfilesActivity_launchesSuccessfully() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            Assert.assertEquals(Lifecycle.State.RESUMED, scenario.getState());

            onView(ViewMatchers.withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(isDisplayed()));
            onView(withId(R.id.tvBrowseProfilesTitle))
                    .check(matches(withText("Browse Profiles")));
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
            User entrant = new User("e-1", "Liam Chen", "liam.chen@gmail.com", "");
            entrant.setRole("ENTRANT");
            User organizer = new User("o-1", "Sophia Liu", "sophia.liu@gmail.com", "");
            organizer.setRole("ORGANIZER");
            User entrant2 = new User("e-2", "Oliver Ng", "oliver.ng@gmail.com", "");
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
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnFilterEntrant).performClick();
                Assert.assertEquals(2, activity.filteredUsers.size());
                Assert.assertEquals("ENTRANT", activity.filteredUsers.get(0).getRole());
                Assert.assertEquals("ENTRANT", activity.filteredUsers.get(1).getRole());
            });
        }
    }

    // US 03.05.01: Organizer filter should show only organizer profiles
    @Test
    public void filterOrganizer_showsOnlyOrganizers() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            injectMixedRoleUsers(scenario);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnFilterOrganizer).performClick();
                Assert.assertEquals(1, activity.filteredUsers.size());
                Assert.assertEquals("ORGANIZER", activity.filteredUsers.get(0).getRole());
            });
        }
    }

    // US 03.05.01: All filter should show every user profile
    @Test
    public void filterAll_showsAllUsers() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            injectMixedRoleUsers(scenario);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnFilterOrganizer).performClick();
                activity.findViewById(R.id.btnFilterAll).performClick();
                Assert.assertEquals(3, activity.filteredUsers.size());
            });
        }
    }

    // US 03.05.01: Empty state should show when no users match the filter
    @Test
    public void filterOrganizer_emptyState_showsMessage() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            // Inject data, click filter, and assert atomically on the UI thread
            // to avoid race with the async Firestore loadProfiles() callback
            scenario.onActivity(activity -> {
                User entrant = new User("e-1", "Emma Wilson", "emma.wilson@gmail.com", "");
                entrant.setRole("ENTRANT");

                activity.allUsers.clear();
                activity.allUsers.add(entrant);
                activity.filteredUsers.clear();
                activity.filteredUsers.add(entrant);

                ListView listView = activity.findViewById(R.id.lvProfiles);
                listView.setVisibility(View.VISIBLE);
                ((ProfileAdapter) listView.getAdapter()).notifyDataSetChanged();

                // Click filter and assert in the same UI thread turn
                activity.findViewById(R.id.btnFilterOrganizer).performClick();

                TextView tvEmpty = activity.findViewById(R.id.tvEmptyProfiles);
                Assert.assertEquals(View.VISIBLE, tvEmpty.getVisibility());
            });
        }
    }

    // US 03.07.01: Deleting an organizer should warn about cascading event deletion
    @Test
    public void deleteOrganizer_dialogShowsCascadeWarning() {
        try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
            scenario.onActivity(activity -> {
                User organizer = new User("o-1", "Marcus Rivera", "marcus.rivera@gmail.com", "");
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
            onView(withText(containsString("Marcus Rivera"))).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
        }
    }

    private void prepareSingleProfileAndClickFirstRow(ActivityScenario<AdminBrowseProfilesActivity> scenario) {
        scenario.onActivity(activity -> {
            User alice = new User("user-123", "Alice Nguyen", "alice.nguyen@gmail.com", "7801234567");

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
            onView(withText("Delete profile for Alice Nguyen?"))
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
                    .check(matches(withText("Deletion")));
        }
    }

    // US 03.02.01: Confirming profile deletion should remove the entrant profile and its
    // related waiting list and inbox records from Firestore.
    @Test
    public void adminDeleteEntrantProfile_removesFirestoreDocuments() throws Exception {
        String entrantId = uniqueId("entrant_liam_chen");
        String organizerId = uniqueId("coach_maya_wilson");
        String eventId = uniqueId("swimming_course_saturday_morning");
        String inboxId = uniqueId("inbox_profile_cleanup");

        ensureFirestoreNetworkEnabled();
        seedUser(entrantId, "Liam Chen", "liam.chen@gmail.com", "ENTRANT", "7805550110");

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Saturday Swimming Skills Clinic");
        event.put("organizerId", organizerId);
        event.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

        Map<String, Object> waitingListEntry = new HashMap<>();
        waitingListEntry.put("userId", entrantId);
        waitingListEntry.put("status", "waitlisted");
        waitingListEntry.put("joinedAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId))
                        .document(entrantId)
                        .set(waitingListEntry),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
        seedInboxEntry(entrantId, inboxId, eventId);

        try {
            try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
                User selectedUser = waitForLoadedUser(scenario, entrantId);
                scenario.onActivity(activity -> activity.showDeleteConfirmationDialog(selectedUser));

                onView(withText("Delete Profile")).inRoot(isDialog()).check(matches(isDisplayed()));
                onView(withText("Confirm")).inRoot(isDialog()).perform(click());
            }

            waitForDocumentDeletion(FirestorePaths.USERS, entrantId);
            waitForDocumentDeletion(FirestorePaths.eventWaitingList(eventId), entrantId);
            waitForEmptyCollection(FirestorePaths.userInbox(entrantId));
        } finally {
            cleanProfileDeletionScenario(entrantId, organizerId, eventId);
        }
    }

    // US 03.07.01: Confirming organizer deletion should remove the organizer profile and
    // their owned event, including event comments.
    @Test
    public void adminDeleteOrganizerProfile_removesOrganizerAndOwnedEvent() throws Exception {
        String entrantId = uniqueId("entrant_oliver_ng");
        String organizerId = uniqueId("organizer_coach_sophia_liu");
        String eventId = uniqueId("swimming_course_for_beginners");
        String commentId = uniqueId("comment_pool_orientation");
        String inboxId = uniqueId("inbox_organizer_cleanup");

        ensureFirestoreNetworkEnabled();
        seedUser(organizerId, "Sophia Liu", "sophia.liu@gmail.com", "ORGANIZER", "7805550111");

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Swimming Course for Beginners");
        event.put("details", "A weekend lesson for new swimmers learning water safety and basic strokes.");
        event.put("organizerId", organizerId);
        event.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

        Map<String, Object> comment = new HashMap<>();
        comment.put("eventId", eventId);
        comment.put("authorId", organizerId);
        comment.put("authorName", "Sophia Liu");
        comment.put("authorRole", "organizer");
        comment.put("content", "Please arrive 15 minutes early for the pool orientation.");
        comment.put("createdAt", Timestamp.now());
        Tasks.await(
                db.collection(FirestorePaths.eventComments(eventId))
                        .document(commentId)
                        .set(comment),
                FIRESTORE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
        seedInboxEntry(organizerId, inboxId, eventId);

        try {
            try (ActivityScenario<AdminBrowseProfilesActivity> scenario = launchAdminActivity()) {
                User selectedUser = waitForLoadedUser(scenario, organizerId);
                scenario.onActivity(activity -> activity.showDeleteConfirmationDialog(selectedUser));

                onView(withText("Delete Profile")).inRoot(isDialog()).check(matches(isDisplayed()));
                onView(withText("Confirm")).inRoot(isDialog()).perform(click());
            }

            waitForDocumentDeletion(FirestorePaths.USERS, organizerId);
            waitForDocumentDeletion(FirestorePaths.EVENTS, eventId);
            waitForDocumentDeletion(FirestorePaths.eventComments(eventId), commentId);
            waitForEmptyCollection(FirestorePaths.userInbox(organizerId));
        } finally {
            cleanProfileDeletionScenario(entrantId, organizerId, eventId);
        }
    }

    // US 03.05.01: Admin profile browser should load Firestore users, then filter the
    // loaded data so organizer-only and entrant-only views contain the expected roles.
    @Test
    public void adminBrowseProfiles_loadsFirestoreUsersAndFiltersByRole() throws Exception {
        ensureFirestoreNetworkEnabled();
        cleanBrowseFirestoreUsers();
        seedUser(BROWSE_FIRESTORE_ENTRANT_ID, "Liam Chen", "liam.chen@gmail.com", "ENTRANT", "7805550120");
        seedUser(BROWSE_FIRESTORE_ORGANIZER_ID, "Sophia Liu", "sophia.liu@gmail.com", "ORGANIZER", "7805550121");

        try {
            Intent intent = new Intent(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    AdminBrowseProfilesActivity.class
            );
            intent.putExtra("role", "admin");
            intent.putExtra("userId", "admin_jordan_clark");

            try (ActivityScenario<AdminBrowseProfilesActivity> scenario = ActivityScenario.launch(intent)) {
                waitForUsersLoaded(scenario, BROWSE_FIRESTORE_ENTRANT_ID, BROWSE_FIRESTORE_ORGANIZER_ID);

                scenario.onActivity(activity -> {
                    activity.findViewById(R.id.btnFilterOrganizer).performClick();
                    boolean organizerFound = false;
                    boolean entrantStillVisible = false;
                    for (User user : activity.filteredUsers) {
                        if (BROWSE_FIRESTORE_ORGANIZER_ID.equals(user.getUserId()))
                            organizerFound = true;
                        if (BROWSE_FIRESTORE_ENTRANT_ID.equals(user.getUserId()))
                            entrantStillVisible = true;
                    }
                    Assert.assertTrue("Organizer filter should include the seeded organizer", organizerFound);
                    Assert.assertFalse("Organizer filter should exclude the seeded entrant", entrantStillVisible);

                    activity.findViewById(R.id.btnFilterEntrant).performClick();
                    boolean entrantFound = false;
                    boolean organizerStillVisible = false;
                    for (User user : activity.filteredUsers) {
                        if (BROWSE_FIRESTORE_ENTRANT_ID.equals(user.getUserId()))
                            entrantFound = true;
                        if (BROWSE_FIRESTORE_ORGANIZER_ID.equals(user.getUserId()))
                            organizerStillVisible = true;
                    }
                    Assert.assertTrue("Entrant filter should include the seeded entrant", entrantFound);
                    Assert.assertFalse("Entrant filter should exclude the seeded organizer", organizerStillVisible);
                });
            }
        } finally {
            cleanBrowseFirestoreUsers();
        }
    }
}
