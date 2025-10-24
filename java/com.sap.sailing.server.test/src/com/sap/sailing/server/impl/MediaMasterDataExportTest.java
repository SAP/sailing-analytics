package com.sap.sailing.server.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.media.MimeType;

public class MediaMasterDataExportTest {
    
    static final String matchingRegattaName = "Matching Regatta";
    static final String missingRegattaName = "Missing Regatta";
    static final String matchingRace1 = "matching race 1";
    static final String matchingRace2 = "matching race 2";
    static final String matchingRace3 = "matching race 3";
    static final String missingRace1 = "missing race 1";
    static final String missingRace2 = "missing race 2";
    static final String missingRace3 = "missing race 3";
    
    private static final boolean displayGroupsInReverseOrder = true;
    private static final ThresholdBasedResultDiscardingRule resultDiscardingRule = new ThresholdBasedResultDiscardingRuleImpl(new int[0]);
    private static final boolean isMedal = false;
    private static final ScoringScheme scoringScheme = new LowPoint();
    private static final CourseArea courseArea = new CourseAreaImpl("Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null);
    
    @Test
    public void testTrackWithAssignedRaceButEmptyLeaderboardGroup() {
        TimePoint startTime = null;
        Duration duration = null;
        MimeType mimeType = MimeType.mp4;
        Collection<MediaTrack> allMediaTracks = Arrays.asList(
                new MediaTrack("dbId 1", "title 1 for existing regatta and race", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(matchingRegattaName, matchingRace1))),
                new MediaTrack("dbId 2", "title 2 for existing one regatta and race and one missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace2), new RegattaNameAndRaceName(matchingRegattaName, matchingRace2)))),
                new MediaTrack("dbId 3", "title 1 for missing regatta", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(missingRegattaName, matchingRace1))),
                new MediaTrack("dbId 4", "title 2 for missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace3), new RegattaNameAndRaceName(matchingRegattaName, missingRace1))))
        );
        Collection<RaceIdentifier> regattaRaces = Collections.emptyList();
        Collection<RaceIdentifier> flexibleRaces = Collections.emptyList();
        TopLevelMasterData topLevelMasterData = createTopLevelMasterData(regattaRaces , flexibleRaces, allMediaTracks);
        Collection<MediaTrack> filteredMediaTracks = topLevelMasterData.getFilteredMediaTracks();
        assertEquals(0, filteredMediaTracks.size());
    }

    @Test
    public void testTracksWithAssignedRaceAndMatchingRegattaInLeaderboardGroup() {
        TimePoint startTime = null;
        Duration duration = null;
        MimeType mimeType = MimeType.mp4;
        Collection<MediaTrack> allMediaTracks = Arrays.asList(
                new MediaTrack("dbId 1", "title 1 for existing regatta and race", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(matchingRegattaName, matchingRace1))),
                new MediaTrack("dbId 2", "title 2 for existing one regatta and race and one missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace2), new RegattaNameAndRaceName(matchingRegattaName, matchingRace2)))),
                new MediaTrack("dbId 3", "title 3 for missing regatta", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(missingRegattaName, matchingRace1))),
                new MediaTrack("dbId 4", "title 4 for missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace3), new RegattaNameAndRaceName(matchingRegattaName, missingRace1)))),
                new MediaTrack("dbId 5", "title 5 for existing regatta and race", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(matchingRegattaName, matchingRace1)))
        );
        Collection<RaceIdentifier> regattaRaces = Arrays.asList(
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace1),
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace2),
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace3)
        );
        Collection<RaceIdentifier> flexibleRaces = Collections.emptyList();
        TopLevelMasterData topLevelMasterData = createTopLevelMasterData(regattaRaces , flexibleRaces, allMediaTracks);
        Collection<MediaTrack> filteredMediaTracks = topLevelMasterData.getFilteredMediaTracks();
        assertEquals(3, filteredMediaTracks.size());
    }

    @Test
    public void testTrackWithAssignedRaceAndMatchingFlexibleLeaderboardInLeaderboardGroup() {
        TimePoint startTime = null;
        Duration duration = null;
        MimeType mimeType = MimeType.mp4;
        Collection<MediaTrack> allMediaTracks = Arrays.asList(
                new MediaTrack("dbId 1", "title 1 for existing regatta and race", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(matchingRegattaName, matchingRace1))),
                new MediaTrack("dbId 2", "title 2 for existing one regatta and race and one missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace2), new RegattaNameAndRaceName(matchingRegattaName, matchingRace2)))),
                new MediaTrack("dbId 3", "title 3 for missing regatta", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(missingRegattaName, matchingRace1))),
                new MediaTrack("dbId 4", "title 4 for missing regatta", "url", startTime, duration, mimeType, new HashSet<RegattaAndRaceIdentifier>(Arrays.asList(new RegattaNameAndRaceName(missingRegattaName, matchingRace3), new RegattaNameAndRaceName(matchingRegattaName, missingRace1)))),
                new MediaTrack("dbId 5", "title 5 for existing regatta and race", "url", startTime, duration, mimeType, Collections.singleton(new RegattaNameAndRaceName(matchingRegattaName, matchingRace1)))
        );
        Collection<RaceIdentifier> regattaRaces = Collections.emptyList();
        Collection<RaceIdentifier> flexibleRaces = Arrays.asList(
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace1),
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace2),
                new RegattaNameAndRaceName(matchingRegattaName, matchingRace3)
        );
        TopLevelMasterData topLevelMasterData = createTopLevelMasterData(regattaRaces , flexibleRaces, allMediaTracks);
        Collection<MediaTrack> filteredMediaTracks = topLevelMasterData.getFilteredMediaTracks();
        assertEquals(3, filteredMediaTracks.size());
    }

    private TopLevelMasterData createTopLevelMasterData(Collection<RaceIdentifier> regattaRaces, Collection<RaceIdentifier> flexibleRaces, Collection<MediaTrack> allMediaTracks) {
        Set<String> raceColumnNames = new HashSet<>();
        for (RaceIdentifier regattaAndRaceIdentifier : regattaRaces) {
            raceColumnNames.add(regattaAndRaceIdentifier.getRaceName());
        }
        Regatta regatta = TrackBasedTest.createTestRegatta("regatta name",  raceColumnNames);
        RegattaLeaderboard regattaLeaderboard = new RegattaLeaderboardImpl(regatta, resultDiscardingRule);
        TrackBasedTest.assignRacesToRegattaLeaderboardColumns(regattaLeaderboard, regattaRaces);
        
        FlexibleLeaderboard flexibleLeaderboard = new FlexibleLeaderboardImpl("flexible leaderboard", resultDiscardingRule, scoringScheme , courseArea);
        for (RaceIdentifier regattaAndRaceIdentifier : flexibleRaces) {
            RaceColumn raceColumn = flexibleLeaderboard.addRaceColumn(regattaAndRaceIdentifier.getRaceName(), isMedal);
            Fleet defaultFleet = raceColumn.getFleets().iterator().next();
            raceColumn.setRaceIdentifier(defaultFleet, regattaAndRaceIdentifier);
        }
        List<? extends Leaderboard> leaderboards = Arrays.asList(regattaLeaderboard, flexibleLeaderboard);
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("name", "description", "displayName", displayGroupsInReverseOrder , leaderboards);
        Set<LeaderboardGroup> groupsToExport = Collections.singleton(leaderboardGroup);
        Iterable<Event> allEvents = Collections.emptyList();
        Map<String, Regatta> regattaForRaceIdString = Collections.emptyMap();
        SensorFixStore sensorFixStore = mock(SensorFixStore.class);
        boolean exportWind = false;
        TopLevelMasterData topLevelMasterData = new TopLevelMasterData(groupsToExport, allEvents,
                regattaForRaceIdString, allMediaTracks, sensorFixStore, exportWind,
                /* race manager device configs */ new HashSet<>(),
                /* RaceTrackingConnectivityParameters to restore */ null);
        return topLevelMasterData;
    }
}
