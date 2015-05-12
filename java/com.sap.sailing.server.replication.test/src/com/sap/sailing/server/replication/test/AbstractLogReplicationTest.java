package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.ranking.RankingMetrics;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;

public abstract class AbstractLogReplicationTest<LogT extends AbstractLog<EventT, VisitorT>, EventT extends AbstractLogEvent<VisitorT>, VisitorT>
        extends AbstractServerReplicationTest {
    protected static final String BOAT_CLASS_NAME_49er = "49er";
    
    /**
     * Uses a new master that loads the existing regatta and race log to append an event to the race log. This will store it to the DB so that if the original
     * master re-loads the race log it should see the new race log event.
     * @throws Exception 
     */
    protected void addEventToDB(RaceLogIdentifier raceLogIdentifier, RaceLogRaceStatusEvent createRaceStatusEvent, String regattaName, String raceColumnName, String fleetName) throws Exception {
        final RacingEventServiceImpl temporaryMaster = createNewMaster();
        Regatta regatta = temporaryMaster.getRegatta(new RegattaName(regattaName+" ("+BOAT_CLASS_NAME_49er+")"));
        Series series = regatta.getSeries().iterator().next();
        RaceLog masterLog = series.getRaceColumnByName(raceColumnName).getRaceLog(series.getFleetByName(fleetName));
        masterLog.add(createRaceStatusEvent);
    }
    
    /**
     * Validation is done based only on the identifier and type of the {@link RaceLogEvent}s.
     */
    protected void addAndValidateEventIds(LogT masterLog, LogT replicaLog, @SuppressWarnings("unchecked") EventT... addedEvents) throws InterruptedException {
        // 1. Check state of replica after initial load...
        assertEqualsOnId(masterLog, replicaLog);
        // 2. ... add all incoming events...
        for (EventT event : addedEvents) {
            masterLog.add(event);
        }
        // 3. ... and give replication some time to deliver messages.
        Thread.sleep(3000);
        assertEqualsOnId(masterLog, replicaLog);
    }

    /**
     * Equality of the events is done based only on the identifier and type.
     */
    protected void assertEqualsOnId(LogT masterLog, LogT replicaLog) {
        replicaLog.lockForRead();
        try {
            masterLog.lockForRead();
            try {
                assertEqualsOnId(masterLog.getRawFixes(), replicaLog.getRawFixes());
            } finally {
                masterLog.unlockAfterRead();
            }
        } finally {
            replicaLog.unlockAfterRead();
        }
    }

    /**
     * Equality of the events is done based only on the {@link RaceLogEvent}'s identifier and type.
     */
    private void assertEqualsOnId(Iterable<EventT> expectedEvents, Iterable<EventT> actualEvents) {
        List<EventT> expectedCollection = new ArrayList<>();
        Util.addAll(expectedEvents, expectedCollection);
        List<EventT> actualCollection = new ArrayList<>();
        Util.addAll(actualEvents, actualCollection);
        assertEquals(Util.size(expectedEvents), Util.size(actualEvents));
        for (int i = 0; i < expectedCollection.size(); i++) {
            assertEquals(expectedCollection.get(i).getClass(), actualCollection.get(i).getClass());
            assertEquals(expectedCollection.get(i).getId(), actualCollection.get(i).getId());
        }
    }

    protected Regatta setupRegatta(final String regattaName, String seriesName, String fleetName, String boatClassName) {
        LinkedHashMap<String, SeriesCreationParametersDTO> seriesCreationParameters = new LinkedHashMap<>();
        SeriesCreationParametersDTO creationParametersForDefaultSeries = new SeriesCreationParametersDTO(
                Arrays.asList(new FleetDTO[] { new FleetDTO(fleetName, 0, Color.BLACK), }), /* medal */false, /* startsWithZero */
                false, /* firstColumnIsNonDiscardableCarryForward */false, /* discardingThresholds */new int[0], /* hasSplitFleetContiguousScoring */
                false);
        seriesCreationParameters.put(seriesName, creationParametersForDefaultSeries);
        // 1. Install some race column on master...
        RegattaCreationParametersDTO regattaCreationParams = new RegattaCreationParametersDTO(seriesCreationParameters);
        AddSpecificRegatta addRegattaOperation = new AddSpecificRegatta(regattaName, boatClassName, 
                /*startDate*/ null, /*endDate*/ null, /* regatta ID */ UUID.randomUUID(), regattaCreationParams, /* persistent */ true,
                new LowPoint(), /* default course area ID */ UUID.randomUUID(), /* useStartTimeInference */ true, RankingMetrics.ONE_DESIGN);
        return master.apply(addRegattaOperation);
    }

    protected FlexibleLeaderboard setupFlexibleLeaderboard(final String leaderboardName) {
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, leaderboardName, new int[] { 19, 44 }, new LowPoint(), null);
        FlexibleLeaderboard masterLeaderboard = master.apply(createTestLeaderboard);
        return masterLeaderboard;
    }

}