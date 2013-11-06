package com.sap.sailing.server.operationaltransformation;

import java.io.Serializable;
import java.util.List;
import java.util.NavigableSet;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRaceWithWindEssentials;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.TrackedRegattaImpl;

public class DummyTrackedRace extends TrackedRaceWithWindEssentials {
    private static final long serialVersionUID = -11522605089325440L;
    private Iterable<? extends Competitor> competitors;
    private Regatta regatta;

    public DummyTrackedRace(final Iterable<? extends Competitor> competitors, final Regatta regatta,
            final WindStore windStore, final TrackedRegatta trackedRegatta) {
        super(new RaceDefinitionImpl("DummyRace", null, null, competitors), trackedRegatta, windStore, -1);
        this.competitors = competitors;
        this.regatta = regatta;
    }

    @Override
    public RaceDefinition getRace() {
        return new RaceDefinitionImpl("dummy", null, null, competitors);
    }

    @Override
    public RegattaAndRaceIdentifier getRaceIdentifier() {
        return null;
    }

    @Override
    public TimePoint getStartOfRace() {
        return null;
    }

    @Override
    public TimePoint getEndOfRace() {
        return null;
    }

    @Override
    public Iterable<Pair<Waypoint, Pair<TimePoint, TimePoint>>> getMarkPassingsTimes() {
        return null;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return false;
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
    public int getRank(Competitor competitor) throws NoWindException {
        return 0;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException {
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
    public GPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        return null;
    }

    @Override
    public Iterable<Mark> getMarks() {
        return null;
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at) {
        return null;
    }

    @Override
    public Wind getWind(Position p, TimePoint at, Iterable<WindSource> windSourcesToExclude) {
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
    public TimePoint getEndOfTracking() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getTimePointOfNewestEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimePoint getTimePointOfOldestEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        // TODO Auto-generated method stub
        return null;
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
    public TimePoint getTimePointOfLastEvent() {
        // TODO Auto-generated method stub
        return null;
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
    public long getDelayToLiveInMillis() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Wind getEstimatedWindDirection(Position position, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tack getTack(Competitor competitor, TimePoint timePoint) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TrackedRegatta getTrackedRegatta() {
        return new TrackedRegattaImpl(regatta);
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
    public List<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest)
            throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean raceIsKnownToStartUpwind() {
        // TODO Auto-generated method stub
        return false;
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
    public void addListener(RaceChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addListener(RaceChangeListener listener, boolean notifyAboutWindFixesAlreadyLoaded) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
        // TODO Auto-generated method stub
        
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
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
            throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WindStore getWindStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalyses) throws NoWindException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void waitUntilWindLoadingComplete() throws InterruptedException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TrackedRaceStatus getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void waitUntilNotLoading() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void detachRaceLog(Serializable identifier) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void detachAllRaceLogs() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachRaceLog(RaceLog raceLog) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public RaceLog getRaceLog(Serializable identifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addCourseDesignChangedListener(CourseDesignChangedListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Distance getStartAdvantage(Competitor competitor, double secondsIntoTheRace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, TimePoint timePoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLineWhenPassingStart(Competitor competitor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLive(TimePoint at) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Sideline> getCourseSidelines() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, double secondsBeforeRaceStart) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Speed getSpeed(Competitor competitor, double secondsBeforeRaceStart) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void addStartTimeChangedListener(StartTimeChangedListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TimePoint getStartTimeReceived() {
        // TODO Auto-generated method stub
        return null;
    }
}
