package com.sap.sailing.server.gateway.test.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceCompetitorMappingFinder;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorsInLogAnalyzer;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.SmartphoneUUIDIdentifier;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardsResourceCheckinAndOutTest extends AbstractJaxRsApiTest {
    private CompetitorWithBoat competitor;
    private RegattaLog log;
    private RegattaLeaderboard leaderboard;
    private BoatClass boatClass = new BoatClassImpl("49er", false);
    private Regatta regatta;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Competitor c = createCompetitors(1).get(0);
        Boat boat = racingEventService.getBaseDomainFactory().getOrCreateBoat("boat", "boat", boatClass, "GER1", null, /* storePersistently */ true);
        competitor = racingEventService.getBaseDomainFactory().getOrCreateCompetitorWithBoat(c.getId(), c.getName(), c.getShortName(),
                c.getColor(), c.getEmail(), c.getFlagImage(), (DynamicTeam) c.getTeam(),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, (DynamicBoat) boat, /* storePersistently */ true);
        regatta = new RegattaImpl("regatta", boatClass, /* canBoatsOfCompetitorsChangePerRace */ false, CompetitorRegistrationType.CLOSED,
                MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), Collections.singleton(new SeriesImpl("series", false, /* isFleetsCanRunInParallel */ true, Collections
                        .singleton(new FleetImpl("fleet")), Arrays.asList("column"), racingEventService)), false,
                new HighPoint(), 0, null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        racingEventService.addRegattaWithoutReplication(regatta);
        leaderboard = racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "regatta", new int[] {});
        regatta.registerCompetitor(competitor);
        log = leaderboard.getRegattaLike().getRegattaLog();
    }

    @Test
    public void testCheckinAndCheckoutWithPermission() throws Exception {
        Mockito.doReturn(true).when(securityService).hasCurrentUserUpdatePermission(leaderboard);
        testCheckinAndCheckout(null);
    }
    
    @Test
    public void testCheckinAndCheckoutWithSecret() throws Exception {
        String secret = "this-is-my-secret";
        regatta.setRegistrationLinkSecret(secret);
        testCheckinAndCheckout(secret);
    }
    
    private void testCheckinAndCheckout(String secretOrNull) throws Exception {
        UUID deviceUuid = UUID.randomUUID();
        
        // checkin
        long fromMillis = 500;
        JSONObject json = new JSONObject();
        json.put(DeviceMappingConstants.JSON_COMPETITOR_ID_AS_STRING, competitor.getId().toString());
        json.put(DeviceMappingConstants.JSON_DEVICE_UUID, deviceUuid.toString());
        json.put(DeviceMappingConstants.JSON_FROM_MILLIS, fromMillis);
        if (secretOrNull != null) {
            json.put(DeviceMappingConstants.JSON_REGISTER_SECRET, secretOrNull);
        }
        Response response = leaderboardsResource.postCheckin(json.toString(), leaderboard.getName());
        assertThat("checkin returns OK", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        Iterable<Competitor> registeredCompetitors = new CompetitorsInLogAnalyzer<>(log).analyze();
        Map<Competitor, List<DeviceMappingWithRegattaLogEvent<Competitor>>> mappings = new RegattaLogDeviceCompetitorMappingFinder(
                log).analyze();
        assertThat("competitor was registered", Util.size(registeredCompetitors), equalTo(1));
        assertThat("device mappings for competitor exist", mappings.size(), equalTo(1));
        List<DeviceMappingWithRegattaLogEvent<Competitor>> mappingsForC = mappings.get(competitor);
        assertThat("exactly one device mapping for competitor exists", mappingsForC.size(), equalTo(1));
        DeviceMapping<Competitor> mappingForC = mappingsForC.get(0);
        assertThat("that mapping is for the correct device",
                ((SmartphoneUUIDIdentifier) mappingForC.getDevice()).getUUID(), equalTo(deviceUuid));
        assertThat("that mapping starts at the correct timepoint", mappingForC.getTimeRange().from().asMillis(),
                equalTo(fromMillis));
        assertThat("that mapping is open-ended", mappingForC.getTimeRange().hasOpenEnd(), equalTo(true));
        // checkout
        long toMillis = 1000;
        json = new JSONObject();
        json.put(DeviceMappingConstants.JSON_TO_MILLIS, toMillis);
        json.put(DeviceMappingConstants.JSON_COMPETITOR_ID_AS_STRING, competitor.getId().toString());
        json.put(DeviceMappingConstants.JSON_DEVICE_UUID, deviceUuid.toString());
        if (secretOrNull != null) {
            json.put(DeviceMappingConstants.JSON_REGISTER_SECRET, secretOrNull);
        }
        response = leaderboardsResource.postCheckout(json.toString(), leaderboard.getName());
        assertThat("checkout returns OK", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        mappings = new RegattaLogDeviceCompetitorMappingFinder(log).analyze();
        mappingForC = mappings.get(competitor).get(0);
        assertTrue(mappingForC.getTimeRange().includes(new MillisecondsTimePoint(toMillis)), "mapping now ends at checkout timepoint");
        assertFalse(mappingForC.getTimeRange().includes(new MillisecondsTimePoint(toMillis+1)), "mapping now ends at checkout timepoint");
    }

}
