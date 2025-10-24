package com.sap.sailing.server.trackfiles.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFixedMarkPassingEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.domain.test.DummyTrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.OutlierFilter;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.server.trackfiles.RouteConverterGPSFixImporterFactory;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

import difflib.PatchFailedException;

/**
 * See also bug 5728. We have seen tracks coming from phones where there seem to be two
 * "interleaved" sub-sequences of fixes: one where the time stamps are rounded to a full
 * second; and one where time stamps have a fractional seconds value. Each of these
 * sub-sequences seems to be consistent in itself, but at their boundaries the track
 * appears jittery and jumpy.<p>
 * 
 * We surmise that a constant offset can be computed by which the full-second time stamps
 * would need to be adjusted in order to result in a consistent track that is smooth also
 * at the boundaries between the two sub-sequences.<p>
 * 
 * This test starts with looking at two tracks that are known to show this issue. Jumpiness
 * can be measured by looking at the number and badness of inconsistencies between computed
 * and reported COG/SOG values between fixes, then "learning" the offset, adjusting all
 * integer-second fixes by the offset and computing number and badness of inconsistencies
 * again.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class JumpyTrackSmootheningTest {
    private static final Logger logger = Logger.getLogger(JumpyTrackSmootheningTest.class.getName());

    private static class Inconsistency {
        private final GPSFixMoving previous;
        private final GPSFixMoving fix;
        private final GPSFixMoving next;
        private final double SPEED_RATIO_TOLERANCE;
        private final double COURSE_DEGREE_TOLERANCE;
        
        public Inconsistency(GPSFixMoving previous, GPSFixMoving fix, GPSFixMoving next, double SPEED_RATIO_TOLERANCE, double COURSE_DEGREE_TOLERANCE) {
            super();
            this.previous = previous;
            this.fix = fix;
            this.next = next;
            this.SPEED_RATIO_TOLERANCE = SPEED_RATIO_TOLERANCE;
            this.COURSE_DEGREE_TOLERANCE = COURSE_DEGREE_TOLERANCE;
        }

        public SpeedWithBearing getInferredBetweenPreviousAndFix() {
            return previous.getSpeedAndBearingRequiredToReach(fix);
        }
        
        public SpeedWithBearing getInferredBetweenFixAndNext() {
            return fix.getSpeedAndBearingRequiredToReach(next);
        }
        
        public SpeedWithBearing getReportedByPrevious() {
            return previous.getSpeed();
        }
        
        public SpeedWithBearing getReportedByFix() {
            return fix.getSpeed();
        }
        
        public SpeedWithBearing getReportedByNext() {
            return next.getSpeed();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("previous:     ");
            sb.append(previous);
            sb.append('\n');
            sb.append("fix:          ");
            sb.append(fix);
            sb.append('\n');
            sb.append("next:         ");
            sb.append(next);
            sb.append('\n');
            sb.append("previous-fix: ");
            sb.append(getInferredBetweenPreviousAndFix());
            sb.append('\n');
            sb.append("fix-next      ");
            sb.append(getInferredBetweenFixAndNext());
            sb.append('\n');
            if (!OutlierFilter.isConsistent(getReportedByPrevious(), getInferredBetweenPreviousAndFix(), SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)) {
                sb.append("Inconsistent between reported by previous and inferred between previous and fix\n");
            }
            if (!OutlierFilter.isConsistent(getInferredBetweenFixAndNext(), getReportedByFix(), SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)) {
                sb.append("Inconsistent between inferred between fix and next and reported by fix\n");
            }
            if (!OutlierFilter.isConsistent(getInferredBetweenFixAndNext(), getReportedByNext(), SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)) {
                sb.append("Inconsistent between inferred between fix and next and reported by next\n");
            }
            return sb.toString();
        }
    }
    
    @Test
    public void testMarkPassingCalculatorForAdjusted() throws Exception {
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = readTrack("GallagherZelenka.gpx.gz");
        final Duration durationForAdjustedTrack;
        final Duration durationForOriginalTrack;
        {
            final Pair<Integer, DynamicGPSFixTrack<Competitor, GPSFixMoving>> replaced = new OutlierFilter().findAndRemoveInconsistenciesOnRawFixes(track);
            final Competitor competitor = track.getTrackedItem();
            final TimePoint startedAt = TimePoint.now();
            final DynamicTrackedRace trackedRace = createRace(replaced.getB());
            final NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor, /* wait for latest update */ true);
            final TimePoint doneAt = TimePoint.now();
            durationForAdjustedTrack = startedAt.until(doneAt);
            logger.info("Duration for computing mark passings with adjusted track: "+durationForAdjustedTrack);
            assertNotNull(markPassings);
            assertEquals(13, markPassings.size());
        }
        {
            final TimePoint startedAt = TimePoint.now();
            final DynamicTrackedRace trackedRace = createRace(track);
            final NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(track.getTrackedItem(), /* wait for latest update */ true);
            final TimePoint doneAt = TimePoint.now();
            durationForOriginalTrack = startedAt.until(doneAt);
            logger.info("Duration for computing mark passings with original track: "+durationForOriginalTrack);
            assertNotNull(markPassings);
            assertEquals(13, markPassings.size());
        }
        assertTrue(durationForAdjustedTrack.times(8).compareTo(durationForOriginalTrack) < 0,
                "Expected duration for mark passing analysis on adjusted track to be at least eight times less than for original track");
    }
    
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> readTrack(String filename) throws Exception {
        final DynamicBoat boat = new BoatImpl("1", "1", new BoatClassImpl(BoatClassMasterdata.MELGES_24), /* sailID */ "1");
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Competitor>(AbstractLeaderboardTest.createCompetitorWithBoat(filename, boat),
                /* millisecondsOverWhichToAverage */ 5000, /* losslessCompaction */ true);
        final InputStream fileInputStream = getClass().getClassLoader().getResourceAsStream(filename);
        final InputStream inputStream;
        if (filename.endsWith(".gz")) {
            inputStream = new GZIPInputStream(fileInputStream);
        } else {
            inputStream = fileInputStream;
        }
        RouteConverterGPSFixImporterFactory.INSTANCE.createRouteConverterGPSFixImporter().importFixes(inputStream,
                Charset.defaultCharset(), (fix, device)->track.add((GPSFixMoving) fix), /* inferSpeedAndBearing */ false, filename);
        return track;
    }
    
    private void addFixedMarkPassingToRaceLog(String isoTimePoint, Competitor competitor, int zeroBasedIndexOfWaypointOfPassing, RaceLog raceLog) throws ParseException {
        final LogEventAuthorImpl author = new LogEventAuthorImpl("me", 1);
        final TimePoint markPassingTimePoint = TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(isoTimePoint));
        raceLog.add(new RaceLogFixedMarkPassingEventImpl(markPassingTimePoint,
                author, competitor, /* pPassId */ 1, markPassingTimePoint, zeroBasedIndexOfWaypointOfPassing));
    }
    
    /**
     * Simulates the "Oak cliff DH Distance Race" R1 with a single competitor, Gallagher / Zelenka, sail number "1" with
     * the marks pinged statically to establish the course. The track of Gallagher / Zelenka is provided as a track of
     * their GPS positions. This could be the raw track, or it may be a filtered variant of the track with outliers
     * removed or adjusted.<p>
     * 
     * The race that is returned with have the mark passing calculator activated, and a test may wait for it to complete
     * its calculation. As a result, a test may determine the impact filtering / adjusting the track may have on the
     * mark passing analysis.
     */
    private DynamicTrackedRace createRace(DynamicGPSFixTrack<Competitor, GPSFixMoving> competitorTrack) throws PatchFailedException, ParseException {
        final Competitor gallagherZelenka = competitorTrack.getTrackedItem();
        final DynamicTrackedRace trackedRace = createTrackedRace("Oak cliff DH Distance Race", "R1", BoatClassMasterdata.MELGES_24, gallagherZelenka);
        final Series defaultSeries = trackedRace.getTrackedRegatta().getRegatta().getSeries().iterator().next();
        final Fleet defaultFleet = defaultSeries.getFleets().iterator().next();
        final RaceColumnInSeries r1RaceColumn = defaultSeries.getRaceColumns().iterator().next();
        r1RaceColumn.setTrackedRace(defaultFleet, trackedRace);
        trackedRace.setStartOfTrackingReceived(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-10-14T17:00:00Z")));
        trackedRace.setStartTimeReceived(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2020-10-14T17:05:00Z")));
//        final Mark lisR32a = createAndPlaceMark(trackedRace, "32A - Mid-Sound Buoy", "LIS R32A", 40.96866998355836, -73.54664996266365, MarkType.BUOY, Color.ofRgb("#FF0000"), "CYLINDER");
//        final Mark lisC17 = createAndPlaceMark(trackedRace, "C17 - Sound Side of Center Island Green", "LIS C17", 40.93856998253614, -73.53271999396384, MarkType.BUOY, Color.ofRgb("#008000"), "CYLINDER");
        final Mark cshG1 = createAndPlaceMark(trackedRace, "Cold Spring Harbor G1", "CSH G1", 40.92594997957349, -73.50404994562268, MarkType.BUOY, Color.ofRgb("#008000"), "CYLINDER");
        final Mark cshl = createAndPlaceMark(trackedRace, "Cold Spring Harbor Light", "CSHL", 40.91418300289661, -73.4931492805481, MarkType.BUOY, null, "CYLINDER");
        final Mark finishBoat = createAndPlaceMark(trackedRace, "Finish Boat", "FB", 40.89873938821256, -73.51117020472884, MarkType.FINISHBOAT, null, null);
        final Mark finishPin = createAndPlaceMark(trackedRace, "Finish Pin", "FP", 40.897511816583574, -73.50983932614326, MarkType.BUOY, null, null);
        final Mark faulkner = createAndPlaceMark(trackedRace, "G15 - North Side Faulkner Island", "Faulkner", 41.22299997601658, -72.65443325042725, MarkType.BUOY, Color.ofRgb("#008000"), "CONICAL");
//        final Mark bayville = createAndPlaceMark(trackedRace, "LIS G19 - Bayville", "Bayville", 40.92419996391982, -73.56988325715065, MarkType.BUOY, Color.ofRgb("#008000"), "CONICAL");
        final Mark matinecock = createAndPlaceMark(trackedRace, "LIS G21 - Matinecock Pt", "Matinecock Pt", 40.90974998194724, -73.63691660575569, MarkType.BUOY, Color.ofRgb("#008000"), "CONICAL");
        final Mark sixMileReef = createAndPlaceMark(trackedRace, "LIS R8C - 6 Mile Reef", "6 Mile Reef", 41.17991665843874, -72.49066662043333, MarkType.BUOY, Color.ofRgb("#FF0000"), "CONICAL");
        final Mark newMark = createAndPlaceMark(trackedRace, "New Mark", "NM", 40.924666626378894, -73.70251664891839, MarkType.BUOY, null, null);
//        final Mark ob2 = createAndPlaceMark(trackedRace, "Oyster Bay Buoy 2", "OB2", 40.91139995958656, -73.50232997909188, MarkType.BUOY, Color.ofRgb("#FF0000"), "CONICAL");
//        final Mark ob4 = createAndPlaceMark(trackedRace, "Oyster Bay Buoy 4", "OB4", 40.90192995965481, -73.50629998371005, MarkType.BUOY, Color.ofRgb("#FF0000"), "CONICAL");
//        final Mark ob5 = createAndPlaceMark(trackedRace, "Oyster Bay Buoy 5 (Seawanhaka)", "OB5", 40.89752623345703, -73.50977637805045, MarkType.BUOY, Color.ofRgb("#008000"), "CYLINDER");
        final Mark cows = createAndPlaceMark(trackedRace, "R32 - The Cows", "Cows", 41.003599972464144, -73.52359998039901, MarkType.BUOY, Color.ofRgb("#FF0000"), "CONICAL");
        final Mark startBoat = createAndPlaceMark(trackedRace, "Start Boat", "SB", 40.8984215464443, -73.51104154251516, MarkType.STARTBOAT, null, null);
        final Mark startPin = createAndPlaceMark(trackedRace, "Start Pin", "SP", 40.89739736169577, -73.50981149822474, MarkType.BUOY, null, null);
//        final Mark cowes32 = createAndPlaceMark(trackedRace, "The Cowes Lighted Bell Buoy 32", "Cowes 32", 40.00361998099834, -73.52387993596494, MarkType.BUOY, null, null);
        final ControlPointWithTwoMarks start = new ControlPointWithTwoMarksImpl(startBoat, startPin, "Start", "S");
        final ControlPointWithTwoMarks finish = new ControlPointWithTwoMarksImpl(finishBoat, finishPin, "Finish", "F");
        trackedRace.getRace().getCourse().update(Arrays.asList(
                new Pair<>(start, PassingInstruction.Line),
                new Pair<>(cshl, PassingInstruction.Port),
                new Pair<>(cshG1, PassingInstruction.Port),
                new Pair<>(cshl, PassingInstruction.Port),
                new Pair<>(cows, PassingInstruction.Starboard),
                new Pair<>(faulkner, PassingInstruction.Starboard),
                new Pair<>(sixMileReef, PassingInstruction.Starboard),
                new Pair<>(cows, PassingInstruction.Port),
                new Pair<>(newMark, PassingInstruction.Port),
                new Pair<>(matinecock, PassingInstruction.Port),
                new Pair<>(cows, PassingInstruction.Starboard),
                new Pair<>(cshl, PassingInstruction.Starboard),
                new Pair<>(finish, PassingInstruction.Line)),
                /* associatedRoles */ Collections.emptyMap(), /* originatingCouseTemplateIdOrNull */ null, DomainFactory.INSTANCE);
        // add fixed mark passings for Gallagher / Zelenka:
        final RaceLog raceLog = r1RaceColumn.getRaceLog(defaultFleet);
        addFixedMarkPassingToRaceLog("2020-10-14T17:05:05Z", gallagherZelenka, 0, raceLog);
        addFixedMarkPassingToRaceLog("2020-10-14T17:20:41Z", gallagherZelenka, 1, raceLog);
        addFixedMarkPassingToRaceLog("2020-10-14T17:29:36Z", gallagherZelenka, 2, raceLog);
        addFixedMarkPassingToRaceLog("2020-10-14T17:36:42Z", gallagherZelenka, 3, raceLog);
        addFixedMarkPassingToRaceLog("2020-10-14T18:21:38Z", gallagherZelenka, 4, raceLog);
        trackedRace.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0.0));
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> competitorTrackInRace = trackedRace.getTrack(gallagherZelenka);
        // TODO switch race into suspended mode to avoid updates during mass fix insertion:
        competitorTrack.lockForRead();
        try {
            for (final GPSFixMoving fix : competitorTrack.getRawFixes()) {
                competitorTrackInRace.add(fix);
            }
        } finally {
            competitorTrack.unlockAfterRead();
        }
        trackedRace.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        // TODO resume race
        return trackedRace;
    }
    
    private DynamicTrackedRace createTrackedRace(String regattaName, String name, BoatClassMasterdata boatClassMasterData, Competitor gallagherZelenka) {
        final BoatClassImpl boatClass = new BoatClassImpl(boatClassMasterData);
        final TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(new RegattaImpl(regattaName, boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ false, /* competitorRegistrationType */ CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */ null, Collections.singleton(new SeriesImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ false,
                        Collections.singleton(new FleetImpl("Default", 0)), Collections.singleton("R1"), new DummyTrackedRegattaRegistry())), /* persistent */ false,
                new LowPoint(), UUID.randomUUID(), new CourseAreaImpl("Default", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null), OneDesignRankingMetric::new,
                /* registrationLinkSecret */ null));
        final Boat boat = ((CompetitorWithBoat) gallagherZelenka).getBoat();
        final Map<Competitor, Boat> competitorsAndTheirBoats = Util.<Competitor, Boat>mapBuilder().put(gallagherZelenka, boat).build();
        final Course course = new CourseImpl("R1 Course", Collections.emptySet());
        final RaceDefinition race = new RaceDefinitionImpl(name, course, boatClass, competitorsAndTheirBoats, UUID.randomUUID());
        return new DynamicTrackedRaceImpl(trackedRegatta, race, /* sidelines */ Collections.emptySet(), new EmptyWindStore(), /* delayToLiveInMillis */ 1000,
                WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND, /* time over which to average speed: */ boatClass.getApproximateManeuverDurationInMilliseconds(),
                /* useInternalMarkPassingAlgorithm */ true, OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }

    private Mark createAndPlaceMark(DynamicTrackedRace trackedRace, String name, String shortName, double latDeg, double lngDeg,
            MarkType markType, Color color, String shape) {
        final Mark mark = new MarkImpl(UUID.randomUUID(), name, markType, color, shape, /* pattern */ null);
        final DynamicGPSFixTrack<Mark, GPSFix> markTrack = trackedRace.getOrCreateTrack(mark);
        final GPSFix markFix = new GPSFixImpl(new DegreePosition(latDeg, lngDeg), trackedRace.getStartOfTracking());
        markTrack.add(markFix);
        return mark;
    }
    
    private void adjustTrackAndAssertNoOutliersInResult(String trackFileName, int maximumNumberOfOutliersAllowed) throws Exception {
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = readTrack(trackFileName);
        track.lockForRead();
        try {
            assertFalse(Util.isEmpty(track.getRawFixes()));
        } finally {
            track.unlockAfterRead();
        }
        final Pair<Integer, DynamicGPSFixTrack<Competitor, GPSFixMoving>> numberOfInconsistenciesAndReplacedTrack = new OutlierFilter().findAndRemoveInconsistenciesOnRawFixes(track);
        assertTrue(numberOfInconsistenciesAndReplacedTrack.getA() > maximumNumberOfOutliersAllowed);
        final int actualNumberOfOutliers = getNumberOfFixesWithInconsistentCogSog(numberOfInconsistenciesAndReplacedTrack.getB());
        assertTrue(actualNumberOfOutliers <= maximumNumberOfOutliersAllowed,
                "Expected number of inconsistencies to be less than or equal to "+maximumNumberOfOutliersAllowed+" but was "+actualNumberOfOutliers);
    }
    
    /**
     * Count severe COG/SOG inconsistencies left; severely inconsistent means a speed difference between inferred and
     * reported of more than 500%, or a course inconsistency of more than 90 degrees.
     */
    private int getNumberOfFixesWithInconsistentCogSog(DynamicGPSFixTrack<Competitor, GPSFixMoving> track) {
        int inconsistencies = 0;
        final Map<GPSFixMoving, Inconsistency> inconsistentFixes = new LinkedHashMap<>();
        track.lockForRead();
        try {
            GPSFixMoving previous = null, fix = null;
            for (final GPSFixMoving next : track.getRawFixes()) {
                if (previous != null && fix != null && OutlierFilter.hasInconsistentCogSog(previous, fix, next, /* speed ratio tolerance */ 5, /* course degree tolerance */ 120)) {
                    inconsistencies++;
                    inconsistentFixes.put(fix, new Inconsistency(previous, fix, next, 5, 120));
                }
                previous = fix;
                fix = next;
            }
        } finally {
            track.unlockAfterRead();
        }
        return inconsistencies;
    }

    @Test
    public void testCZE2471() throws Exception {
        adjustTrackAndAssertNoOutliersInResult("CZE2471.gpx.gz", 15);
    }

    @Test
    public void testCZE2956() throws Exception {
        adjustTrackAndAssertNoOutliersInResult("CZE2956.gpx.gz", 6);
    }
    
    /**
     * See https://my.sapsailing.com/gwt/RaceBoard.html?regattaName=Oak+cliff+DH+Distance+Race&raceName=R1&leaderboardName=Oak+cliff+DH+Distance+Race&leaderboardGroupId=a3902560-6bfa-43be-85e1-2b82a4963416&eventId=bf48a59d-f2af-47b6-a2f7-a5b78b22b9f2&mode=FULL_ANALYSIS
     * for the original race.
     */
    @Test
    public void testGallagherZelenka() throws Exception {
        adjustTrackAndAssertNoOutliersInResult("GallagherZelenka.gpx.gz", 324);
    }
}
