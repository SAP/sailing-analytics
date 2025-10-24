package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RegattaLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveRankingMetric;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveRankingMetricLeaderForBaseline;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SecondsDurationImpl;

/**
 * See bug 5122; Test cases for {@link ORCPerformanceCurveRankingMetric}
 * 
 * @author Axel Uhl (d043530)
 *
 */
@Timeout(value = 100, unit = TimeUnit.MINUTES)
public class ORCPerformanceCurveRankingTest extends OnlineTracTracBasedTest {
    final static MillisecondsTimePoint TIME_14_30_00 = new MillisecondsTimePoint(1555849800000l);
    final static MillisecondsTimePoint TIME_14_30_12 = new MillisecondsTimePoint(1555849812000l);
    
    private final static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    private ORCPerformanceCurveRankingMetric rankingMetric;
    private RaceLog raceLog;
    private RegattaLog regattaLog;
    private LogEventAuthorImpl author;

    public ORCPerformanceCurveRankingTest() throws MalformedURLException, URISyntaxException {
    }
    
    @Override
    protected String getExpectedEventName() {
        return "D-Marin ORC World Championship";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/orc/worlds2019/348f93c0-6798-0137-b07e-60a44ce903c3.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/orc/worlds2019/348f93c0-6798-0137-b07e-60a44ce903c3.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri, ORCPerformanceCurveRankingMetricLeaderForBaseline::new,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH });
        getTrackedRace().recordWind(new WindImpl(/* position */ new DegreePosition(44.37670797575265, 8.925960855558515), TIME_14_30_00,
                new KnotSpeedWithBearingImpl(7.5, new DegreeBearingImpl(246).reverse())), new WindSourceImpl(WindSourceType.WEB));
        raceLog = new RaceLogImpl(UUID.randomUUID());
        regattaLog = getTrackedRace().getTrackedRegatta().getRegatta().getRegattaLog();
        getTrackedRace().attachRegattaLog(regattaLog);
        getTrackedRace().attachRaceLog(raceLog);
        rankingMetric = (ORCPerformanceCurveRankingMetric) getTrackedRace().getRankingMetric();
        final ORCCertificatesCollection certificates = ORCCertificatesImporter.INSTANCE.read(
                getClass().getClassLoader().getResourceAsStream("orc/worlds2019/orcWorlds2019ClassA.json"));
        assertNotNull(certificates);
        assertFalse(Util.isEmpty(certificates.getCertificateIds()));
        author = new LogEventAuthorImpl("Testcase", 1);
        for (final Competitor competitor : getTrackedRace().getRace().getCompetitors()) {
            final Boat boat = getTrackedRace().getRace().getBoatOfCompetitor(competitor);
            ORCCertificate certificate;
            if (boat.getSailID().equals("Air is blue")) {
                certificate = certificates.getCertificateById("ITA00052727");
            } else {
                certificate = certificates.getCertificateById(boat.getSailID());
                if (certificate == null) {
                    certificate = certificates.getCertificateByBoatName(boat.getName() == null ? boat.getSailID() : boat.getName());
                }
            }
            assertNotNull(certificate, "Couldn't find a certificate for boat "+boat);
            final RegattaLogEvent certificateAssignment = new RegattaLogORCCertificateAssignmentEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(),
                    author, UUID.randomUUID(), certificate, boat);
            regattaLog.add(certificateAssignment);
            assertSame(certificate, rankingMetric.getCertificate(boat));
        }
        final RaceLog raceLog = getTrackedRace().getAttachedRaceLogs().iterator().next();
        final int numberOfLegs = getTrackedRace().getRace().getCourse().getNumberOfWaypoints()-1;
        for (int oneBasedLegNumber = 1; oneBasedLegNumber <= numberOfLegs; oneBasedLegNumber++) {
            raceLog.add(new RaceLogORCLegDataEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author,
                    UUID.randomUUID(), /* pPassId */ 0, oneBasedLegNumber, /* twa */ null,
                    new NauticalMileDistance(126).scale(1.0/numberOfLegs),
                    ORCPerformanceCurveLegTypes.LONG_DISTANCE));
        }
        /*
         * Maestro Yachting Ltd with boat Maestro=Maestro
         * Hurakan LTD with boat XIO=XIO
         * JD Maribor?anka with boat Generali (Assilina I=Generali (Assilina I
         * Roman Puchtev with boat Alemaro=Alemaro
         * Giovanni Sylos Labini with boat Luduan reloaded=Luduan reloaded
         * Sandro Paniccia with boat Altair 3=Altair 3
         * Gianclaudio Bassetti with boat WB Seven=WB Seven
         * SoLeVi Ltd / G. Petrochilos with boat Ex Officio=Ex Officio
         * Markos Kashiouris / IRONFX Racing LTD with boat IRONFX=IRONFX
         * Roberto Monti with boat Air is blue=Air is blue
         * Valentin Zavadnikov with boat Synergy=Synergy
         * Francesco Pison with boat Brava=Brava
         * JK Orsan with boat Dubrovnik=Dubrovnik
         */
        setFinishingTime("Air is blue", "2019-06-04T01:49:45+0200");
        setFinishingTime("XIO", "2019-06-04T01:44:45+0200");
        setFinishingTime("Generali (Assilina I", "2019-06-04T03:07:24+0200");
        setFinishingTime("Altair 3", "2019-06-04T04:22:50+0200");
        setFinishingTime("Synergy", "2019-06-04T03:57:06+0200");
        setFinishingTime("Ex Officio", "2019-06-04T07:13:07+0200");
        setFinishingTime("Luduan reloaded", "2019-06-04T08:10:43+0200");
        setFinishingTime("WB Seven", "2019-06-04T08:16:45+0200");
        setFinishingTime("Maestro", "2019-06-04T08:23:07+0200");
        setFinishingTime("IRONFX", "2019-06-04T07:15:36+0200");
        setFinishingTime("Dubrovnik", "2019-06-04T08:09:12+0200");
        setFinishingTime("Brava", "2019-06-04T07:54:14+0200");
        setFinishingTime("Alemaro", "2019-06-04T10:44:15+0200");
    }
    
    private void setFinishingTime(String boatName, String finishingTimeInISOFormat) throws ParseException {
        final Competitor competitor = getCompetitor(boatName);
        final TimePoint finishingTimePoint = new MillisecondsTimePoint(isoDateFormat.parse(finishingTimeInISOFormat));
        final CompetitorResultsImpl positionedCompetitors = new CompetitorResultsImpl();
        final CompetitorResult competitorResult = new CompetitorResultImpl(
                competitor.getId(), competitor.getName(), competitor.getShortName(), boatName,
                getTrackedRace().getBoatOfCompetitor(competitor).getSailID(), /* oneBasedRank==0 means let ranking metric decide */ 0,
                /* maxPointsReason */ null, /* score */ null, finishingTimePoint, /* comment */ null, MergeState.OK);
        positionedCompetitors.add(competitorResult);
        final RaceLogEvent result = new RaceLogFinishPositioningConfirmedEventImpl(MillisecondsTimePoint.now(), author, /* passId */ 0,
                positionedCompetitors);
        raceLog.add(result);
    }

    @Test
    public void testBasicRankingMetricProperties() {
        final SecondsDurationImpl scratchBoatDuration = new SecondsDurationImpl(14*3600 + 44*60 + 45);
        final Competitor winner = getCompetitor("Air is blue");
        final Duration winnerCorrectedTime = rankingMetric.getCorrectedTime(winner, MillisecondsTimePoint.now());
        assertEquals(getTrackedRace().getTimeSailedSinceRaceStart(winner, MillisecondsTimePoint.now()).asSeconds(), winnerCorrectedTime.asSeconds(), 0.00001);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Air is blue", 0, 0, 0);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "XIO", 0, 9, 49);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Generali (Assilina I", 0, 59, 13);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Altair 3", 1, 17, 2);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Synergy", 1, 19, 46);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Ex Officio", 2, 35, 49);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Luduan reloaded", 3, 11, 6);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "WB Seven", 3, 27, 53);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Maestro", 3, 29, 32);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "IRONFX", 3, 33, 20);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Dubrovnik", 4, 8, 36);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Brava", 4, 14, 41);
        assertCorrectedTimeAtEnd(scratchBoatDuration, winnerCorrectedTime, "Alemaro", 6, 28, 31);
    }
    
    private void assertCorrectedTimeAtEnd(Duration scratchBoatDuration, Duration winnerCorrectedTime, String boatName, int hours, int minutes, int seconds) {
        final Duration expectedCorrectedTime = winnerCorrectedTime.plus(new SecondsDurationImpl(3600*hours+60*minutes+seconds));
        final Duration correctedTimeForNamedBoat = rankingMetric.getCorrectedTime(getCompetitor(boatName), MillisecondsTimePoint.now());
        assertEquals(expectedCorrectedTime.asSeconds(), correctedTimeForNamedBoat.asSeconds(), 0.7,
                "Expected corrected time "+expectedCorrectedTime+" but got "+correctedTimeForNamedBoat+" for "+boatName);
    }
    
    private Competitor getCompetitor(String boatName) {
        for (final Competitor competitor : getTrackedRace().getRace().getCompetitors()) {
            final Boat boatOfCompetitor = getTrackedRace().getBoatOfCompetitor(competitor);
            if (Util.equalsWithNull(boatOfCompetitor.getName(), boatName) || Util.equalsWithNull(boatOfCompetitor.getSailID(), boatName)) {
                return competitor;
            }
        }
        return null;
    }

}
