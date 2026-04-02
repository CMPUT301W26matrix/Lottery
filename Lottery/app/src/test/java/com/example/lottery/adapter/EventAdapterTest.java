package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
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

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for {@link EventAdapter}.
 * Covers US 03.04.01: As an administrator, I want to be able to browse events.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EventAdapterTest {

    private Context context;
    private MockedStatic<FirebaseFirestore> mockedFirestore;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);

        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        mockedFirestore = mockStatic(FirebaseFirestore.class);
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDocument = mock(DocumentReference.class);
        CollectionReference mockSubCollection = mock(CollectionReference.class);
        Task<QuerySnapshot> mockTask = mock(Task.class);

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        when(mockDocument.collection(anyString())).thenReturn(mockSubCollection);

        when(mockCollection.get()).thenReturn(mockTask);
        when(mockSubCollection.get()).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
    }

    // ---------------------------------------------------------------
    // resolveDisplayStatus tests (static method, no adapter needed)
    // ---------------------------------------------------------------

    // US 03.04.01: Null event should resolve to "closed"
    @Test
    public void resolveDisplayStatus_nullEvent_returnsClosed() {
        assertEquals("closed", EventAdapter.resolveDisplayStatus(null));
    }

    // US 03.04.01: Firestore status "closed" should be honored even if dates say open
    @Test
    public void resolveDisplayStatus_firestoreStatusClosed_returnsClosed() {
        Event event = new Event();
        event.setStatus("closed");
        // Set a future scheduled date that would normally make the event "open"
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7)));
        assertEquals("closed", EventAdapter.resolveDisplayStatus(event));
    }

    // US 03.04.01: Status check should be case-insensitive
    @Test
    public void resolveDisplayStatus_firestoreStatusClosedCaseInsensitive_returnsClosed() {
        Event event = new Event();
        event.setStatus("Closed");
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7)));
        assertEquals("closed", EventAdapter.resolveDisplayStatus(event));
    }

    // US 03.04.01: Event between registration deadline and draw date should be "pending"
    @Test
    public void resolveDisplayStatus_pendingState_returnsPending() {
        Event event = new Event();
        Timestamp past = new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 7));
        Timestamp future = new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7));
        event.setRegistrationDeadline(past);
        event.setDrawDate(future);
        assertEquals("pending", EventAdapter.resolveDisplayStatus(event));
    }

    // US 03.04.01: Event with future scheduled date should be "open"
    @Test
    public void resolveDisplayStatus_futureScheduledDate_returnsOpen() {
        Event event = new Event();
        Timestamp future = new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7));
        event.setScheduledDateTime(future);
        assertEquals("open", EventAdapter.resolveDisplayStatus(event));
    }

    // US 03.04.01: Event with past scheduled date should be "closed"
    @Test
    public void resolveDisplayStatus_pastScheduledDate_returnsClosed() {
        Event event = new Event();
        Timestamp past = new Timestamp(new Date(System.currentTimeMillis() - 86400000L * 7));
        event.setScheduledDateTime(past);
        assertEquals("closed", EventAdapter.resolveDisplayStatus(event));
    }

    // US 03.04.01: Event with no date fields should default to "closed"
    @Test
    public void resolveDisplayStatus_noDateFields_returnsClosed() {
        Event event = new Event();
        assertEquals("closed", EventAdapter.resolveDisplayStatus(event));
    }

    // ---------------------------------------------------------------
    // ViewHolder bind tests (require adapter + Firestore mocking)
    // ---------------------------------------------------------------

    // US 03.04.01: Null capacity should display "-" instead of "null"
    @Test
    public void bind_nullCapacity_displaysDash() {
        Event event = new Event();
        event.setEventId("evt-null-cap");
        event.setTitle("No Capacity Event");
        event.setCapacity(null);
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7)));

        List<Event> eventList = new ArrayList<>();
        eventList.add(event);
        EventAdapter adapter = new EventAdapter(eventList, e -> {
        });

        FrameLayout parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView tvCapacity = holder.itemView.findViewById(R.id.tvCapacityValue);
        assertEquals("-", tvCapacity.getText().toString());
    }

    // US 03.04.01: Non-null capacity should display the number
    @Test
    public void bind_nonNullCapacity_displaysNumber() {
        Event event = new Event();
        event.setEventId("evt-with-cap");
        event.setTitle("Capacity Event");
        event.setCapacity(50);
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7)));

        List<Event> eventList = new ArrayList<>();
        eventList.add(event);
        EventAdapter adapter = new EventAdapter(eventList, e -> {
        });

        FrameLayout parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView tvCapacity = holder.itemView.findViewById(R.id.tvCapacityValue);
        assertEquals("50", tvCapacity.getText().toString());
    }

    // ---------------------------------------------------------------
    // Basic adapter tests
    // ---------------------------------------------------------------

    // US 03.04.01: Event adapter should report the correct number of events
    @Test
    public void testItemCount() {
        Event event = new Event();
        event.setEventId("event1");
        event.setTitle("Test Event");
        event.setCapacity(100);
        event.setStatus("open");
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L)));

        List<Event> eventList = new ArrayList<>();
        eventList.add(event);
        EventAdapter adapter = new EventAdapter(eventList, e -> {
        });

        assertEquals(1, adapter.getItemCount());
    }

    // US 03.04.01: Event list item should contain title, date, status, and capacity views
    @Test
    public void testOnCreateViewHolder() {
        EventAdapter adapter = new EventAdapter(new ArrayList<>(), e -> {
        });

        FrameLayout parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.tvEventTitle));
    }

    // US 03.04.01: Event list item should display title, capacity, and status correctly
    @Test
    public void testOnBindViewHolder() {
        Event event = new Event();
        event.setEventId("event1");
        event.setTitle("Test Event");
        event.setCapacity(100);
        event.setStatus("open");
        event.setScheduledDateTime(new Timestamp(new Date(System.currentTimeMillis() + 86400000L)));

        List<Event> eventList = new ArrayList<>();
        eventList.add(event);
        EventAdapter adapter = new EventAdapter(eventList, e -> {
        });

        FrameLayout parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView tvTitle = holder.itemView.findViewById(R.id.tvEventTitle);
        TextView tvCapacity = holder.itemView.findViewById(R.id.tvCapacityValue);
        TextView tvStatus = holder.itemView.findViewById(R.id.tvEventStatus);

        assertEquals("Test Event", tvTitle.getText().toString());
        assertEquals("100", tvCapacity.getText().toString());
        assertEquals("OPEN", tvStatus.getText().toString());
    }
}
