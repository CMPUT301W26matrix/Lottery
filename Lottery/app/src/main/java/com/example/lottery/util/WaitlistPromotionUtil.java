package com.example.lottery.util;

import android.util.Log;

import com.example.lottery.model.NotificationItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
     * Uses a Firestore transaction to prevent race conditions when multiple declines
     * happen concurrently.
     *
     * @param db      The Firestore instance.
     * @param eventId The ID of the event to check.
     */
    public static void promoteOneFromWaitlistIfNeeded(FirebaseFirestore db, String eventId) {
        DocumentReference eventRef = db.collection(FirestorePaths.EVENTS).document(eventId);

        // First fetch the waitlist outside the transaction (reads from collections
        // are not supported inside transactions without document references).
        // The transaction will re-read the event doc to get a consistent capacity.
        db.collection(FirestorePaths.eventWaitingList(eventId)).get()
                .addOnSuccessListener(waitlistSnapshot ->
                        runPromotionTransaction(db, eventId, eventRef, waitlistSnapshot))
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching waitlist for promotion", e));
    }

    private static void runPromotionTransaction(FirebaseFirestore db, String eventId,
                                                DocumentReference eventRef,
                                                QuerySnapshot waitlistSnapshot) {
        db.runTransaction(transaction -> {
            // Re-read event inside transaction for consistent capacity check
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            if (!eventDoc.exists()) return null;

            Long capacityVal = eventDoc.getLong("capacity");
            final long capacity = capacityVal != null ? capacityVal : 0L;

            List<DocumentSnapshot> eligibleDocs = new ArrayList<>();
            int activeCount = 0;

            for (DocumentSnapshot doc : waitlistSnapshot.getDocuments()) {
                // Re-read each entrant doc inside the transaction for consistency
                DocumentSnapshot fresh = transaction.get(doc.getReference());
                String status = InvitationFlowUtil.normalizeEntrantStatus(fresh.getString("status"));
                if (InvitationFlowUtil.STATUS_INVITED.equals(status) ||
                        InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                    activeCount++;
                } else if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status) ||
                        InvitationFlowUtil.STATUS_NOT_SELECTED.equals(status)) {
                    eligibleDocs.add(fresh);
                }
            }

            if (activeCount < capacity && !eligibleDocs.isEmpty()) {
                Collections.shuffle(eligibleDocs);
                DocumentSnapshot selected = eligibleDocs.get(0);
                String targetUserId = selected.getId();
                String eventTitle = eventDoc.getString("title");
                String organizerId = eventDoc.getString("organizerId");

                // 1. Read user doc BEFORE any writes (Firestore requires all reads before writes)
                DocumentReference userRef = db.collection(FirestorePaths.USERS).document(targetUserId);
                DocumentSnapshot userDoc = transaction.get(userRef);
                Boolean notifPref = userDoc.getBoolean("notificationsEnabled");
                boolean enabled = notifPref == null || notifPref;

                // 2. Update entrant status to 'invited'
                transaction.update(selected.getReference(),
                        InvitationFlowUtil.buildInvitedEntrantUpdate());

                // 3. Write notification inside the same transaction if enabled
                if (enabled) {
                    String notificationId = UUID.randomUUID().toString();
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
                    DocumentReference inboxRef = db.collection(FirestorePaths.userInbox(targetUserId))
                            .document(notificationId);
                    transaction.set(inboxRef, notification);
                }

                return targetUserId;
            }
            return null;
        }).addOnSuccessListener(targetUserId -> {
            if (targetUserId != null) {
                Log.d(TAG, "Successfully promoted " + targetUserId + " for event " + eventId);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Promotion transaction failed, retrying without notification for event " + eventId, e);
            // Fallback: promote without notification to ensure spot is filled
            runFallbackPromotion(db, eventId, eventRef, waitlistSnapshot);
        });
    }

    /**
     * Fallback promotion that only updates entrant status without reading user preferences
     * or writing notifications. Used when the full transaction fails (e.g., user doc missing).
     */
    private static void runFallbackPromotion(FirebaseFirestore db, String eventId,
                                             DocumentReference eventRef,
                                             QuerySnapshot waitlistSnapshot) {
        db.runTransaction(transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            if (!eventDoc.exists()) return null;

            Long capacityVal = eventDoc.getLong("capacity");
            final long capacity = capacityVal != null ? capacityVal : 0L;

            List<DocumentSnapshot> eligibleDocs = new ArrayList<>();
            int activeCount = 0;

            for (DocumentSnapshot doc : waitlistSnapshot.getDocuments()) {
                DocumentSnapshot fresh = transaction.get(doc.getReference());
                String status = InvitationFlowUtil.normalizeEntrantStatus(fresh.getString("status"));
                if (InvitationFlowUtil.STATUS_INVITED.equals(status) ||
                        InvitationFlowUtil.STATUS_ACCEPTED.equals(status)) {
                    activeCount++;
                } else if (InvitationFlowUtil.STATUS_WAITLISTED.equals(status) ||
                        InvitationFlowUtil.STATUS_NOT_SELECTED.equals(status)) {
                    eligibleDocs.add(fresh);
                }
            }

            if (activeCount < capacity && !eligibleDocs.isEmpty()) {
                Collections.shuffle(eligibleDocs);
                DocumentSnapshot selected = eligibleDocs.get(0);
                transaction.update(selected.getReference(),
                        InvitationFlowUtil.buildInvitedEntrantUpdate());
                return selected.getId();
            }
            return null;
        }).addOnSuccessListener(targetUserId -> {
            if (targetUserId != null) {
                Log.d(TAG, "Fallback promoted " + targetUserId + " for event " + eventId);
                // Best-effort notification delivery after fallback promotion
                deliverNotificationBestEffort(db, eventId, targetUserId);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Fallback promotion also failed for event " + eventId, e));
    }

    /**
     * Best-effort notification delivery after a fallback promotion.
     * Reads event title/organizer and user preference, then writes notification if enabled.
     * Failures are logged but do not affect the already-committed promotion.
     */
    private static void deliverNotificationBestEffort(FirebaseFirestore db, String eventId,
                                                      String targetUserId) {
        db.collection(FirestorePaths.EVENTS).document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    String eventTitle = eventDoc.getString("title");
                    String organizerId = eventDoc.getString("organizerId");

                    db.collection(FirestorePaths.USERS).document(targetUserId).get()
                            .addOnSuccessListener(userDoc -> {
                                Boolean notifPref = userDoc.getBoolean("notificationsEnabled");
                                boolean enabled = notifPref == null || notifPref;
                                if (!enabled) return;

                                String notificationId = UUID.randomUUID().toString();
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
                                db.collection(FirestorePaths.userInbox(targetUserId))
                                        .document(notificationId)
                                        .set(notification)
                                        .addOnFailureListener(e -> Log.w(TAG, "Best-effort notification failed", e));
                            })
                            .addOnFailureListener(e -> Log.w(TAG, "Could not read user for best-effort notification", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Could not read event for best-effort notification", e));
    }
}
