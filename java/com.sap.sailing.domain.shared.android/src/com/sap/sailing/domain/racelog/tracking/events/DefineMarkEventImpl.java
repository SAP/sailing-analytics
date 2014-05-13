package com.sap.sailing.domain.racelog.tracking.events;

import java.io.Serializable;
import java.util.Collections;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.impl.RaceLogEventImpl;
import com.sap.sailing.domain.racelog.tracking.DefineMarkEvent;

public class DefineMarkEventImpl extends RaceLogEventImpl implements DefineMarkEvent {
    private static final long serialVersionUID = 277007856878002208L;
    
    private final Mark mark;
    
    public DefineMarkEventImpl(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable pId, int pPassId, Mark mark) {
        super(createdAt, author, logicalTimePoint, pId, Collections.<Competitor>emptyList(), pPassId);
         this.mark = mark;
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Mark getMark() {
        return mark;
    }

}
