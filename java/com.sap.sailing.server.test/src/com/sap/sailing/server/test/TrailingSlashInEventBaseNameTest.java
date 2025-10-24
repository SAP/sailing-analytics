package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.util.RaceBoardLinkFactory;

public class TrailingSlashInEventBaseNameTest {
    private TrackedRace trackedRace;
    private Leaderboard leaderboard;
    private Event event;
    private LeaderboardGroup leaderboardGroup;
    private static final String raceboardMode = "PLAYER";
    private static final String eventBaseUrlWithoutTrailingSlash = "https://myevent.example.com";

    @BeforeEach
    public void setUp() throws MalformedURLException {
        trackedRace = mock(TrackedRace.class);
        when(trackedRace.getRaceIdentifier()).thenReturn(new RegattaNameAndRaceName("Regatta with spaces", "Race with spaces"));
        leaderboard = mock(Leaderboard.class);
        when(leaderboard.getName()).thenReturn("Leaderboard");
        event = mock(Event.class);
        when(event.getId()).thenReturn(UUID.randomUUID());
        leaderboardGroup = mock(LeaderboardGroup.class);
        when(leaderboardGroup.getId()).thenReturn(UUID.randomUUID());
    }
    
    @Test
    public void testTrailingSlashInEventBaseNameCausesNoHarmInRaceBoardLinks() throws MalformedURLException {
        when(event.getBaseURL()).thenReturn(new URL(eventBaseUrlWithoutTrailingSlash+"/"));
        final String link = RaceBoardLinkFactory.createRaceBoardLink(trackedRace, leaderboard, event, leaderboardGroup, raceboardMode, Locale.US);
        assertTrue(link.contains(eventBaseUrlWithoutTrailingSlash+"/"));
        assertFalse(link.contains(eventBaseUrlWithoutTrailingSlash+"//"));
        assertFalse(link.contains(" "));
    }
    
    @Test
    public void testNoTrailingSlashInEventBaseNameCausesNoHarmInRaceBoardLinks() throws MalformedURLException {
        when(event.getBaseURL()).thenReturn(new URL(eventBaseUrlWithoutTrailingSlash));
        final String link = RaceBoardLinkFactory.createRaceBoardLink(trackedRace, leaderboard, event, leaderboardGroup, raceboardMode, Locale.US);
        assertTrue(link.contains(eventBaseUrlWithoutTrailingSlash+"/"));
        assertFalse(link.contains(eventBaseUrlWithoutTrailingSlash+"//"));
        assertFalse(link.contains(" "));
    }
}
