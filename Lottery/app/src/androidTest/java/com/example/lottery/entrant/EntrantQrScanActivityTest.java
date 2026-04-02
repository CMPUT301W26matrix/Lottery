package com.example.lottery.entrant;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for {@link EntrantQrScanActivity}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Activity initialization and intent handling.</li>
 *   <li>Presence of key UI components (Buttons).</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantQrScanActivityTest {

    private Intent intent;

    /**
     * Sets up the test environment before each test execution.
     * Initializes a mock intent containing a user ID.
     */
    @Before
    public void setUp() {
        intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantQrScanActivity.class);
        intent.putExtra("userId", "testUser123");
    }

    /**
     * Verifies that the activity is correctly instantiated.
     */
    @Test
    public void testActivityNotNull() {
        try (ActivityScenario<EntrantQrScanActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /**
     * Verifies that the scanner and gallery buttons exist in the layout.
     */
    @Test
    public void testButtonsExist() {
        try (ActivityScenario<EntrantQrScanActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.btnOpenScanner));
                assertNotNull(activity.findViewById(R.id.btnPickQrFromGallery));
                assertNotNull(activity.findViewById(R.id.btnBack));
            });
        }
    }
}
