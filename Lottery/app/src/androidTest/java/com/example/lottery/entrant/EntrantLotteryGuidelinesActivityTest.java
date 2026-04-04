package com.example.lottery.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumented tests for {@link EntrantLotteryGuidelinesActivity}.
 * Verifies that the lottery selection guidelines are fully presented
 * so entrants understand the selection process (US 01.05.05).
 */
@RunWith(AndroidJUnit4.class)
public class EntrantLotteryGuidelinesActivityTest {

    private Intent createIntent() {
        return new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantLotteryGuidelinesActivity.class
        );
    }

    // -------------------------------------------------------------------------
    // US 01.05.05 — Guidelines content covers all five required sections
    // -------------------------------------------------------------------------

    /**
     * US 01.05.05: The guidelines screen displays all five sections that inform
     * entrants about the lottery process: How It Works, Joining the Waiting List,
     * Random Selection, After the Draw, and Notifications.
     */
    @Test
    public void guidelinesContent_displaysAllFiveSections() {
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.tv_guidelines_content)).check(matches(isDisplayed()));

            // Section 1: How It Works
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("How It Works"))));

            // Section 2: Joining the Waiting List
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("Joining the Waiting List"))));

            // Section 3: Random Selection
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("Random Selection"))));

            // Section 4: After the Draw
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("After the Draw"))));

            // Section 5: Notifications
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("Notifications"))));
        }
    }

    /**
     * US 01.05.05: The guidelines explain the core lottery mechanic — that
     * participants are randomly selected rather than first-come-first-served,
     * and every entrant has an equal chance.
     */
    @Test
    public void guidelinesContent_explainsEqualChanceSelection() {
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("lottery system"))));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("equal chance"))));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("randomly selected"))));
        }
    }

    /**
     * US 01.05.05: The guidelines explain post-draw behaviour — that selected
     * entrants can accept or decline, and that replacements are drawn when
     * someone declines.
     */
    @Test
    public void guidelinesContent_explainsAcceptDeclineAndReplacement() {
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("accept or decline"))));
            onView(withId(R.id.tv_guidelines_content))
                    .check(matches(withText(containsString("automatically drawn"))));
        }
    }

    /**
     * US 01.05.05: The guidelines title "Lottery Selection Guidelines" is
     * displayed at the top of the content area.
     */
    @Test
    public void guidelinesScreen_displaysTitle() {
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(createIntent())) {
            onView(withText("Lottery Selection Guidelines"))
                    .check(matches(isDisplayed()));
            onView(withText("Guidelines"))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * US 01.05.05: The back button on the guidelines screen closes the activity
     * and returns the entrant to the previous screen.
     */
    @Test
    public void backButton_finishesActivity() {
        try (ActivityScenario<EntrantLotteryGuidelinesActivity> scenario =
                     ActivityScenario.launch(createIntent())) {
            onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_back)).perform(click());

            AtomicBoolean destroyed = new AtomicBoolean(false);
            try {
                scenario.onActivity(activity -> destroyed.set(activity.isFinishing()));
            } catch (Exception e) {
                destroyed.set(true);
            }
            assertTrue("Activity should be finishing after back button click",
                    destroyed.get());
        }
    }
}
