package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.lottery.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Waitlist-specific instrumentation tests for {@link EntrantEventDetailsActivity}.
 *
 * US 01.01.01: Entrant joins the waiting list for a specific event.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsWaitlistTest {

    // US 01.01.01: Waitlist button should show a supported state text
    @Test
    public void waitlistButton_showsSupportedStateText() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailsActivity.class
        );

        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID,
                "test_event_id");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID,
                "test_user_id");

        try (ActivityScenario<EntrantEventDetailsActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(ViewMatchers.withId(R.id.btnWaitlistAction))
                    .check(matches(isDisplayed()))
                    .check(matches(anyOf(
                            withText(R.string.join_wait_list),
                            withText(R.string.leave_wait_list)
                    )));
        }
    }
}
