package com.sap.sailing.domain.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.WindFixesFinder;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.impl.WindSummaryImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.confidence.BearingWithConfidenceCluster;
import com.sap.sse.common.confidence.Weigher;
import com.sap.sse.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * A service class that can calculate a {@link WindSummary} either from a {@link TrackedRace}, or alternatively, from
 * a {@link RaceLog} that may contain wind fixes.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceWindCalculator {
    public WindSummary getWindSummary(TrackedRace trackedRace, RaceLog raceLog) {
        final WindSummary windSummary;
        if (trackedRace != null) {
            windSummary = getTrackedRaceWindSummary(trackedRace);
        } else {
            windSummary = getWindSummaryFromRaceLog(raceLog);
        }
        return windSummary;
    }

    private WindSummary getTrackedRaceWindSummary(TrackedRace trackedRace) {
        final TimePoint startOfRace = trackedRace.getStartOfRace();
        final WindSummary result;
        if (startOfRace == null) {
            result = null;
        } else {
            final TimePoint finishedTime = trackedRace.getFinishedTime();
            final TimePoint endOfRace = trackedRace.getEndOfRace();
            final TimePoint finishTime = finishedTime == null ?
                    endOfRace == null ? MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis()) : endOfRace : finishedTime;
            final TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            final TimePoint toTimePoint;
            if (newestEvent != null && newestEvent.before(finishTime)) {
                toTimePoint = newestEvent;
            } else {
                toTimePoint = finishTime;
            }
            final TimePoint middleOfRace = startOfRace.plus(startOfRace.until(toTimePoint).divide(2));
            final List<TimePoint> pointsToGetWind = Arrays.asList(startOfRace, middleOfRace, toTimePoint);
            final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToUse = new ArrayList<>(3);
            for (TimePoint timePoint : pointsToGetWind) {
                final WindWithConfidence<Pair<Position, TimePoint>> windFixToUse = getWindFromTrackedRace(timePoint, trackedRace);
                if (windFixToUse != null) {
                    windFixesToUse.add(windFixToUse);
                }
            }
            result = createWindSummaryFromWindFixes(middleOfRace, windFixesToUse);
        }
        return result;
    }

    private WindSummary createWindSummaryFromWindFixes(final TimePoint middleOfRace,
            final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToUse) {
        Speed minTrueWindSpeed = null;
        Speed maxTrueWindSpeed = null;
        final BearingWithConfidenceCluster<TimePoint> bwcc = new BearingWithConfidenceCluster<TimePoint>(
                new Weigher<TimePoint>() {
                    private static final long serialVersionUID = -5779398785058438328L;
                    @Override
                    public double getConfidence(TimePoint fix, TimePoint request) {
                        return 1;
                    }
                });
        for (final WindWithConfidence<Pair<Position, TimePoint>> windFixToUse : windFixesToUse) {
            bwcc.add(new BearingWithConfidenceImpl<TimePoint>(windFixToUse.getObject().getBearing(),
                    windFixToUse.getConfidence(), windFixToUse.getObject().getTimePoint()));
            if (minTrueWindSpeed == null || minTrueWindSpeed.compareTo(windFixToUse.getObject()) > 0) {
                minTrueWindSpeed = windFixToUse.getObject();
            }
            if (maxTrueWindSpeed == null || maxTrueWindSpeed.compareTo(windFixToUse.getObject()) < 0) {
                maxTrueWindSpeed = windFixToUse.getObject();
            }
        }
        final WindSummary result;
        if (minTrueWindSpeed != null && maxTrueWindSpeed != null) {
            BearingWithConfidence<TimePoint> average = bwcc.getAverage(middleOfRace);
            result = new WindSummaryImpl(average.getObject().reverse(), minTrueWindSpeed,
                    maxTrueWindSpeed);
        } else {
            result = null;
        }
        return result;
    }

    public Wind checkForLatestWindFixFromRaceLog(RaceLog raceLog) {
        final Wind result;
        if (raceLog == null) {
            result = null;
        } else {
            WindFixesFinder windFixesFinder = new WindFixesFinder(raceLog);
            List<RaceLogWindFixEvent> windList = windFixesFinder.analyze();
            if (!windList.isEmpty()) {
                result = new RaceLogWindFixDeclinationHelper()
                        .getOptionallyDeclinationCorrectedWind(windList.get(windList.size() - 1));
            } else {
                result = null;
            }
        }
        return result;
    }

    public WindSummary getWindSummaryFromRaceLog(RaceLog raceLog) {
        final WindSummary result;
        if (raceLog == null) {
            result = null;
        } else {
            WindFixesFinder windFixesFinder = new WindFixesFinder(raceLog);
            List<RaceLogWindFixEvent> windList = windFixesFinder.analyze();
            // take the first, the last and one in the middle:
            if (!windList.isEmpty()) {
                final int[] indicesToUse = { 0, windList.size()/2, windList.size()-1 };
                final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToUse = new ArrayList<>(indicesToUse.length);
                for (final int i : indicesToUse) {
                    final Wind wind = new RaceLogWindFixDeclinationHelper().getOptionallyDeclinationCorrectedWind(windList.get(i));
                    // construct an artificial WindWithConfidence with 1.0 as confidence, "relative to itself"
                    final WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence =
                            new WindWithConfidenceImpl<Pair<Position,TimePoint>>(wind, /* confidence */ 1.0,
                                    new Pair<>(wind.getPosition(), wind.getTimePoint()), /* useSpeed */ true);
                    windFixesToUse.add(windWithConfidence);
                }
                result = createWindSummaryFromWindFixes(windFixesToUse.get(1).getObject().getTimePoint(), windFixesToUse);
            } else {
                result = null;
            }
        }
        return result;
    }

    public WindWithConfidence<Pair<Position, TimePoint>> getWindFromTrackedRace(
            TimePoint timePoint, TrackedRace trackedRace) {
        WindWithConfidence<Pair<Position, TimePoint>> averagedWindWithConfidence = trackedRace
                .getWindWithConfidence(trackedRace.getCenterOfCourse(timePoint), timePoint);
        final WindWithConfidence<Pair<Position, TimePoint>> result;
        if (averagedWindWithConfidence != null && averagedWindWithConfidence.getObject().getKnots() >= 0.05d) {
            result = averagedWindWithConfidence;
        } else {
            result = null;
        }
        return result;
    }
}
