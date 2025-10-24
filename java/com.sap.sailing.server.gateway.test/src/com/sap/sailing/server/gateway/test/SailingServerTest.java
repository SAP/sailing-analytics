package com.sap.sailing.server.gateway.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.server.gateway.impl.SailingServerImpl;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sse.common.Util;
import com.sap.sse.security.util.RemoteServerUtil;

public class SailingServerTest {
    private SailingServer sailingServer;
    
    @BeforeEach
    public void setUp() throws MalformedURLException {
        final URL devBaseUrl = new URL("https://dev.sapsailing.com");
        sailingServer = new SailingServerImpl(devBaseUrl, RemoteServerUtil.resolveBearerTokenForRemoteServer(devBaseUrl.toString(), "admin", "admin"));
    }
    
    @Test
    public void testLeaderboardGroupIds() throws Exception {
        final Iterable<UUID> leaderboardGroupIds = sailingServer.getLeaderboardGroupIds();
        assertNotNull(leaderboardGroupIds);
        assertFalse(Util.isEmpty(leaderboardGroupIds));
        assertTrue(leaderboardGroupIds.iterator().next() instanceof UUID);
    }

    @Test
    public void testEventIds() throws Exception {
        final Iterable<UUID> eventIds = sailingServer.getEventIds();
        assertNotNull(eventIds);
        assertFalse(Util.isEmpty(eventIds));
        assertTrue(eventIds.iterator().next() instanceof UUID);
    }
}
