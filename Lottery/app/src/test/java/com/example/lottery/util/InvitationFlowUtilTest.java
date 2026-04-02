package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Map;

/**
 * Unit tests for {@link InvitationFlowUtil}.
 *
 * <p>Covers invitation status transitions, notification response handling,
 * and Firestore update payload construction for the lottery draw flow.</p>
 */
public class InvitationFlowUtilTest {

    // -------------------------------------------------------------------------
    // US 01.01.01 — Entrant joins the waiting list
    // -------------------------------------------------------------------------

    // US 01.01.01: buildWaitlistJoinUpdate sets status to "waitlisted"
    @Test
    public void buildWaitlistJoinUpdate_setsStatusToWaitlisted() {
        Map<String, Object> update = InvitationFlowUtil.buildWaitlistJoinUpdate();
        assertEquals(InvitationFlowUtil.STATUS_WAITLISTED, update.get("status"));
    }

    // US 01.01.01: buildWaitlistJoinUpdate includes waitlistedAt timestamp
    @Test
    public void buildWaitlistJoinUpdate_includesWaitlistedAtTimestamp() {
        Map<String, Object> update = InvitationFlowUtil.buildWaitlistJoinUpdate();
        Object waitlistedAt = update.get("waitlistedAt");
        assertNotNull("waitlistedAt timestamp must be present", waitlistedAt);
        assertTrue("waitlistedAt must be a Timestamp", waitlistedAt instanceof Timestamp);
    }

    // US 01.01.01: buildWaitlistJoinUpdate includes registeredAt timestamp
    @Test
    public void buildWaitlistJoinUpdate_includesRegisteredAtTimestamp() {
        Map<String, Object> update = InvitationFlowUtil.buildWaitlistJoinUpdate();
        Object registeredAt = update.get("registeredAt");
        assertNotNull("registeredAt timestamp must be present", registeredAt);
        assertTrue("registeredAt must be a Timestamp", registeredAt instanceof Timestamp);
    }

    // US 01.01.01: buildWaitlistJoinUpdate contains exactly 3 keys
    @Test
    public void buildWaitlistJoinUpdate_containsExactlyThreeKeys() {
        Map<String, Object> update = InvitationFlowUtil.buildWaitlistJoinUpdate();
        assertEquals(3, update.size());
        assertTrue(update.containsKey("status"));
        assertTrue(update.containsKey("waitlistedAt"));
        assertTrue(update.containsKey("registeredAt"));
    }

    // -------------------------------------------------------------------------
    // US 01.01.02 — Entrant leaves the waiting list
    // -------------------------------------------------------------------------

    // US 01.01.02: buildCancelledEntrantUpdate sets status to "cancelled" when entrant leaves
    @Test
    public void buildCancelledEntrantUpdate_setsStatusToCancelled() {
        Map<String, Object> update = InvitationFlowUtil.buildCancelledEntrantUpdate();
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED, update.get("status"));
    }

    // US 01.01.02: buildCancelledEntrantUpdate includes cancelledAt timestamp
    @Test
    public void buildCancelledEntrantUpdate_includesCancelledAtTimestamp() {
        Map<String, Object> update = InvitationFlowUtil.buildCancelledEntrantUpdate();
        Object cancelledAt = update.get("cancelledAt");
        assertNotNull("cancelledAt timestamp must be present", cancelledAt);
        assertTrue("cancelledAt must be a Timestamp", cancelledAt instanceof Timestamp);
    }

    // -------------------------------------------------------------------------
    // US 01.05.01 — Entrant not selected in replacement draw
    // -------------------------------------------------------------------------

    // US 01.05.01: buildNotSelectedEntrantUpdate sets status to "not_selected" after draw
    @Test
    public void buildNotSelectedEntrantUpdate_setsStatusToNotSelected() {
        Map<String, Object> update = InvitationFlowUtil.buildNotSelectedEntrantUpdate();
        assertEquals(InvitationFlowUtil.STATUS_NOT_SELECTED, update.get("status"));
    }

    // US 01.05.01: buildNotSelectedEntrantUpdate includes notSelectedAt timestamp
    @Test
    public void buildNotSelectedEntrantUpdate_includesNotSelectedAtTimestamp() {
        Map<String, Object> update = InvitationFlowUtil.buildNotSelectedEntrantUpdate();
        Object notSelectedAt = update.get("notSelectedAt");
        assertNotNull("notSelectedAt timestamp must be present", notSelectedAt);
        assertTrue("notSelectedAt must be a Timestamp", notSelectedAt instanceof Timestamp);
    }

    // -------------------------------------------------------------------------
    // US 01.05.02 — Entrant accepts an invitation
    // -------------------------------------------------------------------------

    // US 01.05.02: buildEntrantStatusUpdateFromResponse("accepted") sets status to "accepted"
    @Test
    public void buildStatusFromResponse_accepted_setsStatusToAccepted() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("accepted");
        assertEquals(InvitationFlowUtil.STATUS_ACCEPTED, update.get("status"));
    }

    // US 01.05.02: buildEntrantStatusUpdateFromResponse("accepted") sets acceptedAt timestamp
    @Test
    public void buildStatusFromResponse_accepted_includesAcceptedAtTimestamp() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("accepted");
        Object acceptedAt = update.get("acceptedAt");
        assertNotNull("acceptedAt timestamp must be present", acceptedAt);
        assertTrue("acceptedAt must be a Timestamp", acceptedAt instanceof Timestamp);
    }

    // US 01.05.02: buildEntrantStatusUpdateFromResponse("accept") alias also works
    @Test
    public void buildStatusFromResponse_acceptAlias_setsStatusToAccepted() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("accept");
        assertEquals(InvitationFlowUtil.STATUS_ACCEPTED, update.get("status"));
    }

    // -------------------------------------------------------------------------
    // US 01.05.03 — Entrant declines an invitation
    // -------------------------------------------------------------------------

    // US 01.05.03: buildEntrantStatusUpdateFromResponse("declined") sets status to "cancelled"
    @Test
    public void buildStatusFromResponse_declined_setsStatusToCancelled() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("declined");
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED, update.get("status"));
    }

    // US 01.05.03: buildEntrantStatusUpdateFromResponse("declined") sets cancelledAt timestamp
    @Test
    public void buildStatusFromResponse_declined_includesCancelledAtTimestamp() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("declined");
        Object cancelledAt = update.get("cancelledAt");
        assertNotNull("cancelledAt timestamp must be present", cancelledAt);
        assertTrue("cancelledAt must be a Timestamp", cancelledAt instanceof Timestamp);
    }

    // US 01.05.03: buildEntrantStatusUpdateFromResponse("decline") alias also works
    @Test
    public void buildStatusFromResponse_declineAlias_setsStatusToCancelled() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("decline");
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED, update.get("status"));
    }

    // -------------------------------------------------------------------------
    // US 02.05.02 — Organizer sends invitations to selected entrants
    // -------------------------------------------------------------------------

    // US 02.05.02: buildInvitedEntrantUpdate sets status to "invited"
    @Test
    public void buildInvitedEntrantUpdate_setsStatusToInvited() {
        Map<String, Object> update = InvitationFlowUtil.buildInvitedEntrantUpdate();
        assertEquals(InvitationFlowUtil.STATUS_INVITED, update.get("status"));
    }

    // US 02.05.02: buildInvitedEntrantUpdate includes invitedAt timestamp
    @Test
    public void buildInvitedEntrantUpdate_includesInvitedAtTimestamp() {
        Map<String, Object> update = InvitationFlowUtil.buildInvitedEntrantUpdate();
        Object invitedAt = update.get("invitedAt");
        assertNotNull("invitedAt timestamp must be present", invitedAt);
        assertTrue("invitedAt must be a Timestamp", invitedAt instanceof Timestamp);
    }

    // US 02.05.02: buildInvitedEntrantUpdate contains exactly 2 keys
    @Test
    public void buildInvitedEntrantUpdate_containsExactlyTwoKeys() {
        Map<String, Object> update = InvitationFlowUtil.buildInvitedEntrantUpdate();
        assertEquals(2, update.size());
        assertTrue(update.containsKey("status"));
        assertTrue(update.containsKey("invitedAt"));
    }

    // -------------------------------------------------------------------------
    // US 02.05.03 — Replacement draw marks remaining entrants as not selected
    // -------------------------------------------------------------------------

    // US 02.05.03: buildNotSelectedEntrantUpdate for replacement scenario contains exactly 2 keys
    @Test
    public void buildNotSelectedEntrantUpdate_containsExactlyTwoKeys() {
        Map<String, Object> update = InvitationFlowUtil.buildNotSelectedEntrantUpdate();
        assertEquals(2, update.size());
        assertTrue(update.containsKey("status"));
        assertTrue(update.containsKey("notSelectedAt"));
    }

    // -------------------------------------------------------------------------
    // US 02.06.04 — Organizer cancels an entrant
    // -------------------------------------------------------------------------

    // US 02.06.04: buildCancelledEntrantUpdate for organizer-initiated cancellation has correct keys
    @Test
    public void buildCancelledEntrantUpdate_containsExactlyTwoKeys() {
        Map<String, Object> update = InvitationFlowUtil.buildCancelledEntrantUpdate();
        assertEquals(2, update.size());
        assertTrue(update.containsKey("status"));
        assertTrue(update.containsKey("cancelledAt"));
    }

    // -------------------------------------------------------------------------
    // Normalization — entrant status aliases
    // -------------------------------------------------------------------------

    // US 01.01.01: Legacy "waiting" status values normalize to the canonical waitlisted state
    @Test
    public void normalizeEntrantStatus_waitingMapsToWaitlisted() {
        assertEquals(InvitationFlowUtil.STATUS_WAITLISTED,
                InvitationFlowUtil.normalizeEntrantStatus("waiting"));
    }

    // US 01.01.01: Canonical waitlisted values stay unchanged after normalization
    @Test
    public void normalizeEntrantStatus_waitlistedMapsToWaitlisted() {
        assertEquals(InvitationFlowUtil.STATUS_WAITLISTED,
                InvitationFlowUtil.normalizeEntrantStatus("waitlisted"));
    }

    // US 02.05.02: Lottery draw selections normalize to the invited entrant state
    @Test
    public void normalizeEntrantStatus_selectedMapsToInvited() {
        assertEquals(InvitationFlowUtil.STATUS_INVITED,
                InvitationFlowUtil.normalizeEntrantStatus("selected"));
    }

    // US 02.06.01: Canonical invited values stay unchanged after normalization
    @Test
    public void normalizeEntrantStatus_invitedMapsToInvited() {
        assertEquals(InvitationFlowUtil.STATUS_INVITED,
                InvitationFlowUtil.normalizeEntrantStatus("invited"));
    }

    // US 01.05.02: Accepted entrant values normalize to the accepted state
    @Test
    public void normalizeEntrantStatus_acceptedMapsToAccepted() {
        assertEquals(InvitationFlowUtil.STATUS_ACCEPTED,
                InvitationFlowUtil.normalizeEntrantStatus("ACCEPTED"));
    }

    // US 01.05.01 / US 02.05.03: Rejected-by-draw values normalize to not_selected
    @Test
    public void normalizeEntrantStatus_rejectedByDrawMapsToNotSelected() {
        assertEquals(InvitationFlowUtil.STATUS_NOT_SELECTED,
                InvitationFlowUtil.normalizeEntrantStatus("rejected_by_draw"));
    }

    // US 01.05.03 / US 02.06.04: Rejected entrants normalize to cancelled
    @Test
    public void normalizeEntrantStatus_rejectedMapsToCancelled() {
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED,
                InvitationFlowUtil.normalizeEntrantStatus("rejected"));
    }

    // US 01.05.03 / US 02.06.04: Canonical cancelled values stay unchanged
    @Test
    public void normalizeEntrantStatus_cancelledMapsToCancelled() {
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED,
                InvitationFlowUtil.normalizeEntrantStatus("cancelled"));
    }

    // US 01.05.03: Declined invitations normalize to the cancelled entrant state
    @Test
    public void normalizeEntrantStatus_declinedMapsToCancelled() {
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED,
                InvitationFlowUtil.normalizeEntrantStatus("declined"));
    }

    // US 01.01.01 / US 01.05.02 / US 02.05.02: Null statuses normalize safely to empty
    @Test
    public void normalizeEntrantStatus_nullReturnsEmpty() {
        assertEquals("", InvitationFlowUtil.normalizeEntrantStatus(null));
    }

    // US 01.01.01 / US 01.05.02 / US 02.05.02: Unknown statuses normalize safely to empty
    @Test
    public void normalizeEntrantStatus_unknownReturnsEmpty() {
        assertEquals("", InvitationFlowUtil.normalizeEntrantStatus("foobar"));
    }

    // US 01.01.01 / US 02.05.02: Status normalization remains case-insensitive across aliases
    @Test
    public void normalizeEntrantStatus_isCaseInsensitive() {
        assertEquals(InvitationFlowUtil.STATUS_WAITLISTED,
                InvitationFlowUtil.normalizeEntrantStatus("WAITING"));
        assertEquals(InvitationFlowUtil.STATUS_INVITED,
                InvitationFlowUtil.normalizeEntrantStatus("Selected"));
    }

    // -------------------------------------------------------------------------
    // Normalization — notification response aliases
    // -------------------------------------------------------------------------

    // US 01.05.02: "accept" notification responses normalize to accepted
    @Test
    public void normalizeNotificationResponse_acceptMapsToAccepted() {
        assertEquals(InvitationFlowUtil.RESPONSE_ACCEPTED,
                InvitationFlowUtil.normalizeNotificationResponse("accept"));
    }

    // US 01.05.02: Canonical accepted responses stay unchanged
    @Test
    public void normalizeNotificationResponse_acceptedMapsToAccepted() {
        assertEquals(InvitationFlowUtil.RESPONSE_ACCEPTED,
                InvitationFlowUtil.normalizeNotificationResponse("accepted"));
    }

    // US 01.05.03: "decline" notification responses normalize to declined
    @Test
    public void normalizeNotificationResponse_declineMapsToDeclined() {
        assertEquals(InvitationFlowUtil.RESPONSE_DECLINED,
                InvitationFlowUtil.normalizeNotificationResponse("decline"));
    }

    // US 01.05.03: Canonical declined responses stay unchanged
    @Test
    public void normalizeNotificationResponse_declinedMapsToDeclined() {
        assertEquals(InvitationFlowUtil.RESPONSE_DECLINED,
                InvitationFlowUtil.normalizeNotificationResponse("declined"));
    }

    // US 01.05.03: "reject" responses normalize to declined for invitation handling
    @Test
    public void normalizeNotificationResponse_rejectMapsToDeclined() {
        assertEquals(InvitationFlowUtil.RESPONSE_DECLINED,
                InvitationFlowUtil.normalizeNotificationResponse("reject"));
    }

    // US 01.05.03: "rejected" responses normalize to declined for invitation handling
    @Test
    public void normalizeNotificationResponse_rejectedMapsToDeclined() {
        assertEquals(InvitationFlowUtil.RESPONSE_DECLINED,
                InvitationFlowUtil.normalizeNotificationResponse("rejected"));
    }

    // US 01.04.01 / US 01.04.02: Dismissed notification responses normalize to dismissed
    @Test
    public void normalizeNotificationResponse_dismissedMapsToDismissed() {
        assertEquals(InvitationFlowUtil.RESPONSE_DISMISSED,
                InvitationFlowUtil.normalizeNotificationResponse("dismissed"));
    }

    // US 01.04.01 / US 01.04.02: Short-form dismiss responses normalize to dismissed
    @Test
    public void normalizeNotificationResponse_dismissMapsToDismissed() {
        assertEquals(InvitationFlowUtil.RESPONSE_DISMISSED,
                InvitationFlowUtil.normalizeNotificationResponse("dismiss"));
    }

    // US 01.05.03: Cancelled responses normalize to cancelled for entrant updates
    @Test
    public void normalizeNotificationResponse_cancelledMapsToCancelled() {
        assertEquals(InvitationFlowUtil.RESPONSE_CANCELLED,
                InvitationFlowUtil.normalizeNotificationResponse("cancelled"));
    }

    // US 01.05.03: American spelling of canceled also normalizes to cancelled
    @Test
    public void normalizeNotificationResponse_canceledMapsToCancelled() {
        assertEquals(InvitationFlowUtil.RESPONSE_CANCELLED,
                InvitationFlowUtil.normalizeNotificationResponse("canceled"));
    }

    // US 01.04.01 / US 01.05.02: Null notification responses normalize safely to none
    @Test
    public void normalizeNotificationResponse_nullReturnsNone() {
        assertEquals(InvitationFlowUtil.RESPONSE_NONE,
                InvitationFlowUtil.normalizeNotificationResponse(null));
    }

    // US 01.04.01 / US 01.05.02: Unknown notification responses normalize safely to none
    @Test
    public void normalizeNotificationResponse_unknownReturnsNone() {
        assertEquals(InvitationFlowUtil.RESPONSE_NONE,
                InvitationFlowUtil.normalizeNotificationResponse("foobar"));
    }

    // -------------------------------------------------------------------------
    // Edge case — dismissed response produces empty update map
    // -------------------------------------------------------------------------

    // US 01.04.01 / US 01.04.02: Dismissing a notification should not change entrant status
    @Test
    public void buildStatusFromResponse_dismissed_returnsEmptyMap() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("dismissed");
        assertTrue("Dismissed response should not produce a status update", update.isEmpty());
    }

    // US 01.05.02 / US 01.05.03: Unknown responses should not produce entrant updates
    @Test
    public void buildStatusFromResponse_unknownResponse_returnsEmptyMap() {
        Map<String, Object> update =
                InvitationFlowUtil.buildEntrantStatusUpdateFromResponse("unknown_value");
        assertTrue("Unknown response should produce an empty update", update.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Read notification update
    // -------------------------------------------------------------------------

    // US 01.04.01 / US 01.04.02: Opening a notification marks it as read
    @Test
    public void buildReadNotificationUpdate_marksNotificationRead() {
        Map<String, Object> updates = InvitationFlowUtil.buildReadNotificationUpdate();
        assertEquals(Boolean.TRUE, updates.get("isRead"));
    }

    // US 01.04.01 / US 01.04.02: Read-notification updates only modify the read flag
    @Test
    public void buildReadNotificationUpdate_containsOnlyExpectedKeys() {
        Map<String, Object> updates = InvitationFlowUtil.buildReadNotificationUpdate();
        assertEquals(1, updates.size());
        assertTrue(updates.containsKey("isRead"));
    }
}
