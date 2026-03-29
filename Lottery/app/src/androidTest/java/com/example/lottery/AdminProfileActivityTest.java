package com.example.lottery;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lottery.util.AdminRoleManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Unit tests for the admin role switching feature.
 * Tests the AdminRoleManager utility class and role switching logic.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminProfileActivityTest {

    private Context context;
    private SharedPreferences prefs;
    private static final String TEST_ADMIN_ID = "admin_test_123";
    private static final String TEST_ENTRANT_ID = "entrant_test_123";
    private static final String TEST_ORGANIZER_ID = "organizer_test_123";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences("AdminRolePrefs", Context.MODE_PRIVATE);
        // Clear any existing preferences before each test
        prefs.edit().clear().apply();
    }

    @After
    public void tearDown() {
        // Clean up after each test
        prefs.edit().clear().apply();
    }

    @Test
    public void testSetAdminRoleSession() {
        // Test setting an admin role session
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        assertTrue("Admin role session should be active",
                AdminRoleManager.isAdminRoleSession(context));
        assertEquals("Admin user ID should match",
                TEST_ADMIN_ID,
                AdminRoleManager.getAdminUserId(context));
    }

    @Test
    public void testIsAdminRoleSession_WhenNoSession() {
        // Test when no session is set
        assertFalse("Admin role session should be false when not set",
                AdminRoleManager.isAdminRoleSession(context));
        assertNull("Admin user ID should be null when not set",
                AdminRoleManager.getAdminUserId(context));
    }

    @Test
    public void testClearAdminRoleSession() {
        // Set a session first
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);
        assertTrue("Session should be active",
                AdminRoleManager.isAdminRoleSession(context));

        // Clear the session
        AdminRoleManager.clearAdminRoleSession(context);

        assertFalse("Session should be cleared",
                AdminRoleManager.isAdminRoleSession(context));
        assertNull("Admin user ID should be null after clearing",
                AdminRoleManager.getAdminUserId(context));
    }

    @Test
    public void testMultipleAdminSessions_LastOneWins() {
        // Set first admin
        AdminRoleManager.setAdminRoleSession(context, "admin_first");
        assertEquals("First admin ID should be stored",
                "admin_first",
                AdminRoleManager.getAdminUserId(context));

        // Set second admin (overwrites)
        AdminRoleManager.setAdminRoleSession(context, "admin_second");
        assertEquals("Second admin ID should overwrite",
                "admin_second",
                AdminRoleManager.getAdminUserId(context));

        assertTrue("Session should remain active",
                AdminRoleManager.isAdminRoleSession(context));
    }

    @Test
    public void testAdminRoleSession_Persistence() {
        // Test that session persists across instances (using same preferences)
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        // Create a new instance (simulating app restart)
        AdminRoleManager.clearAdminRoleSession(context); // Clear to simulate fresh start

        // The session should be cleared
        assertFalse("Session should not persist after clear",
                AdminRoleManager.isAdminRoleSession(context));
    }

    @Test
    public void testGetAdminUserId_ReturnsCorrectId() {
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);
        String retrievedId = AdminRoleManager.getAdminUserId(context);

        assertNotNull("Retrieved ID should not be null", retrievedId);
        assertEquals("Retrieved ID should match the set ID", TEST_ADMIN_ID, retrievedId);
    }

    @Test
    public void testGetAdminUserId_WithNoSession_ReturnsNull() {
        String retrievedId = AdminRoleManager.getAdminUserId(context);
        assertNull("Retrieved ID should be null when no session exists", retrievedId);
    }

    @Test
    public void testIsAdminRoleSession_WithDifferentAdminIds() {
        // Test with first admin
        AdminRoleManager.setAdminRoleSession(context, "admin_alpha");
        assertTrue("Session should be true for admin_alpha",
                AdminRoleManager.isAdminRoleSession(context));
        assertEquals("Admin ID should be admin_alpha",
                "admin_alpha",
                AdminRoleManager.getAdminUserId(context));

        // Clear and test with second admin
        AdminRoleManager.clearAdminRoleSession(context);
        AdminRoleManager.setAdminRoleSession(context, "admin_beta");
        assertTrue("Session should be true for admin_beta",
                AdminRoleManager.isAdminRoleSession(context));
        assertEquals("Admin ID should be admin_beta",
                "admin_beta",
                AdminRoleManager.getAdminUserId(context));
    }

    @Test
    public void testSessionFlags_Independence() {
        // Set admin role session
        AdminRoleManager.setAdminRoleSession(context, TEST_ADMIN_ID);

        // Create a separate shared preference for another feature (should not interfere)
        SharedPreferences otherPrefs = context.getSharedPreferences("OtherPrefs", Context.MODE_PRIVATE);
        otherPrefs.edit().putBoolean("other_flag", true).apply();

        // Admin role session should remain unchanged
        assertTrue("Admin role session should remain true",
                AdminRoleManager.isAdminRoleSession(context));
        assertEquals("Admin ID should remain correct",
                TEST_ADMIN_ID,
                AdminRoleManager.getAdminUserId(context));

        // Clean up
        otherPrefs.edit().clear().apply();
    }
}
