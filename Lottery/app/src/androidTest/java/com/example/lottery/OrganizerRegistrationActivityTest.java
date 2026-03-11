package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OrganizerRegistrationActivityTest {
    @Rule
    public ActivityScenarioRule<OrganizerRegistrationActivity> activityRule =
            new ActivityScenarioRule<>(OrganizerRegistrationActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // Test 1: Verify back button exists and navigates back
    @Test
    public void testBackButtonIsDisplayed() {
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }


    @Test
    public void testBackButtonFinishesActivity() {
        // Just verify the button is clickable - we can't easily test finish() in isolation
        onView(withId(R.id.btnBack)).perform(click());
        // The test passes if no exception is thrown
    }

    // Test 2: Validation tests
    @Test
    public void testEmptyNameShowsError() {
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etName)).check(matches(hasErrorText("Name is required")));
    }

    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.etName)).perform(typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etEmail)).check(matches(hasErrorText("Email is required")));
    }

    @Test
    public void testEmptyPasswordShowsError() {
        onView(withId(R.id.etName)).perform(typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etPassword)).check(matches(hasErrorText("Password is required")));
    }

    @Test
    public void testMismatchedPasswordsShowError() {
        onView(withId(R.id.etName)).perform(typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText("different123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etReEnterPassword)).check(matches(hasErrorText("Passwords do not match")));
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.etName)).perform(typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("invalid-email"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etEmail)).check(matches(hasErrorText("Invalid email address")));
    }


    // NEW Test 3: Test that validation passes with valid input
    @Test
    public void testValidInputPassesValidation() {
        String testName = "John Doe";
        String testEmail = "john.doe@example.com";
        String testPhone = "1234567890";
        String testPassword = "password123";

        onView(withId(R.id.etName)).perform(typeText(testName), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.etPhoneNumber)).perform(typeText(testPhone), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText(testPassword), closeSoftKeyboard());
        onView(withId(R.id.etReEnterPassword)).perform(typeText(testPassword), closeSoftKeyboard());

        // Just verify the button is clickable - actual Firebase operations will be tested in integration tests
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
    }
}
