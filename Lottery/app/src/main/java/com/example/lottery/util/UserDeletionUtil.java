package com.example.lottery.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/**
 * Utility for cleaning up user-related records before deleting a user document.
 */
public final class UserDeletionUtil {

    private UserDeletionUtil() {
    }

    /**
     * Batch-deletes all co-organizer records for the given user across every event,
     * then invokes onDone. The callback is always invoked regardless of
     * success or failure so that user deletion is never blocked.
     *
     * @param db     Firestore instance
     * @param userId the user whose co-organizer documents should be removed
     * @param onDone callback invoked after cleanup completes or fails
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
