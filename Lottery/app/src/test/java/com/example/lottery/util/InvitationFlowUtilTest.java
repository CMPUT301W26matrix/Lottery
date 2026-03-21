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
    public void normalizeEntrantStatus_mapsCancelledToCanonicalCancelled() {
        assertEquals(InvitationFlowUtil.STATUS_CANCELLED,
                InvitationFlowUtil.normalizeEntrantStatus("CANCELLED"));
    }

    @Test
    public void normalizeNotificationResponse_mapsAcceptedResponse() {
        assertEquals(InvitationFlowUtil.RESPONSE_ACCEPTED,
                InvitationFlowUtil.normalizeNotificationResponse(InvitationFlowUtil.RESPONSE_ACCEPTED));
    }

    @Test
    public void normalizeNotificationResponse_mapsDeclinedResponse() {
        assertEquals(InvitationFlowUtil.RESPONSE_DECLINED,
                InvitationFlowUtil.normalizeNotificationResponse(InvitationFlowUtil.RESPONSE_DECLINED));
    }

    @Test
    public void normalizeNotificationResponse_mapsCancelledResponse() {
        assertEquals(InvitationFlowUtil.RESPONSE_CANCELLED,
                InvitationFlowUtil.normalizeNotificationResponse(InvitationFlowUtil.RESPONSE_CANCELLED));
    }

    @Test
    public void buildHandledNotificationUpdate_marksNotificationRead() {
        Map<String, Object> updates =
                InvitationFlowUtil.buildHandledNotificationUpdate(InvitationFlowUtil.RESPONSE_ACCEPTED);

        assertEquals(Boolean.TRUE, updates.get("isRead"));
    }

    @Test
    public void buildHandledNotificationUpdate_containsOnlyExpectedKeys() {
        Map<String, Object> updates =
                InvitationFlowUtil.buildHandledNotificationUpdate(InvitationFlowUtil.RESPONSE_CANCELLED);

        assertEquals(1, updates.size());
        assertTrue(updates.containsKey("isRead"));
    }
}
