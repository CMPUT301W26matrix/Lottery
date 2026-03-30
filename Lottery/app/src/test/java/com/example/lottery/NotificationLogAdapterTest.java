package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;

/**
 * Unit tests for {@link NotificationLogAdapter}.
 * Covers US 03.08.01: As an administrator, I want to review logs of all
 *     notifications sent to entrants by organizers.
 */
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NotificationLogAdapterTest {

    private NotificationLogAdapter adapter;
    private Context context;
    private MockedStatic<FirebaseFirestore> mockedFirestore;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);

        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        mockedFirestore = mockStatic(FirebaseFirestore.class);
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDocument = mock(DocumentReference.class);
        var mockTask = mock(Task.class);

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        when(mockDocument.get()).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        List<Map<String, Object>> logList = new ArrayList<>();

        Map<String, Object> log1 = new HashMap<>();
        log1.put("eventTitle", "Music Festival");
        log1.put("group", "selected");
        log1.put("message", "You have been selected");
        log1.put("recipientCount", 10L);
        log1.put("createdAt", new Timestamp(new Date()));
        log1.put("senderId", "organizer1");
        logList.add(log1);

        Map<String, Object> log2 = new HashMap<>();
        log2.put("eventTitle", null);
        log2.put("group", null);
        log2.put("message", null);
        log2.put("recipientCount", null);
        log2.put("createdAt", null);
        log2.put("senderId", null);
        logList.add(log2);

        adapter = new NotificationLogAdapter(logList);
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
    }

    // US 03.08.01: Notification log list should display the correct number of log entries
    @Test
    public void testItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // US 03.08.01: Notification log list should handle empty state gracefully
    @Test
    public void testItemCountEmpty() {
        NotificationLogAdapter emptyAdapter = new NotificationLogAdapter(new ArrayList<>());
        assertEquals(0, emptyAdapter.getItemCount());
    }

    // US 03.08.01: Log item should contain event title, organizer, message, group, count, and timestamp views
    @Test
    public void testOnCreateViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.tvLogEventTitle));
        assertNotNull(holder.itemView.findViewById(R.id.tvLogOrganizer));
        assertNotNull(holder.itemView.findViewById(R.id.tvLogMessage));
        assertNotNull(holder.itemView.findViewById(R.id.tvLogGroup));
        assertNotNull(holder.itemView.findViewById(R.id.tvLogRecipientCount));
        assertNotNull(holder.itemView.findViewById(R.id.tvLogTimestamp));
    }

    // US 03.08.01: Log entry should display the event title
    @Test
    public void testBindSetsEventTitle() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvLogEventTitle);
        assertEquals("Music Festival", tvTitle.getText().toString());
    }

    // US 03.08.01: Log entry should display recipient group in uppercase
    @Test
    public void testBindSetsGroupUpperCase() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvGroup = holder.itemView.findViewById(R.id.tvLogGroup);
        assertEquals("SELECTED", tvGroup.getText().toString());
    }

    // US 03.08.01: Log entry should display the notification message
    @Test
    public void testBindSetsMessage() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvMessage = holder.itemView.findViewById(R.id.tvLogMessage);
        assertEquals("You have been selected", tvMessage.getText().toString());
    }

    // US 03.08.01: Log entry should display the number of recipients
    @Test
    public void testBindSetsRecipientCount() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvCount = holder.itemView.findViewById(R.id.tvLogRecipientCount);
        assertEquals("10", tvCount.getText().toString());
    }

    // US 03.08.01: Log entry should display formatted timestamp
    @Test
    public void testBindSetsTimestamp() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTimestamp = holder.itemView.findViewById(R.id.tvLogTimestamp);

        // Timestamp is formatted, so it should not be the fallback
        assertNotNull(tvTimestamp.getText());
        assertNotEquals("N/A", tvTimestamp.getText().toString());
    }

    // US 03.08.01: Log entry should display fallback values for missing fields
    @Test
    public void testBindHandlesNullFields() {
        FrameLayout parent = new FrameLayout(context);
        NotificationLogAdapter.LogViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvLogEventTitle);
        assertEquals("Unknown Event", tvTitle.getText().toString());

        TextView tvGroup = holder.itemView.findViewById(R.id.tvLogGroup);
        assertEquals("", tvGroup.getText().toString());

        TextView tvMessage = holder.itemView.findViewById(R.id.tvLogMessage);
        assertEquals("", tvMessage.getText().toString());

        TextView tvCount = holder.itemView.findViewById(R.id.tvLogRecipientCount);
        assertEquals("0", tvCount.getText().toString());

        TextView tvTimestamp = holder.itemView.findViewById(R.id.tvLogTimestamp);
        assertEquals("N/A", tvTimestamp.getText().toString());

        TextView tvOrganizer = holder.itemView.findViewById(R.id.tvLogOrganizer);
        assertEquals("Unknown Organizer", tvOrganizer.getText().toString());
    }
}
