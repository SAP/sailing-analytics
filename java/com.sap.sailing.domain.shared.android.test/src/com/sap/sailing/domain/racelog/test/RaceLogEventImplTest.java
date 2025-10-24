package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.common.TimePoint;

public class RaceLogEventImplTest {
    
    public void testGetTimePointRedirection() {
        TimePoint createdAt = mock(TimePoint.class);
        TimePoint logicalTimePoint = mock(TimePoint.class);
        
        RaceLogEvent event = new TestRaceLogEvent(createdAt, null, logicalTimePoint, null, null, 0);
        assertSame(createdAt, event.getCreatedAt());
        assertSame(logicalTimePoint, event.getTimePoint());
        assertSame(createdAt, event.getLogicalTimePoint());
    }

    private static class TestRaceLogEvent extends RaceLogEventImpl {
        private static final long serialVersionUID = -5856509136541176818L;

        public TestRaceLogEvent(TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint,
                Serializable pId, List<Competitor> pInvolvedBoats, int pPassId) {
            super(createdAt, logicalTimePoint, author, pId, pInvolvedBoats, pPassId);
        }

        @Override
        public void accept(RaceLogEventVisitor visitor) { }
        
    }
    
}
