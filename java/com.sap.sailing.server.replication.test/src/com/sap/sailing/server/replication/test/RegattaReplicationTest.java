package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
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
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, regattaId, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertTrue(Util.isEmpty((replicatedRegatta.getSeries())));
        assertTrue(Util.isEmpty(replicatedRegatta.getCourseAreas()));
        assertTrue(regattaId.equals(replicatedRegatta.getId()));
        assertNull(replicatedRegatta.getRegattaConfiguration());
    }

    @Test
    public void testSpecificRegattaReplicationWithDifferentBoatsOfCompetitorsCanChangePerRaceSettings() throws InterruptedException {
        final String baseEventName = "Kiel Week 2012";
        final String boatClass1Name = "49er";
        final String boatClass2Name = "49erFX";
        final boolean canBoatsOfCompetitorsChangePerRaceRegatta1 = true;
        final boolean canBoatsOfCompetitorsChangePerRaceRegatta2 = false;
        final Iterable<Series> series = Collections.emptyList();
        final UUID regattaId1 = UUID.randomUUID();
        final UUID regattaId2 = UUID.randomUUID();
        Regatta masterRegatta1 = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClass1Name),
                boatClass1Name, canBoatsOfCompetitorsChangePerRaceRegatta1, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, regattaId1, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        Regatta masterRegatta2 = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClass2Name),
                boatClass2Name, canBoatsOfCompetitorsChangePerRaceRegatta2, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, regattaId2, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);

        Thread.sleep(1000);

        Regatta replicatedRegatta1 = replica.getRegatta(new RegattaName(masterRegatta1.getName()));
        assertNotNull(replicatedRegatta1);
        assertEquals(replicatedRegatta1.canBoatsOfCompetitorsChangePerRace(), canBoatsOfCompetitorsChangePerRaceRegatta1);
        Regatta replicatedRegatta2 = replica.getRegatta(new RegattaName(masterRegatta2.getName()));
        assertNotNull(replicatedRegatta2);
        assertEquals(replicatedRegatta2.canBoatsOfCompetitorsChangePerRace(), canBoatsOfCompetitorsChangePerRaceRegatta2);
    }

    @Test
    public void testUpdateSpecificRegattaReplicationForCourseArea() throws InterruptedException {
        Regatta replicatedRegatta;
        final UUID alphaCourseAreaId = UUID.randomUUID();
        final UUID tvCourseAreaId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        Event event = master.addEvent("Event", /* eventDescription */ null, eventStartDate, eventEndDate, "Venue", true, UUID.randomUUID());
        master.addCourseAreas(event.getId(), new String[] {"Alpha"}, new UUID[] {alphaCourseAreaId},
                /* centerPositions */ new Position[] {null}, /* radiuses */ new Distance[] {null});
        master.addCourseAreas(event.getId(), new String[] {"TV"}, new UUID[] {tvCourseAreaId}, /* centerPositions */ new Position[] {null}, /* radiuses */ new Distance[] {null});
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName("Kiel Week 2012", "49er"), "49er",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Collections.<Series> emptyList(), /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null,
                currentCourseAreaId, null, /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(Util.isEmpty(replicatedRegatta.getCourseAreas()));
        // Test for 'alpha'
        currentCourseAreaId = alphaCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null,
                currentCourseAreaId, null, /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(1, Util.size(replicatedRegatta.getCourseAreas()));
        assertEquals(currentCourseAreaId, replicatedRegatta.getCourseAreas().iterator().next().getId());
        // Test for 'tv'
        currentCourseAreaId = tvCourseAreaId;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null,
                currentCourseAreaId, null, /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertEquals(1, Util.size(replicatedRegatta.getCourseAreas()));
        assertEquals(currentCourseAreaId, replicatedRegatta.getCourseAreas().iterator().next().getId());
        // Test back to 'null'
        currentCourseAreaId = null;
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, null,
                /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(Util.isEmpty(replicatedRegatta.getCourseAreas()));
    }

    public void testUpdateSpecificRegattaReplicationForProcedureAndCourseDesignerAndConfig() throws InterruptedException {
        Regatta replicatedRegatta;
        final UUID alphaCourseAreaId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        Event event = master.addEvent("Event", /* eventDescription */ null, eventStartDate, eventEndDate, "Venue", true, UUID.randomUUID());
        master.addCourseAreas(event.getId(), new String[] {"Alpha"}, new UUID[] {alphaCourseAreaId}, /* centerPositions */ new Position[0], /* radiuses */ new Distance[0]);
        UUID currentCourseAreaId = null;
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName("RR", "49er"), "49er",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Collections.<Series> emptyList(), true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), currentCourseAreaId,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        // Test for 'null'
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, null,
                /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
        Thread.sleep(1000);
        replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertNull(replicatedRegatta.getRegattaConfiguration());
        // Test for values
        RegattaConfigurationImpl config = new RegattaConfigurationImpl();
        config.setDefaultCourseDesignerMode(CourseDesignerMode.BY_MARKS);
        master.apply(new UpdateSpecificRegatta(masterRegatta.getRegattaIdentifier(), /*startDate*/ null, /*endDate*/ null, currentCourseAreaId, config,
                /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* registrationLinkSecret */ null, CompetitorRegistrationType.CLOSED));
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
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Arrays.asList(new Series[] { qualification, finals, medal }), /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
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
        assertTrue(Util.isEmpty(replicatedRegatta.getCourseAreas()));
    }

    @Test
    public void testRegattaUpdateSeriesWithNewSeries() throws InterruptedException {
        final String baseEventName = "Extreme Sailing Series 2020";
        final String boatClassName = "Extreme40";
        final List<String> emptyRaceColumnNamesList = Collections.emptyList();
        Series qualification = new SeriesImpl("Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true,
                Arrays.asList(new Fleet[] { new FleetImpl("Yellow"), new FleetImpl("Blue") }), emptyRaceColumnNamesList, /* trackedRegattaRegistry */ null);
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Arrays.asList(new Series[] { qualification }), /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
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
                finals.isStartsWithZeroScore(), finals.isFirstColumnNonDiscardableCarryForward(),
                finals.hasSplitFleetContiguousScoring(), finals.hasCrossFleetMergedRanking(), finals.getMaximumNumberOfDiscards(), finals.isOneAlwaysStaysOne(), Arrays.asList(new FleetDTO[] { finalsGoldFleet })));
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
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Arrays.asList(new Series[] { qualification }), /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
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
                qualification.isStartsWithZeroScore(), qualification.isFirstColumnNonDiscardableCarryForward(),
                qualification.hasSplitFleetContiguousScoring(), qualification.hasCrossFleetMergedRanking(), qualification.getMaximumNumberOfDiscards(),
                qualification.isOneAlwaysStaysOne(), Arrays.asList(new FleetDTO[] {  })));
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
        CourseArea masterCourseArea = master.addCourseAreas(masterEvent.getId(), new String[] {courseArea}, new UUID[] {UUID.randomUUID()},
                /* centerPositions */ new Position[] {null}, /* radiuses */ new Distance[] {null})[0];
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(eventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(), series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                masterCourseArea.getId(), /* buoyZoneRadiusInHullLengths */ 2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertNotNull(replicatedEvent);
        CourseArea replicatedCourseArea = replica.getCourseArea(masterCourseArea.getId());
        assertNotNull(replicatedCourseArea);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertTrue(Util.isEmpty((replicatedRegatta.getSeries())));
        assertNotNull(replicatedRegatta.getCourseAreas());
        assertEquals(1, Util.size(replicatedRegatta.getCourseAreas()));
        assertEquals(masterCourseArea.getId(), replicatedRegatta.getCourseAreas().iterator().next().getId());
        assertEquals(masterCourseArea.getName(), replicatedRegatta.getCourseAreas().iterator().next().getName());
    }
}
