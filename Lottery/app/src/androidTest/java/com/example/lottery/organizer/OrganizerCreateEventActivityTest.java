package com.example.lottery.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.Matchers.not;

import android.content.Intent;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumentation tests for OrganizerCreateEventActivity.
 *
 * US 02.01.02: Create a private event.
 * US 02.03.01: Optionally Limit Waiting List Size.
 * US 02.02.03: Enable or disable the geolocation requirement for an event.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerCreateEventActivityTest {

    // OrganizerCreateEventActivity requires a userId to be present in Intent or SharedPreferences,
    // otherwise it calls finish() immediately.
    private static final String TEST_USER_ID = "test_user_123";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

    private FirebaseFirestore db;
    private final Set<String> seededEventIds = new HashSet<>();

    @Rule
    public ActivityScenarioRule<OrganizerCreateEventActivity> activityRule =
            new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), OrganizerCreateEventActivity.class)
                    .putExtra("userId", TEST_USER_ID));

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(2000);

        for (String eventId : seededEventIds) {
            for (QueryDocumentSnapshot doc : Tasks.await(
                    db.collection(FirestorePaths.eventWaitingList(eventId)).get(),
                    10, TimeUnit.SECONDS)) {
                Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
            }

            Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10, TimeUnit.SECONDS
            );
        }
        seededEventIds.clear();
    }

    private Intent createLaunchIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(), OrganizerCreateEventActivity.class)
                .putExtra("userId", TEST_USER_ID);
    }

    private Intent editLaunchIntent(String eventId) {
        return createLaunchIntent().putExtra("eventId", eventId);
    }

    private void trackEvent(String eventId) {
        seededEventIds.add(eventId);
    }

    private String readEventId(ActivityScenario<OrganizerCreateEventActivity> scenario) {
        final String[] value = new String[1];
        scenario.onActivity(activity -> {
            try {
                Field field = OrganizerCreateEventActivity.class.getDeclaredField("eventId");
                field.setAccessible(true);
                value[0] = (String) field.get(activity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return value[0];
    }

    private void setDateField(ActivityScenario<OrganizerCreateEventActivity> scenario,
                              String fieldName,
                              int viewId,
                              Date value) {
        scenario.onActivity(activity -> {
            try {
                Field field = OrganizerCreateEventActivity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(activity, value);

                com.google.android.material.textfield.TextInputEditText editText =
                        activity.findViewById(viewId);
                editText.setText(DATE_FORMAT.format(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void waitForCondition(ActivityScenario<OrganizerCreateEventActivity> scenario,
                                  java.util.function.Predicate<OrganizerCreateEventActivity> condition)
            throws Exception {
        AssertionError lastError = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            AtomicBoolean matched = new AtomicBoolean(false);
            scenario.onActivity(activity -> matched.set(condition.test(activity)));
            if (matched.get()) {
                return;
            }
            lastError = new AssertionError("Timed out waiting for organizer create event condition");
            Thread.sleep(250);
        }
        throw lastError;
    }

    private DocumentSnapshot waitForEventDocument(String eventId) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            DocumentSnapshot snapshot = Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).get(),
                    10, TimeUnit.SECONDS
            );
            if (snapshot.exists()) {
                return snapshot;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for event " + eventId + " to persist");
    }

    private void seedExistingEvent(String eventId,
                                   Integer waitingListLimit,
                                   String posterBase64,
                                   Timestamp updatedAt) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2030, Calendar.MARCH, 1, 10, 0, 0);
        Date start = calendar.getTime();
        calendar.set(2030, Calendar.MARCH, 1, 12, 0, 0);
        Date end = calendar.getTime();
        calendar.set(2030, Calendar.FEBRUARY, 20, 9, 0, 0);
        Date regStart = calendar.getTime();
        calendar.set(2030, Calendar.FEBRUARY, 25, 18, 0, 0);
        Date regEnd = calendar.getTime();
        calendar.set(2030, Calendar.FEBRUARY, 26, 12, 0, 0);
        Date draw = calendar.getTime();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Seeded Event");
        event.put("details", "Existing details");
        event.put("place", "Old Place");
        event.put("organizerId", TEST_USER_ID);
        event.put("capacity", 50);
        event.put("waitingListLimit", waitingListLimit);
        event.put("qrCodeContent", "seeded_qr_content");
        event.put("scheduledDateTime", new Timestamp(start));
        event.put("eventEndDateTime", new Timestamp(end));
        event.put("registrationStart", new Timestamp(regStart));
        event.put("registrationDeadline", new Timestamp(regEnd));
        event.put("drawDate", new Timestamp(draw));
        event.put("requireLocation", false);
        event.put("private", false);
        event.put("category", "Other");
        event.put("status", "open");
        event.put("createdAt", Timestamp.now());
        event.put("updatedAt", updatedAt);
        event.put("posterBase64", posterBase64);

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10, TimeUnit.SECONDS
        );
        trackEvent(eventId);
    }

    private void seedWaitlistEntrant(String eventId, String userId) throws Exception {
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("userId", userId);
        entrant.put("userName", "Entrant " + userId);
        entrant.put("email", userId + "@test.com");
        entrant.put("status", "waitlisted");
        entrant.put("registeredAt", Timestamp.now());
        entrant.put("waitlistedAt", Timestamp.now());

        Tasks.await(
                db.collection(FirestorePaths.eventWaitingList(eventId)).document(userId).set(entrant),
                10, TimeUnit.SECONDS
        );
    }

    @Test
    public void testUIComponentsDisplayed() {
        // Check if the header title is displayed
        onView(ViewMatchers.withId(R.id.tvHeader)).check(matches(isDisplayed()));
        onView(withId(R.id.tvHeader)).check(matches(withText("Create New Event")));

        // Check if the Event Title input field is displayed
        onView(withId(R.id.etEventTitle)).check(matches(isDisplayed()));

        // Max Capacity input field
        onView(withId(R.id.etMaxCapacity)).perform(scrollTo()).check(matches(isDisplayed()));

        // Launch Event button
        onView(withId(R.id.btnCreateEvent)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Verifies whether Place text input is properly displayed in createEvent page.
     */
    @Test
    public void testPlaceFieldDisplayed() {
        onView(withId(R.id.etPlace)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Verifies US 02.03.01 AC #1: Toggling the waiting list limit switch
     * correctly shows and hides the numeric input field.
     */
    @Test
    public void testWaitingListLimitToggleBehavior() {
        // Initially, the input field (TextInputLayout) should be GONE (not displayed)
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));

        // Click the switch to enable limit
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Now the input field should be VISIBLE
        onView(withId(R.id.tilWaitingListLimit)).perform(scrollTo()).check(matches(isDisplayed()));

        // Click again to disable
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Should be hidden again
        onView(withId(R.id.tilWaitingListLimit)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies that the input field correctly clears and hides when the switch is disabled.
     */
    @Test
    public void testSwitchClearsInput() {
        // Toggle ON and type something
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());
        onView(withId(R.id.etWaitingListLimit)).perform(scrollTo(), typeText("50"), closeSoftKeyboard());

        // Toggle OFF
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());

        // Toggle ON again - field should be empty
        onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());
        onView(withId(R.id.etWaitingListLimit)).perform(scrollTo()).check(matches(withText("")));
    }

    /**
     * Verifies that supplying an eventId launches the screen in edit mode.
     */
    @Test
    public void testEditModeIntentUpdatesHeaderAndActionText() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerCreateEventActivity.class
        );
        intent.putExtra("eventId", "existing_event_id");
        intent.putExtra("userId", TEST_USER_ID); // Must provide userId to prevent activity from finishing

        try (ActivityScenario<OrganizerCreateEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvHeader)).check(matches(withText("Edit Event")));
            onView(withId(R.id.btnCreateEvent)).perform(scrollTo())
                    .check(matches(withText("Update Event")));
        }
    }

    // US 02.01.02: Toggling the private event switch hides the QR code card.
    @Test
    public void testPrivateEventSwitchHidesQrCard() {
        // QR code card should be visible initially
        onView(withId(R.id.cardQRCode)).perform(scrollTo())
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        // Toggle private event ON
        onView(withId(R.id.swIsPrivate)).perform(scrollTo(), click());

        // QR code card should now be GONE (not just off-screen)
        onView(withId(R.id.cardQRCode))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    // US 02.01.02: Toggling private event switch back OFF restores the QR code card.
    @Test
    public void testPrivateEventSwitchRestoresQrCard() {
        // Toggle ON
        onView(withId(R.id.swIsPrivate)).perform(scrollTo(), click());
        onView(withId(R.id.cardQRCode))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Toggle OFF
        onView(withId(R.id.swIsPrivate)).perform(scrollTo(), click());

        // QR code card should be VISIBLE again
        onView(withId(R.id.cardQRCode)).perform(scrollTo())
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    // US 02.01.02: Private event switch state is correctly readable from the Activity.
    @Test
    public void testPrivateEventSwitchState() {
        onView(withId(R.id.swIsPrivate)).perform(scrollTo(), click());

        activityRule.getScenario().onActivity(activity -> {
            com.google.android.material.switchmaterial.SwitchMaterial sw =
                    activity.findViewById(R.id.swIsPrivate);
            Assert.assertTrue("Private event switch should be ON after click", sw.isChecked());
        });

        onView(withId(R.id.swIsPrivate)).perform(scrollTo(), click());

        activityRule.getScenario().onActivity(activity -> {
            com.google.android.material.switchmaterial.SwitchMaterial sw =
                    activity.findViewById(R.id.swIsPrivate);
            Assert.assertFalse("Private event switch should be OFF after second click", sw.isChecked());
        });
    }

    // US 02.02.03: Geolocation toggle switch should be visible on the create event screen.
    @Test
    public void testGeolocationSwitchExists() {
        onView(withId(R.id.swRequireLocation))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // US 02.02.03: Organizer can toggle geolocation switch on and off.
    @Test
    public void testGeolocationSwitchToggles() {
        onView(withId(R.id.swRequireLocation)).perform(scrollTo(), click());

        activityRule.getScenario().onActivity(activity -> {
            com.google.android.material.switchmaterial.SwitchMaterial sw =
                    activity.findViewById(R.id.swRequireLocation);
            Assert.assertTrue("Geolocation switch should be ON after click", sw.isChecked());
        });

        onView(withId(R.id.swRequireLocation)).perform(scrollTo(), click());

        activityRule.getScenario().onActivity(activity -> {
            com.google.android.material.switchmaterial.SwitchMaterial sw =
                    activity.findViewById(R.id.swRequireLocation);
            Assert.assertFalse("Geolocation switch should be OFF after second click", sw.isChecked());
        });
    }

    /**
     * US 02.01.01 / US 02.01.04 / US 02.03.01: Creating a public event persists the
     * event document with an auto-generated QR code, registration period, and waiting
     * list limit in Firestore.
     */
    @Test
    public void testCreatePublicEvent_persistsEventAndAutoGeneratesQr() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2030, Calendar.MAY, 5, 9, 0, 0);
        Date regStart = calendar.getTime();
        calendar.set(2030, Calendar.MAY, 10, 18, 0, 0);
        Date regEnd = calendar.getTime();
        calendar.set(2030, Calendar.MAY, 15, 19, 0, 0);
        Date eventStart = calendar.getTime();
        calendar.set(2030, Calendar.MAY, 15, 21, 0, 0);
        Date eventEnd = calendar.getTime();
        calendar.set(2030, Calendar.MAY, 11, 12, 0, 0);
        Date drawDate = calendar.getTime();

        String uniqueTitle = "Public Event " + System.currentTimeMillis();

        try (ActivityScenario<OrganizerCreateEventActivity> scenario =
                     ActivityScenario.launch(createLaunchIntent())) {
            String eventId = readEventId(scenario);
            trackEvent(eventId);

            setDateField(scenario, "regStartDate", R.id.etRegStart, regStart);
            setDateField(scenario, "regEndDate", R.id.etRegEnd, regEnd);
            setDateField(scenario, "eventStartDate", R.id.etEventStart, eventStart);
            setDateField(scenario, "eventEndDate", R.id.etEventEnd, eventEnd);
            setDateField(scenario, "drawDate", R.id.etDrawDate, drawDate);

            onView(withId(R.id.etEventTitle)).perform(scrollTo(), replaceText(uniqueTitle), closeSoftKeyboard());
            onView(withId(R.id.etMaxCapacity)).perform(scrollTo(), replaceText("25"), closeSoftKeyboard());
            onView(withId(R.id.etEventDetails)).perform(scrollTo(), replaceText("Persisted event details"), closeSoftKeyboard());
            onView(withId(R.id.etPlace)).perform(scrollTo(), replaceText("CAB 265"), closeSoftKeyboard());
            onView(withId(R.id.swRequireLocation)).perform(scrollTo(), click());
            onView(withId(R.id.swLimitWaitingList)).perform(scrollTo(), click());
            onView(withId(R.id.etWaitingListLimit)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateEvent)).perform(scrollTo(), click());

            DocumentSnapshot snapshot = waitForEventDocument(eventId);
            Assert.assertEquals(uniqueTitle, snapshot.getString("title"));
            Assert.assertEquals("Persisted event details", snapshot.getString("details"));
            Assert.assertEquals("CAB 265", snapshot.getString("place"));
            Assert.assertEquals(TEST_USER_ID, snapshot.getString("organizerId"));
            Assert.assertEquals(Long.valueOf(25), snapshot.getLong("capacity"));
            Assert.assertEquals(Long.valueOf(12), snapshot.getLong("waitingListLimit"));
            Assert.assertEquals(Boolean.TRUE, snapshot.getBoolean("requireLocation"));
            Assert.assertEquals(Boolean.FALSE, snapshot.getBoolean("private"));
            Assert.assertEquals("open", snapshot.getString("status"));
            Assert.assertNotNull(snapshot.getTimestamp("registrationStart"));
            Assert.assertNotNull(snapshot.getTimestamp("registrationDeadline"));
            Assert.assertNotNull(snapshot.getTimestamp("scheduledDateTime"));
            Assert.assertNotNull(snapshot.getTimestamp("eventEndDateTime"));
            Assert.assertNotNull(snapshot.getTimestamp("drawDate"));

            String qrCodeContent = snapshot.getString("qrCodeContent");
            Assert.assertNotNull("Public event should persist an auto-generated QR code", qrCodeContent);
            Assert.assertEquals("QR code should decode back to the saved eventId",
                    eventId, QRCodeUtils.extractEventId(qrCodeContent));
        }
    }

    /**
     * US 02.03.01: Editing an event must reject a waiting list limit that is smaller
     * than the number of already registered entrants, leaving Firestore unchanged.
     */
    @Test
    public void testEditEvent_rejectsWaitingListLimitSmallerThanExistingEntrants() throws Exception {
        String eventId = "limit_guard_" + System.currentTimeMillis();
        Timestamp originalUpdatedAt = Timestamp.now();
        seedExistingEvent(eventId, 3, "", originalUpdatedAt);
        seedWaitlistEntrant(eventId, "wait_user_1");
        seedWaitlistEntrant(eventId, "wait_user_2");

        try (ActivityScenario<OrganizerCreateEventActivity> scenario =
                     ActivityScenario.launch(editLaunchIntent(eventId))) {
            waitForCondition(scenario, activity ->
                    "3".contentEquals(String.valueOf(
                            ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.etWaitingListLimit)).getText()))
            );

            onView(withId(R.id.etWaitingListLimit)).perform(scrollTo(), replaceText("1"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateEvent)).perform(scrollTo(), click());
            Thread.sleep(2500);
        }

        DocumentSnapshot snapshot = Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).get(),
                10, TimeUnit.SECONDS
        );
        Assert.assertEquals("Waiting list limit should remain unchanged after rejected edit",
                Long.valueOf(3), snapshot.getLong("waitingListLimit"));
        Assert.assertEquals("Rejected edit should not update the event timestamp",
                originalUpdatedAt, snapshot.getTimestamp("updatedAt"));
    }

    /**
     * US 02.04.02: Updating an existing event poster through the activity's poster
     * selection callback persists the new poster value to Firestore.
     */
    @Test
    public void testEditEvent_updatesPosterInFirestore() throws Exception {
        String eventId = "poster_update_" + System.currentTimeMillis();
        seedExistingEvent(eventId, 5, "data:image/jpeg;base64,OLD_POSTER", Timestamp.now());
        String newPoster = "data:image/jpeg;base64,NEW_POSTER_DATA";

        try (ActivityScenario<OrganizerCreateEventActivity> scenario =
                     ActivityScenario.launch(editLaunchIntent(eventId))) {
            waitForCondition(scenario, activity ->
                    "Update Event".contentEquals(
                            ((Button) activity.findViewById(R.id.btnCreateEvent)).getText()));

            scenario.onActivity(activity -> {
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putString("posterBase64", newPoster);
                activity.getSupportFragmentManager().setFragmentResult("posterRequest", bundle);
            });

            onView(withId(R.id.btnCreateEvent)).perform(scrollTo(), click());
        }

        DocumentSnapshot snapshot = waitForEventDocument(eventId);
        Assert.assertEquals("Updated poster should be written to Firestore",
                newPoster, snapshot.getString("posterBase64"));
    }
}
