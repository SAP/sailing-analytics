package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RaceColumnListenerWithDefaultAction;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * {@link RegattaLog} attachment is tricky. A {@link Series} can be linked to its {@link Regatta} late, after its
 * constructor has finished. Generally and theoretically it is possible that a {@link Series} changes belongs to
 * different regattas over time (see {@link Series#setRegatta(com.sap.sailing.domain.base.Regatta)}).
 * {@link RegattaLogEvent}s added to a {@link RegattaLog} are expected to be forwarded to the {@link RaceColumnListener}
 * s on {@link Regatta} or the {@link FlexibleLeaderboard}, respectively.
 * <p>
 * 
 * It is important to note that the {@link IsRegattaLike} listeners may be transient. Therefore it seems worthwhile
 * to test serialization / deserialization of the regatta or leaderboard and ensure that after deserialization the
 * regtta log event notification still works.<p>.
 * 
 * Bug 3448 describes a result of failing to do so. This test tries to assert that under a variety of different set-ups
 * these notifications are forwarded properly.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RegattaLogEventNotificationForwardingTest extends AbstractSerializationTest {
    @Test
    public void testFlexibleLeaderboardRegattaLogEventForwarding() {
        final RegattaLogEvent[] receivedRegattaLogEvent = new RegattaLogEvent[1];
        FlexibleLeaderboard leaderboard = createTestLeaderboard();
        leaderboard.addRaceColumnListener(new RaceColumnListenerWithDefaultAction() {
            private static final long serialVersionUID = -3835439531940986851L;

            @Override
            public void defaultAction() {
            }

            @Override
            public void regattaLogEventAdded(RegattaLogEvent event) {
                receivedRegattaLogEvent[0] = event;
            }
        });
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        leaderboard.getRegattaLog().add(event);
        assertSame(event, receivedRegattaLogEvent[0]);
    }

    @Test
    public void testFlexibleLeaderboardRegattaLogEventForwardingAfterDeserializingLeaderboard() throws ClassNotFoundException, IOException {
        final RegattaLogEvent[] receivedRegattaLogEvent = new RegattaLogEvent[1];
        FlexibleLeaderboard leaderboard = createTestLeaderboard();
        FlexibleLeaderboard deserializedLeaderboard = cloneBySerialization(leaderboard, DomainFactory.INSTANCE);
        deserializedLeaderboard.addRaceColumnListener(new RaceColumnListenerWithDefaultAction() {
            private static final long serialVersionUID = -3835439531940986851L;

            @Override
            public void defaultAction() {
            }

            @Override
            public void regattaLogEventAdded(RegattaLogEvent event) {
                receivedRegattaLogEvent[0] = event;
            }
        });
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        deserializedLeaderboard.getRegattaLog().add(event);
        assertSame(event, receivedRegattaLogEvent[0]);
    }

    private RegattaLogRegisterCompetitorEvent createRegattaLogEvent() {
        final RegattaLogRegisterCompetitorEvent event = new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), /* author */ null,
                AbstractLeaderboardTest.createCompetitor("Someone"));
        return event;
    }

    private FlexibleLeaderboard createTestLeaderboard() {
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Flexible Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(new int[0]),
                new LowPoint(), new CourseAreaImpl("Test Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        return leaderboard;
    }
    
    @Test
    public void testRegattaLeaderboardRegattaLogEventForwarding() {
        Regatta regatta = new RegattaImpl("test", null, true, CompetitorRegistrationType.CLOSED, null, null,
                new HashSet<Series>(), false, null, "test", null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        final RegattaLogEvent[] receivedRegattaLogEvent = new RegattaLogEvent[1];
        regatta.addRaceColumnListener(new RaceColumnListenerWithDefaultAction() {
            private static final long serialVersionUID = -3835439531940986851L;

            @Override
            public void defaultAction() {
            }

            @Override
            public void regattaLogEventAdded(RegattaLogEvent event) {
                receivedRegattaLogEvent[0] = event;
            }
        });
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        regatta.getRegattaLog().add(event);
        assertSame(event, receivedRegattaLogEvent[0]);
    }

    @Test
    public void testRegattaLeaderboardRegattaLogEventForwardingAfterRegattaDeserialization() throws ClassNotFoundException, IOException {
        Regatta regatta = new RegattaImpl("test", null, true, CompetitorRegistrationType.CLOSED, null, null,
                new HashSet<Series>(), false, null, "test", null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        Regatta deserializedRegatta = cloneBySerialization(regatta, DomainFactory.INSTANCE);
        final RegattaLogEvent[] receivedRegattaLogEvent = new RegattaLogEvent[1];
        deserializedRegatta.addRaceColumnListener(new RaceColumnListenerWithDefaultAction() {
            private static final long serialVersionUID = -3835439531940986851L;

            @Override
            public void defaultAction() {
            }

            @Override
            public void regattaLogEventAdded(RegattaLogEvent event) {
                receivedRegattaLogEvent[0] = event;
            }
        });
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        deserializedRegatta.getRegattaLog().add(event);
        assertSame(event, receivedRegattaLogEvent[0]);
    }
    
    @Test
    public void testAddCompetitorToRegattaAndEnsureCacheInvalidation() throws NoWindException, InterruptedException, ExecutionException {
        Regatta regatta = createRegatta();
        RegattaLeaderboard leaderboard = createRegattaLeaderboard(regatta);
        final TimePoint now = MillisecondsTimePoint.now();
        leaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(now);
        LeaderboardDTO dto = leaderboard.getLeaderboardDTO(now.plus(10), Collections.emptySet(), /* addOverallDetails */ false,
                /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertTrue(dto.competitors.isEmpty());
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        regatta.getRegattaLog().add(event);
        LeaderboardDTO dto2 = leaderboard.computeDTO(now.plus(20), Collections.emptySet(), /* addOverallDetails */ false,
                /* waitForLatestAnalyses */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertFalse(dto2.competitors.isEmpty());
        assertEquals(event.getCompetitor().getName(), dto2.competitors.get(0).getName());
        LeaderboardDTO dto3 = leaderboard.getLeaderboardDTO(now.plus(20), Collections.emptySet(), /* addOverallDetails */ false,
                /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertFalse(dto3.competitors.isEmpty());
        assertEquals(event.getCompetitor().getName(), dto3.competitors.get(0).getName());
    }

    private Regatta createRegatta() {
        Series series = new SeriesImpl("Test Series", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, 
                /* fleets */ Collections.singleton(new FleetImpl("Default")),
                Collections.singleton("R1"), /* trackedRegattaRegistry */ null);
        Regatta regatta = new RegattaImpl("test", null, true, CompetitorRegistrationType.CLOSED, null, null, Collections.singleton(series), false,
                new LowPoint(), "test", null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    @Test
    public void testAddCompetitorToRegattaAndEnsureCacheInvalidationOnDeserializedRegattaLeaderboard() throws NoWindException,
            InterruptedException, ExecutionException, ClassNotFoundException, IOException {
        Regatta regatta = createRegatta();
        RegattaLeaderboard leaderboard = createRegattaLeaderboard(regatta);
        final TimePoint now = MillisecondsTimePoint.now();
        leaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(now);
        RegattaLeaderboard deserializedLeaderboard = cloneBySerialization(leaderboard, DomainFactory.INSTANCE);
        LeaderboardDTO dto = deserializedLeaderboard.getLeaderboardDTO(now.plus(10), Collections.emptySet(), /* addOverallDetails */ false,
                /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertTrue(dto.competitors.isEmpty());
        final RegattaLogRegisterCompetitorEvent event = createRegattaLogEvent();
        deserializedLeaderboard.getRegatta().getRegattaLog().add(event);
        LeaderboardDTO dto2 = deserializedLeaderboard.computeDTO(now.plus(20), Collections.emptySet(), /* addOverallDetails */ false,
                /* waitForLatestAnalyses */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertFalse(dto2.competitors.isEmpty());
        assertEquals(event.getCompetitor().getName(), dto2.competitors.get(0).getName());
        LeaderboardDTO dto3 = deserializedLeaderboard.getLeaderboardDTO(now.plus(20), Collections.emptySet(), /* addOverallDetails */ false,
                /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertFalse(dto3.competitors.isEmpty());
        assertEquals(event.getCompetitor().getName(), dto3.competitors.get(0).getName());
    }

    protected RegattaLeaderboard createRegattaLeaderboard(Regatta regatta) {
        return new RegattaLeaderboardImpl(regatta, new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
    }
}
