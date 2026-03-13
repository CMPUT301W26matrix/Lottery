package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Waitlist-specific instrumentation tests for {@link EntrantEventDetailsActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsWaitlistTest {

    @Test
    public void waitlistButton_showsSupportedStateText() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailsActivity.class
        );

        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "10766c8d-b1e6-4b95-96e2-92742e8063b2");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID,
                "6xygP8FXpxATgAkKmj27");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.btnWaitlistAction))
                    .check(matches(isDisplayed()))
                    .check(matches(anyOf(
                            withText(R.string.join_wait_list),
                            withText(R.string.leave_wait_list)
                    )));
        }
    }
}
