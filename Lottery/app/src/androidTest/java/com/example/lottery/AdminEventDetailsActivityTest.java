package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminEventDetailsActivityTest {

    @Test
    public void testAdminEventDetailsScreenIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", "admin_event_id");

        try (ActivityScenario<AdminEventDetailsActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvPageHeader)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.cvPoster)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.tvDetailsHeader)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));
            onView(withId(R.id.btnRegister)).check(doesNotExist());
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }
}
