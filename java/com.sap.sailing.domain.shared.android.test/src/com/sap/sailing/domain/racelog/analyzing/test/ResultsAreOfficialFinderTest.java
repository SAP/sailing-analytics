package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogResultsAreOfficialEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ResultsAreOfficialFinder;

public class ResultsAreOfficialFinderTest extends RaceLogAnalyzerTest<ResultsAreOfficialFinder> {
    @Override
    protected ResultsAreOfficialFinder createAnalyzer(RaceLog raceLog) {
        return new ResultsAreOfficialFinder(raceLog);
    }

    @Test
    public void testFalseForNoResultsAreOfficialEvent() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testSingleLast() {
        RaceLogResultsAreOfficialEvent event1 = createEvent(RaceLogResultsAreOfficialEvent.class, 1);
        RaceLogResultsAreOfficialEvent event2 = createEvent(RaceLogResultsAreOfficialEvent.class, 2);
        raceLog.add(event1);
        raceLog.add(event2);
        RaceLogResultsAreOfficialEvent result = analyzer.analyze();
        assertSame(event2, result);
    }
}
