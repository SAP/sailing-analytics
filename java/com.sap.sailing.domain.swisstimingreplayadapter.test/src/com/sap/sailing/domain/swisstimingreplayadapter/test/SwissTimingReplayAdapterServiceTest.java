package com.sap.sailing.domain.swisstimingreplayadapter.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayRace;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayParserImpl;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayServiceImpl;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayToDomainAdapter;
import com.sap.sailing.domain.test.DummyTrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util;

public class SwissTimingReplayAdapterServiceTest {
    
    @Test
    public void testLoadRaceJson() throws Exception {
        String swissTimingUrlText = "/2012_OSG.json";
        InputStream inputStream = getClass().getResourceAsStream(swissTimingUrlText);
        final SwissTimingReplayServiceImpl swissTimingReplayService = new SwissTimingReplayServiceImpl(DomainFactory.INSTANCE);
        List<SwissTimingReplayRace> races = swissTimingReplayService.parseJSONObject(inputStream , swissTimingUrlText);
        assertEquals(201, races.size());
        assertEquals("446483", races.get(0).getRaceId());
        SwissTimingReplayRace race_42 = races.get(42);
        assertEquals("Elliott", race_42.getBoatClass());
        assertEquals("19", race_42.getFlightNumber());
        assertEquals(swissTimingUrlText, race_42.getJsonUrl());
        assertEquals("live.ota.st-sportservice.com/Data/Log?_rsc=SAW010955&_date=02.08.2012&_start=0", race_42.getLink());
        assertEquals("Match 55", race_42.getName());
        assertEquals("6264", race_42.getRaceId());
        assertEquals("SAW010955", race_42.getRsc());
        assertEquals("02.08.2012 15:30", swissTimingReplayService.getStartTimeFormat().format(races.get(42).getStartTime()));
        assertEquals("live.ota.st-sportservice.com/Data/Log?_rsc=SAM009901&_date=30.07.2012&_start=0", races.get(races.size() - 1).getLink());
    }    
    
    @Test
    public void testRaceData_SAW005906_20120805() throws Exception {
        SwissTimingReplayTestListener replayCountListener = new SwissTimingReplayTestListener();
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW005906.20120805.replay"), replayCountListener);
        assertEquals(0, replayCountListener.keyFrameIndexSum);          
        assertEquals(790, replayCountListener.keyFrameIndexPositionCount);
        assertEquals(2013, replayCountListener.eotCount);                    
        assertEquals(2012, replayCountListener.frameCount);                  
        assertEquals(2012, replayCountListener.referenceTimestampCount);     
        assertEquals(2012, replayCountListener.referenceLocationCount);      
        assertEquals(2012, replayCountListener.rsc_cidCount);                
        assertEquals(72432, replayCountListener.competitorsCountSum);       
        assertEquals(72432, replayCountListener.competitorsCount);            
        assertEquals(20120, replayCountListener.markCount);                   
        assertEquals(72432, replayCountListener.trackersCountSum);          
        assertEquals(72432, replayCountListener.trackersCount);               
        assertEquals(40240, replayCountListener.rankingsCountSum);          
        assertEquals(40240, replayCountListener.rankingsCount);               
        assertEquals(321920, replayCountListener.rankingMarkCount);                
    }

    @Test
    public void testRaceData_SAW005905_20120805_EqualsOnlineVersion() throws Exception {
        byte[] localCopy = read(getClass().getResourceAsStream("/SAW005905.20120805.replay"));
        byte[] onlineCopy = read((InputStream) new URL(
                "http://live.ota.st-sportservice.com/Replay?id=446495&_start=0").getContent());
        assertArrayEquals(localCopy, onlineCopy);
    }
    
    private byte[] read(InputStream resourceAsStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read=resourceAsStream.read(buf)) != -1) {
            bos.write(buf, 0, read);
        }
        return bos.toByteArray();
    }

    @Test
    public void testRaceData_SAW005905_20120805() throws Exception {
        SwissTimingReplayTestListener replayCountListener = new SwissTimingReplayTestListener();
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW005905.20120805.replay"), replayCountListener);
        assertEquals(0, replayCountListener.keyFrameIndexSum);          
        assertEquals(715, replayCountListener.keyFrameIndexPositionCount);  
        assertEquals(1156, replayCountListener.eotCount);                    
        assertEquals(1063, replayCountListener.frameCount);                  
        assertEquals(1155, replayCountListener.referenceTimestampCount);     
        assertEquals(1155, replayCountListener.referenceLocationCount);      
        assertEquals(1155, replayCountListener.rsc_cidCount);                
        assertEquals(108, replayCountListener.competitorsCountSum);       
        assertEquals(108, replayCountListener.competitorsCount);            
        assertEquals(63, replayCountListener.markCount);                   
        assertEquals(41580, replayCountListener.trackersCountSum);          
        assertEquals(41580, replayCountListener.trackersCount);               
        assertEquals(1900, replayCountListener.rankingsCountSum);          
        assertEquals(1900, replayCountListener.rankingsCount);               
        assertEquals(9500, replayCountListener.rankingMarkCount);                
    }

    @Test
    public void testRaceData_SAW005905_20120805_online() throws Exception {
        SwissTimingReplayTestListener replayCountListener = new SwissTimingReplayTestListener();
        // race ID is 450053, as extracted from http://live.ota.st-sportservice.com/service?cmd=unity_race_overview&id=91
        byte[] onlineCopy = read((InputStream) new URL(
                "http://live.ota.st-sportservice.com/Replay?id=446495&_start=0").getContent());
        new SwissTimingReplayParserImpl().readData(new ByteArrayInputStream(onlineCopy), replayCountListener);
        assertEquals(0, replayCountListener.keyFrameIndexSum);          
        assertEquals(715, replayCountListener.keyFrameIndexPositionCount);  
        assertEquals(1156, replayCountListener.eotCount);                    
        assertEquals(1063, replayCountListener.frameCount);                  
        assertEquals(1155, replayCountListener.referenceTimestampCount);     
        assertEquals(1155, replayCountListener.referenceLocationCount);      
        assertEquals(1155, replayCountListener.rsc_cidCount);                
        assertEquals(108, replayCountListener.competitorsCountSum);       
        assertEquals(108, replayCountListener.competitorsCount);            
        assertEquals(63, replayCountListener.markCount);                   
        assertEquals(41580, replayCountListener.trackersCountSum);          
        assertEquals(41580, replayCountListener.trackersCount);               
        assertEquals(1900, replayCountListener.rankingsCountSum);          
        assertEquals(1900, replayCountListener.rankingsCount);               
        assertEquals(9500, replayCountListener.rankingMarkCount);                
    }

    @Test
    public void testRaceData_SAW010115_20120811() throws Exception {
        SwissTimingReplayTestListener replayCountListener = new SwissTimingReplayTestListener();
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW010115.20120811.replay"), replayCountListener);
        assertEquals(0, replayCountListener.keyFrameIndexSum);          
        assertEquals(385, replayCountListener.keyFrameIndexPositionCount);  
        assertEquals(387, replayCountListener.eotCount);                    
        assertEquals(386, replayCountListener.frameCount);                  
        assertEquals(386, replayCountListener.referenceTimestampCount);     
        assertEquals(386, replayCountListener.referenceLocationCount);      
        assertEquals(386, replayCountListener.rsc_cidCount);                
        assertEquals(4632, replayCountListener.competitorsCountSum);       
        assertEquals(4632, replayCountListener.competitorsCount);            
        assertEquals(1930, replayCountListener.markCount);                   
        assertEquals(4632, replayCountListener.trackersCountSum);          
        assertEquals(4632, replayCountListener.trackersCount);               
        assertEquals(772, replayCountListener.rankingsCountSum);          
        assertEquals(772, replayCountListener.rankingsCount);               
        assertEquals(2316, replayCountListener.rankingMarkCount);                
    }

    @Test
    public void testRaceData_SAW010955_20120802() throws Exception {
        SwissTimingReplayTestListener replayCountListener = new SwissTimingReplayTestListener();
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW010955.20120802.replay"), replayCountListener);
        assertEquals(0, replayCountListener.keyFrameIndexSum);          
        assertEquals(402, replayCountListener.keyFrameIndexPositionCount);  
        assertEquals(404, replayCountListener.eotCount);                    
        assertEquals(403, replayCountListener.frameCount);                  
        assertEquals(403, replayCountListener.referenceTimestampCount);     
        assertEquals(403, replayCountListener.referenceLocationCount);      
        assertEquals(403, replayCountListener.rsc_cidCount);                
        assertEquals(4836, replayCountListener.competitorsCountSum);       
        assertEquals(4836, replayCountListener.competitorsCount);            
        assertEquals(2015, replayCountListener.markCount);                   
        assertEquals(4836, replayCountListener.trackersCountSum);          
        assertEquals(4836, replayCountListener.trackersCount);               
        assertEquals(806, replayCountListener.rankingsCountSum);          
        assertEquals(806, replayCountListener.rankingsCount);               
        assertEquals(2418, replayCountListener.rankingMarkCount);                
    }

    @Test
    public void testRaceData_SAW010955_20120802_WithDomainAdapter() throws Exception {
        SwissTimingReplayToDomainAdapter replayListener = new SwissTimingReplayToDomainAdapter(null, /* raceName */ "SAW005906",
                "SAW005906", /* boatClass */ null, DomainFactory.INSTANCE, new DummyTrackedRegattaRegistry(), /* useInternalMarkPassingAlgorithm */ false,
                mock(RaceLogResolver.class), EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, /* tracker creator */ null);
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW005906.20120805.replay"), replayListener);
        Iterable<? extends TrackedRace> trackedRaces = replayListener.getTrackedRaces();
        assertFalse(Util.isEmpty(trackedRaces));
        assertEquals(1, Util.size(trackedRaces));
        TrackedRace trackedRace = trackedRaces.iterator().next();
        for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
            assertEquals(Util.size(trackedRace.getRace().getCourse().getWaypoints()), Util.size(trackedRace.getMarkPassings(competitor)));
        }
    }

    @Test
    public void testStartPerformanceDetection() throws Exception {
        SwissTimingReplayToDomainAdapter replayListener = new SwissTimingReplayToDomainAdapter(null, /* raceName */ "SAW005905",
                "SAW005905", /* boatClass */ null, DomainFactory.INSTANCE, new DummyTrackedRegattaRegistry(), /* useInternalMarkPassingAlgorithm */ false,
                mock(RaceLogResolver.class), EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, /* tracker creator */ null);
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW005905.20120805.replay"), replayListener);
        Iterable<? extends TrackedRace> trackedRaces = replayListener.getTrackedRaces();
        TrackedRace trackedRace = trackedRaces.iterator().next();
        for (Entry<Competitor, Boat> competitorAndBoatEntry : trackedRace.getRace().getCompetitorsAndTheirBoats().entrySet()) {
            assertSame(DomainFactory.INSTANCE.getBaseDomainFactory().getOrCreateBoatClass("470"), competitorAndBoatEntry.getValue().getBoatClass());
            Distance distanceToLineAtStart = trackedRace.getDistanceToStartLine(competitorAndBoatEntry.getKey(), trackedRace.getStartOfRace());
            assertTrue(distanceToLineAtStart.getMeters() > 0);
            assertTrue(distanceToLineAtStart.getMeters() < 20);
        }
    }

    @Test
    @Ignore
    public void printRaceData_SAW005906_20120805() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW005906.20120805.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printRaceData_SAM009903_20120731_sapsailing_replay() throws Exception {
        new SwissTimingReplayParserImpl().readData(
                getClass().getResourceAsStream("/SAM009903.20120731_sapsailing.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printRaceData_SAM009903_20120731_replay() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAM009903.20120731.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printReadRaceData_SAW010955_20120802() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW010955.20120802.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printReadRaceData_SAM002901() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAM002901.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printReadRaceData_SAM009904_20120731() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAM009904.20120731.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printRaceData_SAW102101_20120807() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAW102101.20120807.replay"),
                new SwissTimingReplayPrintListener());
    }

    @Test
    @Ignore
    public void printRaceData_SAM102101_20120807() throws Exception {
        new SwissTimingReplayParserImpl().readData(getClass().getResourceAsStream("/SAM102101.20120807.replay"),
                new SwissTimingReplayPrintListener());
    }

}
