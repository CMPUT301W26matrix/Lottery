package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;
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

/**
 * Unit tests for {@link AdminImageAdapter}.
 * Verifies that the adapter correctly binds event poster data to the RecyclerView items.
 * Covers US 03.06.01: As an administrator, I want to browse all uploaded event posters.
 * Covers US 03.03.01: As an administrator, I want to be able to remove event posters.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
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
        event1.setPosterBase64("data:image/jpeg;base64,poster1");
        event1.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000)));
        imageList.add(event1);

        Event event2 = new Event();
        event2.setEventId("event2");
        event2.setTitle("Workshop Poster");
        event2.setPosterBase64("data:image/jpeg;base64,poster2");
        event2.setScheduledDateTime(null);
        imageList.add(event2);

        adapter = new AdminImageAdapter(imageList, event -> {
        });
    }

    /**
     * US 03.06.01: Verifies that the adapter returns the correct item count based on the provided list.
     */
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * US 03.06.01: Verifies that the adapter handles empty lists correctly.
     */
    @Test
    public void testItemCountEmpty() {
        AdminImageAdapter emptyAdapter = new AdminImageAdapter(new ArrayList<>(), event -> {
        });
        assertEquals(0, emptyAdapter.getItemCount());
    }

    /**
     * US 03.06.01: Verifies that the ViewHolder is correctly created and contains necessary views.
     */
    @Test
    public void testOnCreateViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.ivThumbnail));
        assertNotNull(holder.itemView.findViewById(R.id.tvEventTitle));
        assertNotNull(holder.itemView.findViewById(R.id.tvEventDateTime));
    }

    /**
     * US 03.06.01: Verifies that the event title is correctly bound to the view.
     */
    @Test
    public void testOnBindViewHolderSetsTitle() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvEventTitle);
        assertEquals("Concert Poster", tvTitle.getText().toString());
    }

    /**
     * US 03.06.01: Verifies that the adapter handles null dates by clearing the text.
     */
    @Test
    public void testOnBindViewHolderSetsDateTBDWhenNull() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        TextView tvDateTime = holder.itemView.findViewById(R.id.tvEventDateTime);
        // In the actual implementation, null date results in an empty string
        assertEquals("", tvDateTime.getText().toString());
    }

    /**
     * US 03.06.01: Verifies that a scheduled date is correctly formatted and displayed.
     */
    @Test
    public void testOnBindViewHolderSetsFormattedDate() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvDateTime = holder.itemView.findViewById(R.id.tvEventDateTime);
        assertNotNull(tvDateTime.getText());
        assertNotEquals("", tvDateTime.getText().toString());
    }

    /**
     * US 03.06.01: Verifies that the thumbnail ImageView exists.
     */
    @Test
    public void testOnBindViewHolderSetsThumbnail() {
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ImageView ivThumbnail = holder.itemView.findViewById(R.id.ivThumbnail);
        assertNotNull(ivThumbnail);
    }

    /**
     * US 03.03.01: Verifies that clicking an item triggers the listener with the correct event.
     */
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

    /**
     * US 03.06.01: Verifies that event objects retain their IDs.
     */
    @Test
    public void testEventRetainsEventIdAfterSet() {
        Event event = new Event();
        event.setEventId("firestore_doc_id");
        assertEquals("firestore_doc_id", event.getEventId());
    }

    /**
     * US 03.06.01: Verifies that the adapter handles events with null IDs without crashing.
     */
    @Test
    public void testAdapterHandlesNullEventId() {
        Event event = new Event();
        event.setTitle("No ID Event");
        event.setPosterBase64("data:image/jpeg;base64,sample");
        List<Event> singleList = new ArrayList<>();
        singleList.add(event);
        AdminImageAdapter singleAdapter = new AdminImageAdapter(singleList, e -> {
        });
        FrameLayout parent = new FrameLayout(context);
        AdminImageAdapter.ImageViewHolder holder = singleAdapter.onCreateViewHolder(parent, 0);
        singleAdapter.onBindViewHolder(holder, 0);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvEventTitle);
        assertEquals("No ID Event", tvTitle.getText().toString());
    }

    /**
     * US 03.03.01: Verifies that the poster data is accessible.
     */
    @Test
    public void testEventPosterBase64Accessible() {
        Event event = imageList.get(0);
        assertNotNull("Poster Base64 should be set for image deletion", event.getPosterBase64());
        assertEquals("data:image/jpeg;base64,poster1", event.getPosterBase64());
    }

    /**
     * US 03.03.01: Verifies that the poster can be cleared.
     */
    @Test
    public void testClearPosterBase64AfterDeletion() {
        Event event = new Event();
        event.setPosterBase64("data:image/jpeg;base64,sample");
        assertNotNull(event.getPosterBase64());
        event.setPosterBase64(null);
        assertNull("Poster Base64 should be null after clearing", event.getPosterBase64());
    }
}
