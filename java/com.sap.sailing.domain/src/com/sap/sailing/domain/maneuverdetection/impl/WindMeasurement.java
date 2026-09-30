package com.sap.sailing.domain.maneuverdetection.impl;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

/**
 * Represents the time point, position and the wind course measured during analysis of a {@link ManeuverSpot}. In
 * contrast to {@link com.sap.sailing.domain.common.Wind}, this class allows wind course to be {@code null}, whereas
 * time point and position are not allowed to be {@code null}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindMeasurement {

    private final TimePoint timePoint;
    private final Position position;
    private final Bearing windCourse;

    /**
     * Constructs a wind measurement record.
     * 
     * @param timePoint
     *            The time point of wind measurement. Must not be {@code null}.
     * @param position
     *            The position of wind measurement. Must not be {@code null}.
     * @param windCourse
     *            The course of the wind, which may be {@code null}
     */
    public WindMeasurement(TimePoint timePoint, Position position, Bearing windCourse) {
        this.timePoint = timePoint;
        this.position = position;
        this.windCourse = windCourse;
    }

    /**
     * The time point when the wind was measured. Cannot not be {@code null}.
     */
    public TimePoint getTimePoint() {
        return timePoint;
    }

    /**
     * The position where the wind was measured. Cannot not be {@code null}.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * The wind course measured during maneuver analysis.
     * 
     * @return {@code null} if the wind was not available, or not necessary for analysis, otherwise the wind course.
     */
    public Bearing getWindCourse() {
        return windCourse;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((position == null) ? 0 : position.hashCode());
        result = prime * result + ((timePoint == null) ? 0 : timePoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WindMeasurement other = (WindMeasurement) obj;
        if (position == null) {
            if (other.position != null)
                return false;
        } else if (!position.equals(other.position))
            return false;
        if (timePoint == null) {
            if (other.timePoint != null)
                return false;
        } else if (!timePoint.equals(other.timePoint))
            return false;
        return true;
    }

}
