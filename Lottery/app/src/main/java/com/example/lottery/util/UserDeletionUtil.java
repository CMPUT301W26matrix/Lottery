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
     * Batch-deletes all records for the given user across every event (co-organizers and waitlists),
     * then invokes onDone. The callback is always invoked regardless of
     * success or failure so that user deletion is never blocked.
     *
     * @param db     Firestore instance
     * @param userId the user whose documents should be removed
     * @param onDone callback invoked after cleanup completes or fails
     */
    public static void cleanUpUserRecords(FirebaseFirestore db, String userId, Runnable onDone) {
        // Clean up Co-Organizers
        db.collectionGroup(FirestorePaths.CO_ORGANIZERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(coOrganizerSnapshots -> {
                    
                    // Clean up Waitlists / Entrant Lists
                    db.collectionGroup(FirestorePaths.WAITING_LIST)
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(waitlistSnapshots -> {
                                
                                WriteBatch batch = db.batch();
                                
                                for (DocumentSnapshot doc : coOrganizerSnapshots) {
                                    batch.delete(doc.getReference());
                                }
                                
                                for (DocumentSnapshot doc : waitlistSnapshots) {
                                    batch.delete(doc.getReference());
                                }
                                
                                batch.commit()
                                        .addOnSuccessListener(unused -> onDone.run())
                                        .addOnFailureListener(e -> onDone.run());
                                
                            })
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }

    /**
     * @deprecated Use {@link #cleanUpUserRecords(FirebaseFirestore, String, Runnable)} instead.
     */
    @Deprecated
    public static void cleanUpCoOrganizerRecords(FirebaseFirestore db, String userId, Runnable onDone) {
        cleanUpUserRecords(db, userId, onDone);
    }
}
