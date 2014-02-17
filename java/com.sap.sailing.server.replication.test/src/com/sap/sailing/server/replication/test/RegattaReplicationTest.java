package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
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
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;

public class RegattaReplicationTest extends AbstractServerReplicationTest {
    @Test
    public void testSimpleSpecificRegattaReplication() throws InterruptedException {
        final String baseEventName = "Kiel Week 2012";
        final String boatClassName = "49er";
        final Iterable<Series> series = Collections.emptyList();
        final UUID regattaId = UUID.randomUUID();
        Regatta masterRegatta = master.createRegatta(baseEventName, boatClassName, regattaId, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null);
        
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
        
        Event event = master.addEvent("Event", "Venue", ".", true, UUID.randomUUID());
        master.addCourseArea(event.getId(), "Alpha", alphaCourseAreaId);
        master.addCourseArea(event.getId(), "TV", tvCourseAreaId);
        
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta("Kiel Week 2012", "49er", UUID.randomUUID(), Collections.<Series>emptyList(),
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId);
        
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, null));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getDefaultCourseArea());
        
        // Test for 'alpha'
        currentCourseAreaId = alphaCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, null));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(currentCourseAreaId, replicatedRegatta.getDefaultCourseArea().getId());
        
        // Test for 'tv'
        currentCourseAreaId = tvCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, null));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(currentCourseAreaId, replicatedRegatta.getDefaultCourseArea().getId());
        
        // Test back to 'null'
        currentCourseAreaId = null;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, null));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getDefaultCourseArea());
    }
    
    @Test
    public void testRegattaToEventAssociationBeingReplicated() throws InterruptedException {
        final UUID tvCourseAreaId = UUID.randomUUID();
        final UUID golfCourseAreaId = UUID.randomUUID();
        Event event = master.addEvent("Event", "Venue", ".", /*isPublic*/true, UUID.randomUUID());
        master.addCourseArea(event.getId(), "TV", tvCourseAreaId);
        master.addCourseArea(event.getId(), "Golf", golfCourseAreaId);
        Regatta masterRegatta = master.createRegatta("Kiel Week 2012", "49er", UUID.randomUUID(), Collections.<Series>emptyList(),
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), tvCourseAreaId);
        event = master.getEvent(event.getId());
        assertTrue(event.getRegattas().iterator().hasNext());
        assertEquals("Kiel Week 2012", event.getRegattas().iterator().next().getBaseName());
        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(event.getId());
        Iterator<Regatta> regattasInReplicatedEvent = replicatedEvent.getRegattas().iterator();
        assertTrue(regattasInReplicatedEvent.hasNext());
        assertEquals("Kiel Week 2012", regattasInReplicatedEvent.next().getBaseName());
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), golfCourseAreaId, null));
    }
    
    @Test
    public void testUpdateSpecificRegattaReplicationForProcedureAndCourseDesignerAndConfig() throws InterruptedException {
        Regatta replicatedRegatta;
        
        final UUID alphaCourseAreaId = UUID.randomUUID();
        
        Event event = master.addEvent("Event", "Venue", ".", true, UUID.randomUUID());
        master.addCourseArea(event.getId(), "Alpha", alphaCourseAreaId);
        
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta("RR", "49er", UUID.randomUUID(), Collections.<Series>emptyList(),
                true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId);
        
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, null));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getRegattaConfiguration());
        
        // Test for values
        RegattaConfigurationImpl config = new RegattaConfigurationImpl();
        config.setDefaultCourseDesignerMode(CourseDesignerMode.BY_MARKS);
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), currentCourseAreaId, config));
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
        Regatta masterRegatta = master.getOrCreateDefaultRegatta(baseEventName, boatClassName, regattaId);
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
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false,
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Series finals = new SeriesImpl("Finals", /* isMedal */ false,
                Arrays.asList(new Fleet[] { new FleetImpl("Gold", 1), new FleetImpl("Silver", 2) }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Series medal = new SeriesImpl("Medal", /* isMedal */ true,
                Arrays.asList(new Fleet[] { new FleetImpl("Medal") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(baseEventName, boatClassName,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification, finals, medal }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null);
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
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false,
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(baseEventName, boatClassName,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null);
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
        Series finals = new SeriesImpl("Finals", /* isMedal */ false,
                Arrays.asList(new Fleet[] { new FleetImpl("Gold", 1) }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        FleetDTO finalsGoldFleet = new FleetDTO("Gold", 1, Color.GRAY);
        master.apply(new UpdateSeries(masterRegatta.getRegattaIdentifier(), finals.getName(), finals.getName(), finals.isMedal(),
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
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false,
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(baseEventName, boatClassName,
                UUID.randomUUID(), Arrays.asList(new Series[] { qualification }), /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        Iterator<? extends Series> seriesIter = replicatedRegatta.getSeries().iterator();
        Series replicatedQualification = seriesIter.next();
        assertEquals("Qualification", replicatedQualification.getName());
        master.apply(new UpdateSeries(masterRegatta.getRegattaIdentifier(), qualification.getName(), "Simons Quali", 
                qualification.isMedal(),
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
        final String publicationUrl = "http://ess40.sapsailing.com";
        final boolean isPublic = false;
        final String boatClassName = "X40";
        final Iterable<Series> series = Collections.emptyList();
        final String courseArea = "Alpha";
        Event masterEvent = master.addEvent(eventName, venueName, publicationUrl, isPublic, UUID.randomUUID());
        CourseArea masterCourseArea = master.addCourseArea(masterEvent.getId(), courseArea, UUID.randomUUID());
        
        Regatta masterRegatta = master.createRegatta(eventName, boatClassName, UUID.randomUUID(), series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), masterCourseArea.getId());
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
