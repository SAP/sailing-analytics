package com.sap.sailing.domain.abstractlog.race.impl;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.common.TimePoint;

public class RaceLogProtestStartTimeEventImpl extends RaceLogEventImpl implements RaceLogProtestStartTimeEvent {
    private static final long serialVersionUID = -1800827552916395996L;
    
    private final TimePoint protestStartTime;
    
    public RaceLogProtestStartTimeEventImpl(TimePoint createdAt, TimePoint pTimePoint, AbstractLogEventAuthor author,
            Serializable pId, List<Competitor> pInvolvedBoats, int pPassId, TimePoint protestStartTime) {
        super(createdAt, pTimePoint, author, pId, pInvolvedBoats, pPassId);
        this.protestStartTime = protestStartTime;
    }
    
    public RaceLogProtestStartTimeEventImpl(TimePoint pTimePoint, AbstractLogEventAuthor author,
            int pPassId, TimePoint protestStartTime) {
        super(pTimePoint, author, pPassId);
        this.protestStartTime = protestStartTime;
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public TimePoint getProtestStartTime() {
        return protestStartTime;
    }

    @Override
    public String getShortInfo() {
        return "protestStartTime=" + protestStartTime;
    }

}
