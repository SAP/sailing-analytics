package com.sap.sailing.domain.racelog.impl;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;

public class RaceLogStartTimeEventImpl extends RaceLogRaceStatusEventImpl implements RaceLogStartTimeEvent {

    private static final long serialVersionUID = 8185811395997196162L;
    private TimePoint startTime;

    public RaceLogStartTimeEventImpl(TimePoint createdAt, TimePoint pTimePoint, Serializable pId,
            List<Competitor> pInvolvedBoats, int pPassId, TimePoint pStartTime) {
        super(createdAt, pTimePoint, pId, pInvolvedBoats, pPassId, RaceLogRaceStatus.SCHEDULED);
        this.startTime = pStartTime;
    }

    @Override
    public TimePoint getStartTime() {
        return startTime;
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object object) {
        // / TODO: please check the implementation of this method... why is this needed?
        return super.equals(object) && object instanceof RaceLogStartTimeEvent
                && startTime.equals(((RaceLogStartTimeEvent) object).getStartTime());
    }

}
