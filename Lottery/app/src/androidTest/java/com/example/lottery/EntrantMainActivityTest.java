package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Android instrumentation tests for {@link EntrantMainActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMainActivityTest {

    @Test
    public void entrantMainScreen_opensSuccessfully() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantMainActivity.class
        );

        intent.putExtra("userId", "6xygP8FXpxATgAkKmj27");

        try (ActivityScenario<EntrantMainActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withText("Your Wait-Listed Events"))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvEvents))
                    .check(matches(isAssignableFrom(RecyclerView.class)));
        }
    }
}
