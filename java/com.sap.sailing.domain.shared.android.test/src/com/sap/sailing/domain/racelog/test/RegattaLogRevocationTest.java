package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDefinedMarkAnalyzer;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RegattaLogRevocationTest {
    private RegattaLog serverRegattaLog;
    private AbstractLogEventAuthor serverAuthor = new LogEventAuthorImpl("server", 2);
    
    @BeforeEach
    public void setup() {
        serverRegattaLog = new RegattaLogImpl("server");
    }
    
    private static TimePoint now() {
        return MillisecondsTimePoint.now();
    }
    
    private static void assertNumUnrevoked(RegattaLog regattaLog, int expectedNum) {
        regattaLog.lockForRead();
        try {
            assertEquals(expectedNum, Util.size(regattaLog.getUnrevokedEvents()));
        } finally {
            regattaLog.unlockAfterRead();
        }
    }
    
    private static void assertNumAll(RegattaLog regattaLog, int expectedNum) {
        regattaLog.lockForRead();
        try {
            assertEquals(expectedNum, Util.size(regattaLog.getRawFixes()));
        } finally {
            regattaLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testRevokingMarkDefinition() throws NotRevokableException {
        final Mark mark = new MarkImpl("Mark Name");
        RegattaLogDefineMarkEvent event = new RegattaLogDefineMarkEventImpl(
                now(), serverAuthor, now(), "id", mark);
        serverRegattaLog.add(event);
        RegattaLogDefinedMarkAnalyzer analyzer = new RegattaLogDefinedMarkAnalyzer(serverRegattaLog);
        final Collection<Mark> marksBeforeRevoke = analyzer.analyze();
        assertEquals(1, marksBeforeRevoke.size());
        assertSame(mark, marksBeforeRevoke.iterator().next());
        assertNumUnrevoked(serverRegattaLog, 1);

        serverRegattaLog.revokeEvent(serverAuthor, event);
        assertNumUnrevoked(serverRegattaLog, 0);
        assertNumAll(serverRegattaLog, 2);
        final Collection<Mark> marksAfterRevoke = analyzer.analyze();
        assertTrue(marksAfterRevoke.isEmpty());
    }
}
