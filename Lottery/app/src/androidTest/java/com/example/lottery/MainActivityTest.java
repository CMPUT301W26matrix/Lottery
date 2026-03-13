package com.example.lottery;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class MainActivityTest {

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        Intents.init();
        clearSharedPreferences();
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }

        clearSharedPreferences();
        Intents.release();
    }

    private void clearSharedPreferences() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void testEntrantButtonIsDisplayed() {
        onView(withId(R.id.entrant_login_button))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizerButtonIsDisplayed() {
        onView(withId(R.id.organizer_login_button))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAdminButtonIsDisplayed() {
        onView(withId(R.id.admin_login_button))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignInButtonIsDisplayed() {
        onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testChooseYourRoleTextIsDisplayed() {
        onView(withId(R.id.tvChooseRole)).check(matches(isDisplayed()));
        onView(withId(R.id.tvChooseRole)).check(matches(withText(R.string.choose_your_role)));
    }

    @Test
    public void testSignInHintAndButtonAreDisplayed() {
        onView(withId(R.id.tvSignInHint)).check(matches(isDisplayed()));
        onView(withId(R.id.tvSignInHint)).check(matches(withText(R.string.already_have_an_account)));
    }

    @Test
    public void testSwitchToEntrantRegistrationActivity() {
        onView(withId(R.id.entrant_login_button)).perform(click());
        intended(hasComponent(EntrantRegistrationActivity.class.getName()));
    }

    @Test
    public void testSwitchToOrganizerRegistrationActivity() {
        onView(withId(R.id.organizer_login_button)).perform(click());
        intended(hasComponent(OrganizerRegistrationActivity.class.getName()));
    }

    @Test
    public void testSwitchToAdminSignInActivity() {
        onView(withId(R.id.admin_login_button)).perform(click());
        intended(hasComponent(AdminSignInActivity.class.getName()));
    }

    @Test
    public void testSwitchToGeneralSignInActivity() {
        onView(withId(R.id.btnSignIn)).perform(click());
        intended(hasComponent(GeneralSignInActivity.class.getName()));
    }

}
