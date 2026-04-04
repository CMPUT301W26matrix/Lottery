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

        // Trigger first query success (Co-Organizers) with one document
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor1 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor1.capture());

        QuerySnapshot mockSnapshot1 = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref1 = mock(DocumentReference.class);
        when(doc1.getReference()).thenReturn(ref1);
        when(mockSnapshot1.iterator()).thenReturn(Collections.singletonList(doc1).iterator());
        captor1.getAllValues().get(0).onSuccess(mockSnapshot1);

        // Trigger second query success (Waiting List) with one document
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

        // Verify batch operations for co-organizer and waitlist docs
        verify(mockBatch).delete(ref1);
        verify(mockBatch).delete(ref2);

        // Trigger first batch commit success → this calls deleteInbox()
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask, atLeastOnce()).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        // Trigger inbox query success (empty inbox)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> inboxCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(inboxCaptor.capture());
        QuerySnapshot emptyInbox = mock(QuerySnapshot.class);
        when(emptyInbox.isEmpty()).thenReturn(true);
        List<OnSuccessListener<QuerySnapshot>> inboxListeners = inboxCaptor.getAllValues();
        inboxListeners.get(inboxListeners.size() - 1).onSuccess(emptyInbox);

        assertTrue("Callback should be invoked after successful cleanup", callbackInvoked.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withNoDocuments_callsOnDone() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        UserDeletionUtil.cleanUpUserRecords(mockDb, "user456", () -> callbackInvoked.set(true));

        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        when(emptySnapshot.iterator()).thenReturn(Collections.emptyIterator());

        // Trigger both query successes (empty)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor.capture());
        captor.getAllValues().get(0).onSuccess(emptySnapshot);

        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor2 =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(captor2.capture());
        captor2.getAllValues().get(captor2.getAllValues().size() - 1).onSuccess(emptySnapshot);

        // Trigger batch commit success → calls deleteInbox()
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask, atLeastOnce()).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        // Trigger inbox query success (empty inbox)
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> inboxCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnSuccessListener(inboxCaptor.capture());
        QuerySnapshot emptyInbox = mock(QuerySnapshot.class);
        when(emptyInbox.isEmpty()).thenReturn(true);
        inboxCaptor.getAllValues().get(inboxCaptor.getAllValues().size() - 1).onSuccess(emptyInbox);

        assertTrue("Callback should be invoked even with no documents", callbackInvoked.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_queryFails_callsOnDoneAnyway() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        UserDeletionUtil.cleanUpUserRecords(mockDb, "user789", () -> callbackInvoked.set(true));

        // Both parallel queries fail
        ArgumentCaptor<OnFailureListener> failureCaptor =
                ArgumentCaptor.forClass(OnFailureListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnFailureListener(failureCaptor.capture());
        for (OnFailureListener listener : failureCaptor.getAllValues()) {
            listener.onFailure(new Exception("Network error"));
        }

        // Trigger batch commit success (empty batch) → calls deleteInbox()
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask, atLeastOnce()).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        // Trigger inbox query failure too
        ArgumentCaptor<OnFailureListener> inboxFailCaptor =
                ArgumentCaptor.forClass(OnFailureListener.class);
        verify(mockQueryTask, atLeastOnce()).addOnFailureListener(inboxFailCaptor.capture());
        List<OnFailureListener> allFailListeners = inboxFailCaptor.getAllValues();
        allFailListeners.get(allFailListeners.size() - 1).onFailure(new Exception("Network error"));

        assertTrue("Callback should be invoked even when query fails", callbackInvoked.get());
    }
}
