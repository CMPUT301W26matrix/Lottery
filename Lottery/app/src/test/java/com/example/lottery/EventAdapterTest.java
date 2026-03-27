package com.example.lottery;

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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EventAdapterTest {

    private EventAdapter adapter;
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

        List<Event> eventList = new ArrayList<>();
        Event event = new Event();
        event.setEventId("event1");
        event.setTitle("Test Event");
        event.setCapacity(100);
        event.setStatus("open");
        event.setScheduledDateTime(new Timestamp(new java.util.Date(System.currentTimeMillis() + 86400000)));
        eventList.add(event);

        adapter = new EventAdapter(eventList, event1 -> {
        });
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
    }

    @Test
    public void testItemCount() {
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void testOnCreateViewHolder() {
        FrameLayout parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.tvEventTitle));
    }

    @Test
    public void testOnBindViewHolder() {
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

    @Test
    public void testResolveDisplayStatus_open() {
        Event event = new Event();
        event.setScheduledDateTime(new Timestamp(new java.util.Date(System.currentTimeMillis() + 86400000)));
        assertEquals("open", EventAdapter.resolveDisplayStatus(event));
    }

    @Test
    public void testResolveDisplayStatus_closed() {
        Event event = new Event();
        event.setScheduledDateTime(new Timestamp(new java.util.Date(System.currentTimeMillis() - 86400000)));
        assertEquals("closed", EventAdapter.resolveDisplayStatus(event));
    }

    @Test
    public void testResolveDisplayStatus_pending() {
        Event event = new Event();
        event.setRegistrationDeadline(new Timestamp(new java.util.Date(System.currentTimeMillis() - 86400000)));
        event.setDrawDate(new Timestamp(new java.util.Date(System.currentTimeMillis() + 86400000)));
        assertEquals("pending", EventAdapter.resolveDisplayStatus(event));
    }

    @Test
    public void testResolveDisplayStatus_null() {
        assertEquals("closed", EventAdapter.resolveDisplayStatus(null));
    }
}
