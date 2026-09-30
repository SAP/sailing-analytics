package com.sap.sailing.polars.mining;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.datamining.components.FilterCriterion;

/**
 * Contains several filter criteria for incoming fixes concerning their validity for the polar aggregation.
 * 
 * It is used in the backend polar mining pipeline.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarFixFilterCriteria implements FilterCriterion<GPSFixMovingWithPolarContext> {

    private static final Logger logger = Logger.getLogger(PolarFixFilterCriteria.class.getName());

    /**
     * 0 if every competitor should be included. 1 if only the leading competitor should be included and so on.
     */
    private final double pctOfLeadingCompetitorsToInclude;

    /**
     * 
     * @param pctOfLeadingCompetitorsToInclude
     *            0 if only first competitor should be included.<br \>
     *            0.1 if the best 10% should be included 1 if every competitor should be included
     */
    public PolarFixFilterCriteria(double pctOfLeadingCompetitorsToInclude) {
        this.pctOfLeadingCompetitorsToInclude = pctOfLeadingCompetitorsToInclude;
    }

    public PolarFixFilterCriteria() {
        this.pctOfLeadingCompetitorsToInclude = 0;
    }

    @Override
    public boolean matches(GPSFixMovingWithPolarContext element) {
        boolean importantDataIsNotNull = importantDataIsNotNull(element);
        if (!importantDataIsNotNull) {
            return false;
        }
        boolean afterStartTime = isAfterStartTime(element);
        boolean beforeFinishTime = isBeforeFinishTime(element);
        boolean noDirectionChange = !hasDirectionChange(element);
        boolean isInLeadingCompetitors = true;
        if (pctOfLeadingCompetitorsToInclude > 0) {
            isInLeadingCompetitors = isInLeadingCompetitors(element.getRace(), element.getCompetitor(),
                    pctOfLeadingCompetitorsToInclude);
        }
        boolean isValid = importantDataIsNotNull && afterStartTime && beforeFinishTime && noDirectionChange
                && isInLeadingCompetitors;
        if (/* Remove boolean for logging of weird input into polar service */Boolean.FALSE && isValid) {
            checkForAbnormalFixesAndLog(element);
        }
        return isValid;
    }

    private void checkForAbnormalFixesAndLog(GPSFixMovingWithPolarContext element) {
        double twa = element.getAbsoluteAngleToTheWind().getObject().getDegrees();
        if (element.getLegType() == LegType.DOWNWIND) {
            if (Math.abs(twa) < 100) {
                logger.warning(String.format(
                        "Very wide downwind fix input to polar data container. Race: %s, Competitor: %s, Timepoint: %s, "
                                + "TWA: %s", element.getRace().getRace().getName(), element.getCompetitor().getName(),
                        element.getFix().getTimePoint(), twa));
            }
        } else if (element.getLegType() == LegType.UPWIND) {
            if (Math.abs(twa) > 70) {
                logger.warning(String.format(
                        "Very wide upwind fix input to polar data container. Race: %s, Competitor: %s, Timepoint: %s, "
                                + "TWA: %s", element.getRace().getRace().getName(), element.getCompetitor().getName(),
                        element.getFix().getTimePoint(), twa));
            } else if (Math.abs(twa) < 35) {
                logger.warning(String.format(
                        "Very narrow upwind fix input to polar data container. Race: %s, Competitor: %s, Timepoint: %s, "
                                + "TWA: %s", element.getRace().getRace().getName(), element.getCompetitor().getName(),
                        element.getFix().getTimePoint(), twa));
            }
        }
    }

    private boolean importantDataIsNotNull(GPSFixMovingWithPolarContext element) {
        BearingWithConfidence<Void> angleToTheWind = element.getAbsoluteAngleToTheWind();
        WindWithConfidence<Pair<Position, TimePoint>> windSpeed = element.getWind();
        SpeedWithBearingWithConfidence<TimePoint> boatSpeedWithConfidence = element.getBoatSpeed();
        boolean result = false;
        if (angleToTheWind != null && windSpeed != null && boatSpeedWithConfidence != null) {
            result = true;
        }
        return result;
    }

    public static boolean isInLeadingCompetitors(TrackedRace trackedRace, Competitor competitor,
            double numberOfLeadingCompetitorsToInclude) {
        boolean result;
        if (!trackedRace.isLive(new MillisecondsTimePoint(System.currentTimeMillis()))) {
            result = isInLeadingCompetitorsForReplayRace(trackedRace, competitor, numberOfLeadingCompetitorsToInclude);
        } else {
            result = isInLeadingCompetitorsForLiveRace(trackedRace, competitor, numberOfLeadingCompetitorsToInclude);
        }
        return result;
    }

    private static boolean isInLeadingCompetitorsForLiveRace(TrackedRace trackedRace, Competitor competitor,
            double pctOfLeadingCompetitorsToInclude) {
        boolean result = false;
        NavigableSet<MarkPassing> markPassingsOfCompetitor = trackedRace.getMarkPassings(competitor);
        if (!markPassingsOfCompetitor.isEmpty()) {
            Waypoint wayPoint = markPassingsOfCompetitor.last().getWaypoint();
            if (wayPoint != null) {
                Iterator<MarkPassing> markPassingsAtCompetitorsLastWayPoint = trackedRace.getMarkPassingsInOrder(
                        wayPoint).iterator();
                for (int i = 0; i < ((int) Math.max(
                        Math.round(pctOfLeadingCompetitorsToInclude * getNumberOfCompetitors(trackedRace)), 1)); i++) {
                    if (markPassingsAtCompetitorsLastWayPoint.hasNext()) {
                        if (markPassingsAtCompetitorsLastWayPoint.next().getCompetitor().equals(competitor)) {
                            result = true;
                            break;
                        }
                    } else {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static int getNumberOfCompetitors(TrackedRace trackedRace) {
        Iterable<Competitor> competitors = trackedRace.getRace().getCompetitors();
        int result;
        if (competitors instanceof Collection) {
            Collection<Competitor> competitorCollection = (Collection<Competitor>) competitors;
            result = competitorCollection.size();
        } else {
            int counter = 0;
            Iterator<Competitor> iterator = competitors.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                counter++;
            }
            result = counter;
        }
        return result;
    }

    private static boolean isInLeadingCompetitorsForReplayRace(TrackedRace trackedRace, Competitor competitor,
            double pctOfLeadingCompetitorsToInclude) {
        boolean result = false;
        Waypoint lastWaypoint = trackedRace.getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            Iterator<MarkPassing> finishPassings = trackedRace.getMarkPassingsInOrder(lastWaypoint).iterator();
            for (int i = 0; i < ((int) Math.max(
                    Math.round(pctOfLeadingCompetitorsToInclude * getNumberOfCompetitors(trackedRace)), 1)); i++) {
                if (finishPassings.hasNext()) {
                    if (finishPassings.next().getCompetitor().equals(competitor)) {
                        result = true;
                        break;
                    }
                } else {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    private boolean hasDirectionChange(GPSFixMovingWithPolarContext element) {
        GPSFixTrack<Competitor, GPSFixMoving> track = element.getRace().getTrack(element.getCompetitor());
        return track.hasDirectionChange(element.getFix().getTimePoint(), element.getRace().getRace().getBoatClass()
                .getManeuverDegreeAngleThreshold());
    }

    private boolean isBeforeFinishTime(GPSFixMovingWithPolarContext element) {
        TimePoint timepointOfFix = element.getFix().getTimePoint();
        boolean isBeforeFinish;
        TimePoint finishTime = calculateFinishTime(element.getRace(), element.getCompetitor());
        isBeforeFinish = finishTime == null || timepointOfFix.before(finishTime);
        return isBeforeFinish;
    }

    private TimePoint calculateFinishTime(TrackedRace race, Competitor competitor) {
        TimePoint finishTime = race.getEndOfRace();
        if (finishTime != null) {
            Course course = race.getRace().getCourse();
            if (course.getLastWaypoint() != course.getFirstWaypoint()) {
                MarkPassing finishPassing = race.getMarkPassing(competitor, course.getLastWaypoint());
                if (finishPassing != null) {
                    TimePoint passedFinishTimePoint = finishPassing.getTimePoint();
                    if (passedFinishTimePoint.before(finishTime)) {
                        finishTime = passedFinishTimePoint;
                    }
                }
            }
        }
        return finishTime;
    }

    private boolean isAfterStartTime(GPSFixMovingWithPolarContext element) {
        TimePoint timepointOfFix = element.getFix().getTimePoint();
        boolean isAfterStart;
        try {
            TimePoint startTime = calculateStartTime(element.getRace(), element.getCompetitor());
            isAfterStart = timepointOfFix.after(startTime);
        } catch (CompetitorDidNotStartYetException exception) {
            isAfterStart = false;
        }
        return isAfterStart;
    }

    private TimePoint calculateStartTime(TrackedRace race, Competitor competitor)
            throws CompetitorDidNotStartYetException {
        MarkPassing startPassing = race.getMarkPassing(competitor, race.getRace().getCourse().getFirstWaypoint());
        if (startPassing == null) {
            throw new CompetitorDidNotStartYetException();
        }
        TimePoint raceStartTime = race.getStartOfRace();
        TimePoint startTime;
        TimePoint passedStartTimePoint = startPassing.getTimePoint();
        if (raceStartTime == null || passedStartTimePoint.after(raceStartTime)) {
            startTime = passedStartTimePoint;
        } else {
            startTime = raceStartTime;
        }
        return startTime;
    }

    private class CompetitorDidNotStartYetException extends Exception {
        private static final long serialVersionUID = 7906688735433666009L;
    }

    @Override
    public Class<GPSFixMovingWithPolarContext> getElementType() {
        return GPSFixMovingWithPolarContext.class;
    }

}
