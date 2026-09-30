package com.sap.sailing.windestimation.data;

import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MeterPerSecondSpeedImpl;

/**
 * Competitor track with elements (e.g. maneuvers or gps-fixes) which has been fetched during data import.
 * 
 * @author Vladislav Chumak (D069712)
 *
 * @param <T>
 *            The type of elements within a competitor track. E.g. maneuver, or gps-fix.
 */
public class CompetitorTrackWithEstimationData<T> {

    private final String regattaName;
    private final String raceName;
    private final String competitorName;
    private final BoatClass boatClass;
    private final List<T> elements;
    private final double avgIntervalBetweenFixesInSeconds;
    private final Distance distanceTravelled;
    private final TimePoint trackStartTimePoint;
    private final TimePoint trackEndTimePoint;
    private final int markPassingsCount;
    private final int waypointsCount;

    public CompetitorTrackWithEstimationData(String regattaName, String raceName, String competitorName,
            BoatClass boatClass, List<T> elements, double avgIntervalBetweenFixesInSeconds, Distance distanceTravelled,
            TimePoint trackStartTimePoint, TimePoint trackEndTimePoint, int markPassingsCount, int waypointsCount) {
        this.competitorName = competitorName;
        this.boatClass = boatClass;
        this.elements = elements;
        this.avgIntervalBetweenFixesInSeconds = avgIntervalBetweenFixesInSeconds;
        this.distanceTravelled = distanceTravelled;
        this.trackStartTimePoint = trackStartTimePoint;
        this.trackEndTimePoint = trackEndTimePoint;
        this.markPassingsCount = markPassingsCount;
        this.waypointsCount = waypointsCount;
        this.regattaName = regattaName;
        this.raceName = raceName;
    }

    public String getCompetitorName() {
        return competitorName;
    }

    public BoatClass getBoatClass() {
        return boatClass;
    }

    public List<T> getElements() {
        return elements;
    }

    public double getAvgIntervalBetweenFixesInSeconds() {
        return avgIntervalBetweenFixesInSeconds;
    }

    public Distance getDistanceTravelled() {
        return distanceTravelled;
    }

    public Duration getDuration() {
        return trackStartTimePoint == null || trackEndTimePoint == null ? Duration.NULL
                : trackStartTimePoint.until(trackEndTimePoint);
    }

    public TimePoint getTrackStartTimePoint() {
        return trackStartTimePoint;
    }

    public TimePoint getTrackEndTimePoint() {
        return trackEndTimePoint;
    }

    public int getMarkPassingsCount() {
        return markPassingsCount;
    }

    public int getWaypointsCount() {
        return waypointsCount;
    }

    public String getRegattaName() {
        return regattaName;
    }

    public String getRaceName() {
        return raceName;
    }

    public boolean isClean() {
        return avgIntervalBetweenFixesInSeconds < 8 && new MeterPerSecondSpeedImpl(
                distanceTravelled.getMeters() / trackStartTimePoint.until(trackEndTimePoint).asSeconds())
                        .getKnots() > 2;
    }

    public <S> CompetitorTrackWithEstimationData<S> constructWithElements(List<S> elements) {
        return new CompetitorTrackWithEstimationData<S>(getRegattaName(), getRaceName(), getCompetitorName(),
                getBoatClass(), elements, getAvgIntervalBetweenFixesInSeconds(), getDistanceTravelled(),
                getTrackStartTimePoint(), getTrackEndTimePoint(), getMarkPassingsCount(), getWaypointsCount());
    }

}
