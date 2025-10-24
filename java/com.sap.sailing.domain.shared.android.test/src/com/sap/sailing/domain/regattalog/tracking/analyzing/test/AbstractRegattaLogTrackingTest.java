package com.sap.sailing.domain.regattalog.tracking.analyzing.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class AbstractRegattaLogTrackingTest {

    protected RegattaLog log;
    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("author0", 0);
    protected final AbstractLogEventAuthor author1 = new LogEventAuthorImpl("author1", 1);
    
    protected final TimePoint now = MillisecondsTimePoint.now();

    public AbstractRegattaLogTrackingTest() {
        super();
    }

    @BeforeEach
    public void setup() {
        log = new RegattaLogImpl("log");
    }

}