package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.LineLengthAndAdvantage;

public class LineLengthAndAdvantageImpl implements LineLengthAndAdvantage {
    private final TimePoint timePoint;
    private final Waypoint waypoint;
    private final Distance length;
    private final Bearing absoluteAngleDifferenceToTrueWind;
    private final NauticalSide advantageousSidewhileApproachingLine;
    private final Distance advantage;
    private final Mark portMarkWhileApproachingLine;
    private final Mark starboardMarkWhileApproachingLine;
    
    public LineLengthAndAdvantageImpl(TimePoint timePoint, Waypoint waypoint, Distance length,
            Bearing absoluteAngleDifferenceToTrueWind, NauticalSide advantageousSideWhileApproachingLine,
            Distance advantage, Mark portMarkWhileApproachingLine, Mark starboardMarkWhileApproachingLine) {
        super();
        this.timePoint = timePoint;
        this.waypoint = waypoint;
        this.length = length;
        this.absoluteAngleDifferenceToTrueWind = absoluteAngleDifferenceToTrueWind;
        this.advantageousSidewhileApproachingLine = advantageousSideWhileApproachingLine;
        this.advantage = advantage;
        this.portMarkWhileApproachingLine = portMarkWhileApproachingLine;
        this.starboardMarkWhileApproachingLine = starboardMarkWhileApproachingLine;
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
    public Bearing getAbsoluteAngleDifferenceToTrueWind() {
        return absoluteAngleDifferenceToTrueWind;
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

}
