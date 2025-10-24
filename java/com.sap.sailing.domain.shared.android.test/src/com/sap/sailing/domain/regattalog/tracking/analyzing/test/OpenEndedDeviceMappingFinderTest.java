package com.sap.sailing.domain.regattalog.tracking.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.OpenEndedDeviceMappingFinder;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class OpenEndedDeviceMappingFinderTest extends AbstractRegattaLogTrackingTest {
    private OpenEndedDeviceMappingFinder finder;
    private Mark mappedToMark;
    private UUID deviceUuid;
    
    @BeforeEach
    public void setUp() {
        mappedToMark = new MarkImpl(UUID.randomUUID(), "Random Mark");
        deviceUuid = UUID.randomUUID();
        finder = new OpenEndedDeviceMappingFinder(log, mappedToMark, deviceUuid.toString());
    }
    
    @Test
    public void findNoOpenEndedMappingBecauseNoMappingExistsAtAll() {
        assertNull(finder.analyze()); // no event in the log, so result is expected to be null
    }
    
    @Test
    public void findNoOpenEndedMappingBecauseAllMappingsAreClosed() {
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent1 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now(), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(Duration.ONE_HOUR));
        log.add(mappingEvent1);
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent2 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now().plus(Duration.ONE_YEAR), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(Duration.ONE_YEAR).plus(Duration.ONE_HOUR));
        log.add(mappingEvent2);
        assertNull(finder.analyze()); // both events are closed; expect result to be null
    }
    
    @Test
    public void findOpenEndedMappingWhenItsLastOfSeveral() {
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent1 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now(), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(Duration.ONE_HOUR));
        log.add(mappingEvent1);
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent2 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now().plus(Duration.ONE_YEAR), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), /* open end */ null);
        log.add(mappingEvent2);
        assertEquals(mappingEvent2.getId(), finder.analyze());
    }
    
    @Test
    public void findOpenEndedMappingWhenItsFirstOfSeveral() {
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent1 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now(), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), /* open end */ null);
        log.add(mappingEvent1);
        final RegattaLogDeviceMarkMappingEventImpl mappingEvent2 = new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now().plus(Duration.ONE_YEAR), author, mappedToMark,
                new SmartphoneUUIDIdentifierImpl(deviceUuid), MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(Duration.ONE_YEAR).plus(Duration.ONE_HOUR));
        log.add(mappingEvent2);
        assertEquals(mappingEvent1.getId(), finder.analyze());
    }
    
}
