package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isA;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.admin.AdminEventDetailsActivity;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented tests for {@link AdminEventDetailsActivity}.
 * Verifies the administrative view of event details, including deletion and comment moderation features.
 *
 * <p>Covers User Stories:</p>
 * <ul>
 *   <li>US 03.01.01: As an administrator, I want to be able to remove events.</li>
 *   <li>US 03.10.01: As an administrator, I want to remove event comments that violate app policy.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class AdminEventDetailsActivityTest {

    private static final String TEST_EVENT_ID = "test_admin_event_123";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        db = FirebaseFirestore.getInstance();

        // Seed a dummy event in Firestore so the Activity doesn't finish early
        Event testEvent = new Event();
        testEvent.setEventId(TEST_EVENT_ID);
        testEvent.setTitle("Admin Test Event");
        testEvent.setDetails("This is a test event for administrative view.");
        testEvent.setOrganizerId("test_organizer");
        testEvent.touch();

        // Wait for Firestore write to ensure data is present before activity launch
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).set(testEvent), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        // Clean up test data
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(TEST_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
    }

    /**
     * Scrolls a view into the visible area of a NestedScrollView.
     * Standard scrollTo() is incompatible with NestedScrollView.
     */
    private static ViewAction nestedScrollTo() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isA(View.class);
            }

            @Override
            public String getDescription() {
                return "scroll view inside parent NestedScrollView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ViewParent parent = view.getParent();
                while (parent instanceof View) {
                    if (parent instanceof NestedScrollView) {
                        NestedScrollView scrollView = (NestedScrollView) parent;
                        Rect rect = new Rect();
                        view.getDrawingRect(rect);
                        scrollView.offsetDescendantRectToMyCoords(view, rect);
                        scrollView.requestChildRectangleOnScreen(view, rect, true);
                        uiController.loopMainThreadUntilIdle();
                        return;
                    }
                    parent = parent.getParent();
                }

                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withViewDescription(view.toString())
                        .build();
            }
        };
    }

    /**
     * US 03.01.01: Verifies that the admin event details screen displays all essential elements.
     */
    @Test
    public void testAdminEventDetailsScreenIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvPageHeader)).check(matches(isDisplayed()));
            onView(withId(R.id.tvDetailsHeader)).perform(nestedScrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.bottom_nav_container)).check(matches(isDisplayed()));

            // Ensure organizer-only controls are not present
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }

    /**
     * US 03.01.01: Verifies that clicking the delete button triggers a confirmation dialog.
     */
    @Test
    public void testDeleteButtonShowsConfirmationDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo(), click());

            onView(withText(R.string.confirm_deletion)).check(matches(isDisplayed()));
            onView(withText(R.string.confirm_delete_event)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 03.01.01: Verifies that cancelling the deletion dialog dismisses it.
     */
    @Test
    public void testDeleteConfirmationCancelDismissesDialog() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDeleteEvent)).perform(nestedScrollTo(), click());
            onView(withText(R.string.cancel)).perform(click());

            onView(withText(R.string.confirm_deletion)).check(doesNotExist());
        }
    }

    /**
     * US 03.10.01: Verifies that the comments moderation button is available.
     */
    @Test
    public void testCommentsButtonIsDisplayed() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnComments)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 03.01.01: Ensures that the admin view does not expose organizer editing privileges.
     */
    @Test
    public void testAdminScreenDoesNotExposeOrganizerEditButton() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);

        try (ActivityScenario<AdminEventDetailsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvPageHeader)).check(matches(isDisplayed()));
            onView(withId(R.id.btnEditEvent)).check(doesNotExist());
        }
    }
}
