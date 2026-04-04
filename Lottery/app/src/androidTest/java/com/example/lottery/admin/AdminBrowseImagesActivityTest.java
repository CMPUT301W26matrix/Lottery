package com.example.lottery.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.example.lottery.util.FirestorePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class AdminBrowseImagesActivityTest {
    private static final String POSTER_EVENT_ID = "admin_image_swim_clinic_poster";
    private static final String NO_POSTER_EVENT_ID = "admin_image_referee_workshop_plain";
    private static final String POSTER_EVENT_TITLE = "Swimming Clinic Poster Review";
    private static final String NO_POSTER_EVENT_TITLE = "Referee Workshop Schedule";
    private static final String TEST_POSTER = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEA";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ActivityScenario<AdminBrowseImagesActivity> launchDefault() {
        return ActivityScenario.launch(AdminBrowseImagesActivity.class);
    }

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        try {
            deleteEvent(POSTER_EVENT_ID);
            deleteEvent(NO_POSTER_EVENT_ID);
        } catch (Exception ignored) {
        }
        Intents.release();
    }

    private void seedEvent(String eventId, String title, String posterBase64)
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("title", title);
        event.put("organizerId", "natalie.wong");
        event.put("details", "Poster browsing integration coverage for " + title);
        event.put("scheduledDateTime", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))));
        event.put("posterBase64", posterBase64);
        event.put("createdAt", Timestamp.now());
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).set(event), 10, TimeUnit.SECONDS);
    }

    private void deleteEvent(String eventId)
            throws InterruptedException, ExecutionException, TimeoutException {
        Tasks.await(db.collection(FirestorePaths.EVENTS).document(eventId).delete(), 10, TimeUnit.SECONDS);
    }

    private void waitForImagesLoaded(ActivityScenario<AdminBrowseImagesActivity> scenario) throws InterruptedException {
        boolean[] loaded = {false};
        for (int attempt = 0; attempt < 20; attempt++) {
            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView recyclerView = activity.findViewById(R.id.rvImages);
                loaded[0] = recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() >= 1;
            });
            if (loaded[0]) {
                return;
            }
            Thread.sleep(250);
        }
        assertTrue("Expected Firestore-backed image list to load", loaded[0]);
    }

    // US 03.06.01: Admin should see image browser title
    @Test
    public void testBrowseImagesScreenIsDisplayed() {
        try (ActivityScenario<AdminBrowseImagesActivity> ignored = launchDefault()) {
            onView(ViewMatchers.withId(R.id.tvAppTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvAppTitle)).check(matches(withText(R.string.admin_image_browser_title)));
        }
    }

    // US 03.06.01: Empty state should show when no images are uploaded
    @Test
    public void testNoImagesMessageVisibility() {
        try (ActivityScenario<AdminBrowseImagesActivity> scenario = launchDefault()) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.tvNoImages).setVisibility(View.VISIBLE)
            );
            onView(withId(R.id.tvNoImages)).check(matches(isDisplayed()));
        }
    }

    // US 03.03.01: Clicking an image should navigate to image details for removal
    @Test
    public void testOnImageClickLaunchesAdminImageDetailsActivity() {
        try (ActivityScenario<AdminBrowseImagesActivity> scenario = launchDefault()) {
            intending(hasComponent(AdminImageDetailsActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            scenario.onActivity(activity -> {
                Event event = new Event();
                event.setEventId("test_image_event_id");
                activity.onImageClick(event);
            });

            intended(hasComponent(AdminImageDetailsActivity.class.getName()));
            intended(hasExtra("eventId", "test_image_event_id"));
        }
    }

    // US 03.06.01: Admin image browser should load poster-bearing events from Firestore
    // and exclude events that do not actually have uploaded poster data.
    @Test
    public void adminBrowseImages_loadsOnlyFirestoreEventsWithPosters() throws Exception {
        seedEvent(POSTER_EVENT_ID, POSTER_EVENT_TITLE, TEST_POSTER);
        seedEvent(NO_POSTER_EVENT_ID, NO_POSTER_EVENT_TITLE, null);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminBrowseImagesActivity.class);
        intent.putExtra("userId", "admin_jordan_clark");

        try (ActivityScenario<AdminBrowseImagesActivity> scenario = ActivityScenario.launch(intent)) {
            waitForImagesLoaded(scenario);

            onView(withId(R.id.rvImages)).perform(
                    RecyclerViewActions.scrollTo(hasDescendant(withText(POSTER_EVENT_TITLE)))
            );
            onView(withId(R.id.rvImages)).check(matches(hasDescendant(withText(POSTER_EVENT_TITLE))));
            onView(withText(NO_POSTER_EVENT_TITLE)).check(doesNotExist());
        }
    }
}
