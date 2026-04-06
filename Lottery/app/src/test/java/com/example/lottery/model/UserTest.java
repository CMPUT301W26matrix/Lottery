package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for the {@link User} model class.
 *
 * <p>Verifies profile storage, role helpers, notification/geolocation defaults,
 * device identification, and timestamp management.</p>
 */
public class UserTest {

    // -------------------------------------------------------------------------
    // US 01.02.01 — Entrant provides personal information
    // -------------------------------------------------------------------------

    // US 01.02.01: User stores name, email, and phone on construction
    @Test
    public void constructor_storesNameEmailAndPhone() {
        User user = new User("u-1", "Alice Nguyen", "alice.nguyen@gmail.com", "7801112222");

        assertEquals("Alice Nguyen", user.getUsername());
        assertEquals("alice.nguyen@gmail.com", user.getEmail());
        assertEquals("7801112222", user.getPhone());
    }

    // US 01.02.01: Phone number is optional (empty string is valid)
    @Test
    public void constructor_allowsEmptyPhone() {
        User user = new User("u-2", "Bob Martinez", "bob.martinez@gmail.com", "");

        assertEquals("Bob Martinez", user.getUsername());
        assertEquals("bob.martinez@gmail.com", user.getEmail());
        assertEquals("", user.getPhone());
    }

    // US 01.02.01: Phone number is optional (null is valid)
    @Test
    public void constructor_allowsNullPhone() {
        User user = new User("u-3", "Carol Huang", "carol.huang@gmail.com", null);

        assertEquals("Carol Huang", user.getUsername());
        assertEquals("carol.huang@gmail.com", user.getEmail());
        assertNull(user.getPhone());
    }

    // US 01.02.01: UserId is stored alongside profile fields
    @Test
    public void constructor_storesUserId() {
        User user = new User("user-abc", "Daniel Park", "daniel.park@gmail.com", "5551234567");
        assertEquals("user-abc", user.getUserId());
    }

    // -------------------------------------------------------------------------
    // US 01.02.02 — Entrant updates personal information
    // -------------------------------------------------------------------------

    // US 01.02.02: Username is mutable via setter
    @Test
    public void setUsername_updatesValue() {
        User user = new User("u-1", "Emily Watson", "emily.watson@gmail.com", "");
        user.setUsername("Emily Chen");
        assertEquals("Emily Chen", user.getUsername());
    }

    // US 01.02.02: Email is mutable via setter
    @Test
    public void setEmail_updatesValue() {
        User user = new User("u-1", "Priya Sharma", "priya.sharma@outlook.com", "");
        user.setEmail("priya.sharma@gmail.com");
        assertEquals("priya.sharma@gmail.com", user.getEmail());
    }

    // US 01.02.02: Phone is mutable via setter
    @Test
    public void setPhone_updatesValue() {
        User user = new User("u-1", "James Lee", "james.lee@gmail.com", "7801234567");
        user.setPhone("7809876543");
        assertEquals("7809876543", user.getPhone());
    }

    // US 01.02.02: Profile image base64 can be set and retrieved
    @Test
    public void setProfileImageBase64_updatesValue() {
        User user = new User();
        user.setProfileImageBase64("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABI");
        assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABI", user.getProfileImageBase64());
    }

    // US 01.02.02: Interests list can be set and retrieved
    @Test
    public void setInterests_updatesValue() {
        User user = new User();
        List<String> interests = Arrays.asList("music", "sports");
        user.setInterests(interests);
        assertEquals(interests, user.getInterests());
    }

    // -------------------------------------------------------------------------
    // US 01.04.03 — Notifications opt-in/opt-out
    // -------------------------------------------------------------------------

    // US 01.04.03: notificationsEnabled defaults to true on default constructor
    @Test
    public void defaultConstructor_notificationsEnabledIsTrue() {
        User user = new User();
        assertTrue("Notifications should be enabled by default", user.isNotificationsEnabled());
    }

    // US 01.04.03: notificationsEnabled defaults to true on convenience constructor
    @Test
    public void convenienceConstructor_notificationsEnabledIsTrue() {
        User user = new User("u-1", "Olivia Brown", "olivia.brown@gmail.com", "");
        assertTrue("Notifications should be enabled by default via convenience constructor",
                user.isNotificationsEnabled());
    }

    // US 01.04.03: User can opt out of notifications
    @Test
    public void setNotificationsEnabled_canToggleOff() {
        User user = new User();
        assertTrue(user.isNotificationsEnabled());

        user.setNotificationsEnabled(false);
        assertFalse("Notifications should be disabled after toggling off",
                user.isNotificationsEnabled());
    }

    // US 01.04.03: User can opt back into notifications
    @Test
    public void setNotificationsEnabled_canToggleBackOn() {
        User user = new User();
        user.setNotificationsEnabled(false);
        user.setNotificationsEnabled(true);
        assertTrue("Notifications should be re-enabled after toggling back on",
                user.isNotificationsEnabled());
    }

    // -------------------------------------------------------------------------
    // US 01.07.01 — Device ID identification
    // -------------------------------------------------------------------------

    // US 01.07.01: deviceId is stored and retrievable via setter/getter
    @Test
    public void setDeviceId_storesAndReturns() {
        User user = new User();
        user.setDeviceId("device-abc-123");
        assertEquals("device-abc-123", user.getDeviceId());
    }

    // US 01.07.01: deviceId is null by default
    @Test
    public void defaultConstructor_deviceIdIsNull() {
        User user = new User();
        assertNull("deviceId should be null by default", user.getDeviceId());
    }

    // US 01.07.01: Full constructor stores deviceId
    @Test
    public void fullConstructor_storesDeviceId() {
        Timestamp now = Timestamp.now();
        User user = new User("uid", "dev-99", "kevin.ross@gmail.com", "7805559012",
                "Kevin Ross", "ENTRANT", null, true, now, now, null);
        assertEquals("dev-99", user.getDeviceId());
    }

    // -------------------------------------------------------------------------
    // US 02.02.03 — Geolocation settings
    // -------------------------------------------------------------------------

    // US 02.02.03: geolocationEnabled defaults to false
    @Test
    public void defaultConstructor_geolocationEnabledIsFalse() {
        User user = new User();
        assertFalse("Geolocation should be disabled by default", user.isGeolocationEnabled());
    }

    // US 02.02.03: geolocationEnabled can be toggled on
    @Test
    public void setGeolocationEnabled_canToggleOn() {
        User user = new User();
        user.setGeolocationEnabled(true);
        assertTrue("Geolocation should be enabled after toggling on",
                user.isGeolocationEnabled());
    }

    // US 02.02.03: geolocationEnabled can be toggled back off
    @Test
    public void setGeolocationEnabled_canToggleBackOff() {
        User user = new User();
        user.setGeolocationEnabled(true);
        user.setGeolocationEnabled(false);
        assertFalse("Geolocation should be disabled after toggling back off",
                user.isGeolocationEnabled());
    }

    // US 02.02.03: GeoPoint location can be stored and retrieved
    @Test
    public void setLocation_storesGeoPoint() {
        User user = new User();
        GeoPoint gp = new GeoPoint(53.5461, -113.4938);
        user.setLocation(gp);
        assertEquals(gp, user.getLocation());
        assertEquals(53.5461, user.getLocation().getLatitude(), 0.0001);
        assertEquals(-113.4938, user.getLocation().getLongitude(), 0.0001);
    }

    // -------------------------------------------------------------------------
    // US 03.05.01 — Admin browses/manages user profiles by role
    // -------------------------------------------------------------------------

    // US 03.05.01: Default role is ENTRANT
    @Test
    public void defaultConstructor_roleIsEntrant() {
        User user = new User();
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
        assertFalse(user.isOrganizer());
        assertFalse(user.isAdmin());
    }

    // US 03.05.01: isOrganizer returns true for ORGANIZER role
    @Test
    public void isOrganizer_withOrganizerRole_returnsTrue() {
        User user = new User();
        user.setRole("ORGANIZER");
        assertTrue(user.isOrganizer());
        assertFalse(user.isEntrant());
        assertFalse(user.isAdmin());
    }

    // US 03.05.01: isAdmin returns true for ADMIN role
    @Test
    public void isAdmin_withAdminRole_returnsTrue() {
        User user = new User();
        user.setRole("ADMIN");
        assertTrue(user.isAdmin());
        assertFalse(user.isEntrant());
        assertFalse(user.isOrganizer());
    }

    // US 03.05.01: getRole returns the exact value assigned through setRole
    @Test
    public void getRole_returnsAssignedValueAfterSetRole() {
        User user = new User("u-1", "Rachel Kim", "rachel.kim@gmail.com", "");
        user.setRole("ORGANIZER");
        assertEquals("ORGANIZER", user.getRole());
    }

    // US 03.05.01: Role checks are case-insensitive
    @Test
    public void roleChecks_areCaseInsensitive() {
        User user = new User();

        user.setRole("entrant");
        assertTrue("isEntrant should be case-insensitive", user.isEntrant());

        user.setRole("organizer");
        assertTrue("isOrganizer should be case-insensitive", user.isOrganizer());

        user.setRole("admin");
        assertTrue("isAdmin should be case-insensitive", user.isAdmin());
    }

    // US 03.05.01: Null role defaults to ENTRANT
    @Test
    public void setRole_nullDefaultsToEntrant() {
        User user = new User();
        user.setRole("ADMIN");
        user.setRole(null);
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
    }

    // US 03.05.01: Full constructor defaults null role to ENTRANT
    @Test
    public void fullConstructor_nullRoleDefaultsToEntrant() {
        Timestamp now = Timestamp.now();
        User user = new User("uid", "dev-a1b2c3", "nora.gill@gmail.com", "7805551234",
                "Nora Gill", null, null, true, now, now, null);
        assertEquals("ENTRANT", user.getRole());
    }

    // -------------------------------------------------------------------------
    // touch() — timestamp management
    // -------------------------------------------------------------------------

    // US 01.02.02 / US 03.05.01: Updating a user should stamp updatedAt on first save
    @Test
    public void touch_setsUpdatedAt() {
        User user = new User();
        assertNull("updatedAt should be null before touch()", user.getUpdatedAt());

        user.touch();

        assertNotNull("updatedAt should be set after touch()", user.getUpdatedAt());
    }

    // US 01.02.01 / US 03.05.01: New users receive createdAt when first persisted
    @Test
    public void touch_setsCreatedAtIfNull() {
        User user = new User();
        assertNull("createdAt should be null before touch()", user.getCreatedAt());

        user.touch();

        assertNotNull("createdAt should be set after touch()", user.getCreatedAt());
        assertEquals("createdAt and updatedAt should be equal on first touch",
                user.getCreatedAt(), user.getUpdatedAt());
    }

    // US 01.02.02 / US 03.05.01: Updating a user preserves the original createdAt timestamp
    @Test
    public void touch_doesNotOverwriteExistingCreatedAt() {
        User user = new User();
        Timestamp original = Timestamp.now();
        user.setCreatedAt(original);

        user.touch();

        assertEquals("createdAt should remain the original value", original, user.getCreatedAt());
        assertNotNull("updatedAt should be set after touch()", user.getUpdatedAt());
    }

    // US 01.02.02 / US 03.05.01: Subsequent updates advance updatedAt without clearing metadata
    @Test
    public void touch_updatesUpdatedAtOnSubsequentCalls() {
        User user = new User();
        user.touch();
        Timestamp first = user.getUpdatedAt();
        assertNotNull(first);

        // Calling touch again should set a new (or equal) timestamp
        user.touch();
        Timestamp second = user.getUpdatedAt();
        assertNotNull(second);
        // The second timestamp should be >= the first
        assertTrue("Second touch should produce a timestamp >= first",
                second.compareTo(first) >= 0);
    }

    // -------------------------------------------------------------------------
    // Full constructor — all fields
    // -------------------------------------------------------------------------

    // US 01.02.01: setUserId updates the user identifier
    @Test
    public void setUserId_updatesValue() {
        User user = new User();
        user.setUserId("new-uid");
        assertEquals("new-uid", user.getUserId());
    }

    // US 01.02.02: setUpdatedAt directly sets the updatedAt timestamp
    @Test
    public void setUpdatedAt_updatesValue() {
        User user = new User();
        Timestamp ts = Timestamp.now();
        user.setUpdatedAt(ts);
        assertEquals(ts, user.getUpdatedAt());
    }

    // US 01.02.01 / US 01.04.03 / US 02.02.03 / US 03.05.01: Full user records retain all profile and preference fields
    @Test
    public void fullConstructor_setsAllFields() {
        Timestamp now = Timestamp.now();
        GeoPoint loc = new GeoPoint(51.05, -114.07);
        List<String> interests = Arrays.asList("sports", "music");

        User user = new User("uid-1", "dev-1", "lena.patel@gmail.com", "7805559876",
                "Lena Patel", "ORGANIZER", loc, false, now, now, interests);

        assertEquals("uid-1", user.getUserId());
        assertEquals("dev-1", user.getDeviceId());
        assertEquals("lena.patel@gmail.com", user.getEmail());
        assertEquals("7805559876", user.getPhone());
        assertEquals("Lena Patel", user.getUsername());
        assertEquals("ORGANIZER", user.getRole());
        assertEquals(loc, user.getLocation());
        assertFalse(user.isNotificationsEnabled());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertEquals(interests, user.getInterests());
    }
}
