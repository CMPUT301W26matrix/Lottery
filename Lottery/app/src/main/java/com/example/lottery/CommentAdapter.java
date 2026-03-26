package com.example.lottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Comment;
import com.example.lottery.util.FirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments;
    private final boolean isOrganizer;
    private final String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public CommentAdapter(List<Comment> comments, boolean isOrganizer, String eventId) {
        this.comments = comments;
        this.isOrganizer = isOrganizer;
        this.eventId = eventId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        // Display author name with "(Organizer)" marker if role is organizer
        String authorDisplayName = comment.getAuthorName() != null ? comment.getAuthorName() : "Anonymous";
        if ("organizer".equalsIgnoreCase(comment.getAuthorRole())) {
            authorDisplayName += " (Organizer)";
        }
        holder.tvAuthorName.setText(authorDisplayName);

        holder.tvCommentContent.setText(comment.getContent());

        if (comment.getCreatedAt() != null) {
            holder.tvCommentTime.setText(dateFormat.format(comment.getCreatedAt().toDate()));
        } else {
            holder.tvCommentTime.setText("");
        }

        if (isOrganizer) {
            holder.btnDeleteComment.setVisibility(View.VISIBLE);
            holder.btnDeleteComment.setOnClickListener(v -> deleteComment(comment, holder.getAdapterPosition(), v));
        } else {
            holder.btnDeleteComment.setVisibility(View.GONE);
        }
    }

    private void deleteComment(Comment comment, int position, View view) {
        if (comment.getCommentId() == null) return;

        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.eventComments(eventId))
                .document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(view.getContext(), "Comment deleted", Toast.LENGTH_SHORT).show();
                    // List will be updated via SnapshotListener in BottomSheet
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(), "Failed to delete comment", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvCommentContent, tvCommentTime;
        ImageButton btnDeleteComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}
