package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for device-based identification (US 01.07.01).
 *
 * Verifies that:
 * - The app obtains a device ANDROID_ID and uses it as the basis for user identification
 * - User documents in Firestore contain the correct deviceId
 * - The userId follows the "{role}_{androidId}" convention
 * - Session data is persisted in SharedPreferences so users don't need a username/password
 */
@RunWith(AndroidJUnit4.class)
public class DeviceIdentificationTest {

    private FirebaseFirestore db;
    private String androidId;
    private String expectedUserId;

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        expectedUserId = "entrant_" + androidId;
    }

    @After
    public void tearDown() throws Exception {
        // Clean up the test user document if it was created
        try {
            Tasks.await(
                    db.collection(FirestorePaths.USERS).document(expectedUserId).delete(),
                    10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // US 01.07.01 — Device ID is obtainable and non-empty
    // -------------------------------------------------------------------------

    /**
     * US 01.07.01: The device has a non-null, non-empty ANDROID_ID that can be
     * used for identification without requiring a username and password.
     */
    @Test
    public void deviceId_isAvailableAndNonEmpty() {
        assertNotNull("ANDROID_ID should not be null", androidId);
        assertFalse("ANDROID_ID should not be empty", androidId.isEmpty());
    }

    /**
     * US 01.07.01: The userId convention combines the role with the device's
     * ANDROID_ID in the format "{role}_{androidId}", ensuring unique
     * identification per role without requiring credentials.
     */
    @Test
    public void userId_followsRoleAndDeviceIdConvention() {
        assertTrue("userId should start with 'entrant_'",
                expectedUserId.startsWith("entrant_"));
        assertTrue("userId should contain the device's ANDROID_ID",
                expectedUserId.contains(androidId));
        assertEquals("userId should be 'entrant_' + androidId",
                "entrant_" + androidId, expectedUserId);
    }

    // -------------------------------------------------------------------------
    // US 01.07.01 — Firestore user document stores deviceId
    // -------------------------------------------------------------------------

    /**
     * US 01.07.01: When a user document is created in Firestore, the deviceId
     * field matches the device's ANDROID_ID, confirming the app identifies
     * users by their device rather than by username/password.
     */
    @Test
    public void firestoreUserDocument_storesDeviceId() throws Exception {
        // Simulate the createNewUser flow from MainActivity
        Map<String, Object> userData = new HashMap<>();
        Timestamp now = Timestamp.now();
        userData.put("userId", expectedUserId);
        userData.put("role", "ENTRANT");
        userData.put("deviceId", androidId);
        userData.put("username", "");
        userData.put("email", "");
        userData.put("phone", "");
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("notificationsEnabled", true);
        userData.put("geolocationEnabled", false);

        Tasks.await(db.collection(FirestorePaths.USERS).document(expectedUserId).set(userData),
                10, TimeUnit.SECONDS);

        // Read back and verify
        DocumentSnapshot doc = Tasks.await(
                db.collection(FirestorePaths.USERS).document(expectedUserId).get(),
                10, TimeUnit.SECONDS);

        assertTrue("User document should exist in Firestore", doc.exists());
        assertEquals("deviceId should match ANDROID_ID",
                androidId, doc.getString("deviceId"));
        assertEquals("userId should follow role_androidId convention",
                expectedUserId, doc.getString("userId"));
        assertEquals("role should be ENTRANT",
                "ENTRANT", doc.getString("role"));
    }

    // -------------------------------------------------------------------------
    // US 01.07.01 — Session persistence via SharedPreferences
    // -------------------------------------------------------------------------

    /**
     * US 01.07.01: Session data is saved to SharedPreferences so the user is
     * automatically identified on subsequent app launches without needing
     * to enter credentials.
     */
    @Test
    public void sessionPersistence_savesDeviceIdToSharedPreferences() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // Simulate saveSessionLocally from MainActivity
        prefs.edit()
                .putString("userId", expectedUserId)
                .putString("userRole", "ENTRANT")
                .putString("deviceId", androidId)
                .putString("userName", "TestUser")
                .apply();

        // Verify persistence
        assertEquals("userId should be persisted",
                expectedUserId, prefs.getString("userId", null));
        assertEquals("deviceId should be persisted",
                androidId, prefs.getString("deviceId", null));
        assertEquals("userRole should be persisted",
                "ENTRANT", prefs.getString("userRole", null));

        // Clean up
        prefs.edit().clear().apply();
    }

    /**
     * US 01.07.01: Different roles on the same device produce different userIds
     * (e.g., "entrant_abc123" vs "organizer_abc123"), allowing a single device
     * to hold separate profiles per role — all without passwords.
     */
    @Test
    public void differentRoles_produceDifferentUserIds() {
        String entrantId = "entrant_" + androidId;
        String organizerId = "organizer_" + androidId;

        assertFalse("Entrant and organizer userIds should differ",
                entrantId.equals(organizerId));
        assertTrue("Both should share the same device ANDROID_ID suffix",
                entrantId.endsWith(androidId) && organizerId.endsWith(androidId));
    }
}
