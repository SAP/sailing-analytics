package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogProtestStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TestRaceLogRemovalInRegatta extends AbstractTestStoringAndRetrievingRaceLogInRegatta {
    private final String blueFleetName = "Blue";
    
    public TestRaceLogRemovalInRegatta() throws UnknownHostException, MongoException {
        super();
    }

    @Override
    protected List<Fleet> createQualifyingFleets() {
        List<Fleet> result = new ArrayList<>();
        result.add(new FleetImpl(yellowFleetName));
        result.add(new FleetImpl(blueFleetName));
        return result;
    }

    /**
     * See also bug 1854, particularly comment #1; when a race column is created, the race log store will be queried for race log entries for that column.
     * When a race column is explicitly removed from a leaderboard, its race log events will still persist in the database. Therefore, when a new
     * race column with an equal name is created, the old race log events would be loaded. This is usually not the expected behavior. Therefore,
     * all race log events are expected to be removed for all its fleets when a race column is removed. This test asserts this behavior by
     * creating a leaderboard with a race column for two fleets, then adding events to their race logs, then removing the race column and creating
     * it again with equal name, asserting that the race logs for the two fleets are empty and in particular do not contain the events added
     * previously before the column was removed.
     */
    @Test
    public void testNewRaceLogForNewRaceColumn() {
        final Series qualification = regatta.getSeriesByName(seriesName);
        Fleet yellowFleet = qualification.getFleetByName(yellowFleetName);
        Fleet blueFleet = qualification.getFleetByName(blueFleetName);
        RaceLog yellowLog = qualification.getRaceColumnByName(raceColumnName).getRaceLog(yellowFleet);
        RaceLog blueLog = qualification.getRaceColumnByName(raceColumnName).getRaceLog(blueFleet);
        TimeRange protestTime = new TimeRangeImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(Duration.ONE_MINUTE.times(90)));
        RaceLogProtestStartTimeEvent expectedEventYellow = new RaceLogProtestStartTimeEventImpl(now, author, 0, protestTime);
        yellowLog.add(expectedEventYellow);
        RaceLogStartTimeEvent expectedEventBlue = new RaceLogStartTimeEventImpl(now, author, 0, MillisecondsTimePoint.now(), /* courseAreaId */ null);
        blueLog.add(expectedEventBlue);
        // these events are now expected to be stored persistently in the race log store
        //getLastError() is now deprecated - seems to run fine without (at least locally)
        //db.getLastError(); // synchronize on DB
        qualification.removeRaceColumn(raceColumnName);
        qualification.addRaceColumn(raceColumnName, /* trackedRegattaRegistry */ null /* no re-association with tracked regatta required */);
        RaceLog yellowLogAfterRecreation = qualification.getRaceColumnByName(raceColumnName).getRaceLog(yellowFleet);
        RaceLog blueLogAfterRecreation = qualification.getRaceColumnByName(raceColumnName).getRaceLog(blueFleet);
        yellowLogAfterRecreation.lockForRead();
        try {
            assertEquals(0, Util.size(yellowLogAfterRecreation.getRawFixes()));
        } finally {
            yellowLogAfterRecreation.unlockAfterRead();
        }
        blueLogAfterRecreation.lockForRead();
        try {
            assertEquals(0, Util.size(blueLogAfterRecreation.getRawFixes()));
        } finally {
            blueLogAfterRecreation.unlockAfterRead();
        }
    }
    

}
