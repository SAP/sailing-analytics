package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sse.common.Position;

public class CoursePositionsDTO implements IsSerializable {
    public RaceCourseDTO course;
    public List<Position> waypointPositions;
    public Set<MarkDTO> marks;
    
    public int totalLegsCount;
    
    /**
     * The leg number is 0 before the start, the number of the current leg during the race
     * and the number of the last leg at the end of the race even if the race has finished. 
     */
    public int currentLegNumber;
    
    /**
     * <code>null</code> if the start waypoint does not have exactly two marks with valid positions; in this case,
     * {@link #startLineAdvantageousSide}, {@link #startLineAdvantageInMeters} and {@link #startLineLengthInMeters} are
     * also both <code>null</code>.
     */
    public Double startLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind;
    public NauticalSide startLineAdvantageousSide;
    public Double startLineAdvantageInMeters;
    public Double startLineLengthInMeters;

    /**
     * <code>null</code> if the finish waypoint does not have exactly two marks with valid positions; in this case,
     * {@link #finishLineAdvantageousSide}, {@link #finishLineAdvantageInMeters} and
     * {@link #finishLineAdvantageInMeters} are also both <code>null</code>.
     */
    public Double finishLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind;
    public NauticalSide finishLineAdvantageousSide;
    public Double finishLineAdvantageInMeters;
    public Double finishLineLengthInMeters;

    public WaypointDTO getEndWaypointForLegNumber(int legNumber) {
        WaypointDTO result = null;
        if(legNumber > 0 && legNumber <= totalLegsCount && course != null && course.waypoints != null) {
            result = course.waypoints.get(legNumber);
        }
        return result;
    }

    public List<Position> getStartMarkPositions() {
        final List<Position> result = new ArrayList<>();
        if (course != null && course.waypoints != null && !course.waypoints.isEmpty()) {
            for (final MarkDTO mark : course.waypoints.get(0).controlPoint.getMarks()) {
                result.add(mark.position);
            }
        }
        return result;
    }

    public List<Position> getFinishMarkPositions() {
        final List<Position> result = new ArrayList<>();
        if (course != null && course.waypoints != null && !course.waypoints.isEmpty()) {
            for (final MarkDTO mark : course.waypoints.get(course.waypoints.size()-1).controlPoint.getMarks()) {
                result.add(mark.position);
            }
        }
        return result;
    }
}
