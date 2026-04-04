package com.example.lottery.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link UserDeletionUtil}.
 *
 * <p>Verifies that user record cleanup correctly interacts with Firestore
 * and always invokes the completion callback regardless of success or failure.</p>
 */
public class UserDeletionUtilTest {

    private FirebaseFirestore mockDb;
    private Query mockQuery;
    private WriteBatch mockBatch;
    private Task<QuerySnapshot> mockQueryTask;
    private Task<Void> mockCommitTask;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockDb = mock(FirebaseFirestore.class);
        mockQuery = mock(Query.class);
        mockBatch = mock(WriteBatch.class);
        mockQueryTask = mock(Task.class);
        mockCommitTask = mock(Task.class);

        when(mockDb.collectionGroup(anyString())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockDb.batch()).thenReturn(mockBatch);

        // Stub db.collection() for inbox cleanup step
        CollectionReference mockCollectionRef = mock(CollectionReference.class);
        when(mockDb.collection(anyString())).thenReturn(mockCollectionRef);
        when(mockCollectionRef.get()).thenReturn(mockQueryTask);

        // Stub query task to avoid NPE when listeners are added
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Stub batch commit task to avoid NPE when listeners are added
        when(mockBatch.commit()).thenReturn(mockCommitTask);
        when(mockCommitTask.addOnSuccessListener(any())).thenReturn(mockCommitTask);
        when(mockCommitTask.addOnFailureListener(any())).thenReturn(mockCommitTask);
        when(mockCommitTask.addOnCompleteListener(any())).thenReturn(mockCommitTask);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withDocuments_batchDeletesAndCallsOnDone() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        UserDeletionUtil.cleanUpUserRecords(mockDb, "user123", () -> callbackInvoked.set(true));

        // Capture and trigger first query success (Co-Organizers)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor1 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor1.capture());

        QuerySnapshot mockSnapshot1 = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref1 = mock(DocumentReference.class);
        when(doc1.getReference()).thenReturn(ref1);
        when(mockSnapshot1.iterator()).thenReturn(Collections.singletonList(doc1).iterator());
        captor1.getAllValues().get(0).onSuccess(mockSnapshot1);

        // Capture and trigger second query success (Waiting List)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor2 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor2.capture());

        QuerySnapshot mockSnapshot2 = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        when(doc2.getReference()).thenReturn(ref2);
        when(mockSnapshot2.iterator()).thenReturn(Collections.singletonList(doc2).iterator());
        List<OnSuccessListener<QuerySnapshot>> listeners2 = captor2.getAllValues();
        listeners2.get(listeners2.size() - 1).onSuccess(mockSnapshot2);

        // Capture and trigger third query success (Inbox - empty)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor3 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor3.capture());
        QuerySnapshot emptyInbox = mock(QuerySnapshot.class);
        when(emptyInbox.iterator()).thenReturn(Collections.emptyIterator());
        List<OnSuccessListener<QuerySnapshot>> listeners3 = captor3.getAllValues();
        listeners3.get(listeners3.size() - 1).onSuccess(emptyInbox);

        // Verify batch operations
        verify(mockBatch).delete(ref1);
        verify(mockBatch).delete(ref2);

        // Trigger batch commit success
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked after successful cleanup", callbackInvoked.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withNoDocuments_callsOnDone() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        UserDeletionUtil.cleanUpUserRecords(mockDb, "user456", () -> callbackInvoked.set(true));

        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        when(emptySnapshot.iterator()).thenReturn(Collections.emptyIterator());

        // Trigger first query success (empty)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor1 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor1.capture());
        captor1.getAllValues().get(0).onSuccess(emptySnapshot);

        // Trigger second query success (empty)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor2 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor2.capture());
        List<OnSuccessListener<QuerySnapshot>> listeners2 = captor2.getAllValues();
        listeners2.get(listeners2.size() - 1).onSuccess(emptySnapshot);

        // Trigger third query success (inbox - empty)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor3 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor3.capture());
        List<OnSuccessListener<QuerySnapshot>> listeners3 = captor3.getAllValues();
        listeners3.get(listeners3.size() - 1).onSuccess(emptySnapshot);

        // Trigger commit success
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked even with no documents", callbackInvoked.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_queryFails_callsOnDoneAnyway() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        UserDeletionUtil.cleanUpUserRecords(mockDb, "user789", () -> callbackInvoked.set(true));

        ArgumentCaptor<OnFailureListener> failureCaptor =
                ArgumentCaptor.forClass(OnFailureListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnFailureListener(failureCaptor.capture());
        failureCaptor.getValue().onFailure(new Exception("Network error"));

        assertTrue("Callback should be invoked even when query fails", callbackInvoked.get());
    }
}
