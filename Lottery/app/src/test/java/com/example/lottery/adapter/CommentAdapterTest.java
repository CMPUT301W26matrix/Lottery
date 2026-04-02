package com.example.lottery.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.lottery.R;
import com.example.lottery.model.Comment;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for CommentAdapter.
 * Covers US 03.10.01: Admin can remove event comments that violate app policy.
 * Also covers US 02.08.01: Organizer can view and delete entrant comments.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CommentAdapterTest {

    private Context context;
    private List<Comment> commentList;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Lottery);

        commentList = new ArrayList<>();

        Comment entrantComment = new Comment();
        entrantComment.setCommentId("c1");
        entrantComment.setAuthorName("Alice");
        entrantComment.setAuthorRole("entrant");
        entrantComment.setContent("Great event!");
        entrantComment.setCreatedAt(new Timestamp(new Date()));
        commentList.add(entrantComment);

        Comment organizerComment = new Comment();
        organizerComment.setCommentId("c2");
        organizerComment.setAuthorName("Bob");
        organizerComment.setAuthorRole("organizer");
        organizerComment.setContent("Thanks for joining!");
        organizerComment.setCreatedAt(new Timestamp(new Date()));
        commentList.add(organizerComment);

        Comment adminComment = new Comment();
        adminComment.setCommentId("c3");
        adminComment.setAuthorName("Charlie");
        adminComment.setAuthorRole("admin");
        adminComment.setContent("Policy reminder.");
        adminComment.setCreatedAt(new Timestamp(new Date()));
        commentList.add(adminComment);
    }

    // US 01.08.02 / US 02.08.01: Comments should be listed in the adapter
    @Test
    public void testItemCount() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        assertEquals(3, adapter.getItemCount());
    }

    // US 01.08.02 / US 02.08.01: Empty comment list should return zero
    @Test
    public void testItemCountEmpty() {
        CommentAdapter adapter = new CommentAdapter(new ArrayList<>(), false, "event1");
        assertEquals(0, adapter.getItemCount());
    }

    // US 01.08.02 / US 02.08.01: ViewHolder should contain all required views
    @Test
    public void testOnCreateViewHolder() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        assertNotNull(holder);
        assertNotNull(holder.itemView.findViewById(R.id.tvAuthorName));
        assertNotNull(holder.itemView.findViewById(R.id.tvCommentContent));
        assertNotNull(holder.itemView.findViewById(R.id.tvCommentTime));
        assertNotNull(holder.itemView.findViewById(R.id.btnDeleteComment));
    }

    // US 01.08.02: Entrant comments display plain author name
    @Test
    public void testEntrantRoleDisplaysPlainName() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvAuthor = holder.itemView.findViewById(R.id.tvAuthorName);
        assertEquals("Alice", tvAuthor.getText().toString());
    }

    // US 02.08.01: Organizer comments display "(Organizer)" marker
    @Test
    public void testOrganizerRoleDisplaysMarker() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        TextView tvAuthor = holder.itemView.findViewById(R.id.tvAuthorName);
        assertEquals("Bob (Organizer)", tvAuthor.getText().toString());
    }

    // US 03.10.01: Admin comments should display "(Admin)" marker
    @Test
    public void testAdminRoleDisplaysMarker() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 2);

        TextView tvAuthor = holder.itemView.findViewById(R.id.tvAuthorName);
        assertEquals("Charlie (Admin)", tvAuthor.getText().toString());
    }

    // US 01.08.02: Null author name falls back to "Anonymous"
    @Test
    public void testNullAuthorNameDisplaysAnonymous() {
        Comment nullNameComment = new Comment();
        nullNameComment.setCommentId("c4");
        nullNameComment.setAuthorRole("entrant");
        nullNameComment.setContent("Anonymous post");
        nullNameComment.setCreatedAt(new Timestamp(new Date()));

        List<Comment> list = new ArrayList<>();
        list.add(nullNameComment);

        CommentAdapter adapter = new CommentAdapter(list, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvAuthor = holder.itemView.findViewById(R.id.tvAuthorName);
        assertEquals("Anonymous", tvAuthor.getText().toString());
    }

    // US 03.10.01: Admin should see delete button to remove comments
    // US 02.08.01: Organizer should see delete button to remove comments
    @Test
    public void testDeleteButtonVisibleWhenCanDelete() {
        CommentAdapter adapter = new CommentAdapter(commentList, true, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ImageButton btnDelete = holder.itemView.findViewById(R.id.btnDeleteComment);
        assertEquals(View.VISIBLE, btnDelete.getVisibility());
    }

    // US 01.08.02: Entrant should not see delete button
    @Test
    public void testDeleteButtonHiddenWhenCannotDelete() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ImageButton btnDelete = holder.itemView.findViewById(R.id.btnDeleteComment);
        assertEquals(View.GONE, btnDelete.getVisibility());
    }

    // US 01.08.02: Comment content should be displayed correctly
    @Test
    public void testCommentContentDisplayed() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvContent = holder.itemView.findViewById(R.id.tvCommentContent);
        assertEquals("Great event!", tvContent.getText().toString());
    }

    // US 01.08.02: Comment timestamp should be formatted when present
    @Test
    public void testTimestampDisplayedWhenPresent() {
        CommentAdapter adapter = new CommentAdapter(commentList, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTime = holder.itemView.findViewById(R.id.tvCommentTime);
        assertNotNull(tvTime.getText());
        // Timestamp is formatted, so it should not be empty
        assertFalse(tvTime.getText().toString().isEmpty());
    }

    // US 01.08.02: Comment timestamp should be empty when null
    @Test
    public void testTimestampEmptyWhenNull() {
        Comment noTimeComment = new Comment();
        noTimeComment.setCommentId("c5");
        noTimeComment.setAuthorName("Dave");
        noTimeComment.setAuthorRole("entrant");
        noTimeComment.setContent("No time");

        List<Comment> list = new ArrayList<>();
        list.add(noTimeComment);

        CommentAdapter adapter = new CommentAdapter(list, false, "event1");
        FrameLayout parent = new FrameLayout(context);
        CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        TextView tvTime = holder.itemView.findViewById(R.id.tvCommentTime);
        assertEquals("", tvTime.getText().toString());
    }

    // US 03.10.01: Deleted comment should have isDeleted() return true
    @Test
    public void testDeletedCommentFlagIsTrue() {
        Comment comment = new Comment();
        comment.setDeleted(true);
        assertTrue("Deleted comment should return true for isDeleted()", comment.isDeleted());
    }

    // US 03.10.01: Non-deleted comment should have isDeleted() return false
    @Test
    public void testNonDeletedCommentFlagIsFalse() {
        Comment comment = new Comment();
        comment.setDeleted(false);
        assertFalse("Non-deleted comment should return false for isDeleted()", comment.isDeleted());
    }

    // US 03.10.01: Comment default deleted state should be false
    @Test
    public void testCommentDefaultDeletedIsFalse() {
        Comment comment = new Comment();
        assertFalse("New comment should default to not deleted", comment.isDeleted());
    }
}
