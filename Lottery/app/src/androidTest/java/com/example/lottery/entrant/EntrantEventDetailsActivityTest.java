package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.InvitationFlowUtil;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

/**
 * Android instrumentation tests for {@link EntrantEventDetailsActivity}.
 *
 * <p>These tests verify the basic entrant event-details UI structure and ensure
 * that wow-factor ticket controls are present in the layout and hidden by default
 * until the accepted entrant state is reached.
 */
public class EntrantEventDetailsActivityTest {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();
    private final Set<String> seededUserIds = new HashSet<>();

    /**
     * Creates a default intent for simple UI-only tests.
     */
    private ActivityScenario<EntrantEventDetailsActivity> launchDefault() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(
                EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "community_swim_lesson_" + UUID.randomUUID()
        );
        intent.putExtra(
                EntrantEventDetailsActivity.EXTRA_USER_ID,
                "avery_chen_device"
        );
        return ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            for (QueryDocumentSnapshot snapshot : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10,
                    TimeUnit.SECONDS
            )) {
                Tasks.await(snapshot.getReference().delete(), 10, TimeUnit.SECONDS);
            }

            Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }

        for (String userId : seededUserIds) {
            Tasks.await(
                    db.collection(FirestorePaths.USERS).document(userId).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }

        seededEventIds.clear();
        seededUserIds.clear();
    }

    private Intent createIntent(String eventId, String userId) {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        return intent;
    }

    private void seedEvent(String eventId) throws Exception {
        seedEvent(eventId, "A one-week registration window for beginner swimming lessons", null, 100L);
    }

    private void seedEvent(String eventId, String details, String posterBase64) throws Exception {
        seedEvent(eventId, details, posterBase64, 100L);
    }

    private void seedEvent(String eventId, String details, String posterBase64, long waitingListLimit)
            throws Exception {
        seededEventIds.add(eventId);

        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Beginner Swimming Lessons");
        event.put("details", details);
        event.put("place", "Kinsmen Recreation Centre");
        event.put("capacity", 10L);
        event.put("scheduledDateTime", now);
        event.put("eventEndDateTime", now);
        event.put("registrationStart", now);
        event.put("registrationDeadline", new Timestamp(
                new java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
        ));
        event.put("drawDate", now);
        event.put("requireLocation", false);
        event.put("waitingListLimit", waitingListLimit);
        event.put("posterBase64", posterBase64);

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedUser(String userId) throws Exception {
        seededUserIds.add(userId);

        Map<String, Object> user = new HashMap<>();
        Timestamp now = Timestamp.now();
        user.put("userId", userId);
        user.put("username", "Avery Chen");
        user.put("email", "avery.chen@gmail.com");
        user.put("role", "ENTRANT");
        user.put("deviceId", "pixel7_avery");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", false);
        user.put("createdAt", now);
        user.put("updatedAt", now);

        Tasks.await(
                db.collection(FirestorePaths.USERS).document(userId).set(user),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedEntrantStatus(String eventId, String userId, String status) throws Exception {
        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", "Avery Chen");
        record.put("email", "avery.chen@gmail.com");
        record.put("status", status);
        record.put("registeredAt", now);

        if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status)) {
            record.put("waitlistedAt", now);
        } else if (InvitationFlowUtil.STATUS_INVITED.equals(status)) {
            record.put("invitedAt", now);
            record.put("waitlistedAt", now);
        } else if (InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
            record.put("acceptedAt", now);
            record.put("waitlistedAt", now);
        } else if (InvitationFlowUtil.STATUS_CANCELLED.equals(status)) {
            record.put("cancelledAt", now);
            record.put("waitlistedAt", now);
        }

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedPrivateEvent(String eventId, boolean requireLocation) throws Exception {
        seededEventIds.add(eventId);

        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Private Piano Lessons");
        event.put("details", "Private invitation-only piano lessons before the spring recital");
        event.put("place", "Mill Woods Community Centre");
        event.put("scheduledDateTime", now);
        event.put("eventEndDateTime", now);
        event.put("registrationStart", now);
        event.put("registrationDeadline", new Timestamp(
                new java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
        ));
        event.put("drawDate", now);
        event.put("private", true);
        event.put("requireLocation", requireLocation);
        event.put("waitingListLimit", 100L);

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedPrivateInviteStatus(String eventId, String userId) throws Exception {
        Timestamp now = Timestamp.now();
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", "Avery Chen");
        record.put("email", "avery.chen@gmail.com");
        record.put("status", InvitationFlowUtil.STATUS_INVITED);
        record.put("invitedAt", now);
        // No waitlistedAt — entrant was directly invited to a private event

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).set(record),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedWaitlistCount(String eventId, int count) throws Exception {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        for (int i = 0; i < count; i++) {
            String userId = "swim_family_" + eventId + "_" + i;
            Map<String, Object> record = new HashMap<>();
            record.put("userId", userId);
            record.put("userName", "Swim Family " + i);
            record.put("email", "swim.family" + i + "@gmail.com");
            record.put("status", InvitationFlowUtil.STATUS_WAITLISTED);
            record.put("registeredAt", now);
            record.put("waitlistedAt", now);

            batch.set(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId),
                    record
            );
        }

        Tasks.await(batch.commit(), 10, TimeUnit.SECONDS);
    }

    private void waitForCondition(
            ActivityScenario<EntrantEventDetailsActivity> scenario,
            java.util.function.Predicate<EntrantEventDetailsActivity> condition
    ) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            scenario.onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) {
                return;
            }
            lastError = new AssertionError("Timed out waiting for activity condition");
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(250);
        }
        throw lastError;
    }

    private void waitForEntrantStatus(String eventId, String userId, String expectedStatus) throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            com.google.firebase.firestore.DocumentSnapshot snapshot = Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).get(),
                    10,
                    TimeUnit.SECONDS
            );
            String normalized = InvitationFlowUtil.normalizeEntrantStatus(snapshot.getString("status"));
            if (expectedStatus.equals(normalized)) {
                return;
            }

            lastError = new AssertionError(
                    "Timed out waiting for " + userId + " in event " + eventId + " to reach " + expectedStatus
            );
            Thread.sleep(250);
        }
        throw lastError;
    }

    /**
     * Verifies that the title and back button are displayed on launch.
     */
    /**
     * US 01.06.01: Viewing an event from its QR-linked details screen shows the
     * event-details header and a way to return to the previous screen.
     */
    @Test
    public void testInitialUIState() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(ViewMatchers.withId(R.id.tvEventDetailsTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEventDetailsTitle)).check(matches(withText("Event Details")));
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.01.01: Opening a QR-linked event details screen loads the persisted
     * event description and poster for the linked public event.
     */
    @Test
    public void testQrLinkedDetails_loadsDescriptionAndPoster() throws Exception {
        String eventId = "beginner_swim_lessons_" + UUID.randomUUID();
        String userId = "avery_chen_" + UUID.randomUUID();
        seedEvent(
                eventId,
                "A one-week registration window for beginner swimming lessons before canoe season",
                "data:image/jpeg;base64,SWIM_LESSONS_POSTER"
        );
        seedUser(userId);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView description = activity.findViewById(R.id.tvEventDescription);
                ImageView poster = activity.findViewById(R.id.ivEventPoster);
                return "A one-week registration window for beginner swimming lessons before canoe season".contentEquals(description.getText())
                        && poster.getDrawable() != null;
            });

            onView(withId(R.id.cvEventDescription)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEventDescription))
                    .check(matches(withText("A one-week registration window for beginner swimming lessons before canoe season")));
        }
    }

    /**
     * US 01.01.03 / US 01.02.03: The entrant can navigate between exploration
     * and their event history from the event-details experience.
     */
    @Test
    public void testBottomNavigationIsDisplayed() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_explore)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.05.02 / US 01.05.03: Invitation response controls exist on the
     * details screen but stay hidden until the entrant is actually invited.
     */
    @Test
    public void testInvitationUiElementsExist() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.btnAcceptInvite)).check(matches(withText(R.string.accept_invite)));
            onView(withId(R.id.btnDeclineInvite)).check(matches(withText(R.string.decline_invite)));
            onView(withId(R.id.invitationButtonsContainer))
                    .check(matches(withEffectiveVisibility(GONE)));
            onView(withId(R.id.registrationEndedContainer))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 01.06.02: The event-details screen exposes the waitlist action so an
     * entrant can join the event from the details page.
     */
    @Test
    public void testWaitlistActionButtonIsVisibleByDefault() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.join_wait_list)));
        }
    }

    /**
     * US 01.05.02: The confirmation ticket control remains hidden until the
     * entrant has actually accepted the invitation.
     */
    @Test
    public void testDownloadTicketButtonExistsAndIsHiddenByDefault() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.btnDownloadTicket))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 01.05.02: When acceptance unlocks the confirmation ticket action, the
     * button label is the expected entrant-facing copy.
     */
    @Test
    public void testDownloadTicketButtonHasCorrectText() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.btnDownloadTicket))
                    .check(matches(withText(R.string.download_confirmation_ticket)));
        }
    }

    /**
     * US 01.06.01: Entrants viewing event details do not see organizer-only
     * editing controls on the event-details screen.
     */
    @Test
    public void testEntrantScreenDoesNotExposeOrganizerEditButton() {
        try (ActivityScenario<EntrantEventDetailsActivity> ignored = launchDefault()) {
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }

    /**
     * US 01.01.03: Opening event details from event discovery keeps the EXPLORE
     * tab highlighted as the active entrant navigation source.
     */
    @Test
    public void testDefaultSourceTabHighlightsExplore() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "swim_lessons_explore_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "avery_chen_device");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                ImageView historyIcon = activity.findViewById(R.id.iv_nav_history);
                assertEquals(activeColor, exploreIcon.getImageTintList().getDefaultColor());
                assertEquals(inactiveColor, historyIcon.getImageTintList().getDefaultColor());
            });
        }
    }

    /**
     * US 01.02.03: Opening event details from the entrant's event history keeps
     * the MY_EVENTS source highlighted in the bottom navigation.
     */
    @Test
    public void testSourceTabMyEventsHighlightsMyEvents() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "swim_lessons_history_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "avery_chen_device");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_SOURCE_TAB, "MY_EVENTS");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                ImageView historyIcon = activity.findViewById(R.id.iv_nav_history);
                assertEquals(inactiveColor, exploreIcon.getImageTintList().getDefaultColor());
                assertEquals(activeColor, historyIcon.getImageTintList().getDefaultColor());
            });
        }
    }

    /**
     * US 01.01.03: Invalid navigation source data safely falls back to the
     * EXPLORE tab instead of breaking the entrant navigation state.
     */
    @Test
    public void testInvalidSourceTabFallsBackToExplore() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailsActivity.class
        );
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "swim_lessons_fallback_" + UUID.randomUUID());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "avery_chen_device");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_SOURCE_TAB, "INVALID_TAB");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                int activeColor = ContextCompat.getColor(activity, R.color.primary_blue);
                ImageView exploreIcon = activity.findViewById(R.id.iv_nav_explore);
                assertEquals(activeColor, exploreIcon.getImageTintList().getDefaultColor());
            });
        }
    }

    /**
     * US 01.01.02: An entrant already on the waitlist sees the leave action when
     * their participation record is loaded from Firestore.
     */
    @Test
    public void testLeaveWaitlistButtonTextWhenInWaitlist() throws Exception {
        String eventId = "community_swim_waitlist_" + UUID.randomUUID();
        String userId = "avery_waitlist_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId);
        seedEntrantStatus(eventId, userId, InvitationFlowUtil.STATUS_WAITLISTED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView button = activity.findViewById(R.id.btnWaitlistAction);
                return activity.findViewById(R.id.btnWaitlistAction).getVisibility() == android.view.View.VISIBLE
                        && activity.getString(R.string.leave_wait_list).contentEquals(button.getText());
            });
            onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.leave_wait_list)));
        }
    }

    /**
     * US 01.05.02: An invited entrant sees the accept/decline controls after the
     * invitation status is loaded from Firestore.
     */
    @Test
    public void testInvitedState_showsInvitationButtons_hidesWaitlistButton() throws Exception {
        String eventId = "community_swim_invitation_" + UUID.randomUUID();
        String userId = "avery_invited_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId);
        seedEntrantStatus(eventId, userId, InvitationFlowUtil.STATUS_INVITED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> activity.findViewById(R.id.invitationButtonsContainer)
                    .getVisibility() == android.view.View.VISIBLE);
            onView(withId(R.id.invitationButtonsContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAcceptInvite)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeclineInvite)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 01.05.02: After accepting an invitation, the entrant sees the membership
     * cancellation action and confirmation ticket control for that event.
     */
    @Test
    public void testAcceptedState_showsCancelMembershipAndTicketButton() throws Exception {
        String eventId = "community_swim_signup_" + UUID.randomUUID();
        String userId = "avery_signed_up_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId);
        seedEntrantStatus(eventId, userId, InvitationFlowUtil.STATUS_ACCEPTED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> {
                TextView button = activity.findViewById(R.id.btnWaitlistAction);
                return activity.findViewById(R.id.btnDownloadTicket).getVisibility() == android.view.View.VISIBLE
                        && activity.getString(R.string.cancel_event_membership).contentEquals(button.getText());
            });
            onView(withId(R.id.btnWaitlistAction)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.cancel_event_membership)));
            onView(withId(R.id.btnDownloadTicket)).check(matches(isDisplayed()));
            onView(withId(R.id.invitationButtonsContainer)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 01.05.03: A cancelled entrant is told they cannot rejoin the event and
     * the waitlist action is removed.
     */
    @Test
    public void testCancelledState_showsCannotRejoinMessage() throws Exception {
        String eventId = "community_swim_cancelled_" + UUID.randomUUID();
        String userId = "avery_cancelled_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId);
        seedEntrantStatus(eventId, userId, InvitationFlowUtil.STATUS_CANCELLED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> activity.findViewById(R.id.registrationEndedContainer)
                    .getVisibility() == android.view.View.VISIBLE);
            onView(withId(R.id.registrationEndedContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.tvRegistrationEndedTitle)).check(matches(withText(R.string.cannot_rejoin_event_title)));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 01.05.04: The entrant sees the total number of people currently on the
     * waitlist, based on live Firestore waitlist records for the event.
     */
    @Test
    public void testWaitlistCount_displaysFormattedCount() throws Exception {
        String eventId = "community_swim_count_" + UUID.randomUUID();
        String userId = "avery_count_" + UUID.randomUUID();
        seedEvent(eventId);
        seedUser(userId);
        seedWaitlistCount(eventId, 42);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> ((TextView) activity.findViewById(R.id.tvWaitlistCount))
                    .getText().toString().contains("42"));
            onView(withId(R.id.tvWaitlistCount)).check(matches(isDisplayed()));
            onView(withId(R.id.tvWaitlistCount)).check(matches(withText(containsString("42"))));
        }
    }

    /**
     * US 02.01.03: A privately-invited entrant (no prior waitlist history)
     * sees Accept/Decline buttons and the waitlist action is hidden.
     */
    @Test
    public void testPrivateInvite_showsAcceptDeclineButtons() throws Exception {
        String eventId = "private_piano_invitation_" + UUID.randomUUID();
        String userId = "avery_piano_invite_" + UUID.randomUUID();
        seedPrivateEvent(eventId, false);
        seedUser(userId);
        seedPrivateInviteStatus(eventId, userId);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> activity.findViewById(R.id.invitationButtonsContainer)
                    .getVisibility() == android.view.View.VISIBLE);
            onView(withId(R.id.invitationButtonsContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAcceptInvite)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeclineInvite)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWaitlistAction)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 02.01.03: After a privately-invited entrant accepts (no requireLocation),
     * they join the waitlist, the invitation buttons are hidden, and
     * "Leave Wait List" is shown.
     */
    @Test
    public void testPrivateInviteAccept_noLocation_joinsWaitlistAndHidesInviteButtons() throws Exception {
        String eventId = "private_piano_acceptance_" + UUID.randomUUID();
        String userId = "avery_piano_accept_" + UUID.randomUUID();
        seedPrivateEvent(eventId, false);
        seedUser(userId);
        seedPrivateInviteStatus(eventId, userId);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            // Wait for invite buttons to appear
            waitForCondition(scenario, activity -> activity.findViewById(R.id.invitationButtonsContainer)
                    .getVisibility() == android.view.View.VISIBLE);

            // Click Accept
            onView(withId(R.id.btnAcceptInvite)).perform(
                    androidx.test.espresso.action.ViewActions.click());

            // Wait for status to update — should show "Leave Wait List"
            waitForCondition(scenario, activity -> {
                TextView btn = activity.findViewById(R.id.btnWaitlistAction);
                return btn.getVisibility() == android.view.View.VISIBLE
                        && activity.getString(R.string.leave_wait_list).contentEquals(btn.getText());
            });

            onView(withId(R.id.btnWaitlistAction)).check(matches(withText(R.string.leave_wait_list)));
            onView(withId(R.id.invitationButtonsContainer)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * US 02.01.03: After a privately-invited entrant accepts an event that
     * requires location, the location consent dialog is shown.
     */
    @Test
    public void testPrivateInviteAccept_withLocation_showsLocationDialog() throws Exception {
        String eventId = "private_piano_location_" + UUID.randomUUID();
        String userId = "avery_piano_location_" + UUID.randomUUID();
        seedPrivateEvent(eventId, true);
        // Seed user with geolocation enabled so the geo-disabled dialog is not shown
        seededUserIds.add(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", "GeoUser");
        user.put("email", "avery.location@gmail.com");
        user.put("role", "ENTRANT");
        user.put("deviceId", "pixel7_avery_location");
        user.put("notificationsEnabled", true);
        user.put("geolocationEnabled", true);
        user.put("createdAt", Timestamp.now());
        user.put("updatedAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.USERS).document(userId).set(user), 10, TimeUnit.SECONDS);

        seedPrivateInviteStatus(eventId, userId);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, userId))) {
            waitForCondition(scenario, activity -> activity.findViewById(R.id.invitationButtonsContainer)
                    .getVisibility() == android.view.View.VISIBLE);

            // Click Accept — should trigger location consent dialog
            onView(withId(R.id.btnAcceptInvite)).perform(
                    androidx.test.espresso.action.ViewActions.click());

            // Verify the location consent dialog appears
            onView(withText("Location Required")).check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.05.01 / US 02.05.03: When a previously invited entrant declines, the system promotes
     * one replacement applicant from the remaining pool and delivers a promotion
     * notification to the promoted entrant's inbox.
     */
    @Test
    public void testDeclinedInvitation_promotesReplacementApplicant() throws Exception {
        String eventId = "beginner_swim_promotion_" + UUID.randomUUID();
        String invitedUserId = "declining_family_" + UUID.randomUUID();
        String waitlistedUserId = "waitlisted_family_" + UUID.randomUUID();
        String notSelectedUserId = "backup_family_" + UUID.randomUUID();

        seedEvent(eventId, "Weekly swimming lessons for beginners at the local rec centre", null, 10L);
        seedUser(invitedUserId);
        seedUser(waitlistedUserId);
        seedUser(notSelectedUserId);
        seedEntrantStatus(eventId, invitedUserId, InvitationFlowUtil.STATUS_INVITED);
        seedEntrantStatus(eventId, waitlistedUserId, InvitationFlowUtil.STATUS_WAITLISTED);
        seedEntrantStatus(eventId, notSelectedUserId, InvitationFlowUtil.STATUS_NOT_SELECTED);

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(createIntent(eventId, invitedUserId))) {
            waitForCondition(scenario, activity -> activity.findViewById(R.id.invitationButtonsContainer)
                    .getVisibility() == android.view.View.VISIBLE);

            onView(withId(R.id.btnDeclineInvite)).perform(androidx.test.espresso.action.ViewActions.click());
        }

        waitForEntrantStatus(eventId, invitedUserId, InvitationFlowUtil.STATUS_CANCELLED);

        // Poll for promotion — the async WaitlistPromotionUtil transaction runs after the decline
        String promotedUserId = null;
        String[] candidateIds = new String[]{waitlistedUserId, notSelectedUserId};
        for (int attempt = 0; attempt < 30; attempt++) {
            for (String candidateId : candidateIds) {
                com.google.firebase.firestore.DocumentSnapshot snapshot = Tasks.await(
                        db.collection(FirestorePaths.eventWaitingList(eventId)).document(candidateId).get(),
                        10,
                        TimeUnit.SECONDS
                );
                String normalized = InvitationFlowUtil.normalizeEntrantStatus(snapshot.getString("status"));
                if (InvitationFlowUtil.STATUS_INVITED.equals(normalized)) {
                    promotedUserId = candidateId;
                    break;
                }
            }
            if (promotedUserId != null) break;
            Thread.sleep(250);
        }

        org.junit.Assert.assertNotNull(
                "One replacement applicant should be promoted to invited after the decline",
                promotedUserId
        );

        // Poll for inbox notification — written in the same or subsequent transaction
        boolean inboxPromotionFound = false;
        for (int attempt = 0; attempt < 30; attempt++) {
            for (QueryDocumentSnapshot inbox : Tasks.await(
                    db.collection(FirestorePaths.userInbox(promotedUserId)).get(),
                    10,
                    TimeUnit.SECONDS
            )) {
                if ("waitlist_promoted".equals(inbox.getString("type"))) {
                    inboxPromotionFound = true;
                    break;
                }
            }
            if (inboxPromotionFound) break;
            Thread.sleep(250);
        }

        org.junit.Assert.assertTrue(
                "Promoted replacement applicant should receive a waitlist promotion notification",
                inboxPromotionFound
        );
    }
}
