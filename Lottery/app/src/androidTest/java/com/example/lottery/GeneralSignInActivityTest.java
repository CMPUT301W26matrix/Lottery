package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

public class GeneralSignInActivityTest {
    @Rule
    public ActivityScenarioRule<GeneralSignInActivity> activityRule =
            new ActivityScenarioRule<>(GeneralSignInActivity.class);

    @Test
    public void testAllViewsAreDisplayed() {
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        onView(withId(R.id.tvRegisterTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etEmail)).check(matches(hasErrorText("Email is required")));
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.etEmail)).perform(typeText("invalid-email"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etEmail)).check(matches(hasErrorText("Invalid email address")));
    }

    @Test
    public void testEmptyPasswordShowsError() {
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etPassword)).check(matches(hasErrorText("Password is required")));
    }

    @Test
    public void testSignInButtonIsClickable() {
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
        // Just verify navigation logic doesn't crash on incomplete data
        onView(withId(R.id.btnContinue)).perform(click());
    }

    @Test
    public void testValidInputPassesValidation() {
        String testEmail = "test@example.com";
        String testPassword = "password123";

        onView(withId(R.id.etEmail)).perform(typeText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText(testPassword), closeSoftKeyboard());

        onView(withId(R.id.btnContinue)).perform(click());
        // Validation passes, moves to Firebase logic (which we don't test here)
    }
}
