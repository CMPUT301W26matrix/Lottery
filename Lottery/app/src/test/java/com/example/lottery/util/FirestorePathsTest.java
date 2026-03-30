package com.example.lottery.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for FirestorePaths utility class.
 */
public class FirestorePathsTest {

    @Test
    public void testUserDocPath() {
        assertEquals("users/uid123", FirestorePaths.userDoc("uid123"));
    }

    @Test
    public void testUserInboxPath() {
        assertEquals("users/uid123/inbox", FirestorePaths.userInbox("uid123"));
    }

    @Test
    public void testEventDocPath() {
        assertEquals("events/event456", FirestorePaths.eventDoc("event456"));
    }

    @Test
    public void testEventWaitingListPath() {
        assertEquals("events/event456/waitingList", FirestorePaths.eventWaitingList("event456"));
    }
}
