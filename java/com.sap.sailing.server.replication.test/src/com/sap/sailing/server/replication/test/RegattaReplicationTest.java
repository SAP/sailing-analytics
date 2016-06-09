package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RegattaReplicationTest extends AbstractServerReplicationTest {
    @Test
    public void testSimpleSpecificRegattaReplication() throws InterruptedException {
        final String baseEventName = "Kiel Week 2012";
        final String boatClassName = "49er";
        final Iterable<Series> series = Collections.emptyList();
        final UUID regattaId = UUID.randomUUID();
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName,
                /*startDate*/ null, /*endDate*/ null, regattaId, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        
        Thread.sleep(1000);
        
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertTrue(Util.isEmpty((replicatedRegatta.getSeries())));
        assertNull(replicatedRegatta.getDefaultCourseArea());        
        assertTrue(regattaId.equals(replicatedRegatta.getId()));
        assertNull(replicatedRegatta.getRegattaConfiguration());
    }
    
    @Test
    public void testUpdateSpecificRegattaReplicationForCourseArea() throws InterruptedException {
        Regatta replicatedRegatta;
        
        final UUID alphaCourseAreaId = UUID.randomUUID();
        final UUID tvCourseAreaId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());

        Event event = master.addEvent("Event", /* eventDescription */ null, eventStartDate, eventEndDate, "Venue", true, UUID.randomUUID());
        master.addCourseAreas(event.getId(), new String[] {"Alpha"}, new UUID[] {alphaCourseAreaId});
        master.addCourseAreas(event.getId(), new String[] {"TV"}, new UUID[] {tvCourseAreaId});
        
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName("Kiel Week 2012", "49er"), "49er", 
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID(), Collections.<Series>emptyList(),
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, 
                currentCourseAreaId, null, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getDefaultCourseArea());
        
        // Test for 'alpha'
        currentCourseAreaId = alphaCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, 
                currentCourseAreaId, null, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(currentCourseAreaId, replicatedRegatta.getDefaultCourseArea().getId());
        
        // Test for 'tv'
        currentCourseAreaId = tvCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null,
                currentCourseAreaId, null, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(currentCourseAreaId, replicatedRegatta.getDefaultCourseArea().getId());
        
        // Test back to 'null'
        currentCourseAreaId = null;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, null, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getDefaultCourseArea());
    }
    
    @Test
    public void testRegattaToEventAssociationBeingReplicated() throws InterruptedException {
        final UUID tvCourseAreaId = UUID.randomUUID();
        final UUID golfCourseAreaId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        Event event = master.addEvent("Event", /* eventDescription */ null, eventStartDate, eventEndDate, "Venue", /*isPublic*/true, UUID.randomUUID());
        master.addCourseAreas(event.getId(), new String[] {"TV"}, new UUID[] {tvCourseAreaId});
        master.addCourseAreas(event.getId(), new String[] {"Golf"}, new UUID[] {golfCourseAreaId});
        final String regattaName = RegattaImpl.getDefaultName("Kiel Week 2012", "49er");
        Regatta masterRegatta = master.createRegatta(regattaName, "49er", /*startDate*/ null, /*endDate*/ null, UUID.randomUUID(), Collections.<Series>emptyList(),
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), tvCourseAreaId, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        event = master.getEvent(event.getId());
        assertTrue(event.getRegattas().iterator().hasNext());
        assertEquals(regattaName, event.getRegattas().iterator().next().getName());
        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(event.getId());
        Iterator<Regatta> regattasInReplicatedEvent = replicatedEvent.getRegattas().iterator();
        assertTrue(regattasInReplicatedEvent.hasNext());
        assertEquals(regattaName, regattasInReplicatedEvent.next().getName());
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, golfCourseAreaId, null, /* useStartTimeInference */ true));
    }
    
    @Test
    public void testUpdateSpecificRegattaReplicationForProcedureAndCourseDesignerAndConfig() throws InterruptedException {
        Regatta replicatedRegatta;
        
        final UUID alphaCourseAreaId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        
        Event event = master.addEvent("Event", /* eventDescription */ null, eventStartDate, eventEndDate, "Venue", true, UUID.randomUUID());
        master.addCourseAreas(event.getId(), new String[] {"Alpha"}, new UUID[] {alphaCourseAreaId});
        
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName("RR", "49er"), "49er", /*startDate*/ null, /*endDate*/ null, UUID.randomUUID(), Collections.<Series>emptyList(),
                true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, null, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getRegattaConfiguration());
        
        // Test for values
        RegattaConfigurationImpl config = new RegattaConfigurationImpl();
        config.setDefaultCourseDesignerMode(CourseDesignerMode.BY_MARKS);
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, config, /* useStartTimeInference */ true));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNotNull(replicatedRegatta.getRegattaConfiguration());
        assertEquals(CourseDesignerMode.BY_MARKS, replicatedRegatta.getRegattaConfiguration().getDefaultCourseDesignerMode());
    }

    @Test
    public void testDefaultRegattaReplication() throws InterruptedException {
        final String baseEventName = "Kiel Week 2012";
        final String boatClassName = "49er";
        final UUID regattaId = UUID.randomUUID();
        Regatta masterRegatta = master.getOrCreateDefaultRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, regattaId);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(regattaId.equals(replicatedRegatta.getId()));
    }
    
    @Test
    public void testSpecificRegattaReplicationWithTwoEmptySeries() throws InterruptedException {
        final String baseEventName = "Kiel Week 2012";
        final String boatClassName = "49er";
        final List<String> emptyRaceColumnNamesList = Collections.emptyList();
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Series finals = new SeriesImpl("Finals", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Gold", 1), new FleetImpl("Silver", 2) }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Series medal = new SeriesImpl("Medal", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Medal") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, /*startDate*/ null, /*endDate*/ null,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification, finals, medal }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertFalse(Util.isEmpty((replicatedRegatta.getSeries())));
        Iterator<? extends Series> seriesIter = replicatedRegatta.getSeries().iterator();
        Series replicatedQualification = seriesIter.next();
        assertEquals("Qualification", replicatedQualification.getName());
        assertEquals(2, Util.size(replicatedQualification.getFleets()));
        assertNotNull(replicatedQualification.getFleetByName("Yellow"));
        assertNotNull(replicatedQualification.getFleetByName("Blue"));
        assertEquals(0, replicatedQualification.getFleetByName("Yellow").compareTo(replicatedQualification.getFleetByName("Blue")));
        Series replicatedFinals = seriesIter.next();
        assertEquals("Finals", replicatedFinals.getName());
        assertEquals(2, Util.size(replicatedFinals.getFleets()));
        assertNotNull(replicatedFinals.getFleetByName("Silver"));
        assertNotNull(replicatedFinals.getFleetByName("Gold"));
        assertEquals(1, replicatedFinals.getFleetByName("Gold").getOrdering());
        assertEquals(2, replicatedFinals.getFleetByName("Silver").getOrdering());
        Series replicatedMedal = seriesIter.next();
        assertEquals("Medal", replicatedMedal.getName());
        assertEquals(1, Util.size(replicatedMedal.getFleets()));
        assertNotNull(replicatedMedal.getFleetByName("Medal"));
        assertNull(replicatedRegatta.getDefaultCourseArea());
    }
    
    @Test
    public void testRegattaUpdateSeriesWithNewSeries() throws InterruptedException {
        final String baseEventName = "Extreme Sailing Series 2020";
        final String boatClassName = "Extreme40";
        final List<String> emptyRaceColumnNamesList = Collections.emptyList();
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, /*startDate*/ null, /*endDate*/ null,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertFalse(Util.isEmpty((replicatedRegatta.getSeries())));
        Iterator<? extends Series> seriesIter = replicatedRegatta.getSeries().iterator();
        Series replicatedQualification = seriesIter.next();
        assertEquals("Qualification", replicatedQualification.getName());
        assertEquals(2, Util.size(replicatedQualification.getFleets()));
        assertFalse(seriesIter.hasNext());
        Series finals = new SeriesImpl("Finals", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Gold", 1) }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        FleetDTO finalsGoldFleet = new FleetDTO("Gold", 1, Color.GRAY);
        master.apply(new UpdateSeries(masterRegatta.getRegattaIdentifier(), finals.getName(), finals.getName(), finals.isMedal(), finals.isFleetsCanRunInParallel(),
                new int[] {},
                finals.isStartsWithZeroScore(), finals.isFirstColumnIsNonDiscardableCarryForward(),
                finals.hasSplitFleetContiguousScoring(), Arrays.asList(new FleetDTO[] { finalsGoldFleet })));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertFalse(Util.isEmpty((replicatedRegatta.getSeries())));
        seriesIter = replicatedRegatta.getSeries().iterator();
        replicatedQualification = seriesIter.next();
        assertEquals("Qualification", replicatedQualification.getName());
        assertEquals(2, Util.size(replicatedQualification.getFleets()));
        assertTrue(seriesIter.hasNext());
        Series replicatedFinals = seriesIter.next();
        assertEquals("Finals", replicatedFinals.getName());
        assertEquals(1, Util.size(replicatedFinals.getFleets()));
        assertNotNull(replicatedFinals.getFleetByName("Gold"));
        assertEquals(1, replicatedFinals.getFleetByName("Gold").getOrdering());
    }
    
    @Test
    public void testSeriesNameChangeReplicationTest() throws InterruptedException {
        final String baseEventName = "Extreme Sailing Series 2021";
        final String boatClassName = "Extreme40";
        final List<String> emptyRaceColumnNamesList = Collections.emptyList();
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, /*startDate*/ null, /*endDate*/ null,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        Iterator<? extends Series> seriesIter = replicatedRegatta.getSeries().iterator();
        Series replicatedQualification = seriesIter.next();
        assertEquals("Qualification", replicatedQualification.getName());
        master.apply(new UpdateSeries(masterRegatta.getRegattaIdentifier(), qualification.getName(), "Simons Quali", 
                qualification.isMedal(), qualification.isFleetsCanRunInParallel(),
                new int[] {},
                qualification.isStartsWithZeroScore(), qualification.isFirstColumnIsNonDiscardableCarryForward(),
                qualification.hasSplitFleetContiguousScoring(), Arrays.asList(new FleetDTO[] {  })));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        seriesIter = replicatedRegatta.getSeries().iterator();
        replicatedQualification = seriesIter.next();
        assertEquals("Simons Quali", replicatedQualification.getName());
    }
    
    @Test
    public void testSpecificRegattaReplicationWithCourseArea() throws InterruptedException {
        final String eventName = "ESS Singapur";
        final String venueName = "Singapur, Singapur";
        final boolean isPublic = false;
        final String boatClassName = "X40";
        final Iterable<Series> series = Collections.emptyList();
        final String courseArea = "Alpha";
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        Event masterEvent = master.addEvent(eventName, /* eventDescription */ null, eventStartDate, eventEndDate, venueName, isPublic, UUID.randomUUID());
        CourseArea masterCourseArea = master.addCourseAreas(masterEvent.getId(), new String[] {courseArea}, new UUID[] {UUID.randomUUID()})[0];
        
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(eventName, boatClassName), boatClassName, /*startDate*/ null, /*endDate*/ null, UUID.randomUUID(), series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), masterCourseArea.getId(), /* useStartTimeInference */ true, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertNotNull(replicatedEvent);
        CourseArea replicatedCourseArea = replica.getCourseArea(masterCourseArea.getId());
        assertNotNull(replicatedCourseArea);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertTrue(Util.isEmpty((replicatedRegatta.getSeries())));
        assertNotNull(replicatedRegatta.getDefaultCourseArea());
        assertEquals(masterCourseArea.getId(), replicatedRegatta.getDefaultCourseArea().getId());
        assertEquals(masterCourseArea.getName(), replicatedRegatta.getDefaultCourseArea().getName());
    }
}
