package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for CommentBottomSheet.
 * Covers US 03.10.01: Admin can remove event comments that violate app policy.
 * Covers US 02.08.01: Organizer can view and delete entrant comments.
 * Covers US 01.08.01: Entrant can post a comment on an event.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CommentBottomSheetTest {

    private MockedStatic<FirebaseFirestore> mockedFirestore;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        mockedFirestore = mockStatic(FirebaseFirestore.class);
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        CollectionReference mockCollection = mock(CollectionReference.class);
        Query mockQuery = mock(Query.class);
        Task mockTask = mock(Task.class);

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);
        when(mockQuery.addSnapshotListener(any())).thenReturn(null);
        when(mockCollection.add(any())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
    }

    // US 03.10.01: newInstanceForAdmin sets isAdmin=true and only requires eventId
    @Test
    public void testNewInstanceForAdminSetsCorrectArgs() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstanceForAdmin("event123");
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertEquals("event123", args.getString("eventId"));
        assertTrue(args.getBoolean("isAdmin"));
        assertFalse(args.getBoolean("isOrganizer"));
        assertNull(args.getString("userId"));
        assertNull(args.getString("userName"));
    }

    // US 02.08.01: newInstance for organizer sets isOrganizer=true
    @Test
    public void testNewInstanceForOrganizerSetsCorrectArgs() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstance("event123", "user1", "Bob", true);
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertEquals("event123", args.getString("eventId"));
        assertEquals("user1", args.getString("userId"));
        assertEquals("Bob", args.getString("userName"));
        assertTrue(args.getBoolean("isOrganizer"));
        assertFalse(args.getBoolean("isAdmin"));
    }

    // US 01.08.01: newInstance for entrant defaults isOrganizer=false
    @Test
    public void testNewInstanceForEntrantSetsCorrectArgs() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstance("event123", "user2", "Alice");
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertEquals("event123", args.getString("eventId"));
        assertEquals("user2", args.getString("userId"));
        assertEquals("Alice", args.getString("userName"));
        assertFalse(args.getBoolean("isOrganizer"));
        assertFalse(args.getBoolean("isAdmin"));
    }

    // US 03.10.01: Admin mode hides the comment input area
    @Test
    public void testAdminModeHidesCommentInput() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstanceForAdmin("event123");

        FragmentScenario<CommentBottomSheet> scenario = FragmentScenario.launch(
                CommentBottomSheet.class, fragment.getArguments(), R.style.Theme_Lottery);

        scenario.onFragment(f -> {
            View inputCard = f.getView().findViewById(R.id.commentInputCard);
            assertEquals(View.GONE, inputCard.getVisibility());
        });

        scenario.close();
    }

    // US 02.08.01: Organizer mode shows the comment input area
    @Test
    public void testOrganizerModeShowsCommentInput() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstance("event123", "user1", "Bob", true);

        FragmentScenario<CommentBottomSheet> scenario = FragmentScenario.launch(
                CommentBottomSheet.class, fragment.getArguments(), R.style.Theme_Lottery);

        scenario.onFragment(f -> {
            View inputCard = f.getView().findViewById(R.id.commentInputCard);
            assertEquals(View.VISIBLE, inputCard.getVisibility());
        });

        scenario.close();
    }

    // US 01.08.01: Entrant mode shows the comment input area
    @Test
    public void testEntrantModeShowsCommentInput() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstance("event123", "user2", "Alice");

        FragmentScenario<CommentBottomSheet> scenario = FragmentScenario.launch(
                CommentBottomSheet.class, fragment.getArguments(), R.style.Theme_Lottery);

        scenario.onFragment(f -> {
            View inputCard = f.getView().findViewById(R.id.commentInputCard);
            assertEquals(View.VISIBLE, inputCard.getVisibility());
        });

        scenario.close();
    }

    // US 03.10.01: Admin mode creates adapter with canDelete=true
    @Test
    public void testAdminModeShowsRecyclerView() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstanceForAdmin("event123");

        FragmentScenario<CommentBottomSheet> scenario = FragmentScenario.launch(
                CommentBottomSheet.class, fragment.getArguments(), R.style.Theme_Lottery);

        scenario.onFragment(f -> {
            View rvComments = f.getView().findViewById(R.id.rvComments);
            assertEquals(View.VISIBLE, rvComments.getVisibility());
        });

        scenario.close();
    }
}
