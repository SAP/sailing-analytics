package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.ws.rs.core.Response;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class RegattasResourceCompetitorRanksTest extends TrackBasedTest {
    private final CompetitorWithBoat regularCompetitor = createCompetitorWithBoat("Regular competitor");
    private final CompetitorWithBoat otherCompetitor = createCompetitorWithBoat("Other competitor");
    private final TimePoint start = new MillisecondsTimePoint(1000000);
    private RegattaLeaderboard leaderboard;
    private RacingEventService service;
    private DummyRegattasResource resource;
    private ThreadState subjectThreadState;

    @BeforeEach
    public void setUp() {
        subjectThreadState = new SubjectThreadState(mock(Subject.class));
        subjectThreadState.bind();
        setTrackedRace(createTestTrackedRace("Competitor ranks", "R1", "505",
                createCompetitorAndBoatsMap(regularCompetitor, otherCompetitor), start,
                /* useMarkPassingCalculator */ false));
        getTrackedRace().setStartTimeReceived(start);
        getTrackedRace().updateMarkPassings(regularCompetitor, createMarkPassings(regularCompetitor, start,
                new MillisecondsTimePoint(1060000), new MillisecondsTimePoint(1120000),
                new MillisecondsTimePoint(1180000), new MillisecondsTimePoint(1240000)));
        getTrackedRace().updateMarkPassings(otherCompetitor, createMarkPassings(otherCompetitor, start,
                new MillisecondsTimePoint(1070000), new MillisecondsTimePoint(1140000),
                new MillisecondsTimePoint(1210000), new MillisecondsTimePoint(1280000)));
        getTrackedRace().setEndOfTrackingReceived(new MillisecondsTimePoint(1300000));
        final Regatta regatta = getTrackedRace().getTrackedRegatta().getRegatta();
        final Fleet fleet = new FleetImpl("Default");
        final Series series = new SeriesImpl("Series", /* isMedal */ false,
                /* isFleetsCanRunInParallel */ false, Collections.singleton(fleet), Collections.singleton("R1"),
                /* trackedRegattaRegistry */ null);
        regatta.addSeries(series);
        leaderboard = new RegattaLeaderboardImpl(regatta, new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboard.getRaceColumnByName("R1").setTrackedRace(fleet, getTrackedRace());
        service = mock(RacingEventService.class);
        final DynamicTrackedRegatta trackedRegatta = mock(DynamicTrackedRegatta.class);
        when(service.getRegattaByName(regatta.getName())).thenReturn(regatta);
        when(service.getTrackedRegatta(regatta)).thenReturn(trackedRegatta);
        when(trackedRegatta.getExistingTrackedRace(getTrackedRace().getRace())).thenReturn(getTrackedRace());
        when(service.getLeaderboardByName(regatta.getName())).thenReturn(leaderboard);
        when(service.getBaseDomainFactory()).thenReturn(DomainFactory.INSTANCE);
        resource = spy(new DummyRegattasResource());
        doReturn(service).when(resource).getService();
        final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
        when(securityService.hasCurrentUserOneOfExplicitPermissions(regularCompetitor,
                SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)).thenReturn(/* value */ true);
        when(securityService.hasCurrentUserOneOfExplicitPermissions(otherCompetitor,
                SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)).thenReturn(/* value */ true);
        doReturn(securityService).when(resource).getSecurityService();
    }

    @AfterEach
    public void tearDown() {
        if (leaderboard != null) {
            leaderboard.destroy();
        }
        subjectThreadState.restore();
    }

    @Test
    public void testCompetitorRanksWithoutSuppression() throws Exception {
        assertCompetitorRanks(/* suppressOtherCompetitor */ false);
    }

    @Test
    public void testCompetitorRanksWithSuppressedCompetitor() throws Exception {
        assertCompetitorRanks(/* suppressOtherCompetitor */ true);
    }

    private void assertCompetitorRanks(final boolean suppressOtherCompetitor) throws Exception {
        leaderboard.setSuppressed(otherCompetitor, suppressOtherCompetitor);
        // Suppression removes the leaderboard row, but the competitor still belongs to the tracked race.
        assertTrue(Util.contains(getTrackedRace().getRace().getCompetitors(), otherCompetitor));
        final LeaderboardDTO dto = leaderboard.getLeaderboardDTO(/* timePoint */ null, Collections.singleton("R1"),
                /* addOverallDetails */ false, service, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertEquals(suppressOtherCompetitor ? 1 : 2, dto.rows.size());
        final Response response = resource.getCompetitorRanks(leaderboard.getName(), getTrackedRace().getRace().getName());
        final String entity = StreamingOutputUtil.getEntityAsString(response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), entity);
        final JSONObject json = (JSONObject) JSONValue.parse(entity);
        final JSONArray legs = (JSONArray) json.get("legs");
        assertEquals(/* expected */ 4, legs.size());
        final JSONArray competitors = (JSONArray) ((JSONObject) legs.get(/* index */ 0)).get("competitors");
        assertEquals(/* expected */ 2, competitors.size());
        for (final Object value : competitors) {
            final JSONObject competitor = (JSONObject) value;
            assertEquals(Boolean.TRUE, competitor.get("started"));
            assertEquals(Boolean.TRUE, competitor.get("finished"));
            if (regularCompetitor.getId().toString().equals(competitor.get("id"))) {
                assertEquals(/* expected */ 1L, competitor.get("rank"));
                assertEquals(/* expected */ 60000L, competitor.get("timeSinceGun-ms"));
            } else {
                assertEquals(otherCompetitor.getId().toString(), competitor.get("id"));
                assertEquals(/* expected */ 70000L, competitor.get("timeSinceGun-ms"));
                if (suppressOtherCompetitor) {
                    assertNull(competitor.get("rank"));
                    assertNull(competitor.get("tacks"));
                    assertFalse(competitor.containsKey("averageSOG-kts"));
                } else {
                    assertEquals(/* expected */ 2L, competitor.get("rank"));
                    assertNotNull(competitor.get("tacks"));
                }
            }
        }
    }
}
