package com.sap.sailing.domain.regattalog.tracking.analyzing.test;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRevokeEventImpl;
import com.sap.sse.common.Util;

public class RevokeEventTest extends AbstractRegattaLogTrackingTest {
    @Test
    public void revokedEventIsExcluded() {
        RegattaLogEvent event = new RegattaLogDeviceCompetitorMappingEventImpl(now, now, author, UUID.randomUUID(), null, null, null, null);
        RegattaLogRevokeEvent revokeEvent = new RegattaLogRevokeEventImpl(now.plus(1), now.plus(1), author, UUID.randomUUID(), event.getId(),
                null, null, null);
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assertions.assertFalse(Util.contains(log.getUnrevokedEvents(), event));
        log.unlockAfterRead();
    }
    
    @Test
    public void eventRevokedByAuthorWithLowerPrioIsNotExcluded() {
    	RegattaLogEvent event = new RegattaLogDeviceCompetitorMappingEventImpl(now, now, author, UUID.randomUUID(), null, null, null, null);
    	RegattaLogRevokeEvent revokeEvent = new RegattaLogRevokeEventImpl(now.plus(1), now.plus(1), author1, UUID.randomUUID(), event.getId(),
                null, null, null);
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assertions.assertTrue(Util.contains(log.getUnrevokedEvents(), event));
        log.unlockAfterRead();
    }
    
    @Test
    public void revokingRevokeEventIsNotHarmful() {
        RegattaLogEvent event = new RegattaLogDeviceCompetitorMappingEventImpl(now, now, author, UUID.randomUUID(), null, null, null, null);
        RegattaLogRevokeEvent revokeEvent = new RegattaLogRevokeEventImpl(now.plus(1), now.plus(1), author, UUID.randomUUID(), event.getId(),
                null, null, null);
        RegattaLogRevokeEvent revokeEvent2 = new RegattaLogRevokeEventImpl(now.plus(2), now.plus(2), author, UUID.randomUUID(), revokeEvent.getId(),
                null, null, null);
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assertions.assertFalse(Util.contains(log.getUnrevokedEvents(), event));
        Assertions.assertFalse(Util.contains(log.getUnrevokedEvents(), revokeEvent));
        Assertions.assertFalse(Util.contains(log.getUnrevokedEvents(), revokeEvent2));
        log.unlockAfterRead();
    }
}
