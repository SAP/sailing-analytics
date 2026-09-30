package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.NavigableSet;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.BearingChangeAnalyzer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public abstract class AbstractManeuverDetectorImpl implements ManeuverDetector {

    /**
     * Tracked race whose tracks are being processed for maneuver detection.
     */
    protected final TrackedRace trackedRace;

    /**
     * The competitor, whose maneuvers are being discovered
     */
    protected final Competitor competitor;

    /**
     * The track of competitor
     */
    protected final GPSFixTrack<Competitor, GPSFixMoving> track;

    /**
     * Constructs maneuver detector which is supposed to be used for maneuver detection within the provided tracked race
     * for provided competitor.
     * 
     * @param trackedRace
     *            The tracked race whose maneuvers are supposed to be detected
     * @param competitor
     *            The competitor, whose maneuvers shall be discovered
     */
    public AbstractManeuverDetectorImpl(TrackedRace trackedRace, Competitor competitor) {
        this.trackedRace = trackedRace;
        this.competitor = competitor;
        this.track = trackedRace != null ? trackedRace.getTrack(competitor) : null;
    }

    @Override
    public TrackTimeInfo getTrackTimeInfo() {
        NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor);
        TimePoint earliestTrackRecord = null;
        TimePoint latestRawFixTimePoint = null;
        MarkPassing crossedFinishLine = null;
        // getLastWaypoint() will wait for a read lock on the course; do this outside the synchronized block to avoid
        // deadlocks
        final Waypoint lastWaypoint = trackedRace.getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            trackedRace.lockForRead(markPassings);
            try {
                if (markPassings != null && !markPassings.isEmpty()) {
                    earliestTrackRecord = markPassings.iterator().next().getTimePoint();
                    crossedFinishLine = trackedRace.getMarkPassing(competitor, lastWaypoint);
                }
            } finally {
                trackedRace.unlockAfterRead(markPassings);
            }
        }
        if (earliestTrackRecord == null) {
            GPSFixMoving firstRawFix = track.getFirstRawFix();
            if (firstRawFix != null) {
                earliestTrackRecord = firstRawFix.getTimePoint();
            }
        }
        if (earliestTrackRecord != null) {
            TimePoint latestTrackRecord;
            if (crossedFinishLine != null) {
                latestTrackRecord = crossedFinishLine.getTimePoint();
            } else {
                final GPSFixMoving lastRawFix = track.getLastRawFix();
                if (lastRawFix != null) {
                    latestTrackRecord = lastRawFix.getTimePoint();
                    latestRawFixTimePoint = latestTrackRecord;
                } else {
                    latestTrackRecord = null;
                }
            }
            if (latestTrackRecord != null) {
                if (latestRawFixTimePoint == null) {
                    final GPSFixMoving lastRawFix = track.getLastRawFix();
                    if (lastRawFix != null) {
                        latestRawFixTimePoint = lastRawFix.getTimePoint();
                    }
                }
                if (latestRawFixTimePoint != null) {
                    if (!earliestTrackRecord.equals(latestTrackRecord)) {
                        return new TrackTimeInfo(earliestTrackRecord, latestTrackRecord, latestRawFixTimePoint);
                    }
                    GPSFixMoving firstRawFix = track.getFirstRawFix();
                    if (firstRawFix != null) {
                        return new TrackTimeInfo(firstRawFix.getTimePoint(), latestRawFixTimePoint,
                                latestRawFixTimePoint);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the number of cases, when the boats bow was headed through the wind coming from behind.
     */
    protected int getNumberOfJibes(ManeuverCurveBoundaries maneuverBoundaries, Wind wind) {
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        int numberOfJibes = wind == null ? 0
                : bearingChangeAnalyzer.didPass(maneuverBoundaries.getSpeedWithBearingBefore().getBearing(),
                        maneuverBoundaries.getDirectionChangeInDegrees(),
                        maneuverBoundaries.getSpeedWithBearingAfter().getBearing(), wind.getBearing());
        return numberOfJibes;
    }

    /**
     * Gets the number of cases, when the boats bow was headed through the wind coming from the front.
     */
    protected int getNumberOfTacks(ManeuverCurveBoundaries maneuverBoundaries, Wind wind) {
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        int numberOfTacks = wind == null ? 0
                : bearingChangeAnalyzer.didPass(maneuverBoundaries.getSpeedWithBearingBefore().getBearing(),
                        maneuverBoundaries.getDirectionChangeInDegrees(),
                        maneuverBoundaries.getSpeedWithBearingAfter().getBearing(), wind.getFrom());
        return numberOfTacks;
    }

    /**
     * Maps the provided {@code courseChangeInDegrees} from {@link Bearing} to {@link NauticalSide}.
     */
    protected NauticalSide getDirectionOfCourseChange(double courseChangeInDegrees) {
        return courseChangeInDegrees < 0 ? NauticalSide.PORT : NauticalSide.STARBOARD;
    }

    /**
     * Gets the approximated duration of the maneuver main curve considering the boat class of the competitor.
     */
    protected Duration getApproximateManeuverDuration() {
        return trackedRace.getRace().getBoatOfCompetitor(competitor).getBoatClass().getApproximateManeuverDuration();
    }

}
