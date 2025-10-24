package com.sap.sailing.domain.test.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.BiFunction;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.leaderboard.Leaderboard.RankComparableRank;
import com.sap.sailing.domain.leaderboard.impl.CompetitorAndRankComparable;
import com.sap.sailing.domain.leaderboard.impl.RankAndRankComparable;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.RaceAbortedListener;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.SensorFixTrack;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindSummary;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.metering.CPUMeter;

/**
 * Simple mock for {@link TrackedRace} for leaderboard testing; the leaderboard only requests {@link #hasStarted(TimePoint)} and
 * {@link #getRank(Competitor)} and {@link #getRank(Competitor, TimePoint)}. Additionally, a mocked {@link RaceDefinition} is produced
 * from the competitor list.
 *
 * @author Axel Uhl (D043530)
 *
 */
public class MockedTrackedRaceWithStartTimeAndRanks implements TrackedRace {
    private static final long serialVersionUID = 2708044935347796930L;
    private final TimePoint startTime;
    private final List<Competitor> competitorsFromBestToWorst;
    private final Map<Competitor, Boat> competitorsAndBoats;
    private final Regatta regatta;
    private RaceDefinition race;

    /**
     * @param competitorsFromBestToWorst
     *            copied, so not live; the list passed may change afterwards without effects on the rankings in this
     *            mocked tracked race
     */
    public MockedTrackedRaceWithStartTimeAndRanks(TimePoint startTime, List<Competitor> competitorsFromBestToWorst) {
        this(startTime, competitorsFromBestToWorst, mock(Regatta.class));
        when(regatta.getName()).thenReturn("Test Regatta");
        final CPUMeter cpuMeter = CPUMeter.create();
        when(regatta.getCPUMeter()).thenReturn(cpuMeter);
    }

    public MockedTrackedRaceWithStartTimeAndRanks(TimePoint startTime, List<Competitor> competitorsFromBestToWorst, Regatta regatta) {
        this.regatta = regatta;
        this.startTime = startTime;
        // copies the list to make sure that later modifications to the list passed to this constructor don't affect the ranking produced by this race
        this.competitorsFromBestToWorst = new ArrayList<>(competitorsFromBestToWorst);
        BoatClass boatClass = new BoatClassImpl("49er", /* upwind start */ true);
        competitorsAndBoats = new HashMap<>();
        int i = 1;
        for (Competitor c: competitorsFromBestToWorst) {
            Boat b = new BoatImpl("Boat" + i++, c.getName(), boatClass, c.getName(), null);
            competitorsAndBoats.put(c, b);
        }
        this.race = new RaceDefinitionImpl("Mocked Race", new CourseImpl("Mock Course", Collections.emptyList()), boatClass,
                competitorsAndBoats);
    }

    @Override
    public void initializeAfterDeserialization() {
    }

    @Override
    public RaceDefinition getRace() {
        return race;
    }

    @Override
    public RegattaAndRaceIdentifier getRaceIdentifier() {
        return null;
    }

    @Override
    public TimePoint getStartOfRace() {
        return startTime;
    }

    @Override
    public TimePoint getStartOfRace(boolean inferred) {
        return startTime;
    }

    @Override
    public TimePoint getFinishingTime() {
        return null;
    }

    @Override
    public TimePoint getFinishedTime() {
        return null;
    }

    @Override
    public TimePoint getEndOfRace() {
        return null;
    }

    @Override
    public Iterable<Util.Pair<Waypoint, Util.Pair<TimePoint, TimePoint>>> getMarkPassingsTimes() {
        return null;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return at.compareTo(startTime) >= 0;
    }

    @Override
    public Iterable<TrackedLeg> getTrackedLegs() {
        return null;
    }

    @Override
    public TrackedLeg getTrackedLeg(Leg leg) {
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public TrackedLeg getCurrentLeg(TimePoint timePoint) {
        return null;
    }

    @Override
    public TrackedLeg getTrackedLegFinishingAt(Waypoint endOfLeg) {
        return null;
    }

    @Override
    public TrackedLeg getTrackedLegStartingAt(Waypoint startOfLeg) {
        return null;
    }

    @Override
    public GPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, TimePoint at) {
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, Leg leg) {
        return null;
    }

    @Override
    public long getUpdateCount() {
        return 0;
    }

    @Override
    public int getRankDifference(Competitor competitor, Leg leg, TimePoint timePoint) {
        return 0;
    }

    @Override
    public int getRank(Competitor competitor) {
        return competitorsFromBestToWorst.indexOf(competitor) + 1;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return competitorsFromBestToWorst.indexOf(competitor) + 1;
    }

    @Override
    public Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
        return null;
    }

    @Override
    public MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint) {
        return null;
    }

    @Override
    public GPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        return null;
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint, MarkPositionAtTimePointCache markPositionCache) {
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at) {
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at, Set<WindSource> windSourcesToExclude) {
        return null;
    }

    @Override
    public Set<WindSource> getWindSources(WindSourceType type) {
        return Collections.emptySet();
    }

    @Override
    public Set<WindSource> getWindSources() {
        return Collections.emptySet();
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource) {
        return null;
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation) {
        return null;
    }

    @Override
    public void waitForNextUpdate(int sinceUpdate) throws InterruptedException {
    }

    @Override
    public TimePoint getStartOfTracking() {
        return null;
    }

    @Override
    public TimePoint getEndOfTracking() {
        return null;
    }

    @Override
    public TimePoint getTimePointOfNewestEvent() {
        return null;
    }

    @Override
    public TimePoint getTimePointOfOldestEvent() {
        return null;
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        return null;
    }

    @Override
    public TimePoint getTimePointOfLastEvent() {
        return null;
    }

    @Override
    public long getMillisecondsOverWhichToAverageSpeed() {
        return 0;
    }

    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        return 0;
    }

    @Override
    public long getDelayToLiveInMillis() {
        return 0;
    }

    @Override
    public Wind getEstimatedWindDirection(TimePoint timePoint) {
        return null;
    }

    @Override
    public Tack getTack(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Tack getTack(SpeedWithBearing speedWithBearing, Wind wind, TimePoint timePoint) {
        return null;
    }

    @Override
    public TrackedRegatta getTrackedRegatta() {
        return new DynamicTrackedRegattaImpl(regatta);
    }

    @Override
    public Wind getDirectionFromStartToNextMark(TimePoint at) {
        return null;
    }

    @Override
    public List<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to) {
        return null;
    }

    @Override
    public Iterable<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest) {
        return null;
    }

    @Override
    public Iterable<Maneuver> getManeuvers(Competitor competitor, boolean waitForLatest) {
        return null;
    }

    @Override
    public boolean raceIsKnownToStartUpwind() {
        return false;
    }

    @Override
    public void addListener(RaceChangeListener listener) {
    }

    @Override
    public void addListener(RaceChangeListener listener, boolean notifyAboutWindFixesAlreadyLoaded,
            boolean notifyAboutGPSFixesAlreadyLoaded) {
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
    }

    @Override
    public Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Distance getDistanceFoiled(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Duration getDurationFoiled(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint, WindPositionMode windPositionMode) {
        return null;
    }

    @Override
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at) {
        return null;
    }

    @Override
    public Set<WindSource> getWindSourcesToExclude() {
        return null;
    }

    @Override
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Set<WindSource> windSourcesToExclude) {
        return null;
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint) {
        return null;
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis) {
        return null;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public WindStore getWindStore() {
        return null;
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint) {
        return null;
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        return competitorsFromBestToWorst;
    }

    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(TimePoint timePoint) {
        final LinkedHashMap<Competitor, RankAndRankComparable> competitorsFromBestToWorstAndRankComparable = new LinkedHashMap<>();
        for (int i = 1; i <= competitorsFromBestToWorst.size(); i++) {
            final Competitor competitor = competitorsFromBestToWorst.get(i-1);
            competitorsFromBestToWorstAndRankComparable.put(competitor, new RankAndRankComparable(getRank(competitor), new RankComparableRank(getRank(competitor))));
        }
        return competitorsFromBestToWorstAndRankComparable;
    }
    
    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint) {
        final List<CompetitorAndRankComparable> competitorsFromBestToWorstAndRankComparable = new ArrayList<>();
        for (int i = 1; i <= competitorsFromBestToWorst.size(); i++) {
            final Competitor competitor = competitorsFromBestToWorst.get(i-1);
            competitorsFromBestToWorstAndRankComparable.add(new CompetitorAndRankComparable(competitor, new RankComparableRank(getRank(competitor))));
        }
        return competitorsFromBestToWorstAndRankComparable;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly, boolean waitForLatestAnalyses) {
        return null;
    }

    @Override
    public void waitUntilLoadingFromWindStoreComplete() {
    }

    @Override
    public Iterable<Mark> getMarks() {
        return null;
    }

    @Override
    public boolean hasWindData() {
        return false;
    }

    @Override
    public boolean hasGPSData() {
        return false;
    }

    @Override
    public void lockForRead(Iterable<MarkPassing> markPassings) {
    }

    @Override
    public void unlockAfterRead(Iterable<MarkPassing> markPassings) {
    }

    @Override
    public TrackedRaceStatus getStatus() {
        return null;
    }

    @Override
    public void waitUntilNotLoading() {
    }

    @Override
    public RaceLog detachRaceLog(Serializable identifier) {
        return null;
    }

    @Override
    public void attachRaceLog(RaceLog raceLog) {
    }

    @Override
    public RaceLog getRaceLog(Serializable identifier) {
        return null;
    }

    @Override
    public void addCourseDesignChangedListener(CourseDesignChangedListener listener) {
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor,
            long millisecondsBeforeRaceStart) {
        return null;
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, long millisecondsBeforeRaceStart,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLineWhenPassingStart(Competitor competitor) {
        return null;
    }

    @Override
    public boolean isLive(TimePoint at) {
        return false;
    }

    @Override
    public Iterable<Sideline> getCourseSidelines() {
        return null;
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, long millisecondsBeforeRaceStart) {
        return null;
    }

    @Override
    public Speed getSpeed(Competitor competitor, long millisecondsBeforeRaceStart) {
        return null;
    }

    public void addStartTimeChangedListener(StartTimeChangedListener listener) {
    }

    @Override
    public void removeStartTimeChangedListener(StartTimeChangedListener listener) {
    }

    @Override
    public TimePoint getStartTimeReceived() {
        return null;
    }

    @Override
    public LineDetails getStartLine(TimePoint at) {
        return null;
    }

    @Override
    public Competitor getNextCompetitorToPortOnStartLine(Competitor relativeTo, TimePoint timePoint, BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        return null;
    }

    @Override
    public Competitor getNextCompetitorToStarboardOnStartLine(Competitor relativeTo, TimePoint timePoint, BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        return null;
    }

    @Override
    public Pair<Bearing, Position> getStartLineBearingAndStarboardMarkPosition(TimePoint timePoint) {
        return null;
    }

    @Override
    public LineDetails getFinishLine(TimePoint at) {
        return null;
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidence(long resolutionInMillis) {
        return null;
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidenceWithNumberOfSamples(int numberOfSamples) {
        return null;
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidence(TimePoint formTimePoint,
            TimePoint toTimePoint, int numberOfSamples) {
        return null;
    }

    @Override
    public Distance getCourseLength() {
        return null;
    }

    @Override
    public Speed getSpeedWhenCrossingStartLine(Competitor competitor) {
        return null;
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLine(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLineProjectedOntoLine(Competitor competitor,
            TimePoint timePoint) {
        return null;
    }

    @Override
    public SortedMap<Competitor, Distance> getDistancesFromStarboardSideOfStartLineProjectedOntoLine(
            TimePoint timePoint, BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        return null;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis) {
        return null;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint from, TimePoint to,
            boolean upwindOnly, boolean waitForLatestAnalysis) {
        return null;
    }

    @Override
    public void addRaceAbortedListener(RaceAbortedListener listener) {
    }

    @Override
    public Position getCenterOfCourse(TimePoint at) {
        return null;
    }

    @Override
    public void attachRegattaLog(RegattaLog regattaLog) {
    }

    @Override
    public void waitForLoadingToFinish() throws InterruptedException {
    }

    @Override
    public Boolean isGateStart() {
        return null;
    }

    @Override
    public Distance getDistanceTraveledIncludingGateStart(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Distance getAdditionalGateStartDistance(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public GPSFixTrack<Mark, GPSFix> getTrack(Mark mark) {
        return null;
    }

    @Override
    public long getGateStartGolfDownTime() {
        return 0;
    }

    @Override
    public boolean isUsingMarkPassingCalculator() {
        return false;
    }

    @Override
    public int getLastLegStarted(TimePoint timePoint) {
        return 0;
    }

    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return competitorsFromBestToWorst;
    }

    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final LinkedHashMap<Competitor, RankAndRankComparable> competitorsFromBestToWorstAndRankComparable = new LinkedHashMap<Competitor, RankAndRankComparable>();
        for (int i = 1; i <= competitorsFromBestToWorst.size(); i++) {
            final Competitor competitor = competitorsFromBestToWorst.get(i-1);
            competitorsFromBestToWorstAndRankComparable.put(competitor, new RankAndRankComparable(getRank(competitor), new RankComparableRank(getRank(competitor))));
        }
        return competitorsFromBestToWorstAndRankComparable;
    }
    
    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        List<CompetitorAndRankComparable> competitorsFromBestToWorstAndRankComparable = new ArrayList<CompetitorAndRankComparable>();
        for(int i = 1; i <= competitorsFromBestToWorst.size(); i++) {
            final Competitor competitor = competitorsFromBestToWorst.get(i-1);
            competitorsFromBestToWorstAndRankComparable.add(new CompetitorAndRankComparable(competitor, new RankComparableRank(getRank(competitor))));
        }
        return competitorsFromBestToWorstAndRankComparable;
    }

    @Override
    public RankingMetric getRankingMetric() {
        return null;
    }

    @Override
    public void setPolarDataService(PolarDataService polarDataService) {
    }

    @Override
    public TargetTimeInfo getEstimatedTimeToComplete(TimePoint timepoint) throws NotEnoughDataHasBeenAddedException,
            NoWindException {
        return null;
    }

    @Override
    public void attachRaceExecutionProvider(RaceExecutionOrderProvider raceExecutionOrderProvider) {
    }

    @Override
    public void detachRaceExecutionOrderProvider(RaceExecutionOrderProvider raceExecutionOrderProvider) {
    }

    @Override
    public IsManagedByCache<DomainFactory> resolve(DomainFactory domainFactory) {
        return this;
    }

    @Override
    public void updateStartAndEndOfTracking(boolean waitForGPSFixesToLoad) {
    }

    @Override
    public <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> TrackT getSensorTrack(
            Competitor competitor, String trackName) {
        return null;
    }

    @Override
    public Iterable<RegattaLog> getAttachedRegattaLogs() {
        return Collections.emptySet();
    }

    @Override
    public Iterable<RaceLog> getAttachedRaceLogs() {
        return Collections.emptySet();
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor, boolean waitForLatestUpdates) {
        return null;
    }

    @Override
    public Distance getAverageRideHeight(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    @Override
    public Boat getBoatOfCompetitor(Competitor competitor) {
        return competitorsAndBoats.get(competitor);
    }

    @Override
    public Competitor getCompetitorOfBoat(Boat boat) {
        if (boat == null) {
            return null;
        }
        for (Map.Entry<Competitor, Boat> competitorWithBoat : competitorsAndBoats.entrySet()) {
            if (boat.equals(competitorWithBoat.getValue())) {
                return competitorWithBoat.getKey();
            }
        }
        return null;
    }

    public Distance getEstimatedDistanceToComplete(TimePoint now) {
        return null;
    }

    @Override
    public <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> Iterable<TrackT> getSensorTracks(
            String trackName) {
        return Collections.emptySet();
    }

	@Override
	public Speed getAverageSpeedOverGround(Competitor competitor, TimePoint timePoint) {
		return null;
	}

    @Override
    public Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) throws NoWindException {
        return null;
    }

    @Override
    public SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public WindSummary getWindSummary() {
        return null;
    }

    @Override
    public boolean recordWind(Wind wind, WindSource windSource, boolean applyFilter) {
        return false;
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {

    }

    @Override
    public PolarDataService getPolarDataService() {
        return null;
    }

    @Override
    public void setWindEstimation(IncrementalWindEstimation windEstimation) {
    }

    @Override
    public TrackingConnectorInfo getTrackingConnectorInfo() {
        return null;
    }

    @Override
    public void runWhenDoneLoading(Runnable runnable) {
    }

    @Override
    public void runSynchronizedOnStatus(Runnable runnable) {
    }

    @Override
    public boolean hasFinishedLoading() {
        return false;
    }

    @Override
    public UUID getCourseAreaId() {
        return null;
    }

    @Override
    public Double getPercentTargetBoatSpeed(Competitor competitor, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }
}
