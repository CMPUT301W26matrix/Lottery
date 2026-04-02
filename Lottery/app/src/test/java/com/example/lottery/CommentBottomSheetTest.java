package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import com.example.lottery.fragment.CommentBottomSheet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for CommentBottomSheet factory methods and argument passing.
 * Covers US 03.10.01: Admin can remove event comments that violate app policy.
 * Covers US 02.08.01: Organizer can view and delete entrant comments.
 * Covers US 01.08.01: Entrant can post a comment on an event.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CommentBottomSheetTest {

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

    // US 03.10.01: Admin args should not include userId or userName
    @Test
    public void testAdminDoesNotCarryUserIdentity() {
        CommentBottomSheet fragment = CommentBottomSheet.newInstanceForAdmin("event456");
        Bundle args = fragment.getArguments();

        assertNotNull(args);
        assertNull(args.getString("userId"));
        assertNull(args.getString("userName"));
        assertTrue(args.getBoolean("isAdmin"));
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

    // US 01.08.01: Convenience overload delegates to full newInstance with isOrganizer=false
    @Test
    public void testEntrantConvenienceOverloadDefaultsOrganizerFalse() {
        CommentBottomSheet fromConvenience = CommentBottomSheet.newInstance("e1", "u1", "Name");
        CommentBottomSheet fromFull = CommentBottomSheet.newInstance("e1", "u1", "Name", false);

        Bundle a = fromConvenience.getArguments();
        Bundle b = fromFull.getArguments();

        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.getString("eventId"), b.getString("eventId"));
        assertEquals(a.getString("userId"), b.getString("userId"));
        assertEquals(a.getString("userName"), b.getString("userName"));
        assertEquals(a.getBoolean("isOrganizer"), b.getBoolean("isOrganizer"));
    }

    // US 03.10.01 / US 02.08.01: Admin and organizer args are mutually exclusive
    @Test
    public void testAdminAndOrganizerAreMutuallyExclusive() {
        CommentBottomSheet admin = CommentBottomSheet.newInstanceForAdmin("e1");
        CommentBottomSheet organizer = CommentBottomSheet.newInstance("e1", "u1", "Bob", true);

        assertTrue(admin.getArguments().getBoolean("isAdmin"));
        assertFalse(admin.getArguments().getBoolean("isOrganizer"));

        assertFalse(organizer.getArguments().getBoolean("isAdmin"));
        assertTrue(organizer.getArguments().getBoolean("isOrganizer"));
    }
}
