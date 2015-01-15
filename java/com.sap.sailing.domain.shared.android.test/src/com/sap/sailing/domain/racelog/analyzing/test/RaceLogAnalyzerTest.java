package com.sap.sailing.domain.racelog.analyzing.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.UUID;

import org.junit.Before;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogAnalyzer;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class RaceLogAnalyzerTest<AnalyzerType extends RaceLogAnalyzer<?>> {

    protected AnalyzerType analyzer;
    protected RaceLog raceLog;

    @Before
    public void setUpBase() {
        raceLog = new RaceLogImpl("RaceLogTest", "test-identifier");
        analyzer = createAnalyzer(raceLog);
    }
    
    protected abstract AnalyzerType createAnalyzer(RaceLog raceLog);
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds) {
        return createEvent(type, milliseconds, 0, UUID.randomUUID());
    }
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds, Serializable id) {
        return createEvent(type, milliseconds, 0, id);
    }
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds, int passId) {
        return createEvent(type, milliseconds, passId, UUID.randomUUID());
    }
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds, int passId, Serializable id) {
        return createEvent(type, milliseconds, passId, id, mock(AbstractLogEventAuthor.class));
    }
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds, int passId, AbstractLogEventAuthor author) {
        return createEvent(type, milliseconds, passId, UUID.randomUUID(), author);
    }
    
    protected static <T extends RaceLogEvent> T createEvent(Class<T> type, long milliseconds, int passId, Serializable id, AbstractLogEventAuthor author) {
        T event = mock(type);
        when(event.getAuthor()).thenReturn(author);
        when(event.getPassId()).thenReturn(passId);
        when(event.getCreatedAt()).thenReturn(new MillisecondsTimePoint(milliseconds));
        when(event.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(milliseconds));
        when(event.getId()).thenReturn(id);
        return event;
    }

}
