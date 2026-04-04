package com.example.lottery.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for cleaning up user-related records before deleting a user document.
 */
public final class UserDeletionUtil {

    private UserDeletionUtil() {
    }

    /**
     * Batch-deletes all records for the given user across every event (co-organizers and waitlists),
     * and also clears their local sub-collections (like inbox), then invokes onDone.
     *
     * @param db     Firestore instance
     * @param userId the user whose documents should be removed
     * @param onDone callback invoked after cleanup completes or fails
     */
    public static void cleanUpUserRecords(FirebaseFirestore db, String userId, Runnable onDone) {
        // Two parallel queries for co-organizers and waitlists tracked by an atomic counter.
        // Inbox is deleted in a separate batch to avoid exceeding Firestore's 500-write limit.
        AtomicInteger remaining = new AtomicInteger(2);
        WriteBatch batch = db.batch();

        Runnable commitIfDone = () -> {
            if (remaining.decrementAndGet() == 0) {
                batch.commit()
                        .addOnSuccessListener(unused -> deleteInbox(db, userId, onDone))
                        .addOnFailureListener(e -> deleteInbox(db, userId, onDone));
            }
        };

        // Co-organizers
        db.collectionGroup(FirestorePaths.CO_ORGANIZERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    addToBatch(batch, snap);
                    commitIfDone.run();
                })
                .addOnFailureListener(e -> commitIfDone.run());

        // Waitlists
        db.collectionGroup(FirestorePaths.WAITING_LIST)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    addToBatch(batch, snap);
                    commitIfDone.run();
                })
                .addOnFailureListener(e -> commitIfDone.run());
    }

    /**
     * Deletes all inbox documents for the user in a separate batch, then invokes onDone.
     */
    private static void deleteInbox(FirebaseFirestore db, String userId, Runnable onDone) {
        db.collection(FirestorePaths.userInbox(userId))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        onDone.run();
                        return;
                    }
                    WriteBatch inboxBatch = db.batch();
                    for (DocumentSnapshot doc : snap) {
                        inboxBatch.delete(doc.getReference());
                    }
                    inboxBatch.commit()
                            .addOnSuccessListener(unused -> onDone.run())
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private static void addToBatch(WriteBatch batch, QuerySnapshot snap) {
        if (snap != null) {
            for (DocumentSnapshot doc : snap) {
                batch.delete(doc.getReference());
            }
        }
    }

    /**
     * Batch-deletes only co-organizer records for the given user across every event.
     */
    public static void cleanUpCoOrganizerRecords(FirebaseFirestore db, String userId, Runnable onDone) {
        db.collectionGroup(FirestorePaths.CO_ORGANIZERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> onDone.run())
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }
}
