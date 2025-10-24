package com.sap.sailing.server.gateway.test.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMarkMappingFinder;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardsResourcePingMarkTest extends AbstractJaxRsApiTest {
    private Mark mark;
    private RegattaLog log;
    private RegattaLeaderboard leaderboard;
    
    public static final String PING_MARK_JSON =
            "    {\n" + 
            "      \"timestamp\" : 1427142552000,\n" + 
            "      \"latitude\" : 54.325246,\n" + 
            "      \"longitude\" : 10.148556,\n" + 
            "      \"speed\" : 3.61,\n" + 
            "      \"course\" : 258.11,\n" + 
            "    }";

    public static final String PING2_MARK_JSON =
            "    {\n" + 
            "      \"timestamp\" : 1427142562000,\n" + 
            "      \"latitude\" : 54.425246,\n" + 
            "      \"longitude\" : 10.248556,\n" + 
            "      \"speed\" : 3.61,\n" + 
            "      \"course\" : 258.11,\n" + 
            "    }";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mark = racingEventService.getBaseDomainFactory().getOrCreateMark("id", "name", "n");
        Regatta regatta = new RegattaImpl("regatta", new BoatClassImpl("49er", false), /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), Collections.singleton(new SeriesImpl("series", false, /* isFleetsCanRunInParallel */ true, Collections
                        .singleton(new FleetImpl("fleet")), Arrays.asList("column"), racingEventService)), false,
                new HighPoint(), 0, null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        racingEventService.addRegattaWithoutReplication(regatta);
        leaderboard = racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "regatta", new int[] {});
        log = leaderboard.getRegattaLike().getRegattaLog();
    }

    @Test
    public void testCheckinAndCheckout() throws Exception {
        {
            Response response = leaderboardsResource.pingMark(PING_MARK_JSON, leaderboard.getName(),
                    mark.getId().toString(), null);
        assertThat("response is ok", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
            Map<Mark, List<DeviceMappingWithRegattaLogEvent<Mark>>> mappings = new RegattaLogDeviceMarkMappingFinder(
                    log).analyze();
            List<DeviceMappingWithRegattaLogEvent<Mark>> mappingsForMark = mappings.get(mark);
        assertThat("one mapping was created for the one ping", mappingsForMark.size(), equalTo(1));
        assertOneFixPerMapping(mappingsForMark);
        }
        {
        // now produce a second ping; this should produce also only one fix; no additional artificial fixes are
        // created (anymore; they used to be before bug2851)
            Response response = leaderboardsResource.pingMark(PING2_MARK_JSON, leaderboard.getName(),
                    mark.getId().toString(), null);
        assertThat("response is ok", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
            Map<Mark, List<DeviceMappingWithRegattaLogEvent<Mark>>> mappings = new RegattaLogDeviceMarkMappingFinder(
                    log).analyze();
            List<DeviceMappingWithRegattaLogEvent<Mark>> mappingsForMark = mappings.get(mark);
        assertThat("Assert no additional mapping was created for the second ping", mappingsForMark.size(), equalTo(2));
        assertOneFixPerMapping(mappingsForMark);
        }
    }

    private void assertOneFixPerMapping(List<DeviceMappingWithRegattaLogEvent<Mark>> mappingsForMark)
            throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        for (DeviceMappingWithRegattaLogEvent<Mark> i : mappingsForMark) {
            DeviceIdentifier device = i.getDevice();
            // first ping produces exactly one fix:
            assertThat("all fixes stored", racingEventService.getSensorFixStore().getNumberOfFixes(device),
                    equalTo(1L));
        }
    }
}
