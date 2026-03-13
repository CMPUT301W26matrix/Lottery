package com.example.lottery.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

/**
 * Unit tests for invitation status normalization and notification sync payloads.
 */
public class InvitationFlowUtilTest {

    @Test
    public void normalizeEntrantStatus_mapsNotificationAcceptedToCanonicalAccepted() {
        assertEquals(InvitationFlowUtil.STATUS_ACCEPTED,
                InvitationFlowUtil.normalizeEntrantStatus("ACCEPTED"));
    }

    @Test
    public void normalizeEntrantStatus_mapsRejectedToCanonicalDeclined() {
        assertEquals(InvitationFlowUtil.STATUS_DECLINED,
                InvitationFlowUtil.normalizeEntrantStatus("REJECTED"));
    }

    @Test
    public void normalizeEntrantStatus_mapsCancelledToCanonicalDeclined() {
        assertEquals(InvitationFlowUtil.STATUS_DECLINED,
                InvitationFlowUtil.normalizeEntrantStatus("CANCELLED"));
    }

    @Test
    public void entrantStatusFromNotificationResponse_mapsAcceptedResponse() {
        assertEquals(InvitationFlowUtil.STATUS_ACCEPTED,
                InvitationFlowUtil.entrantStatusFromNotificationResponse(InvitationFlowUtil.RESPONSE_ACCEPTED));
    }

    @Test
    public void entrantStatusFromNotificationResponse_mapsRejectedResponse() {
        assertEquals(InvitationFlowUtil.STATUS_DECLINED,
                InvitationFlowUtil.entrantStatusFromNotificationResponse(InvitationFlowUtil.RESPONSE_REJECTED));
    }

    @Test
    public void entrantStatusFromNotificationResponse_mapsCancelledResponse() {
        assertEquals(InvitationFlowUtil.STATUS_DECLINED,
                InvitationFlowUtil.entrantStatusFromNotificationResponse(InvitationFlowUtil.RESPONSE_CANCELLED));
    }

    @Test
    public void buildHandledNotificationUpdate_marksNotificationHandledAndRead() {
        Map<String, Object> updates =
                InvitationFlowUtil.buildHandledNotificationUpdate(InvitationFlowUtil.RESPONSE_ACCEPTED);

        assertEquals(Boolean.TRUE, updates.get("isRead"));
        assertEquals(Boolean.TRUE, updates.get("actionTaken"));
        assertEquals(InvitationFlowUtil.RESPONSE_ACCEPTED, updates.get("response"));
    }

    @Test
    public void buildHandledNotificationUpdate_containsOnlyExpectedKeys() {
        Map<String, Object> updates =
                InvitationFlowUtil.buildHandledNotificationUpdate(InvitationFlowUtil.RESPONSE_CANCELLED);

        assertEquals(3, updates.size());
        assertTrue(updates.containsKey("isRead"));
        assertTrue(updates.containsKey("actionTaken"));
        assertTrue(updates.containsKey("response"));
    }
}
