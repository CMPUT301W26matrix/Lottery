package com.example.lottery;

import static java.util.regex.Pattern.matches;

import org.junit.Test;

public class MainActivityTest {
    @Test
    public void testRoleButtonsAreDisplayed() {
        onViewCreated(withId(R.id.entrant_login_button)).check(matches(isDisplayed()));
        onViewCreated(withId(R.id.organizer_login_button)).check(matches(isDisplayed()));
        onViewCreated(withId(R.id.admin_login_button)).check(matches(isDisplayed()));
    }
}
