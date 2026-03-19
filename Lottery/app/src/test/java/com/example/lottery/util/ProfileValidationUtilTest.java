package com.example.lottery.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ProfileValidationUtil}.
 *
 * <p>These tests verify the validation rules used for entrant
 * personal information such as name and email.</p>
 */
public class ProfileValidationUtilTest {

    /**
     * Verifies that a normal non-empty name is valid.
     */
    @Test
    public void isValidName_validName_returnsTrue() {
        assertTrue(ProfileValidationUtil.isValidName("Mike"));
    }

    /**
     * Verifies that an empty name is invalid.
     */
    @Test
    public void isValidName_emptyName_returnsFalse() {
        assertFalse(ProfileValidationUtil.isValidName(""));
    }

    /**
     * Verifies that a null name is invalid.
     */
    @Test
    public void isValidName_null_returnsFalse() {
        assertFalse(ProfileValidationUtil.isValidName(null));
    }

    /**
     * Verifies that a valid email passes validation.
     */
    @Test
    public void isValidEmail_validEmail_returnsTrue() {
        assertTrue(ProfileValidationUtil.isValidEmail("mike@gmail.com"));
    }

    /**
     * Verifies that an invalid email fails validation.
     */
    @Test
    public void isValidEmail_invalidEmail_returnsFalse() {
        assertFalse(ProfileValidationUtil.isValidEmail("mikegmail.com"));
    }

    /**
     * Verifies that an empty email fails validation.
     */
    @Test
    public void isValidEmail_emptyEmail_returnsFalse() {
        assertFalse(ProfileValidationUtil.isValidEmail(""));
    }
}