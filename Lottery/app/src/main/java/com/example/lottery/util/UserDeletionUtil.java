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
     * and also clears their local sub-collections (like inbox), then invokes onDone.
     *
     * @param db     Firestore instance
     * @param userId the user whose documents should be removed
     * @param onDone callback invoked after cleanup completes or fails
     */
    public static void cleanUpUserRecords(FirebaseFirestore db, String userId, Runnable onDone) {
        // Step 1: Clean up Co-Organizers collection group
        db.collectionGroup(FirestorePaths.CO_ORGANIZERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(coOrganizerSnapshots -> {

                    // Step 2: Clean up Waitlists collection group
                    db.collectionGroup(FirestorePaths.WAITING_LIST)
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(waitlistSnapshots -> {

                                // Step 3: Clean up User's local Inbox sub-collection
                                db.collection(FirestorePaths.userInbox(userId))
                                        .get()
                                        .addOnSuccessListener(inboxSnapshots -> {

                                            WriteBatch batch = db.batch();

                                            // Delete Co-organizer references
                                            if (coOrganizerSnapshots != null) {
                                                for (DocumentSnapshot doc : coOrganizerSnapshots) {
                                                    batch.delete(doc.getReference());
                                                }
                                            }

                                            // Delete Waitlist references
                                            if (waitlistSnapshots != null) {
                                                for (DocumentSnapshot doc : waitlistSnapshots) {
                                                    batch.delete(doc.getReference());
                                                }
                                            }

                                            // Delete local Inbox documents
                                            if (inboxSnapshots != null) {
                                                for (DocumentSnapshot doc : inboxSnapshots) {
                                                    batch.delete(doc.getReference());
                                                }
                                            }

                                            // Commit all deletions in one batch
                                            batch.commit()
                                                    .addOnSuccessListener(unused -> onDone.run())
                                                    .addOnFailureListener(e -> onDone.run());

                                        })
                                        .addOnFailureListener(e -> onDone.run());
                            })
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
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
