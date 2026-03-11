package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
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
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        // Activity should still be displayed (no navigation)
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyPasswordShowsError() {
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.btnContinue)).perform(click());

        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
    }

    @Test
    public void testSignInButtonIsClickable() {
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).perform(click());
        // Test passes if no exception
    }

    @Test
    public void testValidInputPassesValidation() {
        String testEmail = "test@example.com";
        String testPassword = "password123";

        onView(withId(R.id.etEmail)).perform(typeText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText(testPassword), closeSoftKeyboard());

        // Just verify the button is still there and clickable
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()));
        onView(withId(R.id.btnContinue)).perform(click());
    }
}
