package com.sap.sailing.domain.shared.tracking.impl;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;

public class LineDetailsImpl implements LineDetails {
    private final TimePoint timePoint;
    private final Waypoint waypoint;
    private final Distance length;
    private final Bearing angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind;
    private final NauticalSide advantageousSidewhileApproachingLine;
    private final Distance advantage;
    private final Mark portMarkWhileApproachingLine;
    private final Mark starboardMarkWhileApproachingLine;
    private final Position portMarkPosition;
    private final Position starboardMarkPosition;
    
    public LineDetailsImpl(TimePoint timePoint, Waypoint waypoint, Distance length,
            Bearing angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind, NauticalSide advantageousSideWhileApproachingLine,
            Distance advantage, Mark portMarkWhileApproachingLine, Mark starboardMarkWhileApproachingLine,
            Position portMarkPosition, Position starboardMarkPosition) {
        super();
        this.timePoint = timePoint;
        this.waypoint = waypoint;
        this.length = length;
        this.angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind = angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind;
        this.advantageousSidewhileApproachingLine = advantageousSideWhileApproachingLine;
        this.advantage = advantage;
        this.portMarkWhileApproachingLine = portMarkWhileApproachingLine;
        this.starboardMarkWhileApproachingLine = starboardMarkWhileApproachingLine;
        this.portMarkPosition = portMarkPosition;
        this.starboardMarkPosition = starboardMarkPosition;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }
    
    @Override
    public Waypoint getWaypoint() {
        return waypoint;
    }

    @Override
    public Distance getLength() {
        return length;
    }

    @Override
    public Bearing getAngleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind() {
        return angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind;
    }

    @Override
    public NauticalSide getAdvantageousSideWhileApproachingLine() {
        return advantageousSidewhileApproachingLine;
    }

    @Override
    public Distance getAdvantage() {
        return advantage;
    }

    @Override
    public Mark getStarboardMarkWhileApproachingLine() {
        return starboardMarkWhileApproachingLine;
    }

    @Override
    public Mark getPortMarkWhileApproachingLine() {
        return portMarkWhileApproachingLine;
    }

    @Override
    public Position getPortMarkPosition() {
        return portMarkPosition;
    }

    @Override
    public Position getStarboardMarkPosition() {
        return starboardMarkPosition;
    }

}
