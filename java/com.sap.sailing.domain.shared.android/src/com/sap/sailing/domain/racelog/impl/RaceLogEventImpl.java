package com.sap.sailing.domain.racelog.impl;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;

public abstract class RaceLogEventImpl implements RaceLogEvent {

    private static final long serialVersionUID = -5810258278984777732L;

    private final TimePoint createdAt;
    private final TimePoint logicalTimePoint;
    private final Serializable id;
    private final List<Competitor> involvedBoats;
    private final int passId;
    private final RaceLogEventAuthor author;

    public RaceLogEventImpl(TimePoint createdAt, RaceLogEventAuthor author, TimePoint pTimePoint,
            Serializable pId, List<Competitor> pInvolvedBoats, int pPassId) {
        this.createdAt = createdAt;
        this.author = author;
        this.logicalTimePoint = pTimePoint;
        this.id = pId;
        this.involvedBoats = pInvolvedBoats;
        this.passId = pPassId;
    }

    @Override
    public TimePoint getCreatedAt() {
        return createdAt;
    }

    @Override
    public TimePoint getTimePoint() {
        return logicalTimePoint;
    }

    @Override
    public Serializable getId() {
        return id;
    }

    @Override
    public List<Competitor> getInvolvedBoats() {
        return involvedBoats;
    }

    @Override
    public int getPassId() {
        return passId;
    }
    
    @Override
    public RaceLogEventAuthor getAuthor() {
        return author;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": createdAt: " + getCreatedAt() + ", logicalTimePoint: " + getTimePoint()
                + ", id: " + getId() + ", involvedBoats: " + getInvolvedBoats() + ", passId: " + getPassId() +
                ", author: "+getAuthor();
    }
}
