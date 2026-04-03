package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lottery.R;
import com.example.lottery.entrant.EntrantMapActivity;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UI tests for EntrantsListActivity.
 * Verifies that the organizer can switch between entrant list tabs and interact with functional components.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsListActivityTest {

    private static final String TEST_EVENT_ID =
            "test_event_id_" + Long.toHexString(System.currentTimeMillis());
    private static final String TEST_USER_ID = "test_user_id";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEntrantIds = new HashSet<>();
    private final Set<File> createdExportFiles = new HashSet<>();

    @Rule
    public ActivityScenarioRule<EntrantsListActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), EntrantsListActivity.class)
                    .putExtra("eventId", TEST_EVENT_ID)
                    .putExtra("userId", TEST_USER_ID));

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() throws Exception {
        for (String entrantId : seededEntrantIds) {
            Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                            .document(entrantId)
                            .delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }
        seededEntrantIds.clear();

        for (File file : createdExportFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
        createdExportFiles.clear();

        Intents.release();
    }

    private void seedAcceptedEntrant(String userId, String userName, String email) throws Exception {
        seededEntrantIds.add(userId);

        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", userName);
        record.put("email", email);
        record.put("status", InvitationFlowUtil.STATUS_ACCEPTED);
        record.put("registeredAt", now);
        record.put("acceptedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                        .document(userId)
                        .set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedWaitlistedEntrant(String userId, String userName, String email) throws Exception {
        seededEntrantIds.add(userId);

        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", userName);
        record.put("email", email);
        record.put("status", InvitationFlowUtil.STATUS_WAITLISTED);
        record.put("registeredAt", now);
        record.put("waitlistedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                        .document(userId)
                        .set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedInvitedEntrant(String userId, String userName, String email) throws Exception {
        seededEntrantIds.add(userId);

        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", userName);
        record.put("email", email);
        record.put("status", InvitationFlowUtil.STATUS_INVITED);
        record.put("registeredAt", now);
        record.put("invitedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                        .document(userId)
                        .set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void waitForActivityCondition(java.util.function.Predicate<EntrantsListActivity> condition)
            throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            activityRule.getScenario().onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) {
                return;
            }
            lastError = new AssertionError("Timed out waiting for entrants list condition");
            Thread.sleep(250);
        }
        throw lastError;
    }

    private File createExportTargetFile(String fileName) throws Exception {
        File documentsDir = ApplicationProvider.getApplicationContext()
                .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir == null) {
            throw new IllegalStateException("External documents directory is unavailable");
        }
        if (!documentsDir.exists() && !documentsDir.mkdirs()) {
            throw new IllegalStateException("Failed to create test documents directory");
        }

        File exportFile = new File(documentsDir, fileName);
        if (exportFile.exists() && !exportFile.delete()) {
            throw new IllegalStateException("Failed to reset export target file");
        }
        if (!exportFile.createNewFile()) {
            throw new IllegalStateException("Failed to create export target file");
        }
        createdExportFiles.add(exportFile);
        return exportFile;
    }

    private String waitForFileContents(File file) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            if (file.exists() && file.length() > 0) {
                try (
                        InputStream inputStream = new FileInputStream(file);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
                ) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return outputStream.toString(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    lastError = new AssertionError("Failed to read exported CSV", e);
                }
            }
            Thread.sleep(250);
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new AssertionError("Timed out waiting for exported CSV file");
    }

    private void waitForEntrantStatus(String entrantId, String expectedStatus) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            DocumentSnapshot snapshot = Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(TEST_EVENT_ID))
                            .document(entrantId)
                            .get(),
                    10,
                    TimeUnit.SECONDS
            );

            String normalized = InvitationFlowUtil.normalizeEntrantStatus(snapshot.getString("status"));
            if (expectedStatus.equals(normalized)) {
                return;
            }

            lastError = new AssertionError(
                    "Timed out waiting for entrant " + entrantId + " to reach status " + expectedStatus
            );
            Thread.sleep(250);
        }
        throw lastError;
    }

    private ViewAction clickChildViewWithId(int id) {
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(id);
                if (child != null) {
                    child.performClick();
                }
            }
        };
    }

    /**
     * US 02.02.01 / US 02.06.01 / US 02.06.02 / US 02.06.03: The organizer can
     * access each entrant status view and the map action from the entrants screen.
     */
    @Test
    public void testInitializedPageVisibility() {
        onView(ViewMatchers.withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_invited_btn)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_cancelled_btn)).perform(scrollTo()).check(matches(isDisplayed()));

        onView(withId(R.id.entrants_list_view_location_btn)).check(matches(isDisplayed()));
    }

    /**
     * US 02.06.03: Selecting the Signed Up tab shows the accepted entrants list
     * and hides the other entrant groups.
     */
    @Test
    public void testSwitchToSignedUpList() {
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());
        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(isDisplayed()));

        // Verify other layouts are hidden
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.invited_entrants_list_layout)).check(matches(not(isDisplayed())));
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * US 02.02.01: Returning to the Waited List tab restores the waitlist view
     * after navigating away to another entrant group.
     */
    @Test
    public void testSwitchToWaitedListedList() {
        // First switch to another tab to ensure we are testing a real transition
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());

        onView(withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo(), click());
        onView(withId(R.id.waited_list_entrants_list_layout)).check(matches(isDisplayed()));

        onView(withId(R.id.signed_up_entrants_list_layout)).check(matches(not(isDisplayed())));
    }

    /**
     * US 02.02.01: Waitlisted entrants seeded for the event are rendered in the
     * Waited List view instead of the empty state.
     */
    @Test
    public void testWaitedList_showsSeededWaitlistedEntrants() throws Exception {
        seedWaitlistedEntrant("waitlisted_user_1", "Waitlisted User One", "wait1@example.com");

        onView(withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.waited_list_empty_text)
                .getVisibility() == android.view.View.GONE);

        onView(allOf(withText("Waitlisted User One"),
                isDescendantOfA(withId(R.id.waited_list_events_view))))
                .check(matches(isDisplayed()));
    }

    /**
     * US 02.02.01: The Waited List view only shows waitlisted entrants and excludes
     * entrants currently in invited or accepted states.
     */
    @Test
    public void testWaitedList_filtersOutNonWaitlistedEntrants() throws Exception {
        seedWaitlistedEntrant("waitlisted_user_2", "Waitlisted Only", "wait2@example.com");
        seedInvitedEntrant("invited_user_hidden", "Invited Hidden", "invited@example.com");
        seedAcceptedEntrant("accepted_user_hidden", "Accepted Hidden", "accepted@example.com");

        onView(withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.waited_list_empty_text)
                .getVisibility() == android.view.View.GONE);

        onView(allOf(withText("Waitlisted Only"),
                isDescendantOfA(withId(R.id.waited_list_events_view))))
                .check(matches(isDisplayed()));
        onView(allOf(withText("Invited Hidden"),
                isDescendantOfA(withId(R.id.waited_list_events_view))))
                .check(doesNotExist());
        onView(allOf(withText("Accepted Hidden"),
                isDescendantOfA(withId(R.id.waited_list_events_view))))
                .check(doesNotExist());
    }

    /**
     * US 02.02.02: Viewing entrant locations launches the map screen for the
     * current event so the organizer can inspect all saved entrant locations.
     */
    @Test
    public void testViewLocationLaunchesMapForEvent() {
        onView(withId(R.id.entrants_list_invited_btn)).perform(scrollTo(), click());
        onView(withId(R.id.entrants_list_view_location_btn)).perform(click());

        intended(hasComponent(EntrantMapActivity.class.getName()));
        intended(hasExtra("eventId", TEST_EVENT_ID));
    }

    /**
     * US 02.05.02: The organizer can start the sampling workflow from the waitlist
     * tab by opening the draw dialog and entering a sample size.
     */
    @Test
    public void testClickSampleFragmentVisibility() {
        // Removed scrollTo() because entrants_list_sample_btn is not inside a ScrollView.
        onView(withId(R.id.entrants_list_sample_btn)).perform(click());
        // Check for dialog elements (SampleFragment uses AlertDialog)
        onView(withText("Sample Winners")).check(matches(isDisplayed()));
    }


    /**
     * US 02.02.01
     * Verifies that the empty state text is shown when no entrants exist in the Waited List tab.
     */
    @Test
    public void testEmptyStateWaitedList() {
        // Default tab is Waited List; with test_event_id there are no entrants
        onView(withId(R.id.waited_list_empty_text)).check(matches(isDisplayed()));
        onView(withId(R.id.waited_list_empty_text)).check(matches(withText(R.string.no_entrants_in_list)));
    }

    /**
     * US 02.06.01
     * Verifies that the empty state text is shown when switching to the Invited tab with no entrants.
     */
    @Test
    public void testEmptyStateInvitedList() {
        onView(withId(R.id.entrants_list_invited_btn)).perform(scrollTo(), click());
        onView(withId(R.id.invited_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.06.03
     * Verifies that the empty state text is shown when switching to the Accepted tab with no entrants.
     */
    @Test
    public void testEmptyStateSignedUpList() {
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());
        onView(withId(R.id.signed_up_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.06.02
     * Verifies that the empty state text is shown when switching to the Cancelled tab with no entrants.
     */
    @Test
    public void testEmptyStateCancelledList() {
        onView(withId(R.id.entrants_list_cancelled_btn)).perform(scrollTo(), click());
        onView(withId(R.id.cancelled_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.05.03
     * Verifies that the empty state text is shown when switching to the Not Selected tab with no entrants.
     */
    @Test
    public void testEmptyStateNotSelectedList() {
        onView(withId(R.id.entrants_list_not_selected_btn)).perform(scrollTo(), click());
        onView(withId(R.id.not_selected_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.05.02: Clicking the sample button on the waitlist tab opens the
     * dialog where the organizer starts drawing attendees from the waitlist.
     */
    @Test
    public void testSampleButton_opensDrawDialog() {
        onView(withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo(), click());
        onView(withId(R.id.entrants_list_sample_btn)).perform(click());
        onView(withText("Sample Winners")).check(matches(isDisplayed()));
    }

    /**
     * US 02.05.03: Switching to the not-selected tab shows its layout and empty state,
     * confirming the organizer can view entrants who were not chosen.
     */
    @Test
    public void testNotSelectedTab_showsLayoutAndEmptyState() {
        onView(withId(R.id.entrants_list_not_selected_btn)).perform(scrollTo(), click());
        onView(withId(R.id.not_selected_entrants_list_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.not_selected_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.06.02: Switching to the cancelled tab shows its layout and empty state,
     * confirming the organizer can view cancelled entrants.
     */
    @Test
    public void testCancelledTab_showsLayoutAndEmptyState() {
        onView(withId(R.id.entrants_list_cancelled_btn)).perform(scrollTo(), click());
        onView(withId(R.id.cancelled_entrants_list_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelled_empty_text)).check(matches(isDisplayed()));
    }

    /**
     * US 02.06.04: Viewing an invited entrant's details allows the organizer to
     * cancel an entrant who has not signed up yet, and persists the cancelled
     * status back to Firestore.
     */
    @Test
    public void testInvitedEntrantDetails_cancelEntrantPersistsCancelledStatus() throws Exception {
        seedInvitedEntrant("invited_cancel_1", "Invited To Cancel", "cancel-me@example.com");

        onView(withId(R.id.entrants_list_invited_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.invited_empty_text)
                .getVisibility() == android.view.View.GONE);

        onView(withId(R.id.invited_events_view)).perform(
                RecyclerViewActions.actionOnItem(
                        hasDescendant(withText("Invited To Cancel")),
                        clickChildViewWithId(R.id.viewDetailsButton)
                )
        );

        onView(withText("Cancel Entrant")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Cancel Entrant")).inRoot(isDialog()).perform(click());
        onView(withText("Yes, Cancel")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Yes, Cancel")).inRoot(isDialog()).perform(click());

        waitForEntrantStatus("invited_cancel_1", InvitationFlowUtil.STATUS_CANCELLED);

        onView(withId(R.id.entrants_list_cancelled_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.cancelled_empty_text)
                .getVisibility() == android.view.View.GONE);
        onView(allOf(withText("Invited To Cancel"),
                isDescendantOfA(withId(R.id.cancelled_entrants_view))))
                .check(matches(isDisplayed()));
    }

    /**
     * US 02.06.04: The cancel action is only available for entrants who have not
     * signed up yet; accepted entrants must not expose the organizer cancellation action.
     */
    @Test
    public void testSignedUpEntrantDetails_doNotShowCancelAction() throws Exception {
        seedAcceptedEntrant("accepted_locked_1", "Already Signed Up", "accepted@example.com");

        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.signed_up_empty_text)
                .getVisibility() == android.view.View.GONE);

        onView(withId(R.id.signed_up_events_view)).perform(
                RecyclerViewActions.actionOnItem(
                        hasDescendant(withText("Already Signed Up")),
                        clickChildViewWithId(R.id.viewDetailsButton)
                )
        );

        onView(withText("Already Signed Up")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Cancel Entrant")).check(doesNotExist());
    }

    /**
     * US 02.06.05: Exporting enrolled entrants only becomes available on the
     * Signed Up tab and launches the document picker when accepted entrants exist.
     */
    @Test
    public void testExportCsvButton_launchesDocumentPickerForSignedUpEntrants() throws Exception {
        onView(withId(R.id.entrants_list_export_csv_btn)).check(matches(not(isDisplayed())));

        seedAcceptedEntrant("accepted_user_1", "Accepted Entrant", "accepted@example.com");
        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.signed_up_empty_text)
                .getVisibility() == android.view.View.GONE);
        onView(withId(R.id.entrants_list_export_csv_btn)).check(matches(isDisplayed()));

        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));

        onView(withId(R.id.entrants_list_export_csv_btn)).perform(click());

        intended(hasAction(Intent.ACTION_CREATE_DOCUMENT));

        onView(withId(R.id.entrants_list_waited_list_btn)).perform(scrollTo(), click());
        onView(withId(R.id.entrants_list_export_csv_btn)).check(matches(not(isDisplayed())));
    }

    /**
     * US 02.06.05: Exporting enrolled entrants writes a CSV file containing the
     * accepted entrants currently loaded into the Signed Up list.
     */
    @Test
    public void testExportCsv_writesAcceptedEntrantRowsToFile() throws Exception {
        seedAcceptedEntrant("csv_user_1", "Accepted Entrant", "accepted@example.com");
        seedAcceptedEntrant("csv_user_2", "Quoted, \"Entrant\"", "quoted@example.com");

        File exportFile = createExportTargetFile("accepted_entrants_test.csv");
        Uri exportUri = FileProvider.getUriForFile(
                ApplicationProvider.getApplicationContext(),
                ApplicationProvider.getApplicationContext().getPackageName() + ".provider",
                exportFile
        );

        Intent resultData = new Intent().setData(exportUri);
        resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));

        onView(withId(R.id.entrants_list_signed_up_btn)).perform(scrollTo(), click());
        waitForActivityCondition(activity -> activity.findViewById(R.id.signed_up_empty_text)
                .getVisibility() == android.view.View.GONE);
        onView(withId(R.id.entrants_list_export_csv_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_list_export_csv_btn)).perform(click());

        String csvContent = waitForFileContents(exportFile);

        org.junit.Assert.assertTrue(csvContent.contains("\"User ID\",\"Name\",\"Email\",\"Status\""));
        org.junit.Assert.assertTrue(csvContent.contains(
                "\"csv_user_1\",\"Accepted Entrant\",\"accepted@example.com\",\"accepted\""));
        org.junit.Assert.assertTrue(csvContent.contains(
                "\"csv_user_2\",\"Quoted, \"\"Entrant\"\"\",\"quoted@example.com\",\"accepted\""));
        onView(withId(R.id.signed_up_empty_text))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
    }
}
