package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EditProfileActivity.
 *
 * These tests verify that the Edit Profile screen
 * displays the required input fields.
 */
@RunWith(AndroidJUnit4.class)
public class EditProfileActivityTest {

    /**
     * Verifies that name field is visible.
     */
    @Test
    public void nameField_isDisplayed() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditProfileActivity.class
        );

        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EditProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.et_edit_name)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies email field is visible.
     */
    @Test
    public void emailField_isDisplayed() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditProfileActivity.class
        );

        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EditProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.et_edit_email)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies phone field is visible.
     */
    @Test
    public void phoneField_isDisplayed() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditProfileActivity.class
        );

        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EditProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.et_edit_phone)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies save button is visible.
     */
    @Test
    public void saveButton_isDisplayed() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditProfileActivity.class
        );

        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EditProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.btn_save_profile)).check(matches(isDisplayed()));
        }

    }

    /**
     * Verifies that the user can type in profile fields.
     */
    @Test
    public void userCanEnterProfileInformation() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EditProfileActivity.class
        );

        intent.putExtra("userId", "testUser");

        try (ActivityScenario<EditProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.et_edit_name))
                    .perform(typeText("Mike"), closeSoftKeyboard());

            onView(withId(R.id.et_edit_email))
                    .perform(typeText("mike@gmail.com"), closeSoftKeyboard());

            onView(withId(R.id.et_edit_phone))
                    .perform(typeText("1234567890"), closeSoftKeyboard());
        }
    }

}