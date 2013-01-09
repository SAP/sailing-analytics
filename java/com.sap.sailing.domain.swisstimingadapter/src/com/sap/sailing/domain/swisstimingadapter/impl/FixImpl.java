package com.sap.sailing.domain.swisstimingadapter.impl;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.TrackerType;

public class FixImpl implements Fix {
    private final String boatID;
    private final TrackerType trackerType;
    private final Long ageOfDataInMilliseconds;
    private final Position position;
    private final SpeedWithBearing speed;
    private final Integer nextMarkIndex;
    private final Integer rank;
    private final Speed averageSpeedOverGroundPerLeg;
    private final Speed velocityMadeGood;
    private final Distance distanceToLeader;
    private final Distance distanceToNextMark;
    
    /**
     * A disqualification reason, such as "DNC" or "DNS". May be <code>null</code>.
     */
    private final String boatIRM;
    
    public FixImpl(String boatID, TrackerType trackerType, Long ageOfDataInMilliseconds, Position position,
            SpeedWithBearing speed, Integer nextMarkIndex, Integer rank, Speed averageSpeedOverGroundPerLeg,
            Speed velocityMadeGood, Distance distanceToLeader, Distance distanceToNextMark, String boatIRM) {
        super();
        this.boatID = boatID;
        this.trackerType = trackerType;
        this.ageOfDataInMilliseconds = ageOfDataInMilliseconds;
        this.position = position;
        this.speed = speed;
        this.nextMarkIndex = nextMarkIndex;
        this.rank = rank;
        this.averageSpeedOverGroundPerLeg = averageSpeedOverGroundPerLeg;
        this.velocityMadeGood = velocityMadeGood;
        this.distanceToLeader = distanceToLeader;
        this.distanceToNextMark = distanceToNextMark;
        this.boatIRM = boatIRM;
    }

    @Override
    public String getBoatIRM() {
        return boatIRM;
    }

    @Override
    public String getBoatID() {
        return boatID;
    }

    @Override
    public TrackerType getTrackerType() {
        return trackerType;
    }

    @Override
    public Long getAgeOfDataInMilliseconds() {
        return ageOfDataInMilliseconds;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public SpeedWithBearing getSpeed() {
        return speed;
    }

    @Override
    public Integer getNextMarkIndex() {
        return nextMarkIndex;
    }

    @Override
    public Integer getRank() {
        return rank;
    }

    @Override
    public Speed getVelocityMadeGood() {
        return velocityMadeGood;
    }

    @Override
    public Distance getDistanceToLeader() {
        return distanceToLeader;
    }

    @Override
    public Distance getDistanceToNextMark() {
        return distanceToNextMark;
    }

    @Override
    public Speed getAverageSpeedOverGroundPerLeg() {
        return averageSpeedOverGroundPerLeg;
    }

}
