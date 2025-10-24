package com.sap.sailing.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.test.LeaderboardScoringAndRankingTestBase;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@Disabled("bug5147: first ensuring that we have no regressions; then using this as test-first driver")
public class TestCrossFleetScoring extends LeaderboardScoringAndRankingTestBase {
    private final BoatClass boatClass = new BoatClassImpl(BoatClassMasterdata.PIRATE);
    private final TimePoint referenceTimePoint = MillisecondsTimePoint.now();
    private Leaderboard leaderboard;

    private Waypoint start;
    private Waypoint windward;
    private Waypoint finish;
    private final Map<DynamicTrackedRace, List<CompetitorWithBoat>> trackedRaces = new HashMap<>();
    final Map<Fleet, List<CompetitorWithBoat>> fleetsAndCompetitors = new HashMap<>();
    private final Map<String, CompetitorWithBoat> competitors = new HashMap<>();

    /**
     * Creates a regatta with a regatta leaderboard that has a single series with two fleets, Yellow and Blue. The
     * series is set to use cross-fleet merged ranking. Two competitors are assigned to each fleet, and a single race
     * column "R1" is created with a tracked race per fleet. The course is a simple windward-leeward course with one
     * lap (start/finish line - windward mark - start/finish line). Wind is set such that the upwind leg really happens
     * to point towards the direction the wind is coming from.
     */
    private void setUp(TimeOnTimeFactorMapping timeOnTimeFactors,
            Function<Competitor, Double> timeOnDistanceAllowance) {
        // create Competitors and their fleets.
        final ArrayList<Series> series = new ArrayList<>();
        for (String FleetName : new String[] { "Yellow", "Blue" }) {
            CompetitorWithBoat c1 = TrackBasedTest.createCompetitorWithBoat("Fast" + FleetName + "Boat");
            CompetitorWithBoat c2 = TrackBasedTest.createCompetitorWithBoat("Slow" + FleetName + "Boat");
            List<CompetitorWithBoat> competitorsForFleet = Arrays.asList(c1, c2);
            fleetsAndCompetitors.put(new FleetImpl(FleetName, 0), competitorsForFleet);
            competitors.putAll(competitorsForFleet.stream()
                    .collect(Collectors.toMap(Competitor::getName, competitor -> competitor)));
        }
        // create the Race and Regatta. The Regatta shall use ToT as metric. 
        final List<String> raceColumnNames = new ArrayList<>();
        raceColumnNames.add("R1");
        final Series zeroRankSeries = new SeriesImpl("zero Rank", /* isMedal */false,
                /* isFleetsCanRunInParallel */ true, fleetsAndCompetitors.keySet(), raceColumnNames,
                /* trackedRegattaRegistry */ null);
        zeroRankSeries.setCrossFleetMergedRanking(true);
        series.add(zeroRankSeries);
        final Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()),
                boatClass, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */ null, series, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", /* course area */null,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        // create a two-lap upwind/downwind course:
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate", "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        start = new WaypointImpl(leeGate);
        waypoints.add(start);
        windward = new WaypointImpl(windwardMark);
        waypoints.add(windward);
        finish = new WaypointImpl(leeGate);
        waypoints.add(finish);
        Course course = new CourseImpl("Test Course", waypoints);
        // for each fleet add a race to the race column. Sailed on the same course
        for (Map.Entry<Fleet, List<CompetitorWithBoat>> fleetAndCompetitors : fleetsAndCompetitors.entrySet()) {
            final RaceColumn r1Column = series.get(0).getRaceColumnByName("R1");
            final Map<Competitor, Boat> competitorsAndBoats = TrackBasedTest
                    .createCompetitorAndBoatsMap(fleetAndCompetitors.getValue()
                            .toArray(new CompetitorWithBoat[fleetAndCompetitors.getValue().size()]));
            RaceDefinition race = new RaceDefinitionImpl("R1 "+fleetAndCompetitors.getKey().getName(), course, boatClass, competitorsAndBoats);
            DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race,
                    Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0,
                    /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                    /* delay for wind estimation cache invalidation */ 0, /* useMarkPassingCalculator */ false,
                    tr -> new TimeOnTimeAndDistanceRankingMetric(tr, timeOnTimeFactors, // time-on-time
                            c -> new MillisecondsDurationImpl((long) (1000. * timeOnDistanceAllowance.apply(c)))),
                    mock(RaceLogAndTrackedRaceResolver.class), null, /* markPassingRaceFingerprintRegistry */ null);
            // in this simplified artificial course, the top mark is exactly north of the right leeward gate
            DegreePosition topPosition = new DegreePosition(1, 0);
            trackedRace.getOrCreateTrack(left)
                    .addGPSFix(new GPSFixImpl(new DegreePosition(0, -0.000001), referenceTimePoint));
            trackedRace.getOrCreateTrack(right)
                    .addGPSFix(new GPSFixImpl(new DegreePosition(0, 0.000001), referenceTimePoint));
            trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, referenceTimePoint));
            trackedRace.recordWind(
                    new WindImpl(topPosition, referenceTimePoint,
                            new KnotSpeedWithBearingImpl(/* speedInKnots */14.7, new DegreeBearingImpl(180))),
                    new WindSourceImpl(WindSourceType.WEB));
            assertEquals(120, trackedRace.getCourseLength().getNauticalMiles(), 0.02);
            r1Column.setTrackedRace(fleetAndCompetitors.getKey(), trackedRace);
            trackedRaces.put(trackedRace, fleetAndCompetitors.getValue());
        }
    }

    /**
     *  Apply the GPSFixes and Markroundings given in {@code competitorsAndMarkPassingsWithGpsFixes} for each competitor. 
     *  Afterwards test if the Competitors are in the same order as in the {@code expectedCompetitorOrder} when viewing the Leaderboard at {@code timePointOfViewingTheLeaderboard}.
     *  
     * @param expectedCompetitorOrder The Names of the Competitors in the expected order
     * @param competitorsAndMarkPassingsWithGpsFixes Structure of markpassings and GPS Fixes for each competitor. 
     * @param timePointOfViewingTheLeaderboard TimePoint at which the leaderboard is viewed. 
     */
    private void testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(String[] expectedCompetitorOrder,
            Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes,
            TimePoint timePointOfViewingTheLeaderboard) {
        for (Map.Entry<DynamicTrackedRace, List<CompetitorWithBoat>> trackedRaceAndCompetitors : trackedRaces.entrySet()) {
            DynamicTrackedRace trackedRace = trackedRaceAndCompetitors.getKey();
            for (Competitor competitor : trackedRaceAndCompetitors.getValue()) {
                trackedRace.updateMarkPassings(competitor,
                        competitorsAndMarkPassingsWithGpsFixes.get(competitor).getA());
                List<GPSFixMovingImpl> gpsPositions = competitorsAndMarkPassingsWithGpsFixes.get(competitor).getB();
                for (GPSFixMovingImpl gpsPosition : gpsPositions) {
                    trackedRace.getTrack(competitor).add(gpsPosition);
                }
            }
        }
        final Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(timePointOfViewingTheLeaderboard);
        Iterator<Competitor> it = rankedCompetitors.iterator();
        for (String currentCompetitor : expectedCompetitorOrder) {
            if (it.hasNext())
                assertEquals(competitors.get(currentCompetitor), it.next());
            else
                fail("There are different a number of Competitors in the Leaderboard and the expectedOrder array");
        }
    }

    /**
     * Generate the structure of MarkPassings and GpsFixes for the competitors given in {@code competitors}.
     * @param competitors Collection of competitors for wich the structure should be generated. 
     * @return MarkPassing and gpsFix Structure for all {@code competitors}
     */
    private Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> generateCompetitorsAndMarkPassingsWithGpsFixes(
            Collection<CompetitorWithBoat> competitors) {
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassings = new HashMap<>();
        for (Competitor competitor : competitors) {
            competitorsAndMarkPassings.put(competitor,
                    new Pair<List<MarkPassing>, List<GPSFixMovingImpl>>(new ArrayList<>(), new ArrayList<>()));
        }
        return competitorsAndMarkPassings;

    }
    /**
     * @param competitorsAndMarkPassingsWithGpsFixes DataStructure where the markPassing and gpsFix should be added 
     * @param competitor Competitor for which the markpassing and gpsFix should be added
     * @param markPassing markPassing to be added
     * @param gpsFix gpsFix to be added
     */
    private void addMarkPassingAndGPSFix(
            Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes,
            Competitor competitor, MarkPassingImpl markPassing, GPSFixMovingImpl gpsFix) {
        competitorsAndMarkPassingsWithGpsFixes.get(competitor).getA().add(markPassing);
        competitorsAndMarkPassingsWithGpsFixes.get(competitor).getB().add(gpsFix);
    }

    /**
     * Add StartMarkPassing for all competitors at the given timepoint 
     */
    private void addStartMarkPassing(
            Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes,
            Iterable<String> competitorNames, TimePoint startOfRace) {
        for (String competitorName : competitorNames) {
            Competitor competitor = competitors.get(competitorName);
            MarkPassingImpl markPassing = new MarkPassingImpl(startOfRace, start, competitor);
            GPSFixMovingImpl gpsFix = new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace,
                    new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(45)), /* optionalTrueHeading */ null);
            addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, competitor, markPassing, gpsFix);

        }
    }

    /**
     * Add StartMarkPassings for both fleets 10 Minutes apart 
     */
    private void addStartMarkPassing10MinutesApart(
            Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes,
            TimePoint startOfRace) {
        addStartMarkPassing(competitorsAndMarkPassingsWithGpsFixes, Arrays.asList("FastYellowBoat", "SlowYellowBoat"),
                startOfRace);
        addStartMarkPassing(competitorsAndMarkPassingsWithGpsFixes, Arrays.asList("FastBlueBoat", "SlowBlueBoat"),
                startOfRace.plus(Duration.ONE_MINUTE.times(10)));
    }

    /**
     * Append gpsFix for Competitor for the given TimePoint to the DataStructure
     */
    private void appendGPSFixForCompetitor(
            Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes,
            String competitorName, DegreePosition position, TimePoint timePoint) {
        GPSFixMovingImpl gpsFix = new GPSFixMovingImpl(position, timePoint,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(45)), /* optionalTrueHeading */ null);
        competitorsAndMarkPassingsWithGpsFixes.get(competitors.get(competitorName)).getB().add(gpsFix);

    }

    /**
     * The fleet launched second has completely overtaken the fleet launched first. The distance between the fleets is
     * so large that the fleet that started second is in front. Within the fleets, the slow and fast competitors have sailed the same
     * distance, so the slow competitor is ahead due to the ToT factor. 
     * This leads to the following ranking: SlowBlueBoat, FastBlueBoat, SlowBlueBoat, FastBlueBoat
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeSecondFleetOvertookFirstFleetOnFirstLegAllCompetitorsHaveSailedTheSameDistance() {
        String[] expectedOrder = new String[] { "SlowBlueBoat", "FastBlueBoat", "SlowYellowBoat", "FastYellowBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.75, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.75, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * The fleet launched second has completely overtaken the fleet launched first. The distance between the fleets is
     * so large that the fleet that started second is in front. Within the fleets, the slow competitor has sailed minimally less
     * distance than the fast compeitor. However, due to the ToT factor, the slow competitors are still ahead of the fast competitors. 
     * This leads to the following ranking: "SlowBlueBoat", "FastBlueBoat", "SlowYellowBoat", "FastYellowBoat"
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeSecondFleetOvertookFirstFleetOnFirstLegAllFastCompetitorsSaildFurtherThanSlowButSlowOvertookFastDueToToT() {
        String[] expectedOrder = new String[] { "SlowBlueBoat", "FastBlueBoat", "SlowYellowBoat", "FastYellowBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.19, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.75, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.74, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * The competitor fastBlue has a large distance advantage over the other competitors. While slowBlue sails just behind
     * fastYellow. slowYewllow is again spatially behind slowBlue. 
     * This leads to the following ranking: "FastBlueBoat","SlowBlueBoat", "FastYellowBoat", "SlowYellowBoat"
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeFastBlueOvertookFleet() {
        String[] expectedOrder = new String[] { "FastBlueBoat", "SlowBlueBoat", "FastYellowBoat", "SlowYellowBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.4, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.1, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.9, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.39, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * The competitors fastYellow and fastBlue sailed the same distance. Same is true for slowYellow and slowBlue. The distance
     * between the fast and slow competitors is such that the ToT factors put the slow boats in the lead, resulting in the following ranking:
     * "SlowBlueBoat","SlowYellowBoat", "FastBlueBoat","FastYellowBoat"
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeSlowBoatsOvertookFastBoats() {
        String[] expectedOrder = new String[] {  "SlowBlueBoat","SlowYellowBoat", "FastBlueBoat","FastYellowBoat"  };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.3, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.3, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * The competitors fastYellow and fastBlue sailed the same distance. Same is true for slowYellow and slowBlue. The distance
     * between the fast and slow competitors is such that the ToT factors put the fast boats in the lead, resulting in the following ranking:
     * "FastBlueBoat", "FastYellowBoat", "SlowBlueBoat", "SlowYellowBoat"
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeSlowBoatsBehindFastBoats() {
        String[] expectedOrder = new String[] { "FastBlueBoat", "FastYellowBoat", "SlowBlueBoat", "SlowYellowBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     *  fastBlue has overtaken fastYellow but is still spatially behind fastYellow, resulting in the following order:
     *  "FastBlueBoat", "FastYellowBoat", "SlowYellowBoat", "SlowBlueBoat"
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeFastBlueHasOvertakenFastYellowButIsStillSpatiallBehindFastYellow() {
        String[] expectedOrder = new String[] { "FastBlueBoat", "FastYellowBoat", "SlowYellowBoat", "SlowBlueBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.3, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.12, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * fastBlue has caught up to slowYellow enough to lead to an overtake on the leaderboard. slowBlue continues behind
     * slowYellow, resulting in the following order: "FastBlueBoat","FastYellowBoat",  "SlowYellowBoat", "SlowBlueBoat"
     */

    @Test
    public void testCrossFleetScoringForTimeOnTimeFastBlueHasOvertakenFastYellowButIsStillSpatiallBehindSlowYellow() {
        String[] expectedOrder = new String[] { "FastBlueBoat","FastYellowBoat",  "SlowYellowBoat", "SlowBlueBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.72, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.19, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.04, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * Just a normal race without overtaking
     */
    @Test
    public void testCrossFleetScoringForTimeOnTimeAllFastBoatsPerformTheSameAndAllSlowBoatsPerformTheSame() {
        String[] expectedOrder = new String[] { "FastBlueBoat", "FastYellowBoat", "SlowBlueBoat", "SlowYellowBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.2, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.416666666, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.1666666666, 0),
                timePointOfViewingTheLeaderboard);
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }

    /**
     * First Fleet has already rounded the mark and leads. While the fastBoats are Ahead of the slow Boats
     */
    @Test
    public void testTimeOnTimeFirstFleetRoundedMarkAndLeads() {
        String[] expectedOrder = new String[] { "FastYellowBoat", "SlowYellowBoat", "FastBlueBoat", "SlowBlueBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint markRounding = startOfRace.plus(Duration.ONE_MINUTE.times(30));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        // markRounding for FastYellow
        Competitor fastYellow = competitors.get("FastYellowBoat");
        MarkPassingImpl markPassingFastYellow = new MarkPassingImpl(markRounding, windward, fastYellow);
        GPSFixMovingImpl gpsFixFastYellow = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, fastYellow, markPassingFastYellow, gpsFixFastYellow);
        // markRounding for SlowYellow
        Competitor slowYellow = competitors.get("SlowYellowBoat");
        MarkPassingImpl markPassingSlowYellow = new MarkPassingImpl(markRounding, windward, slowYellow);
        GPSFixMovingImpl gpsFixSlowYellow = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, slowYellow, markPassingSlowYellow, gpsFixSlowYellow);
        // add current Position
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.4, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.9, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.1, 0),
                timePointOfViewingTheLeaderboard);
        //test
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }
    
    /**
     * Both Fleets have rounded the first mark. FastBlue has overtaken SlowYellow
     */
    @Test
    public void testTimeOnTimeBothFleetsRoundedMarkAndNoOvertaking() {
        String[] expectedOrder = new String[] { "FastYellowBoat", "FastBlueBoat", "SlowYellowBoat" , "SlowBlueBoat" };
        TimePoint startOfRace = referenceTimePoint.plus(Duration.ONE_MINUTE.times(10));
        TimePoint markRounding = startOfRace.plus(Duration.ONE_MINUTE.times(30));
        TimePoint timePointOfViewingTheLeaderboard = startOfRace.plus(Duration.ONE_HOUR);
        setUp(c -> c.getName().contains("Fast") ? 2.0 : 1.0, c -> 0.0);
        Map<Competitor, Pair<List<MarkPassing>, List<GPSFixMovingImpl>>> competitorsAndMarkPassingsWithGpsFixes = generateCompetitorsAndMarkPassingsWithGpsFixes(
                competitors.values());
        addStartMarkPassing10MinutesApart(competitorsAndMarkPassingsWithGpsFixes, startOfRace);
        // markRounding for FastYellow
        Competitor fastYellow = competitors.get("FastYellowBoat");
        MarkPassingImpl markPassingFastYellow = new MarkPassingImpl(markRounding, windward, fastYellow);
        GPSFixMovingImpl gpsFixFastYellow = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, fastYellow, markPassingFastYellow, gpsFixFastYellow);
        // markRounding for SlowYellow
        Competitor slowYellow = competitors.get("SlowYellowBoat");
        MarkPassingImpl markPassingSlowYellow = new MarkPassingImpl(markRounding, windward, slowYellow);
        GPSFixMovingImpl gpsFixSlowYellow = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, slowYellow, markPassingSlowYellow, gpsFixSlowYellow);
        // markRounding for FastBlue
        Competitor fastBlue = competitors.get("FastYellowBoat");
        MarkPassingImpl markPassingFastBlue = new MarkPassingImpl(markRounding, windward, fastBlue);
        GPSFixMovingImpl gpsFixFastBlue = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, fastBlue, markPassingFastBlue, gpsFixFastBlue);
        // markRounding for SlowYellow
        Competitor slowBlue = competitors.get("SlowYellowBoat");
        MarkPassingImpl markPassingSlowslowBlue = new MarkPassingImpl(markRounding, windward, slowBlue);
        GPSFixMovingImpl gpsFixSlowslowBlue = new GPSFixMovingImpl(new DegreePosition(1.0, 0), markRounding,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null);
        addMarkPassingAndGPSFix(competitorsAndMarkPassingsWithGpsFixes, slowBlue, markPassingSlowslowBlue, gpsFixSlowslowBlue);
        // add current Position
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastYellowBoat", new DegreePosition(0.1, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowYellowBoat", new DegreePosition(0.4, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "FastBlueBoat", new DegreePosition(0.5, 0),
                timePointOfViewingTheLeaderboard);
        appendGPSFixForCompetitor(competitorsAndMarkPassingsWithGpsFixes, "SlowBlueBoat", new DegreePosition(0.9, 0),
                timePointOfViewingTheLeaderboard);
        //test
        testForConfigurationInCompetitorsAndMarkPassingsWithGpsFixes(expectedOrder, competitorsAndMarkPassingsWithGpsFixes, timePointOfViewingTheLeaderboard);
    }
}
