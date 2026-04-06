package com.example.lottery.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for the {@link Comment} model class.
 *
 * Covers US 01.08.01 (entrant posts a comment) and
 * US 02.08.01 / US 02.08.02 (organizer views/deletes/posts comments).
 */
public class CommentTest {

    // US 01.08.01: Default constructor creates a valid object for Firestore deserialization
    @Test
    public void defaultConstructor_createsNonNullObject() {
        Comment c = new Comment();
        assertNotNull(c);
        assertNull(c.getCommentId());
        assertNull(c.getEventId());
        assertNull(c.getAuthorId());
        assertNull(c.getAuthorName());
        assertNull(c.getAuthorRole());
        assertNull(c.getContent());
        assertNull(c.getCreatedAt());
        assertNull(c.getUpdatedAt());
        assertFalse(c.isDeleted());
    }

    // US 01.08.01: Full constructor populates all fields
    @Test
    public void fullConstructor_setsAllFields() {
        Timestamp now = Timestamp.now();
        Comment c = new Comment("c1", "spring_bbq_2025", "alice_nguyen",
                "Alice Nguyen", "entrant",
                "Looking forward to the BBQ this weekend!", now, now, false);

        assertEquals("c1", c.getCommentId());
        assertEquals("spring_bbq_2025", c.getEventId());
        assertEquals("alice_nguyen", c.getAuthorId());
        assertEquals("Alice Nguyen", c.getAuthorName());
        assertEquals("entrant", c.getAuthorRole());
        assertEquals("Looking forward to the BBQ this weekend!", c.getContent());
        assertEquals(now, c.getCreatedAt());
        assertEquals(now, c.getUpdatedAt());
        assertFalse(c.isDeleted());
    }

    // US 01.08.01: Full constructor with deleted=true
    @Test
    public void fullConstructor_deletedTrue() {
        Timestamp now = Timestamp.now();
        Comment c = new Comment("c2", "yoga_class_fall", "coach_bob",
                "Bob Martinez", "organizer",
                "This comment was removed for violating guidelines.", now, now, true);
        assertTrue(c.isDeleted());
    }

    // US 01.08.01: commentId getter/setter
    @Test
    public void setCommentId_and_getCommentId() {
        Comment c = new Comment();
        c.setCommentId("comment-42");
        assertEquals("comment-42", c.getCommentId());
    }

    // US 01.08.01: eventId getter/setter
    @Test
    public void setEventId_and_getEventId() {
        Comment c = new Comment();
        c.setEventId("event-99");
        assertEquals("event-99", c.getEventId());
    }

    // US 01.08.01: authorId getter/setter
    @Test
    public void setAuthorId_and_getAuthorId() {
        Comment c = new Comment();
        c.setAuthorId("user-7");
        assertEquals("user-7", c.getAuthorId());
    }

    // US 01.08.01: authorName getter/setter
    @Test
    public void setAuthorName_and_getAuthorName() {
        Comment c = new Comment();
        c.setAuthorName("Carol");
        assertEquals("Carol", c.getAuthorName());
    }

    // US 02.08.02: authorRole getter/setter — organizer can comment with organizer role
    @Test
    public void setAuthorRole_and_getAuthorRole() {
        Comment c = new Comment();
        c.setAuthorRole("organizer");
        assertEquals("organizer", c.getAuthorRole());
    }

    // US 01.08.01: content getter/setter
    @Test
    public void setContent_and_getContent() {
        Comment c = new Comment();
        c.setContent("Can I bring my kids to this event?");
        assertEquals("Can I bring my kids to this event?", c.getContent());
    }

    // US 01.08.01: createdAt getter/setter
    @Test
    public void setCreatedAt_and_getCreatedAt() {
        Comment c = new Comment();
        Timestamp ts = Timestamp.now();
        c.setCreatedAt(ts);
        assertEquals(ts, c.getCreatedAt());
    }

    // US 01.08.01: updatedAt getter/setter
    @Test
    public void setUpdatedAt_and_getUpdatedAt() {
        Comment c = new Comment();
        Timestamp ts = Timestamp.now();
        c.setUpdatedAt(ts);
        assertEquals(ts, c.getUpdatedAt());
    }

    // US 02.08.01: deleted flag can be toggled for soft-delete
    @Test
    public void setDeleted_and_isDeleted() {
        Comment c = new Comment();
        assertFalse(c.isDeleted());
        c.setDeleted(true);
        assertTrue(c.isDeleted());
        c.setDeleted(false);
        assertFalse(c.isDeleted());
    }
}
