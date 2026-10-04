package com.sap.sailing.domain.test;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
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
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class TrackBasedTest {
    private DynamicTrackedRaceImpl trackedRace;
    
    final static Fleet regattaFleet = new FleetImpl("fleet name");
    final static BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */true);
    
    protected DynamicTrackedRaceImpl getTrackedRace() {
        return trackedRace;
    }

    protected void setTrackedRace(DynamicTrackedRaceImpl trackedRace) {
        this.trackedRace = trackedRace;
    }

    @SafeVarargs
    public static Map<Competitor,Boat> createCompetitorAndBoatsMap(CompetitorWithBoat... competitorsWithBoats) {
        Map<Competitor,Boat> result = new LinkedHashMap<>(); 
        for (CompetitorWithBoat competitorWithBoat: competitorsWithBoats) {
            result.put(competitorWithBoat, competitorWithBoat.getBoat());
        }
        return result;
    }

    public static CompetitorWithBoat createCompetitorWithBoat(String competitorName) {
        Competitor c = new CompetitorImpl(UUID.randomUUID(), competitorName, "HP", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                        new PersonImpl(competitorName, new NationalityImpl("GER"),
                        /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                        new NationalityImpl("NED"),
                        /* dateOfBirth */null, "This is Rigo, the coach")), 
                        /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat b = new BoatImpl(c.getId(), competitorName + "'s boat", boatClass, null, null);
        return new CompetitorWithBoatImpl(c, b);
    }

    /**
     * For {@link #trackedRace}'s race course, creates a list of mark passings using the time points specified, in order
     * for the waypoints.
     */
    protected List<MarkPassing> createMarkPassings(Competitor competitor, TimePoint... timePoints) {
        List<MarkPassing> result = new ArrayList<MarkPassing>();
        Iterator<Waypoint> wpIter = getTrackedRace().getRace().getCourse().getWaypoints().iterator();
        for (TimePoint timePoint : timePoints) {
            result.add(new MarkPassingImpl(timePoint, wpIter.next(), competitor));
        }
        return result;
    }

    /**
     * Creates a simple two-lap upwind-downwind course for a race/event with given name and boat class name with the
     * competitors specified. The marks are laid out such that the upwind/downwind leg detection should be alright. The
     * start line equals the finish line and the leeward gate. Wind is coming from the north. A single wind fix with
     * bearing 180deg (from=0deg) is added to the {@link WindSourceType#WEB} wind track using
     * <code>timePointForFixes</code> as time point. The leeward gate is located at N54.4680424, E10.234451 and
     * N54.4680424, E10.24. The windward mark is located at N54.48, E10.24.
     * @param timePointForFixes
     *            a wind fix will be inserted into the {@link WindSourceType#WEB} wind track which is aligned with the
     *            course layout; the value of this parameter will be used as the time stamp for this wind fix. Using a
     *            time that is reasonably within the race time (mark passing times or whatever is collected for the
     *            tracked race returned by this method) is important because otherwise confidences of wind readouts may
     *            be ridiculously low.
     * @param useMarkPassingCalculator whether or not to use the internal mark passing calculator
     */
    public static DynamicTrackedRaceImpl createTestTrackedRace(String regattaName, String raceName, String boatClassName,
            Map<Competitor, Boat> competitorsAndBoats, TimePoint timePointForFixes, boolean useMarkPassingCalculator) {
        return createTestTrackedRace(regattaName, raceName, boatClassName, competitorsAndBoats, timePointForFixes, useMarkPassingCalculator,
                mock(RaceLogAndTrackedRaceResolver.class));
    }
    
    /**
     * Creates a simple two-lap upwind-downwind course for a race/event with given name and boat class name with the
     * competitors specified, with a {@link OneDesignRankingMetric} as ranking metric. The marks are laid out such that
     * the upwind/downwind leg detection should be alright. The start line equals the finish line and the leeward gate.
     * Wind is coming from the north. A single wind fix with bearing 180deg (from=0deg) is added to the
     * {@link WindSourceType#WEB} wind track using <code>timePointForFixes</code> as time point. The leeward gate is
     * located at N54.4680424, E10.234451 and N54.4680424, E10.24. The windward mark is located at N54.48, E10.24.
     * 
     * @param timePointForFixes
     *            a wind fix will be inserted into the {@link WindSourceType#WEB} wind track which is aligned with the
     *            course layout; the value of this parameter will be used as the time stamp for this wind fix. Using a
     *            time that is reasonably within the race time (mark passing times or whatever is collected for the
     *            tracked race returned by this method) is important because otherwise confidences of wind readouts may
     *            be ridiculously low.
     * @param useMarkPassingCalculator
     *            whether or not to use the internal mark passing calculator
     */
    public static DynamicTrackedRaceImpl createTestTrackedRace(String regattaName, String raceName, String boatClassName,
            Map<Competitor, Boat> competitorsAndBoats, TimePoint timePointForFixes, boolean useMarkPassingCalculator, RaceLogAndTrackedRaceResolver raceLogResolver) {
        return createTestTrackedRace(regattaName, raceName, boatClassName, competitorsAndBoats, timePointForFixes, useMarkPassingCalculator, raceLogResolver,
                OneDesignRankingMetric::new);
    }
    
    /**
     * Like {@link #createTestTrackedRace(String, String, String, Map, TimePoint, boolean, RaceLogResolver)}, only that additionally
     * a specific {@link RankingMetricConstructor} can be provided which otherwise would default to {@link OneDesignRankingMetric}.
     */
    public static DynamicTrackedRaceImpl createTestTrackedRace(String regattaName, String raceName, String boatClassName,
            Map<Competitor, Boat> competitorsAndBoats, TimePoint timePointForFixes, boolean useMarkPassingCalculator, RaceLogAndTrackedRaceResolver raceLogResolver,
            RankingMetricConstructor rankingMetricConstructor) {
        BoatClassImpl boatClass = new BoatClassImpl(boatClassName, /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName(regattaName, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate", "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(leeGate));
        Course course = new CourseImpl(raceName, waypoints);
        RaceDefinition race = new RaceDefinitionImpl(raceName, course, boatClass, competitorsAndBoats);
        regatta.addRace(race);
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0, useMarkPassingCalculator,
                rankingMetricConstructor, raceLogResolver, /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null, /* maneuverRaceFingerprintRegistry */ null);
        // in this simplified artificial course, the top mark is exactly north of the right leeward gate
        DegreePosition topPosition = new DegreePosition(54.48, 10.24);
        TimePoint afterTheRace = new MillisecondsTimePoint(timePointForFixes.asMillis() + 36000000); // 10h after the fix timed
        trackedRace.setStartOfTrackingReceived(timePointForFixes);
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.234451), new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.24), new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.234451), afterTheRace));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.24), afterTheRace));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, afterTheRace));
        trackedRace.recordWind(new WindImpl(topPosition, timePointForFixes, new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(180))), new WindSourceImpl(WindSourceType.WEB));
        return trackedRace;
    }

    public static RegattaImpl createTestRegatta(String regattaName, Iterable<String> raceColumnNames) {
        final BoatClass boatClass = new BoatClassImpl(BoatClassMasterdata._12M);
        final TimePoint startDate = MillisecondsTimePoint.now();
        final TimePoint endDate = startDate.plus(MillisecondsDurationImpl.ONE_DAY);
        final boolean isMedal = false;
        final boolean persistent = false;
        final ScoringScheme scoringScheme = new LowPoint();
        final CourseArea courseArea = new CourseAreaImpl("Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null);
        final Serializable regatteId = "regatta id";
        Iterable<? extends Fleet> regattaFleets = Collections.singleton(regattaFleet);
        TrackedRegattaRegistry trackedRegattaRegistry = mock(TrackedRegattaRegistry.class);
        Series series = new SeriesImpl("series name", isMedal, /* isFleetsCanRunInParallel */ true, regattaFleets, raceColumnNames, trackedRegattaRegistry);
        Iterable<? extends Series> regattaSeries = Collections.singleton(series);
        return new RegattaImpl(regattaName, boatClass, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                startDate, endDate, regattaSeries, persistent, scoringScheme, regatteId, courseArea,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
    }

    public static void assignRacesToRegattaLeaderboardColumns(RegattaLeaderboard leaderboard, Collection<RaceIdentifier> raceIdentifiers) {
        Iterator<RaceIdentifier> regattaRacesIterator = raceIdentifiers.iterator();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            raceColumn.setRaceIdentifier(regattaFleet, regattaRacesIterator.next());
        }
    }
}
