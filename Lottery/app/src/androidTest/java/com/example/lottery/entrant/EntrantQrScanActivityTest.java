package com.example.lottery.entrant;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.init;
import static androidx.test.espresso.intent.Intents.release;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.util.FirestorePaths;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for {@link EntrantQrScanActivity}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Activity initialization and intent handling.</li>
 *   <li>Presence of key UI components (Buttons).</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantQrScanActivityTest {

    private Intent intent;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> seededEventIds = new HashSet<>();
    private final Set<String> seededUserIds = new HashSet<>();

    /**
     * Sets up the test environment before each test execution.
     * Initializes a mock intent containing a user ID.
     */
    @Before
    public void setUp() {
        init();
        intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantQrScanActivity.class);
        intent.putExtra("userId", "morgan_lee_device");
    }

    @After
    public void tearDown() throws Exception {
        for (String eventId : seededEventIds) {
            Tasks.await(
                    db.collection(FirestorePaths.EVENTS).document(eventId).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }
        seededEventIds.clear();

        for (String userId : seededUserIds) {
            Tasks.await(
                    db.collection(FirestorePaths.USERS).document(userId).delete(),
                    10,
                    TimeUnit.SECONDS
            );
        }
        seededUserIds.clear();

        release();
    }

    private void seedUser(String userId) throws Exception {
        seededUserIds.add(userId);

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("username", "Morgan Lee");
        user.put("email", "morgan.lee@gmail.com");
        user.put("role", "ENTRANT");
        user.put("notificationsEnabled", true);
        user.put("createdAt", Timestamp.now());
        user.put("updatedAt", Timestamp.now());

        Tasks.await(
                db.collection(FirestorePaths.USERS).document(userId).set(user),
                10,
                TimeUnit.SECONDS
        );
    }

    private void seedPublicQrEvent(String eventId, String details, String posterBase64) throws Exception {
        seededEventIds.add(eventId);

        Timestamp now = Timestamp.now();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", "Beginner Swimming Lessons");
        event.put("details", details);
        event.put("place", "Kinsmen Recreation Centre");
        event.put("organizerId", "kinsmen_aquatics_team");
        event.put("capacity", 25L);
        event.put("scheduledDateTime", now);
        event.put("eventEndDateTime", now);
        event.put("registrationStart", now);
        event.put("registrationDeadline", now);
        event.put("drawDate", now);
        event.put("private", false);
        event.put("requireLocation", false);
        event.put("posterBase64", posterBase64);
        event.put("qrCodeContent", QRCodeUtils.generateUniqueQrContent(eventId));

        Tasks.await(
                db.collection(FirestorePaths.EVENTS).document(eventId).set(event),
                10,
                TimeUnit.SECONDS
        );
    }

    /**
     * Verifies that the activity is correctly instantiated.
     */
    @Test
    public void testActivityNotNull() {
        try (ActivityScenario<EntrantQrScanActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /**
     * Verifies that the scanner and gallery buttons exist in the layout.
     */
    @Test
    public void testButtonsExist() {
        try (ActivityScenario<EntrantQrScanActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.btnOpenScanner));
                assertNotNull(activity.findViewById(R.id.btnPickQrFromGallery));
                assertNotNull(activity.findViewById(R.id.btnBack));
            });
        }
    }

    /**
     * US 02.01.01: Scanning a valid promotional QR code launches the event-details
     * screen with the decoded eventId so the linked event description and poster can
     * be displayed in-app.
     */
    @Test
    public void testValidQrScan_navigatesToLinkedEventDetails() throws Exception {
        String userId = "morgan_lee_" + UUID.randomUUID();
        String eventId = "beginner_swim_lessons_" + UUID.randomUUID();
        seedUser(userId);
        seedPublicQrEvent(
                eventId,
                "A one-week registration window for beginner swimming lessons before canoe season",
                "data:image/jpeg;base64,SWIM_LESSONS_POSTER"
        );

        Intent launchIntent = new Intent(ApplicationProvider.getApplicationContext(), EntrantQrScanActivity.class);
        launchIntent.putExtra("userId", userId);

        try (ActivityScenario<EntrantQrScanActivity> scenario = ActivityScenario.launch(launchIntent)) {
            scenario.onActivity(activity -> {
                try {
                    Method handleScanResult = EntrantQrScanActivity.class
                            .getDeclaredMethod("handleScanResult", String.class);
                    handleScanResult.setAccessible(true);
                    String qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
                    handleScanResult.invoke(activity, qrCodeContent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        intended(org.hamcrest.Matchers.allOf(
                hasComponent(EntrantEventDetailsActivity.class.getName()),
                hasExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, eventId),
                hasExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId)
        ));
    }
}
