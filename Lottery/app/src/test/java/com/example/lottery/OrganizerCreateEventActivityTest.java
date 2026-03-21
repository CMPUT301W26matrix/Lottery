package com.example.lottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Test;

/**
 * Unit tests for organizer ownership behavior in {@link OrganizerCreateEventActivity}.
 */
public class OrganizerCreateEventActivityTest {

    @Test
    public void organizerIdForUserReturnsUidWhenUserExists() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("organizer-123");

        assertEquals("organizer-123", OrganizerCreateEventActivity.organizerIdForUser(user));
    }

    @Test
    public void organizerIdForUserReturnsNullWhenUserMissing() {
        assertNull(OrganizerCreateEventActivity.organizerIdForUser(null));
    }

    @Test
    public void organizerIdForUserReturnsNullWhenUidEmpty() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("");

        assertNull(OrganizerCreateEventActivity.organizerIdForUser(user));
    }
}
