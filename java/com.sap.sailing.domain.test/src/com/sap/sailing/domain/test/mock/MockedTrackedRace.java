package com.sap.sailing.domain.test.mock;

import java.io.Serializable;
import java.util.List;
import java.util.NavigableSet;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaListener;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;

public class MockedTrackedRace implements DynamicTrackedRace {
    private static final long serialVersionUID = 5827912985564121181L;
    private final WindTrack windTrack = new WindTrackImpl(/* millisecondsOverWhichToAverage */ 30000, /* useSpeed */ true, "TestWindTrack");
    
    public WindTrack getWindTrack() {
        return windTrack;
    }

    @Override
    public RaceDefinition getRace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getStartOfRace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<TrackedLeg> getTrackedLegs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLeg getTrackedLeg(Leg leg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLeg getCurrentLeg(TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLeg getTrackedLegFinishingAt(Waypoint endOfLeg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLeg getTrackedLegStartingAt(Waypoint startOfLeg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, TimePoint at) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, Leg leg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getUpdateCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRankDifference(Competitor competitor, Leg leg, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRank(Competitor competitor) throws NoWindException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Distance getStartAdvantage(Competitor competitor, double secondsIntoTheRace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DynamicGPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void waitForNextUpdate(int sinceUpdate) throws InterruptedException {
        // TODO Auto-generated method stub

    }

    @Override
    public TimePoint getStartOfTracking() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getTimePointOfNewestEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recordFix(Competitor competitor, GPSFixMoving fix) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void recordWind(Wind wind, WindSource windSource) {
        if (windSource.getType() == WindSourceType.EXPEDITION) {
            windTrack.add(wind);
        }
    }

    @Override
    public void addListener(RaceChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateMarkPassings(Competitor competitor, Iterable<MarkPassing> markPassings) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStartTimeReceived(TimePoint start) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public DynamicGPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TimePoint getTimePointOfLastEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMillisecondsOverWhichToAverageSpeed(long millisecondsOverWhichToAverageSpeed) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMillisecondsOverWhichToAverageWind(long millisecondsOverWhichToAverageWind) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long getMillisecondsOverWhichToAverageSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Wind getEstimatedWindDirection(Position position, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return new DynamicTrackedRegatta() {
            private static final long serialVersionUID = 2651590861333064588L;

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
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterable<RaceDefinition> getAllRaces() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public BoatClass getBoatClass() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterable<Competitor> getCompetitors() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public void addRace(RaceDefinition race) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void removeRace(RaceDefinition raceDefinition) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public RaceDefinition getRaceByName(String raceName) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public void addRegattaListener(RegattaListener listener) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void removeRegattaListener(RegattaListener listener) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public RegattaIdentifier getRegattaIdentifier() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public String getBaseName() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Iterable<? extends Series> getSeries() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Series getSeriesByName(String seriesName) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public boolean isPersistent() {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public void addRaceColumnListener(RaceColumnListener listener) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void removeRaceColumnListener(RaceColumnListener listener) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public ScoringScheme getScoringScheme() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public CourseArea getDefaultCourseArea() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };
            }

            @Override
            public Iterable<TrackedRace> getTrackedRaces() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Iterable<TrackedRace> getTrackedRaces(BoatClass boatClass) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void addTrackedRace(TrackedRace trackedRace) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void removeTrackedRace(TrackedRace trackedRace) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void addRaceListener(RaceListener listener) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public int getNetPoints(Competitor competitor, TimePoint timePoint) throws NoWindException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public DynamicTrackedRace getTrackedRace(RaceDefinition race) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DynamicTrackedRace getExistingTrackedRace(RaceDefinition race) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void removeTrackedRace(RaceDefinition raceDefinition) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public DynamicTrackedRace createTrackedRace(RaceDefinition raceDefinition, WindStore windStore,
                    long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
                    DynamicRaceDefinitionSet raceDefinitionSetToUpdate) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tack getTack(Competitor competitor, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Wind getDirectionFromStartToNextMark(TimePoint at) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean raceIsKnownToStartUpwind() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setRaceIsKnownToStartUpwind(boolean raceIsKnownToStartUpwind) {
        // TODO Auto-generated method stub
    }

    @Override
    public RegattaAndRaceIdentifier getRaceIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getEndOfRace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getWindwardDistanceToOverallLeader(Competitor competitor, TimePoint timePoint)
            throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at, Iterable<WindSource> windSourcesToExclude) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<WindSource> getWindSources(WindSourceType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<WindSource> getWindSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Iterable<WindSource> windSourcesToExclude) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(Position position, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<WindSource> getWindSourcesToExclude() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getEndOfTracking() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getTimePointOfOldestEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setStartOfTrackingReceived(TimePoint startOfTrackingReceived) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setEndOfTrackingReceived(TimePoint endOfTrackingReceived) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Iterable<Pair<Waypoint, Pair<TimePoint, TimePoint>>> getMarkPassingsTimes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recordFix(Mark mark, GPSFix fix) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public WindStore getWindStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
        // TODO Auto-generated method stub
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getDelayToLiveInMillis() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setDelayToLiveInMillis(long delayToLiveInMillis) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAndFixDelayToLiveInMillis(long delayToLiveInMillis) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly, boolean waitForLatestAnalyses)
            throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void waitUntilWindLoadingComplete() throws InterruptedException {
        // TODO Auto-generated method stub
    }

	@Override
	public Iterable<Mark> getMarks() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public boolean hasWindData() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasGPSData() {
        // TODO Auto-generated method stub
        return false;
    }
	
    @Override
    public void lockForRead(Iterable<MarkPassing> markPassings) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void unlockAfterRead(Iterable<MarkPassing> markPassings) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TrackedRaceStatus getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setStatus(TrackedRaceStatus newStatus) {
        // TODO Auto-generated method stub
    }

    @Override
    public void waitUntilNotLoading() {
        // TODO Auto-generated method stub
    }

    @Override
    public void detachRaceLog() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachRaceLog(RaceLog raceLog) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public RaceLog getRaceLog() {
            // TODO Auto-generated method stub
            return null;
    }
}
