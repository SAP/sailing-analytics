package com.sap.sailing.domain.racelog.tracking.analyzing.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class AbstractRaceLogTrackingTest {

    protected RaceLog log;
    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("author0", 0);
    protected final AbstractLogEventAuthor author1 = new LogEventAuthorImpl("author1", 1);
    
    protected final TimePoint now = MillisecondsTimePoint.now();

    public AbstractRaceLogTrackingTest() {
        super();
    }

    @BeforeEach
    public void setup() {
        log = new RaceLogImpl("log");
    }

}