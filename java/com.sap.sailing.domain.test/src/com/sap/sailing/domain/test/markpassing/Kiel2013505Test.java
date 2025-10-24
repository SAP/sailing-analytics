package com.sap.sailing.domain.test.markpassing;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class Kiel2013505Test extends AbstractMarkPassingTest {

    public Kiel2013505Test() throws MalformedURLException, URISyntaxException {
        super();
    }

    /**
     * Marks the race as using a gate start
     */
    @Override
    protected void setUp(String raceNumber) throws Exception {
        super.setUp(raceNumber);
        final RaceLog raceLog = new RaceLogImpl("505 Race Log");
        getTrackedRace().attachRaceLog(raceLog);
        raceLog.add(new RaceLogStartProcedureChangedEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0), 0, RacingProcedureType.GateStart));
    }

    @Test
    public void testRace5() throws Exception {
        testRace("5");
    }

    @Override
    protected String getFileName() {
        return "event_20130626_KielerWoch-505_race_";
    }
    
    @Override
    protected String getExpectedEventName() {
        return "Kieler Woche 2013 - International";
    }
}


