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

    /**
     * Test that the constructor correctly stores
     * name, email, and phone number values.
     */
    @Test
    public void constructor_storesNameEmailAndPhoneCorrectly() {
        User user = new User("Alice", "alice@email.com", "7801234567");

        assertEquals("Alice", user.getName());
        assertEquals("alice@email.com", user.getEmail());
        assertEquals("7801234567", user.getPhoneNumber());
    }

    /**
     * Test that a user can be created with an empty phone number.
     */
    @Test
    public void constructor_allowsEmptyPhoneNumber() {
        User user = new User("Bob", "bob@email.com", "");

        assertEquals("Bob", user.getName());
        assertEquals("bob@email.com", user.getEmail());
        assertEquals("", user.getPhoneNumber());
    }

    /**
     * Test that the constructor overload correctly stores the firestore userId as well.
     */
    @Test
    public void constructor_storesUserIdNameEmailAndPhoneCorrectly() {
        User user = new User("user-123", "Alice", "alice@email.com", "7801234567");

        assertEquals("user-123", user.getUserId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@email.com", user.getEmail());
        assertEquals("7801234567", user.getPhoneNumber());
    }

    @Test
    public void defaultConstructor_setsRoleToEntrant() {
        User user = new User();
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
    }

    @Test
    public void setRole_organizerIsOrganizer() {
        User user = new User();
        user.setRole("ORGANIZER");
        assertTrue(user.isOrganizer());
        assertFalse(user.isEntrant());
        assertFalse(user.isAdmin());
    }

    @Test
    public void setRole_adminIsAdmin() {
        User user = new User();
        user.setRole("ADMIN");
        assertTrue(user.isAdmin());
        assertFalse(user.isEntrant());
        assertFalse(user.isOrganizer());
    }

    @Test
    public void isOrganizer_caseInsensitive() {
        User user = new User();
        user.setRole("organizer");
        assertTrue(user.isOrganizer());
    }

    @Test
    public void isEntrant_caseInsensitive() {
        User user = new User();
        user.setRole("entrant");
        assertTrue(user.isEntrant());
    }

    @Test
    public void setRole_nullFallsBackToEntrant() {
        User user = new User();
        user.setRole(null);
        assertEquals("ENTRANT", user.getRole());
        assertTrue(user.isEntrant());
    }

    @Test
    public void getRole_returnsSetValue() {
        User user = new User("u-1", "Test", "t@test.com", "");
        user.setRole("ORGANIZER");
        assertEquals("ORGANIZER", user.getRole());
    }
}
