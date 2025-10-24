package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.orc.RegattaLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RegattaLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveLegAdapter;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveRankingMetric;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * An {@link ORCPerformanceCurveRankingMetric} observes its {@link TrackedRace} for the regatta log
 * and the race logs, regarding the attachment and detachment of such logs, as well as regarding
 * the appearance of new certificate assignment events in those logs. This test verifies that
 * the dynamics work as expected.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TestORCRankingMetricCollaborationWithRaceLogAndRegattaLog {
    private Boat boat1, boat2;
    private CompetitorWithBoat competitor1, competitor2;
    private DynamicTrackedRaceImpl trackedRace;
    private RaceLog raceLog;
    private RegattaLog regattaLog;
    private ORCPerformanceCurveRankingMetric rankingMetric;
    private AbstractLogEventAuthor authorP1 = new LogEventAuthorImpl("Test Author P1", 1);
    private AbstractLogEventAuthor authorP2 = new LogEventAuthorImpl("Test Author P2", 2);
    
    @BeforeEach
    public void setUp() {
        final RaceLogAndTrackedRaceResolver raceLogResolver = createRaceLogResolver();
        competitor1 = TrackBasedTest.createCompetitorWithBoat("C1");
        competitor2 = TrackBasedTest.createCompetitorWithBoat("C2");
        final Map<Competitor, Boat> competitorsAndBoats = TrackBasedTest.createCompetitorAndBoatsMap(competitor1, competitor2);
        boat1 = competitorsAndBoats.get(competitor1);
        boat2 = competitorsAndBoats.get(competitor2);
        trackedRace = TrackBasedTest.createTestTrackedRace("TestRegatta", "TestRace", "F18", competitorsAndBoats,
                MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false, raceLogResolver, ORCPerformanceCurveRankingMetric::new);
        regattaLog = trackedRace.getTrackedRegatta().getRegatta().getRegattaLog();
        trackedRace.attachRegattaLog(regattaLog);
        assertSame(regattaLog, trackedRace.getAttachedRegattaLogs().iterator().next());
        raceLog = new RaceLogImpl(UUID.randomUUID());
        rankingMetric = (ORCPerformanceCurveRankingMetric) trackedRace.getRankingMetric();
    }
    
    private void attachRaceLog() {
        trackedRace.attachRaceLog(raceLog);
    }

    private RaceLogAndTrackedRaceResolver createRaceLogResolver() {
        return DomainFactory.TEST_RACE_LOG_RESOLVER;
    }
    
    @Test
    public void testCertificateAssignments() throws InterruptedException {
        // no logs attached; therefore no certs
        assertNoCertificate(boat1);
        assertNoCertificate(boat2);
        attachRaceLog();
        // empty race log attached, still no certs:
        assertNoCertificate(boat1);
        assertNoCertificate(boat2);
        ORCCertificate boat1RegattaCert1 = mock(ORCCertificate.class);
        regattaLog.add(createRegattaLogCertificateAssignmentEvent(boat1RegattaCert1, boat1, authorP1));
        assertCertificate(boat1, boat1RegattaCert1);
        ORCCertificate boat1RegattaCert2 = mock(ORCCertificate.class);
        Thread.sleep(10); // ensure we get a new timestamp
        regattaLog.add(createRegattaLogCertificateAssignmentEvent(boat1RegattaCert2, boat1, authorP2));
        // the priority for Cert2 was 2 and therefore the P1 assignment still takes precedence
        assertCertificate(boat1, boat1RegattaCert1);
        Thread.sleep(10); // ensure we get a new timestamp
        regattaLog.add(createRegattaLogCertificateAssignmentEvent(boat1RegattaCert2, boat1, authorP1));
        assertCertificate(boat1, boat1RegattaCert2);
        ORCCertificate boat2RegattaCert1 = mock(ORCCertificate.class);
        Thread.sleep(10); // ensure we get a new timestamp
        regattaLog.add(createRegattaLogCertificateAssignmentEvent(boat2RegattaCert1, boat2, authorP2));
        assertCertificate(boat2, boat2RegattaCert1);
        // now override on race log level:
        ORCCertificate boat1RaceCert1 = mock(ORCCertificate.class);
        Thread.sleep(10); // ensure we get a new timestamp
        raceLog.add(createRaceLogCertificateAssignmentEvent(boat1RaceCert1, boat1, authorP2));
        assertCertificate(boat1, boat1RaceCert1);
        trackedRace.detachRaceLog(raceLog.getId());
        // after detaching race log, boat1 must have defaulted again to the regatta log's definition
        assertCertificate(boat1, boat1RegattaCert2);
    }
    
    @Test
    public void testNoCourseDefinitionsInRaceLog() {
        final ORCPerformanceCurveCourse orcCourse = rankingMetric.getTotalCourse();
        assertEquals(trackedRace.getRace().getCourse().getLegs().size(), Util.size(orcCourse.getLegs()));
        for (final ORCPerformanceCurveLeg leg : orcCourse.getLegs()) {
            assertTrue(leg instanceof ORCPerformanceCurveLegAdapter); // only adapters, no explicit leg specification yet
        }
    }

    @Test
    public void testOneLegDefinitionInRaceLog() throws NotRevokableException {
        attachRaceLog();
        final double lengthInNauticalMiles = 1.7;
        final double twaInDegrees = 2.0;
        final RaceLogORCLegDataEvent firstLegSpecificationEvent = createLegSpecificationEvent(
                /* one-based leg number */ 1, ORCPerformanceCurveLegTypes.TWA, /* distance in nautical miles */ lengthInNauticalMiles,
                /* TWA */ twaInDegrees, authorP2);
        raceLog.add(firstLegSpecificationEvent);
        final ORCPerformanceCurveCourse orcCourse = rankingMetric.getTotalCourse();
        assertEquals(trackedRace.getRace().getCourse().getLegs().size(), Util.size(orcCourse.getLegs()));
        final ORCPerformanceCurveLeg firstLeg = orcCourse.getLegs().iterator().next();
        assertFalse(firstLeg instanceof ORCPerformanceCurveLegAdapter);
        assertEquals(lengthInNauticalMiles, firstLeg.getLength().getNauticalMiles(), 0.000001);
        assertEquals(twaInDegrees, firstLeg.getTwa().getDegrees(), 0.0000001);
        // now revoke event and ensure that the course defaults back to all adapters:
        raceLog.revokeEvent(authorP2, firstLegSpecificationEvent);
        assertEquals(trackedRace.getRace().getCourse().getLegs().size(), Util.size(rankingMetric.getTotalCourse().getLegs()));
        for (final ORCPerformanceCurveLeg leg : rankingMetric.getTotalCourse().getLegs()) {
            assertTrue(leg instanceof ORCPerformanceCurveLegAdapter); // only adapters, no explicit leg specification anymore
        }
    }
    
    @Test
    public void testHigherPriorityLegDefinitionInRaceLogWins() throws NotRevokableException {
        attachRaceLog();
        final double lengthInNauticalMiles = 1.7;
        final double twaP1InDegrees = 1.0;
        final double twaP2InDegrees = 2.0;
        final RaceLogORCLegDataEvent firstLegP2SpecificationEvent = createLegSpecificationEvent(
                /* one-based leg number */ 1, ORCPerformanceCurveLegTypes.TWA, /* distance in nautical miles */ lengthInNauticalMiles,
                /* TWA */ twaP2InDegrees, authorP2);
        raceLog.add(firstLegP2SpecificationEvent);
        final RaceLogORCLegDataEvent firstLegP1SpecificationEvent = createLegSpecificationEvent(
                /* one-based leg number */ 1, ORCPerformanceCurveLegTypes.TWA, /* distance in nautical miles */ lengthInNauticalMiles,
                /* TWA */ twaP1InDegrees, authorP1);
        raceLog.add(firstLegP1SpecificationEvent);
        final ORCPerformanceCurveCourse orcCourse = rankingMetric.getTotalCourse();
        assertEquals(trackedRace.getRace().getCourse().getLegs().size(), Util.size(orcCourse.getLegs()));
        final ORCPerformanceCurveLeg firstLeg = orcCourse.getLegs().iterator().next();
        assertFalse(firstLeg instanceof ORCPerformanceCurveLegAdapter);
        assertEquals(lengthInNauticalMiles, firstLeg.getLength().getNauticalMiles(), 0.000001);
        assertEquals(twaP1InDegrees, firstLeg.getTwa().getDegrees(), 0.0000001);
        // now revoke P1 event and ensure that the course defaults to the P2 definition:
        raceLog.revokeEvent(authorP1, firstLegP1SpecificationEvent);
        assertEquals(trackedRace.getRace().getCourse().getLegs().size(), Util.size(rankingMetric.getTotalCourse().getLegs()));
        assertEquals(twaP2InDegrees, rankingMetric.getTotalCourse().getLegs().iterator().next().getTwa().getDegrees(), 0.0000001);
    }

    private RaceLogORCLegDataEvent createLegSpecificationEvent(int oneBasedLegNumber,
            ORCPerformanceCurveLegTypes legType, double lengthInNauticalMiles, double twaInDegrees,
            AbstractLogEventAuthor author) {
        return new RaceLogORCLegDataEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author,
                UUID.randomUUID(), /* pass */ 0, oneBasedLegNumber, new DegreeBearingImpl(twaInDegrees),
                new NauticalMileDistance(lengthInNauticalMiles), legType);
    }

    private RaceLogORCCertificateAssignmentEvent createRaceLogCertificateAssignmentEvent(
            ORCCertificate certificate, Boat boat, AbstractLogEventAuthor author) {
        return new RaceLogORCCertificateAssignmentEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID.randomUUID(), /* pass */ 0, certificate, boat);
    }
    
    private RegattaLogORCCertificateAssignmentEvent createRegattaLogCertificateAssignmentEvent(
            ORCCertificate certificate, Boat boat, AbstractLogEventAuthor author) {
        return new RegattaLogORCCertificateAssignmentEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID.randomUUID(), certificate, boat);
    }
    
    private void assertNoCertificate(Boat boat) {
        final ORCCertificate certificate = rankingMetric.getCertificate(boat);
        assertNull(certificate);
    }

    private void assertCertificate(Boat boat, ORCCertificate expectedCertificate) {
        final ORCCertificate certificate = rankingMetric.getCertificate(boat);
        assertSame(expectedCertificate, certificate);
    }
}
