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
        // Step 1: Clean up Co-Organizers
        // We use collectionGroup to find the user in any event's coOrganizers sub-collection
        db.collectionGroup(FirestorePaths.CO_ORGANIZERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(coOrganizerSnapshots -> {

                    // Step 2: Clean up Waitlists / Entrant Lists
                    // Similarly, remove the user from any event's waitingList (which includes all participation statuses)
                    db.collectionGroup(FirestorePaths.WAITING_LIST)
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(waitlistSnapshots -> {

                                WriteBatch batch = db.batch();

                                // Add all found co-organizer documents to the deletion batch
                                if (coOrganizerSnapshots != null) {
                                    for (DocumentSnapshot doc : coOrganizerSnapshots) {
                                        batch.delete(doc.getReference());
                                    }
                                }

                                // Add all found participation/waitlist documents to the deletion batch
                                if (waitlistSnapshots != null) {
                                    for (DocumentSnapshot doc : waitlistSnapshots) {
                                        batch.delete(doc.getReference());
                                    }
                                }

                                // Commit the batch and trigger completion
                                batch.commit()
                                        .addOnSuccessListener(unused -> onDone.run())
                                        .addOnFailureListener(e -> onDone.run());

                            })
                            .addOnFailureListener(e -> {
                                // Fallback: Even if waitlist query fails, delete whatever co-organizers we found
                                WriteBatch batch = db.batch();
                                if (coOrganizerSnapshots != null) {
                                    for (DocumentSnapshot doc : coOrganizerSnapshots) {
                                        batch.delete(doc.getReference());
                                    }
                                }
                                batch.commit().addOnCompleteListener(task -> onDone.run());
                            });
                })
                .addOnFailureListener(e -> onDone.run());
    }

    /**
     * Batch-deletes only co-organizer records for the given user across every event,
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
