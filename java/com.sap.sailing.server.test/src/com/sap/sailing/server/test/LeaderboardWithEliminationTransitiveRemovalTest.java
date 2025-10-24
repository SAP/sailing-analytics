package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.DelegatingRegattaLeaderboardWithCompetitorElimination;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboardWithEliminations;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class LeaderboardWithEliminationTransitiveRemovalTest {
    private RacingEventService server;
    private Regatta regatta;
    private RegattaLeaderboard regattaLeaderboard;
    private DelegatingRegattaLeaderboardWithCompetitorElimination regattaLeaderboardWithEliminations;
    
    @BeforeEach
    public void setUp() {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase().drop();
        final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
        server = new RacingEventServiceImpl() {
            @Override
            public SecurityService getSecurityService() {
                return securityService;
            }
        };
        final LinkedHashMap<String, SeriesCreationParametersDTO> seriesStructure = new LinkedHashMap<>();
        final RegattaCreationParametersDTO regattaStructure = new RegattaCreationParametersDTO(seriesStructure);
        regatta = server.apply(new AddSpecificRegatta("Test", "Laser Int.", /* canBoatsOfCompetitorsChangePerRace */ false,
                /* competitorRegistrationType */ CompetitorRegistrationType.CLOSED, /* registrationLinkSecret */ null,
                /* startDate */ null, /* endDate */ null, /* id */ UUID.randomUUID(),
                regattaStructure, /* persistent */ true, new LowPoint(),
                /* courseAreaIds */ Collections.singleton(server.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Default", /* centerPosition */ null, /* radius */ null).getId()),
                /* buoyZoneRadiusInHullLengths */ null, /* useStartTimeInference */ false,
                /* controlTrackingFromStartAndFinishTimes */ true, /* autoRestartTrackingUponCompetitorSetChange */ false,
                RankingMetrics.ONE_DESIGN));
        regattaLeaderboard = server.apply(new CreateRegattaLeaderboard(regatta.getRegattaIdentifier(),
                /* leaderboardDisplayName */ null, /* discardThresholds */ new int[0]));
        regattaLeaderboardWithEliminations =
                server.apply(new CreateRegattaLeaderboardWithEliminations("With Eliminations", /* display name */ null,
                        regattaLeaderboard.getName()));
    }
    
    @Test
    public void testAllThere() {
        assertNotNull(regatta);
        assertNotNull(regattaLeaderboard);
        assertNotNull(regattaLeaderboardWithEliminations);
        assertSame(regatta, regattaLeaderboard.getRegatta());
        assertSame(regatta, regattaLeaderboardWithEliminations.getRegatta());
    }
    
    @Test
    public void testRemovingRegattaRemovesAllLeaderboards() {
        server.apply(new RemoveRegatta(regatta.getRegattaIdentifier()));
        assertNull(server.getLeaderboardByName(regattaLeaderboard.getName()));
        assertNull(server.getLeaderboardByName(regattaLeaderboardWithEliminations.getName()));
    }

    @Test
    public void testRemovingRegattaLeaderboardRemovesLeaderboardWithEliminations() {
        server.apply(new RemoveLeaderboard(regattaLeaderboard.getName()));
        assertNull(server.getLeaderboardByName(regattaLeaderboard.getName()));
        assertNull(server.getLeaderboardByName(regattaLeaderboardWithEliminations.getName()));
    }
}
