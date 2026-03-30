package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.model.Event;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AdminImageAdapterTest {

    private AdminImageAdapter adapter;
    private List<Event> imageList;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);

        imageList = new ArrayList<>();

        Event event1 = new Event();
        event1.setEventId("event1");
        event1.setTitle("Concert Poster");
        event1.setPosterUri("https://example.com/poster1.jpg");
        event1.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000)));
        imageList.add(event1);

        Event event2 = new Event();
        event2.setEventId("event2");
        event2.setTitle("Workshop Poster");
        event2.setPosterUri("https://example.com/poster2.jpg");
        event2.setScheduledDateTime(null);
        imageList.add(event2);

        adapter = new AdminImageAdapter(imageList, event -> {
        });
    }

    // US 03.06.01: Admin image browser should display the correct number of uploaded images
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 03.06.01: Admin image browser should handle empty image list gracefully
    @Test
    public void testItemCountEmpty() {
        AdminImageAdapter emptyAdapter = new AdminImageAdapter(new ArrayList<>(), event -> {
        });
        assertEquals(0, emptyAdapter.getItemCount());
    }

    // US 03.06.01: Image list item should contain thumbnail, title, and date views
    @Test
    public void testOnCreateViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.ivThumbnail));
        assertNotNull(holder.itemView.findViewById(R.id.tvEventTitle));
        assertNotNull(holder.itemView.findViewById(R.id.tvEventDateTime));
    }

    // US 03.06.01: Image list should display the event title for each uploaded poster
    @Test
    public void testOnBindViewHolderSetsTitle() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvEventTitle);
        assertEquals("Concert Poster", tvTitle.getText().toString());
    }

    // US 03.06.01: Image list should show "Date TBD" when event has no scheduled date
    @Test
    public void testOnBindViewHolderSetsDateTBDWhenNull() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        TextView tvDateTime = holder.itemView.findViewById(R.id.tvEventDateTime);
        assertEquals("Date TBD", tvDateTime.getText().toString());
    }

    // US 03.06.01: Image list should display formatted date for scheduled events
    @Test
    public void testOnBindViewHolderSetsFormattedDate() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvDateTime = holder.itemView.findViewById(R.id.tvEventDateTime);
        assertNotNull(tvDateTime.getText());
        // Date is formatted, so it should not be "Date TBD"
        assertNotNull(tvDateTime.getText().toString());
        assertNotEquals("Date TBD", tvDateTime.getText().toString());
    }

    // US 03.06.01: Image list should display poster thumbnail for browsing
    @Test
    public void testOnBindViewHolderSetsThumbnail() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ImageView ivThumbnail = holder.itemView.findViewById(R.id.ivThumbnail);
        assertNotNull(ivThumbnail);
    }

    // US 03.03.01: Clicking an image should navigate to details for removal
    @Test
    public void testClickListenerTriggered() {
        final boolean[] clicked = {false};
        final String[] clickedEventId = {null};
        AdminImageAdapter clickAdapter = new AdminImageAdapter(imageList, event -> {
            clicked[0] = true;
            clickedEventId[0] = event.getEventId();
        });

        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = clickAdapter.onCreateViewHolder(parent, 0);
        clickAdapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        assertTrue(clicked[0]);
        assertEquals("event1", clickedEventId[0]);
    }

    // US 03.06.01: Event should retain eventId after being set (simulates setEventId after toObject)
    @Test
    public void testEventRetainsEventIdAfterSet() {
        Event event = new Event();
        event.setEventId("firestore_doc_id");
        assertEquals("firestore_doc_id", event.getEventId());
    }

    // US 03.06.01: Event with null eventId should not crash adapter
    @Test
    public void testAdapterHandlesNullEventId() {
        Event event = new Event();
        event.setTitle("No ID Event");
        event.setPosterUri("https://example.com/poster.jpg");
        // eventId is null - adapter should still bind without crash
        List<Event> singleList = new ArrayList<>();
        singleList.add(event);
        AdminImageAdapter singleAdapter = new AdminImageAdapter(singleList, e -> {
        });
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = singleAdapter.onCreateViewHolder(parent, 0);
        singleAdapter.onBindViewHolder(holder, 0);
        // Should not throw
        TextView tvTitle = holder.itemView.findViewById(R.id.tvEventTitle);
        assertEquals("No ID Event", tvTitle.getText().toString());
    }

    // US 03.03.01: Event posterUri should be accessible for deletion
    @Test
    public void testEventPosterUriAccessible() {
        Event event = imageList.get(0);
        assertNotNull("Poster URI should be set for image deletion", event.getPosterUri());
        assertEquals("https://example.com/poster1.jpg", event.getPosterUri());
    }

    // US 03.03.01: Setting posterUri to null simulates clearing after deletion
    @Test
    public void testClearPosterUriAfterDeletion() {
        Event event = new Event();
        event.setPosterUri("https://example.com/poster.jpg");
        assertNotNull(event.getPosterUri());
        event.setPosterUri(null);
        assertNull("Poster URI should be null after clearing", event.getPosterUri());
    }
}
