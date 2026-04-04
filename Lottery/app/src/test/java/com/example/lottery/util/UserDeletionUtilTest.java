package com.example.lottery.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
import org.mockito.MockedStatic;

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
    private Task<QuerySnapshot> mockCoOrgTask;
    private Task<QuerySnapshot> mockWaitlistTask;
    private Task<QuerySnapshot> mockInboxTask;
    private Task<Void> mockCommitTask;
    private Task<List<Task<?>>> mockAllTask;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockDb = mock(FirebaseFirestore.class);
        mockQuery = mock(Query.class);
        mockBatch = mock(WriteBatch.class);
        mockCoOrgTask = mock(Task.class);
        mockWaitlistTask = mock(Task.class);
        mockInboxTask = mock(Task.class);
        mockCommitTask = mock(Task.class);
        mockAllTask = mock(Task.class);

        // collectionGroup queries return different tasks per call
        when(mockDb.collectionGroup(anyString())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockCoOrgTask, mockWaitlistTask);

        // collection (inbox) query
        CollectionReference mockCollectionRef = mock(CollectionReference.class);
        when(mockDb.collection(anyString())).thenReturn(mockCollectionRef);
        when(mockCollectionRef.get()).thenReturn(mockInboxTask);

        when(mockDb.batch()).thenReturn(mockBatch);

        // Stub batch commit
        when(mockBatch.commit()).thenReturn(mockCommitTask);
        when(mockCommitTask.addOnSuccessListener(any())).thenReturn(mockCommitTask);
        when(mockCommitTask.addOnFailureListener(any())).thenReturn(mockCommitTask);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_withDocuments_batchDeletesAndCallsOnDone() {
        // Set up co-organizer query result
        QuerySnapshot coOrgSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref1 = mock(DocumentReference.class);
        when(doc1.getReference()).thenReturn(ref1);
        when(coOrgSnapshot.iterator()).thenReturn(Collections.singletonList(doc1).iterator());
        when(mockCoOrgTask.isSuccessful()).thenReturn(true);
        when(mockCoOrgTask.getResult()).thenReturn(coOrgSnapshot);

        // Set up waitlist query result
        QuerySnapshot waitlistSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        when(doc2.getReference()).thenReturn(ref2);
        when(waitlistSnapshot.iterator()).thenReturn(Collections.singletonList(doc2).iterator());
        when(mockWaitlistTask.isSuccessful()).thenReturn(true);
        when(mockWaitlistTask.getResult()).thenReturn(waitlistSnapshot);

        // Set up inbox query result (empty)
        QuerySnapshot inboxSnapshot = mock(QuerySnapshot.class);
        when(inboxSnapshot.iterator()).thenReturn(Collections.emptyIterator());
        when(mockInboxTask.isSuccessful()).thenReturn(true);
        when(mockInboxTask.getResult()).thenReturn(inboxSnapshot);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        try (MockedStatic<Tasks> mockedTasks = mockStatic(Tasks.class)) {
            when(mockAllTask.addOnCompleteListener(any())).thenAnswer(invocation -> {
                OnCompleteListener<List<Task<?>>> listener = invocation.getArgument(0);
                listener.onComplete(mockAllTask);
                return mockAllTask;
            });
            mockedTasks.when(() -> Tasks.whenAllComplete(any(List.class)))
                    .thenReturn(mockAllTask);

            UserDeletionUtil.cleanUpUserRecords(mockDb, "user123", () -> callbackInvoked.set(true));
        }

        // Verify batch delete operations
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
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        when(emptySnapshot.iterator()).thenReturn(Collections.emptyIterator());

        when(mockCoOrgTask.isSuccessful()).thenReturn(true);
        when(mockCoOrgTask.getResult()).thenReturn(emptySnapshot);
        when(mockWaitlistTask.isSuccessful()).thenReturn(true);
        when(mockWaitlistTask.getResult()).thenReturn(emptySnapshot);
        when(mockInboxTask.isSuccessful()).thenReturn(true);
        when(mockInboxTask.getResult()).thenReturn(emptySnapshot);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        try (MockedStatic<Tasks> mockedTasks = mockStatic(Tasks.class)) {
            when(mockAllTask.addOnCompleteListener(any())).thenAnswer(invocation -> {
                OnCompleteListener<List<Task<?>>> listener = invocation.getArgument(0);
                listener.onComplete(mockAllTask);
                return mockAllTask;
            });
            mockedTasks.when(() -> Tasks.whenAllComplete(any(List.class)))
                    .thenReturn(mockAllTask);

            UserDeletionUtil.cleanUpUserRecords(mockDb, "user456", () -> callbackInvoked.set(true));
        }

        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked even with no documents", callbackInvoked.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cleanUp_queryFails_callsOnDoneAnyway() {
        when(mockCoOrgTask.isSuccessful()).thenReturn(false);
        when(mockWaitlistTask.isSuccessful()).thenReturn(false);
        when(mockInboxTask.isSuccessful()).thenReturn(false);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        try (MockedStatic<Tasks> mockedTasks = mockStatic(Tasks.class)) {
            when(mockAllTask.addOnCompleteListener(any())).thenAnswer(invocation -> {
                OnCompleteListener<List<Task<?>>> listener = invocation.getArgument(0);
                listener.onComplete(mockAllTask);
                return mockAllTask;
            });
            mockedTasks.when(() -> Tasks.whenAllComplete(any(List.class)))
                    .thenReturn(mockAllTask);

            UserDeletionUtil.cleanUpUserRecords(mockDb, "user789", () -> callbackInvoked.set(true));
        }

        // Even with failed queries, batch commit is still called (empty batch)
        ArgumentCaptor<OnSuccessListener<Void>> commitCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);
        verify(mockCommitTask).addOnSuccessListener(commitCaptor.capture());
        commitCaptor.getValue().onSuccess(null);

        assertTrue("Callback should be invoked even when queries fail", callbackInvoked.get());
    }
}
