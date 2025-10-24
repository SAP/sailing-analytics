package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ReplicationStopTest extends AbstractServerReplicationTest {
    @Test
    public void testReplicaStop() throws Exception {
        assertNotSame(master, replica);
        assertEquals(Util.size(master.getAllRegattas()), Util.size(replica.getAllRegattas()));
        Event event = addEventOnMaster("Test 1 Event");
        Thread.sleep(1000);
        assertNotNull(replica.getEvent(event.getId()));
        stopReplicatingToMaster();
        Event event2 = addEventOnMaster("Test 2 Event");
        assertNull(replica.getEvent(event2.getId()));
    }
    
    private Event addEventOnMaster(String eventName) {
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        final String venueName = "Masquat, Oman";
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        return master.addEvent(eventName, /* eventDescription */ null, eventStartDate, eventEndDate, venueName, isPublic, UUID.randomUUID());
    }
}