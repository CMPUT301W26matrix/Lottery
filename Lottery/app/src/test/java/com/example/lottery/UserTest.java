package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lottery.model.User;

import org.junit.Test;

/**
 * Unit tests for the User model class.
 * <p>
 * These tests verify that the User object correctly stores
 * and returns the user’s name, email, and phone number.
 */
public class UserTest {

    // US 03.05.01: User profile should store name, email, and phone correctly
    @Test
    public void constructor_storesNameEmailAndPhoneCorrectly() {
        User user = new User("user-1", "Alice", "alice@email.com", "7801234567");

        assertEquals("Alice", user.getUsername());
        assertEquals("alice@email.com", user.getEmail());
        assertEquals("7801234567", user.getPhone());
    }

    // US 03.05.01: User profile should allow empty phone number
    @Test
    public void constructor_allowsEmptyPhoneNumber() {
        User user = new User("user-2", "Bob", "bob@email.com", "");

        assertEquals("Bob", user.getUsername());
        assertEquals("bob@email.com", user.getEmail());
        assertEquals("", user.getPhone());
    }

    // US 03.05.01: User profile should store userId along with other fields
    @Test
    public void constructor_storesUserIdNameEmailAndPhoneCorrectly() {
        User user = new User("user-123", "Alice", "alice@email.com", "7801234567");

        assertEquals("user-123", user.getUserId());
        assertEquals("Alice", user.getUsername());
        assertEquals("alice@email.com", user.getEmail());
        assertEquals("7801234567", user.getPhone());
    }

    // US 03.05.01: Default user role should be ENTRANT for profile browsing
    @Test
    public void defaultConstructor_setsRoleToEntrant() {
        User user = new User();
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
    }

    // US 03.07.01: Organizer role should be correctly identified for policy enforcement
    @Test
    public void setRole_organizerIsOrganizer() {
        User user = new User();
        user.setRole("ORGANIZER");
        assertTrue(user.isOrganizer());
        assertFalse(user.isEntrant());
        assertFalse(user.isAdmin());
    }

    // US 03.02.01: Admin role should be correctly identified for deletion guard
    @Test
    public void setRole_adminIsAdmin() {
        User user = new User();
        user.setRole("ADMIN");
        assertTrue(user.isAdmin());
        assertFalse(user.isEntrant());
        assertFalse(user.isOrganizer());
    }

    // US 03.07.01: Organizer role check should be case-insensitive
    @Test
    public void isOrganizer_caseInsensitive() {
        User user = new User();
        user.setRole("organizer");
        assertTrue(user.isOrganizer());
    }

    // US 03.05.01: Entrant role check should be case-insensitive for profile filtering
    @Test
    public void isEntrant_caseInsensitive() {
        User user = new User();
        user.setRole("entrant");
        assertTrue(user.isEntrant());
    }

    // US 03.05.01: Null role should default to ENTRANT for safe profile browsing
    @Test
    public void setRole_nullFallsBackToEntrant() {
        User user = new User();
        user.setRole(null);
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
    }

    // US 03.05.01: Role getter should return the value that was set
    @Test
    public void getRole_returnsSetValue() {
        User user = new User("u-1", "Test", "t@test.com", "");
        user.setRole("ORGANIZER");
        assertEquals("ORGANIZER", user.getRole());
    }

    // US 03.02.01: Admin role should prevent profile deletion
    @Test
    public void isAdmin_withAdminRole_shouldBlockDeletion() {
        User admin = new User("a-1", "Admin", "admin@test.com", "");
        admin.setRole("ADMIN");
        assertTrue("Admin user should be identified for deletion guard", admin.isAdmin());
        assertFalse("Admin should not be identified as organizer", admin.isOrganizer());
        assertFalse("Admin should not be identified as entrant", admin.isEntrant());
    }

    // US 03.02.01: Entrant role should allow profile deletion
    @Test
    public void isAdmin_withEntrantRole_shouldAllowDeletion() {
        User entrant = new User("e-1", "Entrant", "entrant@test.com", "");
        entrant.setRole("ENTRANT");
        assertFalse("Entrant should not trigger admin deletion guard", entrant.isAdmin());
    }

    // US 03.07.01: Organizer role should allow profile deletion (for policy violations)
    @Test
    public void isAdmin_withOrganizerRole_shouldAllowDeletion() {
        User organizer = new User("o-1", "Organizer", "org@test.com", "");
        organizer.setRole("ORGANIZER");
        assertFalse("Organizer should not trigger admin deletion guard", organizer.isAdmin());
    }
}
