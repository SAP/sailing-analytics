package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.impl.ScoreCorrectionImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public class LeaderboardForKielWeekTest extends OnlineTracTracBasedTest {

    private final Logger logger = Logger.getLogger(LeaderboardForKielWeekTest.class.getName());
    private FlexibleLeaderboardImpl leaderboard;

    public LeaderboardForKielWeekTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Test
    public void leaderboardWithOneRaceTest() throws URISyntaxException, NoWindException, IOException, InterruptedException {
        leaderboard = new FlexibleLeaderboardImpl("Kiel Week 2011 505s", new ScoreCorrectionImpl(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        loadRace("357c700a-9d9a-11e0-85be-406186cbf87c"); // 505 Race 2
        Competitor hasso = getCompetitorByName("Dr.Plattner");
        RaceColumn column = leaderboard.addRace(getTrackedRace(), "Test Race 1", /* medalRace */ false, defaultFleet);
        assertEquals(21, leaderboard.getTotalPoints(hasso, now), 0.000000001);
        Pair<Competitor, RaceColumn> key = new Pair<Competitor, RaceColumn>(hasso, column);
        assertEquals(21, leaderboard.getContent(now).get(key).getTotalPoints(), 0.000000001);
        assertEquals(21, leaderboard.getEntry(hasso, column, now).getTotalPoints(), 0.000000001);
        loadRace("e876c3a0-9da8-11e0-85be-406186cbf87c"); // 505 Race 3
        column = leaderboard.addRace(getTrackedRace(), "Test Race 2", /* medalRace */ false, defaultFleet);
        key = new Pair<Competitor, RaceColumn>(hasso, column);
        // In Race 3, Hasso ranked 31st
        assertEquals(52, leaderboard.getTotalPoints(hasso, now), 0.000000001);
        assertEquals(31, leaderboard.getContent(now).get(key).getTotalPoints(), 0.000000001);
        assertEquals(31, leaderboard.getEntry(hasso, column, now).getTotalPoints(), 0.000000001);
        loadRace("7c666e50-9dde-11e0-85be-406186cbf87c"); // 505 Race 4
        column = leaderboard.addRace(getTrackedRace(), "Test Race 3", /* medalRace */ false, defaultFleet);
        key = new Pair<Competitor, RaceColumn>(hasso, column);
        // now the second race is discarded because Hasso ranked worst compared to the other two; in race 4 he ranked 11th
        assertEquals(32, leaderboard.getTotalPoints(hasso, now), 0.000000001);
        assertEquals(11, leaderboard.getContent(now).get(key).getTotalPoints(), 0.000000001);
        assertEquals(11, leaderboard.getEntry(hasso, column, now).getTotalPoints(), 0.000000001);
    }

    private void loadRace(String raceId) throws MalformedURLException, IOException, InterruptedException,
            URISyntaxException {
        logger.info("Loading race " + raceId);
        setUp("event_20110609_KielerWoch", raceId, ReceiverType.RACECOURSE, ReceiverType.RACESTARTFINISH, ReceiverType.MARKPASSINGS);
        logger.info("Recording wind for " + raceId);
        getTrackedRace().recordWind(new WindImpl(/* position */ null, MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(70))), new WindSourceImpl(WindSourceType.WEB));
        logger.info("Fixing mark positions for " + raceId);
        fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        logger.info("Loaded race " + raceId);
    }
}
