package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddLeaderboardGroupToEvent;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public class RemoveLeaderboardGroupTest {
    private RacingEventService server;
    private Event pfingstbusch;
    private UUID newGroupId;
    
    @BeforeEach
    public void setUp() {
        final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
        server = new RacingEventServiceImpl() {
            @Override
            public SecurityService getSecurityService() {
                return securityService;
            }
        };
        List<Event> allEvents = new ArrayList<>();
        Util.addAll(server.getAllEvents(), allEvents);
        for (final Event e : allEvents) {
            server.apply(new RemoveEvent(e.getId()));
        }
        Map<String, Leaderboard> allLeaderboards = new HashMap<>(server.getLeaderboards());
        for (final String leaderboardName : allLeaderboards.keySet()) {
            server.apply(new RemoveLeaderboard(leaderboardName));
        }
        Map<UUID, LeaderboardGroup> allLeaderboardGroups = new HashMap<>(server.getLeaderboardGroups());
        for (final LeaderboardGroup leaderboardGroup : allLeaderboardGroups.values()) {
            server.apply(new RemoveLeaderboardGroup(leaderboardGroup.getId()));
        }
        server.apply(new RemoveRegatta(new RegattaName("Pfingstbusch (29er)")));
        server.apply(new RemoveRegatta(new RegattaName("Pfingstbusch (470)")));
        server.apply(new RemoveRegatta(new RegattaName("Aalregatta (ORC)")));
        final Calendar cal = new GregorianCalendar();
        cal.set(2014, 5, 6, 10, 00);
        final TimePoint pfingstbuschStartDate = new MillisecondsTimePoint(cal.getTime());
        cal.set(2014, 5, 8, 16, 00);
        final TimePoint pfingstbuschEndDate = new MillisecondsTimePoint(cal.getTime());
        pfingstbusch = server.apply(new CreateEvent("Pfingstbusch", /* eventDescription */ null, pfingstbuschStartDate, pfingstbuschEndDate,
                "Kiel", /* isPublic */ true, UUID.randomUUID(), /* officialWebsiteURLAsString */ null, /*baseURL*/null,
                /* sailorsInfoWebsiteURLAsString */ null, /* images */Collections.<ImageDescriptor> emptyList(),
                /* videos */Collections.<VideoDescriptor> emptyList(), /* leaderboardGroupIds */ Collections.<UUID> emptyList()));
        newGroupId = UUID.randomUUID();
        final LeaderboardGroup pfingstbuschLeaderboardGroup = server
                .apply(new CreateLeaderboardGroup(newGroupId, "Pfingstbusch", "Pfingstbusch", /* displayName */ null,
                /* displayGroupsInReverseOrder */ false, /* leaderboard names */ Collections.emptyList(),
                new int[0], /* overallLeaderboardScoringSchemeType */ ScoringSchemeType.LOW_POINT));
        server.apply(new AddLeaderboardGroupToEvent(pfingstbusch.getId(), pfingstbuschLeaderboardGroup.getId()));
    }
    
    @Test
    public void testOverallLeaderboardPresent() {
        final Leaderboard overallLeaderboard = server.getLeaderboardByName("Pfingstbusch "+LeaderboardNameConstants.OVERALL);
        assertNotNull(overallLeaderboard);
    }

    @Test
    public void testOverallLeaderboardDisappearsWhenUpdatingLeaderboardGroup() {
        server.apply(new UpdateLeaderboardGroup(newGroupId, "Pfingstbusch", "Pfingstbusch", /* displayName */ null,
                /* newLeaderboardNames */ Collections.emptyList(),
                new int[0], /* overallLeaderboardScoringSchemeType */ null));
        final Leaderboard overallLeaderboard = server.getLeaderboardByName("Pfingstbusch "+LeaderboardNameConstants.OVERALL);
        assertNull(overallLeaderboard);
    }

    @Test
    public void testRemovingOverallLeaderboardUnlinksItFromLeaderboardGroup() {
        assertNotNull(pfingstbusch.getLeaderboardGroups().iterator().next().getOverallLeaderboard());
        server.apply(new RemoveLeaderboard("Pfingstbusch "+LeaderboardNameConstants.OVERALL));
        final Leaderboard overallLeaderboard = server.getLeaderboardByName("Pfingstbusch "+LeaderboardNameConstants.OVERALL);
        assertNull(overallLeaderboard);
        assertNull(pfingstbusch.getLeaderboardGroups().iterator().next().getOverallLeaderboard());
    }

    /**
     * Unlike earlier versions, the RemoveLeaderboardGroup no longer removes the OverallLeaderboard, because it cannot
     * ensure proper Ownership handling. This is no handled in the SailingServiceImpl
     */
    @Test
    public void testRemovingLeaderboardGroupDoesNotRemoveOverallLeaderboard() {
        assertEquals(1, Util.size(pfingstbusch.getLeaderboardGroups()));
        server.apply(new RemoveLeaderboardGroup(newGroupId));
        final Leaderboard overallLeaderboard = server
                .getLeaderboardByName("Pfingstbusch " + LeaderboardNameConstants.OVERALL);
        assertNotNull(overallLeaderboard);
        assertFalse(pfingstbusch.getLeaderboardGroups().iterator().hasNext());
    }
}
