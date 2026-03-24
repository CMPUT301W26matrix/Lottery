package com.example.lottery.util;

import android.util.Log;

import com.example.lottery.model.NotificationItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Utility to handle automatic promotion of entrants from the waitlist when a spot opens up.
 *
 * <p>Reuses project-wide status transition logic (InvitationFlowUtil) and notification patterns.</p>
 */
public class WaitlistPromotionUtil {

    private static final String TAG = "WaitlistPromotionUtil";

    /**
     * Checks for available capacity and promotes one waitlisted entrant if possible.
     * Triggered automatically when an invited entrant declines.
     *
     * @param db      The Firestore instance.
     * @param eventId The ID of the event to check.
     */
    public static void promoteOneFromWaitlistIfNeeded(FirebaseFirestore db, String eventId) {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) return;

                    // Use getLong("capacity") to match EntrantsListActivity logic
                    Long capacityVal = eventDoc.getLong("capacity");
                    final long capacity = capacityVal != null ? capacityVal : 0L;

                    final String eventTitle = eventDoc.getString("title");
                    final String organizerId = eventDoc.getString("organizerId"); // Assumed field name for sender identity

                    db.collection(FirestorePaths.eventWaitingList(eventId)).get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<DocumentSnapshot> eligibleDocs = new ArrayList<>();
                                int activeCount = 0; // count of those already invited or accepted

                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    String status = InvitationFlowUtil.normalizeEntrantStatus(doc.getString("status"));
                                    if (InvitationFlowUtil.STATUS_INVITED.equals(status) ||
                                            InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                                        activeCount++;
                                    } else if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status) || 
                                               InvitationFlowUtil.STATUS_NOT_SELECTED.equals(status)) {
                                        // Both waitlisted and those previously not selected are eligible for promotion
                                        eligibleDocs.add(doc);
                                    }
                                }

                                // Promote one if there's room and someone is eligible
                                if (activeCount < capacity && !eligibleDocs.isEmpty()) {
                                    Collections.shuffle(eligibleDocs);
                                    DocumentSnapshot selectedEntrant = eligibleDocs.get(0);
                                    String targetUserId = selectedEntrant.getId();

                                    performPromotion(db, eventId, eventTitle, organizerId, targetUserId, selectedEntrant);
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error fetching waitlist for promotion", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching event for promotion", e));
    }

    /**
     * Executes the status update and notification delivery in a single atomic batch.
     */
    private static void performPromotion(FirebaseFirestore db, String eventId, String eventTitle,
                                         String organizerId, String targetUserId, DocumentSnapshot doc) {
        // We first check the user's notification preference.
        // Even if notifications are disabled, the status update (promotion) must still happen.
        db.collection(FirestorePaths.USERS).document(targetUserId).get()
                .addOnSuccessListener(userDoc -> {
                    boolean enabled = userDoc.contains("notificationsEnabled") 
                            ? userDoc.getBoolean("notificationsEnabled") : true;

                    WriteBatch batch = db.batch();
                    String notificationId = UUID.randomUUID().toString();

                    // 1. Update entrant status to 'invited' - ALWAYS DO THIS
                    batch.update(doc.getReference(), InvitationFlowUtil.buildInvitedEntrantUpdate());

                    // 2. Create notification for the newly selected entrant - ONLY IF ENABLED
                    if (enabled) {
                        NotificationItem notification = new NotificationItem(
                                notificationId,
                                "You've been selected!",
                                "A spot opened up for " + (eventTitle != null ? eventTitle : "an event") + ". You are invited to join!",
                                "waitlist_promoted",
                                eventId,
                                eventTitle,
                                organizerId != null ? organizerId : "",
                                "ORGANIZER",
                                false,
                                Timestamp.now()
                        );
                        batch.set(db.collection(FirestorePaths.userInbox(targetUserId)).document(notificationId), notification);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> Log.d(TAG, "Successfully promoted " + targetUserId + " for event " + eventId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to commit promotion batch", e));
                })
                .addOnFailureListener(e -> {
                    // Fallback: If we can't read the user doc, we still promote them (fail-safe for business logic)
                    WriteBatch batch = db.batch();
                    batch.update(doc.getReference(), InvitationFlowUtil.buildInvitedEntrantUpdate());
                    batch.commit();
                });
    }
}
