package com.example.lottery;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Comment;
import com.example.lottery.util.FirestorePaths;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CommentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_IS_ORGANIZER = "isOrganizer";
    private static final String ARG_IS_ADMIN = "isAdmin";

    private String eventId;
    private String userId;
    private String userName;
    private boolean isOrganizer;
    private boolean isAdmin;

    private FirebaseFirestore db;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private EditText etComment;

    public static CommentBottomSheet newInstance(String eventId, String userId, String userName) {
        return newInstance(eventId, userId, userName, false);
    }

    public static CommentBottomSheet newInstance(String eventId, String userId, String userName, boolean isOrganizer) {
        CommentBottomSheet fragment = new CommentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        args.putBoolean(ARG_IS_ORGANIZER, isOrganizer);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommentBottomSheet newInstanceForAdmin(String eventId) {
        CommentBottomSheet fragment = new CommentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putBoolean(ARG_IS_ADMIN, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            userId = getArguments().getString(ARG_USER_ID);
            userName = getArguments().getString(ARG_USER_NAME);
            isOrganizer = getArguments().getBoolean(ARG_IS_ORGANIZER);
            isAdmin = getArguments().getBoolean(ARG_IS_ADMIN);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_comment_bottom_sheet, container, false);

        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        etComment = view.findViewById(R.id.etComment);
        ImageButton btnPostComment = view.findViewById(R.id.btnPostComment);

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList, isOrganizer || isAdmin, eventId);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(adapter);

        if (isAdmin) {
            view.findViewById(R.id.commentInputCard).setVisibility(View.GONE);
        } else {
            btnPostComment.setOnClickListener(v -> postComment());
        }

        loadComments();

        return view;
    }

    private void loadComments() {
        db.collection(FirestorePaths.eventComments(eventId))
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Comment comment = dc.getDocument().toObject(Comment.class);
                        comment.setCommentId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                if (!containsComment(comment.getCommentId())) {
                                    commentList.add(comment);
                                }
                                break;
                            case MODIFIED:
                                updateCommentInList(comment);
                                break;
                            case REMOVED:
                                removeCommentFromList(comment);
                                break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private boolean containsComment(String commentId) {
        for (Comment c : commentList) {
            if (c.getCommentId().equals(commentId)) return true;
        }
        return false;
    }

    private void updateCommentInList(Comment updatedComment) {
        for (int i = 0; i < commentList.size(); i++) {
            if (commentList.get(i).getCommentId().equals(updatedComment.getCommentId())) {
                commentList.set(i, updatedComment);
                return;
            }
        }
    }

    private void removeCommentFromList(Comment removedComment) {
        for (int i = 0; i < commentList.size(); i++) {
            if (commentList.get(i).getCommentId().equals(removedComment.getCommentId())) {
                commentList.remove(i);
                return;
            }
        }
    }

    private void postComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        // Clear input immediately for better UX
        etComment.setText("");

        Comment comment = new Comment();
        comment.setEventId(eventId);
        comment.setAuthorId(userId);
        comment.setAuthorName(userName);
        comment.setAuthorRole(isAdmin ? "admin" : (isOrganizer ? "organizer" : "entrant"));
        comment.setContent(content);
        comment.setCreatedAt(com.google.firebase.Timestamp.now()); // Set local timestamp for faster UI sync

        db.collection(FirestorePaths.eventComments(eventId))
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    // Success
                })
                .addOnFailureListener(e -> {
                    // Restore text if it failed
                    etComment.setText(content);
                    Toast.makeText(getContext(), "Failed to post comment", Toast.LENGTH_SHORT).show();
                });
    }
}
