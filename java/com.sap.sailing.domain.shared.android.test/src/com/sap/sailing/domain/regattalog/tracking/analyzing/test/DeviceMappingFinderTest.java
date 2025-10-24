package com.sap.sailing.domain.regattalog.tracking.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogCloseOpenEndedDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceCompetitorMappingFinder;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class DeviceMappingFinderTest extends AbstractRegattaLogTrackingTest {
    private final Competitor competitor = new CompetitorImpl("comp", "Comp", "KYC", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
    private final Competitor competitor2 = new CompetitorImpl("comp2", "Comp2", "KYC", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
    private final DeviceIdentifier device = new SmartphoneImeiIdentifierImpl("imei");
    
    private int time = 0;
    
    private List<DeviceMappingWithRegattaLogEvent<Competitor>> getMappings() {
        return new RegattaLogDeviceCompetitorMappingFinder(log).analyze().get(competitor);
    }
    
    private TimePoint t() {
        return new MillisecondsTimePoint(time++);
    }
    
    private TimePoint t(Long millis) {
        return millis == null ? null : new MillisecondsTimePoint(millis);
    }
    
    private Serializable addMapping(AbstractLogEventAuthor author, DeviceIdentifier device, Long from, Long to, Competitor item) {
        RegattaLogEvent mapping = new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, UUID.randomUUID(), item, device,
                t(from), t(to));
        log.add(mapping);
        return mapping.getId();
    }
    
    private void closeMapping(AbstractLogEventAuthor author, DeviceIdentifier device, Serializable mappingId, long closingTimePointInclusiveAsMillis) {
        RegattaLogEvent mapping = new RegattaLogCloseOpenEndedDeviceMappingEventImpl(t(), author, t(), UUID.randomUUID(), mappingId,
                new MillisecondsTimePoint(closingTimePointInclusiveAsMillis));
        log.add(mapping);
    }
    
    /**
     * Expecting that a single, closed mapping interval spanning a fix will be cut into two intervals excluding the fix time point
     */
    @Test
    public void testFixRemovalForMiddleOfInterval() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        addMapping(author, device, 100l, 200l, competitor);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(150));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(2, deviceMappings.size());
            boolean found100 = false;
            for (final DeviceMappingWithRegattaLogEvent<Competitor> mapping : deviceMappings) {
                if (mapping.getTimeRange().from().asMillis() == 100) {
                    found100 = true;
                    assertEquals(150 /* exclusive */, mapping.getTimeRange().to().asMillis());
                } else {
                    assertEquals(151, mapping.getTimeRange().from().asMillis());
                    assertEquals(201 /* exclusive vs. inclusive */, mapping.getTimeRange().to().asMillis());
                }
            }
            assertTrue(found100);
        }
    }
    
    @Test
    public void testFixRemovalForMiddleOfIntervalClosedSeparately() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        final Serializable mappingId = addMapping(author, device, 100l, null, competitor);
        closeMapping(author, device, mappingId, 200);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201 /* exclusive vs. inclusive */), mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(150));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(2, deviceMappings.size());
            for (final DeviceMappingWithRegattaLogEvent<Competitor> mapping : deviceMappings) {
                if (mapping.getTimeRange().from().asMillis() == 100) {
                    assertEquals(150 /* exclusive */, mapping.getTimeRange().to().asMillis());
                } else {
                    assertEquals(151, mapping.getTimeRange().from().asMillis());
                    assertEquals(201 /* exclusive vs. inclusive */, mapping.getTimeRange().to().asMillis());
                }
            }
        }
    }
    
    @Test
    public void testFixRemovalForStartOfInterval() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        addMapping(author, device, 100l, 200l, competitor);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(100));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            assertEquals(101, deviceMappings.get(0).getTimeRange().from().asMillis());
            assertEquals(201 /* exclusive vs. inclusive */, deviceMappings.get(0).getTimeRange().to().asMillis());
        }
    }
    
    @Test
    public void testFixRemovalForStartOfIntervalClosedSeparately() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        final Serializable mappingId = addMapping(author, device, 100l, null, competitor);
        closeMapping(author, device, mappingId, 200);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(100));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            assertEquals(101, deviceMappings.get(0).getTimeRange().from().asMillis());
            assertEquals(201 /* exclusive */, deviceMappings.get(0).getTimeRange().to().asMillis());
        }
    }

    @Test
    public void testFixRemovalForEndOfInterval() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        addMapping(author, device, 100l, 200l, competitor);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(200));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            assertEquals(100, deviceMappings.get(0).getTimeRange().from().asMillis());
            assertEquals(200 /* exclusive */, deviceMappings.get(0).getTimeRange().to().asMillis());
        }
    }
    
    @Test
    public void testFixRemovalForEndOfIntervalClosedSeparately() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        final Serializable mappingId = addMapping(author, device, 100l, null, competitor);
        closeMapping(author, device, mappingId, 200);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(201) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(200));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            assertEquals(100, deviceMappings.get(0).getTimeRange().from().asMillis());
            assertEquals(200 /* exclusive */, deviceMappings.get(0).getTimeRange().to().asMillis());
        }
    }

    @Test
    public void testFixRemovalForMillisecondInterval() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        addMapping(author, device, 100l, 100l, competitor);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(101) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(100));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertTrue(mappings.isEmpty());
        }
    }
    
    @Test
    public void testFixRemovalForMillisecondIntervalClosedSeparately() throws NotRevokableException {
        final RegattaLogDeviceCompetitorMappingFinder finder = new RegattaLogDeviceCompetitorMappingFinder(log);
        final Serializable mappingId = addMapping(author, device, 100l, null, competitor);
        closeMapping(author, device, mappingId, 100);
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertEquals(1, mappings.size());
            assertTrue(mappings.containsKey(competitor));
            final List<DeviceMappingWithRegattaLogEvent<Competitor>> deviceMappings = mappings.get(competitor);
            assertEquals(1, deviceMappings.size());
            final DeviceMappingWithRegattaLogEvent<Competitor> mapping = deviceMappings.iterator().next();
            assertEquals(new MillisecondsTimePoint(100), mapping.getTimeRange().from());
            assertEquals(new MillisecondsTimePoint(101) /* exclusive vs. inclusive */, mapping.getTimeRange().to());
        }
        finder.removeTimePointFromMapping(competitor, new MillisecondsTimePoint(100));
        {
            final Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = finder.analyze();
            assertTrue(mappings.isEmpty());
        }
    }

    @Test
    public void notDisturbedByMappingsForOtherItemsAndDevices() {
    	//two mappings for first competitor found? (competitor2 mappings should not "distract")
        addMapping(author, device, 10L, 40L, competitor);
        addMapping(author, device, 10L, 40L, competitor2);
        addMapping(author, device, 20L, 30L, competitor2);
        addMapping(author, device, 50L, 90L, competitor);
        Assertions.assertEquals(2, getMappings().size());
        
        //non-overlap with new device
        addMapping(author, new SmartphoneImeiIdentifierImpl("imei2"), 110L, 150L, competitor);
        Assertions.assertEquals(3, getMappings().size());
    }
    
    @Test
    public void openRangesAreClosed() {
    	//close one range
    	Serializable id = addMapping(author, device, 10L, null, competitor);
    	closeMapping(author, device, id, 20);
        List<DeviceMappingWithRegattaLogEvent<Competitor>> mappings = getMappings();
    	assertEquals(1, mappings.size());
        DeviceMappingWithRegattaLogEvent<Competitor> mapping = mappings.get(0);
    	assertEquals(10, mapping.getTimeRange().from().asMillis());
    	assertEquals(21 /* exclusive vs. inclusive */, mapping.getTimeRange().to().asMillis());
    	
    	//another independent range
    	id = addMapping(author, device, 0L, null, competitor);
        closeMapping(author, device, id, 5);
    	mappings = getMappings();
    	assertEquals(2, mappings.size());
    	
    	//another closing mapping, that should take precedence (because added later)
        closeMapping(author, device, id, 3);
    	assertEquals(2, getMappings().size());
    	assertEquals(4 /* exclusive vs. inclusive */, getMappings().get(1).getTimeRange().to().asMillis());
    	
    	//open-ended at end, not closed
    	addMapping(author, device, 30L, null, competitor);
    	assertEquals(3, getMappings().size());
    }
}
