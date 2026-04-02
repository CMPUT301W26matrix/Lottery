package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;
import com.example.lottery.model.NotificationItem;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NotificationAdapterTest {

    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        notifications = new ArrayList<>();
        // Updated constructor: notificationId, title, message, type, eventId, eventTitle, senderId, senderRole, isRead, createdAt
        notifications.add(new NotificationItem("id1", "Title 1", "Message 1", "type1", "event1", "Event Title 1", "sender1", "organizer", false, Timestamp.now()));
        notifications.add(new NotificationItem("id2", "Title 2", "Message 2", "type2", "event2", "Event Title 2", "sender2", "organizer", true, Timestamp.now()));

        adapter = new NotificationAdapter(notifications, item -> {
        });
    }

    // US 01.04.01: Verify adapter returns correct item count for entrant notifications
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 01.04.01: Verify notification view holder is created with expected layout elements
    @Test
    public void testOnCreateViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.tvNotificationTitle));
    }

    // US 01.04.01: Verify notification adapter binds title, message, and read status correctly
    @Test
    public void testOnBindViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        // In the new NotificationAdapter, if eventTitle is present, the title is formatted as "eventTitle: title"
        assertEquals("Event Title 1: Title 1", holder.tvTitle.getText().toString());
        assertEquals("Message 1", holder.tvMessage.getText().toString());
        assertEquals(View.VISIBLE, holder.tvNew.getVisibility());

        adapter.onBindViewHolder(holder, 1);
        assertEquals("Event Title 2: Title 2", holder.tvTitle.getText().toString());
        assertEquals(View.GONE, holder.tvNew.getVisibility());
    }

    // US 01.04.01: Verify notification adapter displays only title when event title is empty
    @Test
    public void testOnBindViewHolder_noEventTitle() {
        notifications.clear();
        notifications.add(new NotificationItem(
                "id3",
                "Title 3",
                "Message 3",
                "win",
                "event3",
                "", // Empty event title
                "sender3",
                "organizer",
                true,
                Timestamp.now()
        ));

        adapter = new NotificationAdapter(notifications, item -> {
        });

        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Title 3", holder.tvTitle.getText().toString());
    }
}
