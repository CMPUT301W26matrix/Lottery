package com.example.lottery.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link UserDeletionUtil}.
 *
 * <p>Verifies that co-organizer cleanup correctly interacts with Firestore
 * and always invokes the completion callback regardless of success or failure.</p>
 */
public class UserDeletionUtilTest {

    private FirebaseFirestore mockDb;
    private Query mockQuery;
    private WriteBatch mockBatch;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockDb = mock(FirebaseFirestore.class);
        mockQuery = mock(Query.class);
        mockBatch = mock(WriteBatch.class);

        when(mockDb.collectionGroup(anyString())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockDb.batch()).thenReturn(mockBatch);
    }

    // US 03.07.01: Removing an organizer should clean up all co-organizer records via batch delete
    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withDocuments_batchDeletesAndCallsOnDone() {
        // Arrange: query returns two co-organizer documents
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(mockQuery.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            // Will be triggered manually below
            return queryTask;
        });
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        // Capture the success listener so we can invoke it
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> querySuccessCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        // Act
        UserDeletionUtil.cleanUpCoOrganizerRecords(mockDb, "user123", () -> callbackInvoked.set(true));

        // Verify query was made on the correct collection group
        verify(mockDb).collectionGroup(FirestorePaths.CO_ORGANIZERS);
        verify(mockQuery).whereEqualTo("userId", "user123");
        verify(mockQuery).get();

        // Simulate query success with 2 documents
        verify(queryTask).addOnSuccessListener(querySuccessCaptor.capture());

        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        when(doc1.getReference()).thenReturn(ref1);
        when(doc2.getReference()).thenReturn(ref2);
        when(mockSnapshot.iterator()).thenReturn(Arrays.asList(doc1, doc2).iterator());

        Task<Void> commitTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> commitSuccessCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockBatch.commit()).thenReturn(commitTask);
        when(commitTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            return commitTask;
        });
        when(commitTask.addOnFailureListener(any())).thenReturn(commitTask);

        querySuccessCaptor.getValue().onSuccess(mockSnapshot);

        // Verify batch operations
        verify(mockBatch).delete(ref1);
        verify(mockBatch).delete(ref2);
        verify(mockBatch).commit();

        // Simulate commit success
        verify(commitTask).addOnSuccessListener(commitSuccessCaptor.capture());
        commitSuccessCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked after successful commit", callbackInvoked.get());
    }

    // US 03.07.01: Cleanup should complete even when user has no co-organizer records
    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withNoDocuments_commitsEmptyBatchAndCallsOnDone() {
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(mockQuery.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> querySuccessCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        UserDeletionUtil.cleanUpCoOrganizerRecords(mockDb, "user456", () -> callbackInvoked.set(true));

        verify(queryTask).addOnSuccessListener(querySuccessCaptor.capture());

        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.iterator()).thenReturn(Collections.emptyIterator());

        Task<Void> commitTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> commitSuccessCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockBatch.commit()).thenReturn(commitTask);
        when(commitTask.addOnSuccessListener(any())).thenReturn(commitTask);
        when(commitTask.addOnFailureListener(any())).thenReturn(commitTask);

        querySuccessCaptor.getValue().onSuccess(mockSnapshot);

        // No delete calls on batch
        verify(mockBatch, never()).delete(any(DocumentReference.class));
        verify(mockBatch).commit();

        verify(commitTask).addOnSuccessListener(commitSuccessCaptor.capture());
        commitSuccessCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked even with no documents", callbackInvoked.get());
    }

    // US 03.02.01: Profile deletion should proceed even if co-organizer query fails
    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_queryFails_callsOnDoneAnyway() {
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(mockQuery.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        ArgumentCaptor<OnFailureListener> failureCaptor =
                ArgumentCaptor.forClass(OnFailureListener.class);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        UserDeletionUtil.cleanUpCoOrganizerRecords(mockDb, "user789", () -> callbackInvoked.set(true));

        verify(queryTask).addOnFailureListener(failureCaptor.capture());
        failureCaptor.getValue().onFailure(new Exception("Missing index"));

        assertTrue("Callback should be invoked even when query fails", callbackInvoked.get());
    }

    // US 03.02.01: Profile deletion should proceed even if batch commit fails
    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_commitFails_callsOnDoneAnyway() {
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(mockQuery.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> querySuccessCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        UserDeletionUtil.cleanUpCoOrganizerRecords(mockDb, "user000", () -> callbackInvoked.set(true));

        verify(queryTask).addOnSuccessListener(querySuccessCaptor.capture());

        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.iterator()).thenReturn(Collections.emptyIterator());

        Task<Void> commitTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> commitFailureCaptor =
                ArgumentCaptor.forClass(OnFailureListener.class);
        when(mockBatch.commit()).thenReturn(commitTask);
        when(commitTask.addOnSuccessListener(any())).thenReturn(commitTask);
        when(commitTask.addOnFailureListener(any())).thenReturn(commitTask);

        querySuccessCaptor.getValue().onSuccess(mockSnapshot);

        verify(commitTask).addOnFailureListener(commitFailureCaptor.capture());
        commitFailureCaptor.getValue().onFailure(new Exception("Commit failed"));

        assertTrue("Callback should be invoked even when commit fails", callbackInvoked.get());
    }
}
