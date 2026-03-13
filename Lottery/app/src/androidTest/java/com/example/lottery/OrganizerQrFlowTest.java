package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the Organizer QR Code flow.
 * Verifies the listing of events for QR codes and the detail view display.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerQrFlowTest {

    /**
     * Verifies that the OrganizerQrEventListActivity launches correctly.
     */
    @Test
    public void testEventListLaunch() {
        try (ActivityScenario<OrganizerQrEventListActivity> scenario = 
                ActivityScenario.launch(OrganizerQrEventListActivity.class)) {
            // Check if the RecyclerView is displayed
            onView(withId(R.id.rvQrEvents)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the OrganizerQrCodeDetailActivity displays the correct
     * information when passed a title via Intent.
     */
    @Test
    public void testQrDetailLaunchWithData() {
        Context context = ApplicationProvider.getApplicationContext();
        String testTitle = "Test Event for QR";
        String testContent = "MOCK_QR_CONTENT_12345";
        
        Intent intent = new Intent(context, OrganizerQrCodeDetailActivity.class);
        intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_EVENT_TITLE, testTitle);
        intent.putExtra(OrganizerQrCodeDetailActivity.EXTRA_QR_CONTENT, testContent);

        try (ActivityScenario<OrganizerQrCodeDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // Check if the title is set correctly
            onView(withId(R.id.tvDetailEventTitle)).check(matches(withText(testTitle)));
            // Check if the QR code ImageView is visible
            onView(withId(R.id.ivQrCodeLarge)).check(matches(isDisplayed()));
        }
    }
}
