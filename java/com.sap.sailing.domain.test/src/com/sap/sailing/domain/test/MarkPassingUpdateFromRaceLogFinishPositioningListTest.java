package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests the implementation for bug3420: when a {@link RaceLogFinishPositioningConfirmedEvent} is found in a race log
 * that has a valid {@link CompetitorResult#getFinishingTime()} defined for a competitor, a wrapper mark passing is
 * expected to be generated with that time point for the finish waypoint. When the event is invalidated, e.g., by
 * detaching the race log or by starting a new pass or by sending a newer {@link RaceLogFinishPositioningConfirmedEvent}
 * or one that has higher priority, the finish mark passings need to be updated accordingly. This can mean that entirely
 * artificial mark passings need to be removed entirely; mark passings that only replaced an original mark passing by
 * one with a modified finish time need to be reverted back to the original mark passing.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class MarkPassingUpdateFromRaceLogFinishPositioningListTest extends AbstractManeuverDetectionTestCase {
    private FlexibleLeaderboard leaderboard;
    private RaceLog raceLog;
    private final AbstractLogEventAuthor author;
    
    public MarkPassingUpdateFromRaceLogFinishPositioningListTest() throws MalformedURLException, URISyntaxException {
        super();
        author = new LogEventAuthorImpl("Me", 0);
    }

    @Override
    protected String getExpectedEventName() {
        return "ESS Qingdao 2014";
    }


    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20140429_ESSQingdao-Race_4.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20140429_ESSQingdao-Race_4.txt").getCanonicalPath()),
                /* liveUri */null, /* storedUri */storedUri, new ReceiverType[] { ReceiverType.MARKPASSINGS,
                        ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, new MillisecondsTimePoint(dateFormat.parse("05/01/2014-09:02:00")),
                        new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(269))),
                new WindSourceImpl(WindSourceType.WEB));
        leaderboard = new FlexibleLeaderboardImpl("Test", new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), new CourseAreaImpl("Here", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        final RaceColumn raceColumn = leaderboard.addRace(getTrackedRace(), "R", /* medalRace */ false);
        raceLog = raceColumn.getRaceLog(raceColumn.getFleets().iterator().next());
    }
    
    /**
     * Tests the Race 4 for competitor "Alinghi" at a time where the maneuver detection test is likely to compute
     * a penalty although between the jibe and the tack there is a mark passing
     */
    @Test
    public void testOriginalFinishMarkPassingTime() throws ParseException, NoWindException {
        int passId = 0;
        Competitor alinghi = getCompetitorByName("Alinghi");
        final MarkPassing alinghiOriginalFinish = getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint());
        final TimePoint alinghiOriginalFinishingTime = alinghiOriginalFinish.getTimePoint();
        assertNotNull(alinghiOriginalFinish.getTimePoint());
        final TimePoint now = MillisecondsTimePoint.now();
        final CompetitorResults alinghiFinishesNow = createCompetitorResults(new Util.Pair<>(alinghi, now));
        raceLog.add(new RaceLogFinishPositioningConfirmedEventImpl(now, author, passId, alinghiFinishesNow));
        final MarkPassing alinghiCorrectedFinishMarkPassing = getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint());
        assertEquals(now, alinghiCorrectedFinishMarkPassing.getTimePoint());
        raceLog.add(new RaceLogPassChangeEventImpl(now.plus(Duration.ONE_SECOND), author, ++passId)); // this should invalidate the previously edited result
        assertEquals(alinghiOriginalFinishingTime, getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint()).getTimePoint());
        List<MarkPassing> withoutLast = new ArrayList<>();
        final NavigableSet<MarkPassing> alinghiOriginalMarkPassings = getTrackedRace().getMarkPassings(alinghi);
        getTrackedRace().lockForRead(alinghiOriginalMarkPassings);
        try {
            withoutLast.addAll(alinghiOriginalMarkPassings);
        } finally {
            getTrackedRace().unlockAfterRead(alinghiOriginalMarkPassings);
        }
        withoutLast.remove(withoutLast.size()-1);
        getTrackedRace().updateMarkPassings(alinghi, withoutLast);
        assertNull(getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint()));
        raceLog.add(new RaceLogFinishPositioningConfirmedEventImpl(now, author, passId, alinghiFinishesNow));
        final MarkPassing alinghiSyntheticFinish = getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint());
        assertEquals(now, alinghiSyntheticFinish.getTimePoint());
        assertNull(alinghiSyntheticFinish.getOriginal());
        // advancing to the next pass is expected to remove the synthetic mark passing again
        raceLog.add(new RaceLogPassChangeEventImpl(now.plus(Duration.ONE_HOUR), author, ++passId));
        assertNull(getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint()));
        raceLog.add(new RaceLogFinishPositioningConfirmedEventImpl(now, author, passId, alinghiFinishesNow));
        final MarkPassing alinghiSyntheticFinish2 = getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint());
        assertEquals(now, alinghiSyntheticFinish2.getTimePoint());
        assertNull(alinghiSyntheticFinish2.getOriginal());
        List<MarkPassing> withLastAgain = new ArrayList<>(withoutLast);
        withLastAgain.add(alinghiOriginalFinish);
        getTrackedRace().updateMarkPassings(alinghi, withLastAgain);
        final MarkPassing alinghiNoLongerSynthetic = getTrackedRace().getMarkPassing(alinghi, getTrackedRace().getRace().getCourse().getLastWaypoint());
        assertSame(alinghiOriginalFinish, alinghiNoLongerSynthetic.getOriginal());
        assertTrue(alinghiNoLongerSynthetic.getOriginal() != alinghiNoLongerSynthetic);
    }

    @SafeVarargs
    private final CompetitorResults createCompetitorResults(Pair<Competitor, TimePoint>... finishingTimes) {
        final CompetitorResults results = new CompetitorResultsImpl();
        int rank = 1;
        for (Pair<Competitor, TimePoint> finishingTime : finishingTimes) {
            results.add(new CompetitorResultImpl(finishingTime.getA().getId(), finishingTime.getA().getName(), finishingTime.getA().getShortName(),
                    "BoatName", "BoatSailId", rank++, /* maxPointsReason */ null, /* score */null, finishingTime.getB(), /* comment */null, MergeState.OK));
        }
        return results;
    }
}
