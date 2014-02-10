package com.sap.sailing.domain.racelog.tracking.analyzing.test;

import junit.framework.Assert;

import org.junit.Test;

import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.tracking.RevokeEvent;

public class RevokeEventTest extends AbstractRaceLogTrackingTest {
    @Test
    public void revokedEventIsExcluded() {
        RaceLogEvent event = factory.createDeviceCompetitorMappingEvent(now, author, null, null, 0, null, null);
        RevokeEvent revokeEvent = factory.createRevokeEvent(now.plus(1), author, 0, event.getId());
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assert.assertFalse(Util.contains(log.getUnrevokedEvents(), event));
        log.unlockAfterRead();
    }
    
    @Test
    public void eventRevokedByAuthorWithLowerPrioIsNotExcluded() {
        RaceLogEvent event = factory.createDeviceCompetitorMappingEvent(now, author, null, null, 0, null, null);
        RevokeEvent revokeEvent = factory.createRevokeEvent(now.plus(1), author1, 0, event.getId());
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assert.assertTrue(Util.contains(log.getUnrevokedEvents(), event));
        log.unlockAfterRead();
    }
    
    @Test
    public void revokingRevokeEventIsNotHarmful() {
        RaceLogEvent event = factory.createDeviceCompetitorMappingEvent(now, author, null, null, 0, null, null);
        RevokeEvent revokeEvent = factory.createRevokeEvent(now.plus(1), author, 0, event.getId());
        RevokeEvent revokeEvent2 = factory.createRevokeEvent(now.plus(2), author, 2, revokeEvent.getId());
        
        log.add(event);
        log.add(revokeEvent);
        
        log.lockForRead();
        Assert.assertFalse(Util.contains(log.getUnrevokedEvents(), event));
        Assert.assertFalse(Util.contains(log.getUnrevokedEvents(), revokeEvent));
        Assert.assertFalse(Util.contains(log.getUnrevokedEvents(), revokeEvent2));
        log.unlockAfterRead();
    }
}
