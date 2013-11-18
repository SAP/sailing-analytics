package com.sap.sailing.domain.racelog.test;

import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.impl.RaceLogEventImpl;

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

        public TestRaceLogEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
                Serializable pId, List<Competitor> pInvolvedBoats, int pPassId) {
            super(createdAt, author, logicalTimePoint, pId, pInvolvedBoats, pPassId);
        }

        @Override
        public void accept(RaceLogEventVisitor visitor) { }
        
    }
    
}
