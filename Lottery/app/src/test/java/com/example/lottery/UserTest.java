package com.example.lottery;

import static org.junit.Assert.assertEquals;

import com.example.lottery.model.User;

import org.junit.Test;

/**
 * Unit tests for the User model class.
 *
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
}