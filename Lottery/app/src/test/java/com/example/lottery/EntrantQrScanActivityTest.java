package com.example.lottery;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link EntrantQrScanActivity} using Robolectric.
 * 
 * <p>Validates:
 * <ul>
 *   <li>Activity initialization and intent handling.</li>
 *   <li>Presence of key UI components (Buttons).</li>
 * </ul>
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34) // Robolectric supports up to 34 currently
public class EntrantQrScanActivityTest {

    /**
     * Controller to manage the lifecycle of the activity during tests.
     */
    private ActivityController<EntrantQrScanActivity> controller;

    /**
     * The activity instance under test.
     */
    private EntrantQrScanActivity activity;

    /**
     * Sets up the test environment before each test execution.
     * Initializes the activity with a mock intent containing a user ID.
     */
    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra("userId", "testUser123");
        
        controller = Robolectric.buildActivity(EntrantQrScanActivity.class, intent);
        activity = controller.setup().get();
    }

    /**
     * Verifies that the activity is correctly instantiated.
     */
    @Test
    public void testActivityNotNull() {
        assertNotNull(activity);
    }

    /**
     * Verifies that the scanner and gallery buttons exist in the layout.
     */
    @Test
    public void testButtonsExist() {
        assertNotNull(activity.findViewById(R.id.btnOpenScanner));
        assertNotNull(activity.findViewById(R.id.btnPickQrFromGallery));
        assertNotNull(activity.findViewById(R.id.btnBack));
    }
}
