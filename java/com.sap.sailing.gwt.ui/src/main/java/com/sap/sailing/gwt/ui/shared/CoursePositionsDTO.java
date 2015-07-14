package com.sap.sailing.gwt.ui.shared;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Position;

public class CoursePositionsDTO implements IsSerializable {
    public RaceCourseDTO course;
    public List<Position> waypointPositions;
    public List<Position> startMarkPositions;
    public List<Position> finishMarkPositions;
    public Set<MarkDTO> marks;
    
    public int totalLegsCount;
    public int currentLegNumber;
    
    /**
     * <code>null</code> if the start waypoint does not have exactly two marks with valid positions; in this case,
     * {@link #startLineAdvantageousSide}, {@link #startLineAdvantageInMeters} and {@link #startLineLengthInMeters} are
     * also both <code>null</code>.
     */
    public Double startLineAngleToCombinedWind;
    public NauticalSide startLineAdvantageousSide;
    public Double startLineAdvantageInMeters;
    public Double startLineLengthInMeters;

    /**
     * <code>null</code> if the finish waypoint does not have exactly two marks with valid positions; in this case,
     * {@link #finishLineAdvantageousSide}, {@link #finishLineAdvantageInMeters} and
     * {@link #finishLineAdvantageInMeters} are also both <code>null</code>.
     */
    public Double finishLineAngleToCombinedWind;
    public NauticalSide finishLineAdvantageousSide;
    public Double finishLineAdvantageInMeters;
    public Double finishLineLengthInMeters;
}
