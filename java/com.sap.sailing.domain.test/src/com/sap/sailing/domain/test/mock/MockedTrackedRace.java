package com.sap.sailing.domain.test.mock;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaListener;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.leaderboard.impl.CompetitorAndRankComparable;
import com.sap.sailing.domain.leaderboard.impl.CompetitorProviderFromRaceColumnsAndRegattaLike;
import com.sap.sailing.domain.leaderboard.impl.RankAndRankComparable;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeListener;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.RaceAbortedListener;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.SensorFixTrack;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackFactory;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindSummary;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;
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
import com.sap.sse.util.ThreadLocalTransporter;

public class MockedTrackedRace implements DynamicTrackedRace {
    private static final long serialVersionUID = 5827912985564121181L;
    private final WindTrack windTrack = new WindTrackImpl(/* millisecondsOverWhichToAverage */30000, /* useSpeed */
    true, "TestWindTrack");

    @Override
    public void initializeAfterDeserialization() {
    }

    public WindTrack getWindTrack() {
        return windTrack;
    }

    @Override
    public RaceDefinition getRace() {
        return null;
    }

    @Override
    public TimePoint getStartOfRace() {
        return null;
    }

    @Override
    public TimePoint getStartOfRace(boolean inferred) {
        return null;
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
    public int getRank(Competitor competitor) throws NoWindException {
        return 0;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return 0;
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
    public DynamicGPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
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
    public TimePoint getTimePointOfNewestEvent() {
        return null;
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        return new TreeSet<MarkPassing>();
    }

    @Override
    public boolean recordFix(Competitor competitor, GPSFixMoving fix, boolean onlyWhenInTrackingTimesInterval) {
        return false;
    }

    @Override
    public boolean recordWind(Wind wind, WindSource windSource, boolean applyFilter) {
        if (windSource.getType() == WindSourceType.EXPEDITION) {
            windTrack.add(wind);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addListener(RaceChangeListener listener) {
    }

    @Override
    public void addListener(RaceChangeListener listener, boolean notifyAboutWindFixesAlreadyLoaded,
            boolean notifyAboutGPSFixesAlreadyLoaded) {
    }

    @Override
    public void updateMarkPassings(Competitor competitor, Iterable<MarkPassing> markPassings) {
    }

    @Override
    public void setStartTimeReceived(TimePoint start) {
    }

    @Override
    public DynamicGPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        return null;
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {
    }

    @Override
    public TimePoint getTimePointOfLastEvent() {
        return null;
    }

    @Override
    public void setMillisecondsOverWhichToAverageSpeed(long millisecondsOverWhichToAverageSpeed) {
    }

    @Override
    public void setMillisecondsOverWhichToAverageWind(long millisecondsOverWhichToAverageWind) {
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
    public Wind getEstimatedWindDirection(TimePoint timePoint) {
        return null;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return false;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return new DynamicTrackedRegatta() {
            private static final long serialVersionUID = 2651590861333064588L;
            private final CPUMeter cpuMeter = CPUMeter.create();

            @Override
            public Regatta getRegatta() {
                return new Regatta() {
                    private static final long serialVersionUID = -4908774269425170811L;

                    @Override
                    public String getName() {
                        return "A Mocked Test Regatta";
                    }

                    @Override
                    public Serializable getId() {
                        return null;
                    }

                    @Override
                    public CPUMeter getCPUMeter() {
                        return cpuMeter;
                    }

                    @Override
                    public Iterable<RaceDefinition> getAllRaces() {
                        return null;
                    }

                    @Override
                    public BoatClass getBoatClass() {
                        return null;
                    }

                    @Override
                    public Iterable<Competitor> getAllCompetitors() {
                        return null;
                    }

                    @Override
                    public Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered() {
                        return null;
                    }

                    @Override
                    public Iterable<Boat> getAllBoats() {
                        return null;
                    }

                    @Override
                    public boolean canBoatsOfCompetitorsChangePerRace() {
                        return false;
                    }

                    @Override
                    public CompetitorRegistrationType getCompetitorRegistrationType() {
                        return CompetitorRegistrationType.CLOSED;
                    }

                    @Override
                    public void addRace(RaceDefinition race) {
                    }

                    @Override
                    public void removeRace(RaceDefinition raceDefinition) {
                    }

                    @Override
                    public RaceDefinition getRaceByName(String raceName) {
                        return null;
                    }

                    @Override
                    public void addRegattaListener(RegattaListener listener) {
                    }

                    @Override
                    public void removeRegattaListener(RegattaListener listener) {
                    }

                    @Override
                    public RegattaIdentifier getRegattaIdentifier() {
                        return null;
                    }

                    @Override
                    public Iterable<? extends Series> getSeries() {
                        return null;
                    }

                    @Override
                    public Series getSeriesByName(String seriesName) {
                        return null;
                    }

                    @Override
                    public boolean isPersistent() {
                        return false;
                    }

                    @Override
                    public void addRaceColumnListener(RaceColumnListener listener) {
                    }

                    @Override
                    public void removeRaceColumnListener(RaceColumnListener listener) {
                    }

                    @Override
                    public ScoringScheme getScoringScheme() {
                        return null;
                    }

                    @Override
                    public Iterable<CourseArea> getCourseAreas() {
                        return null;
                    }

                    @Override
                    public void setCourseAreas(Iterable<CourseArea> newCourseAreas) {
                    }

                    @Override
                    public boolean definesSeriesDiscardThresholds() {
                        return false;
                    }

                    @Override
                    public RegattaAndRaceIdentifier getRaceIdentifier(RaceDefinition race) {
                        return null;
                    }

                    @Override
                    public RegattaConfiguration getRegattaConfiguration() {
                        return null;
                    }

                    @Override
                    public void setRegattaConfiguration(RegattaConfiguration configuration) {
                    }

                    @Override
                    public void addSeries(Series series) {
                    }

                    @Override
                    public boolean useStartTimeInference() {
                        return false;
                    }

                    @Override
                    public void removeSeries(Series series) {
                    }

                    @Override
                    public void setUseStartTimeInference(boolean useStartTimeInference) {
                    }

                    @Override
                    public RegattaLog getRegattaLog() {
                        return null;
                    }

                    @Override
                    public TimePoint getStartDate() {
                        return null;
                    }

                    @Override
                    public void setStartDate(TimePoint startDate) {

                    }

                    @Override
                    public TimePoint getEndDate() {
                        return null;
                    }

                    @Override
                    public void setEndDate(TimePoint startDate) {

                    }

                    @Override
                    public Double getBuoyZoneRadiusInHullLengths(){
                        return 1.0;
                    }

                    @Override
                    public void setBuoyZoneRadiusInHullLengths(Double buoyZoneRadiusInHullLengths){
                    }

                    @Override
                    public RegattaLikeIdentifier getRegattaLikeIdentifier() {
                        return null;
                    }

                    @Override
                    public void addListener(RegattaLikeListener listener) {
                    }

                    @Override
                    public void removeListener(RegattaLikeListener listener) {
                    }

                    @Override
                    public RaceExecutionOrderProvider getRaceExecutionOrderProvider() {
                        return null;
                    }

                    @Override
                    public RankingMetricConstructor getRankingMetricConstructor() {
                        return null;
                    }

                    @Override
                    public Double getTimeOnTimeFactor(Competitor competitor, Optional<Runnable> changeCallback) {
                        return null;
                    }

                    @Override
                    public Duration getTimeOnDistanceAllowancePerNauticalMile(Competitor competitor, Optional<Runnable> changeCallback) {
                        return null;
                    }

                    @Override
                    public RaceColumn getRaceColumnByName(String raceColumnName) {
                        return null;
                    }

                    @Override
                    public IsRegattaLike getRegattaLike() {
                        return null;
                    }

                    @Override
                    public RaceLog getRacelog(String raceColumnName, String fleetName) {
                        return null;
                    }

                    @Override
                    public Iterable<? extends RaceColumn> getRaceColumns() {
                        return null;
                    }

                    @Override
                    public Iterable<Competitor> getCompetitorsRegisteredInRegattaLog() {
                        return null;
                    }

                    @Override
                    public void registerCompetitor(Competitor competitor) {
                    }

                    @Override
                    public void registerCompetitors(Iterable<Competitor> competitors) {
                    }

                    @Override
                    public void deregisterCompetitor(Competitor competitor) {
                    }

                    @Override
                    public void deregisterCompetitors(Iterable<Competitor> competitor) {
                    }

                    @Override
                    public CompetitorProviderFromRaceColumnsAndRegattaLike getOrCreateCompetitorsProvider() {
                        return null;
                    }

                    @Override
                    public Iterable<Boat> getBoatsRegisteredInRegattaLog() {
                        return null;
                    }

                    @Override
                    public void registerBoat(Boat boat) {
                    }

                    @Override
                    public void registerBoats(Iterable<Boat> boat) {
                    }

                    @Override
                    public void deregisterBoat(Boat boat) {
                    }

                    @Override
                    public void deregisterBoats(Iterable<Boat> boat) {
                    }

                    @Override
                    public boolean isControlTrackingFromStartAndFinishTimes() {
                        return false;
                    }

                    @Override
                    public void setControlTrackingFromStartAndFinishTimes(
                            boolean controlTrackingFromStartAndFinishTimes) {
                    }

                    @Override
                    public void setFleetsCanRunInParallelToTrue() {
                    }

                    @Override
                    public String getRegistrationLinkSecret() {
                        return null;
                    }

                    @Override
                    public void setRegistrationLinkSecret(String registrationLinkSecret) {
                    }

                    @Override
                    public void setCompetitorRegistrationType(CompetitorRegistrationType competitorRegistrationType) {
                    }

                    @Override
                    public boolean isAutoRestartTrackingUponCompetitorSetChange() {
                        return false;
                    }

                    @Override
                    public void setAutoRestartTrackingUponCompetitorSetChange(
                            boolean autoRestartTrackingUponCompetitorSetChange) {
                    }
                };
            }

            @Override
            public CPUMeter getCPUMeter() {
                return cpuMeter;
            }

            @Override
            public Iterable<DynamicTrackedRace> getTrackedRaces() {
                return null;
            }

            @Override
            public void addTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
            }

            @Override
            public void removeTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
            }

            @Override
            public void addRaceListener(RaceListener listener, Optional<ThreadLocalTransporter> threadLocalTransporter, boolean synchronous) {
            }

            @Override
            public int getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException {
                return 0;
            }

            @Override
            public DynamicTrackedRace getTrackedRace(RaceDefinition race) {
                return null;
            }

            @Override
            public DynamicTrackedRace getExistingTrackedRace(RaceDefinition race) {
                return null;
            }

            @Override
            public DynamicTrackedRace createTrackedRace(RaceDefinition raceDefinition, Iterable<Sideline> sidelines,
                    WindStore windStore, long delayToLiveInMillis,
                    long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
                    DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useMarkPassingcalculator,
                    RaceLogAndTrackedRaceResolver raceLogResolver,
                    Optional<ThreadLocalTransporter> threadLocalTransporter,
                    TrackingConnectorInfo trackingConnectorInfo, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
                return null;
            }

            @Override
            public void lockTrackedRacesForRead() {
            }

            @Override
            public void unlockTrackedRacesAfterRead() {
            }

            @Override
            public void lockTrackedRacesForWrite() {
            }

            @Override
            public void unlockTrackedRacesAfterWrite() {
            }

            @Override
            public Future<Boolean> removeRaceListener(RaceListener listener) {
                return null;
            }
        };
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint, MarkPositionAtTimePointCache markPositionCache) {
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
    public void setRaceIsKnownToStartUpwind(boolean raceIsKnownToStartUpwind) {
    }

    @Override
    public RegattaAndRaceIdentifier getRaceIdentifier() {
        return null;
    }

    @Override
    public TimePoint getEndOfRace() {
        return null;
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
    public Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode) {
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
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Set<WindSource> windSourcesToExclude) {
        return null;
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint) {
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
    public TimePoint getEndOfTracking() {
        return null;
    }

    @Override
    public TimePoint getTimePointOfOldestEvent() {
        return null;
    }

    @Override
    public void setStartOfTrackingReceived(TimePoint startOfTrackingReceived) {
    }

    @Override
    public void setEndOfTrackingReceived(TimePoint endOfTrackingReceived) {
    }

    @Override
    public Iterable<Util.Pair<Waypoint, Util.Pair<TimePoint, TimePoint>>> getMarkPassingsTimes() {
        return null;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalysis) {
        return null;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource) {
        return null;
    }

    @Override
    public void recordFix(Mark mark, GPSFix fix, boolean onlyWhenInTrackingTimesInterval) {
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
    }

    @Override
    public WindStore getWindStore() {
        return null;
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint) {
        return null;
    }

    @Override
    public long getDelayToLiveInMillis() {
        return 0;
    }

    @Override
    public void setDelayToLiveInMillis(long delayToLiveInMillis) {
    }

    @Override
    public void setAndFixDelayToLiveInMillis(long delayToLiveInMillis) {
    }

    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(TimePoint timePoint) {
        return getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
     
    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint) {
        return null;
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        return getCompetitorsFromBestToWorst(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint from, TimePoint to,
            boolean upwindOnly, boolean waitForLatestAnalyses) {
        return null;
    }

    @Override
    public void waitUntilLoadingFromWindStoreComplete() throws InterruptedException {
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
    public void setStatus(TrackedRaceStatus newStatus) {
    }

    @Override
    public void onStatusChanged(TrackingDataLoader loader, TrackedRaceStatus status) {
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
    public void onCourseDesignChangedByRaceCommittee(CourseBase courseDesign) {
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
    public void invalidateStartTime() {
    }

    @Override
    public void invalidateEndTime() {
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

    public void onStartTimeChangedByRaceCommittee(TimePoint newStartTime) {
    }

    @Override
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
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalysis) {
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
    public void onAbortedByRaceCommittee(Flags flag) {
    }

    @Override
    public Position getCenterOfCourse(TimePoint at) {
        return null;
    }

    @Override
    public boolean isUsingMarkPassingCalculator() {
        return false;
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
    public int getLastLegStarted(TimePoint timePoint) {
        return 0;
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
        return null;
    }

    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(
            TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return null;
    }

    @Override
    public RankingMetric getRankingMetric() {
        return null;
    }

    @Override
    public IsManagedByCache<DomainFactory> resolve(DomainFactory domainFactory) {
        return this;
    }

    @Override
    public void updateMarkPassingsAfterRaceLogChanges() {
    }

    @Override
    public void updateStartAndEndOfTracking(boolean waitForGPSFixesToLoad) {
    }

    @Override
    public void setFinishingTime(TimePoint newFinishingTime) {
    }

    @Override
    public void setFinishedTime(TimePoint newFinishedTime) {
    }

    @Override
    public <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> TrackT getSensorTrack(
            Competitor competitor, String trackName) {
        return null;
    }

    @Override
    public <FixT extends SensorFix, TrackT extends DynamicSensorFixTrack<Competitor, FixT>> TrackT getDynamicSensorTrack(
            Competitor competitor, String trackName) {
        return null;
    }

    @Override
    public <FixT extends SensorFix, TrackT extends DynamicSensorFixTrack<Competitor, FixT>> TrackT getOrCreateSensorTrack(
            Competitor competitor, String trackName, TrackFactory<TrackT> newTrackFactory) {
        return null;
    }

    @Override
    public boolean isWithinStartAndEndOfTracking(TimePoint timePoint) {
        return true;
    }

    @Override
    public Iterable<RegattaLog> getAttachedRegattaLogs() {
        return Collections.emptySet();
    }

    @Override
    public void recordSensorFix(Competitor competitor, String trackName, SensorFix fix,
            boolean onlyWhenInTrackingTimeInterval) {
    }

    @Override
    public void addSensorTrack(Competitor trackedItem, String trackName, DynamicSensorFixTrack<Competitor, ?> track) {
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
        return null;
    }

    @Override
    public Competitor getCompetitorOfBoat(Boat boat) {
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
