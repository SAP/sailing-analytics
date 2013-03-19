package com.sap.sailing.domain.racelog.impl;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogPassChangeEvent;

public class RaceLogPassChangeEventImpl extends RaceLogEventImpl implements RaceLogPassChangeEvent {
    private static final long serialVersionUID = -3737606977320640630L;

    public RaceLogPassChangeEventImpl(TimePoint createdAt, TimePoint pTimePoint,
            Serializable pId, List<Competitor> pInvolvedBoats, int pPassId) {
        super(createdAt, pTimePoint, pId, pInvolvedBoats, pPassId);
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }

}
