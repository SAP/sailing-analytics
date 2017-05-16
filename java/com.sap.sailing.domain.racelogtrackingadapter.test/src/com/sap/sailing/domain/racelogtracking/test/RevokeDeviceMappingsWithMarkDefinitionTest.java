package com.sap.sailing.domain.racelogtracking.test;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.racelogtracking.PingDeviceIdentifierImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import junit.framework.Assert;

public class RevokeDeviceMappingsWithMarkDefinitionTest {
    private RegattaLog regattaLog;
    private AbstractLogEventAuthor author;
    private Mark mark;
    
    @Before
    public void prepare() {
        regattaLog = new RegattaLogImpl("RevokeTest");
        author = new LogEventAuthorImpl("RevokeTest", 1);
        mark = new MarkImpl("mark");
    }
    
    @Test
    public void testRevoking() {
        final PingDeviceIdentifierImpl device = new PingDeviceIdentifierImpl();
        
        final RegattaLogDefineMarkEvent defineMarkEvent = new RegattaLogDefineMarkEventImpl(MillisecondsTimePoint.now(), author, 
                MillisecondsTimePoint.now(), UUID.randomUUID(), mark);
        
        regattaLog.add(defineMarkEvent);
        regattaLog.add(new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now(), author, mark, device, 
                MillisecondsTimePoint.now(), MillisecondsTimePoint.now()));
        
        regattaLog.revokeDefineMarkEventAndRelatedDeviceMappings(defineMarkEvent, author, null /* the logger should not be needed */);

        for (RegattaLogEvent event : regattaLog.getUnrevokedEvents()) {
            if (event instanceof RegattaLogDefineMarkEventImpl) {
                Assert.fail("Define mark not revoked.");
            }
            if (event instanceof RegattaLogDeviceMarkMappingEventImpl) {
                Assert.fail("Device mapping not revoked.");
            }
        }
    }
    
    @Test
    public void testRevokingWithoutDeviceMapping() {
        final RegattaLogDefineMarkEvent defineMarkEvent = new RegattaLogDefineMarkEventImpl(MillisecondsTimePoint.now(), author, 
                MillisecondsTimePoint.now(), UUID.randomUUID(), mark);
        
        regattaLog.add(defineMarkEvent);
        
        regattaLog.revokeDefineMarkEventAndRelatedDeviceMappings(defineMarkEvent, author, null /* the logger should not be needed */);

        for (RegattaLogEvent event : regattaLog.getUnrevokedEvents()) {
            if (event instanceof RegattaLogDefineMarkEventImpl) {
                Assert.fail("Define mark not revoked.");
            }
        }
    }
}