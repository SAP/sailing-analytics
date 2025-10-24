package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.LeaderboardSearchResult;
import com.sap.sailing.domain.base.LeaderboardSearchResultBase;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.test.AbstractTracTracLiveTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardSearchResultBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardGroupBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardSearchResultJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TrackingConnectorInfoJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.VenueJsonSerializer;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.KeywordQueryWithOptionalEventQualification;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.AddLeaderboardGroupToEvent;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.tagging.TaggingServiceImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.search.Result;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public class SearchServiceTest {
    private RacingEventService server;
    private Venue kiel;
    private Venue flensburg;
    private Event pfingstbusch;
    private Regatta pfingstbusch29er;
    private CompetitorWithBoat hassoPlattner;
    private CompetitorWithBoat alexanderRies;
    private CompetitorWithBoat antonKoch;
    private CompetitorWithBoat tobiasSchadewaldt;
    private CompetitorWithBoat philippBuhl;
    private CompetitorWithBoat dennisGehrlein;
    private Regatta pfingstbusch470;
    private Regatta aalRegatta;
    private DynamicTrackedRace pfingstbusch29erTrackedR1;
    private DynamicTrackedRace pfingstbusch29erTrackedR3;
    private Event aalEvent;
    private DynamicTrackedRace pfingstbusch470TrackedR1;
    private DynamicTrackedRace pfingstbusch470TrackedR2;
    private DynamicTrackedRace aalOrcTrackedR1;
    private DynamicTrackedRace aalOrcTrackedR2;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() {
        UserGroupImpl defaultTenant = new UserGroupImpl(new UUID(0, 1), "defaultTenant");
        User currentUser = Mockito.mock(User.class);
        securityService = Mockito.mock(SecurityService.class);
        SecurityManager securityManager = Mockito.mock(org.apache.shiro.mgt.SecurityManager.class);
        Subject fakeSubject = Mockito.mock(Subject.class);
        SecurityUtils.setSecurityManager(securityManager);
        Mockito.doReturn(fakeSubject).when(securityManager).createSubject(Mockito.any());
        Mockito.doReturn(defaultTenant).when(securityService).getServerGroup();
        Mockito.doReturn(currentUser).when(securityService).getCurrentUser();
        Mockito.doReturn(true).when(securityService).hasCurrentUserReadPermission(Mockito.any());
        Mockito.doNothing().when(securityService).checkCurrentUserReadPermission(Mockito.any());
        Mockito.doReturn(true).when(securityService)
                .hasCurrentUserReadPermission(Mockito.any(WithQualifiedObjectIdentifier.class));
        Mockito.doReturn(true).when(fakeSubject).isAuthenticated();
        server = Mockito.spy(new RacingEventServiceImpl());
        Mockito.doReturn(securityService).when(server).getSecurityService();
        TaggingServiceImpl taggingServer = Mockito.spy(new TaggingServiceImpl(server));
        Mockito.doReturn(taggingServer).when(server).getTaggingService();
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
        pfingstbusch = server.apply(new CreateEvent("Pfingsbusch", /* eventDescription */ null, pfingstbuschStartDate, pfingstbuschEndDate,
                "Kiel", /* isPublic */ true, UUID.randomUUID(), /* officialWebsiteURLAsString */ null, /*baseURL*/null,
                /* sailorsInfoWebsiteURLAsString */ null, /* images */Collections.<ImageDescriptor> emptyList(), /* videos */Collections.<VideoDescriptor> emptyList(), /* leaderboardGroupIds */ Collections.<UUID> emptyList()));
        kiel = pfingstbusch.getVenue();
        final CourseArea kielAlpha = server.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Alpha", /* centerPosition */ null, /* radius */ null);
        kiel.addCourseArea(kielAlpha);
        final CourseArea kielBravo = server.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Bravo", /* centerPosition */ null, /* radius */ null);
        kiel.addCourseArea(kielBravo);
        final LinkedHashMap<String, SeriesCreationParametersDTO> seriesCreationParams = new LinkedHashMap<String, SeriesCreationParametersDTO>();
        seriesCreationParams.put("Default",
                new SeriesCreationParametersDTO(Collections.singletonList(new FleetDTO("Default", /* order */-1, Color.RED)),
                /* medal */false, /* fleetsCanRunInParallel */ true, /* startsWithZero */false, /* firstColumnIsNonDiscardableCarryForward */false,
                /* discardingThresholds */null, /* hasSplitFleetContiguousScoring */false, /* hasCrossFleetMergedRanking */ false, /* maximumNumberOfDiscards */ null, /* oneAlwaysStaysOne */ false));
        pfingstbusch29er = server.apply(new AddSpecificRegatta(RegattaImpl.getDefaultName("Pfingstbusch", "29er"),
                "29er", /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ UUID.randomUUID().toString(), /* startDate */ null, /* endDate */ null,
                UUID.randomUUID(),
                new RegattaCreationParametersDTO(seriesCreationParams), /* persistent */
                true, new LowPoint(), Collections.singleton(kielAlpha.getId()), /* buoyZoneRadiusInHullLengths */2.0,
                /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, RankingMetrics.ONE_DESIGN));
        server.apply(new AddColumnToSeries(pfingstbusch29er.getRegattaIdentifier(), "Default", "R1"));
        server.apply(new AddColumnToSeries(pfingstbusch29er.getRegattaIdentifier(), "Default", "R2"));
        server.apply(new AddColumnToSeries(pfingstbusch29er.getRegattaIdentifier(), "Default", "R3"));
        RegattaLeaderboard pfingstbusch29erLeaderboard = server.apply(new CreateRegattaLeaderboard(pfingstbusch29er.getRegattaIdentifier(),
                /* leaderboardDisplayName */ null, /* discardThresholds */ new int[0]));
        pfingstbusch470 = server.apply(new AddSpecificRegatta(RegattaImpl.getDefaultName("Pfingstbusch", "470"), "470",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ UUID.randomUUID().toString(), /* startDate */ null, /* endDate */ null,
                UUID.randomUUID(),
                new RegattaCreationParametersDTO(seriesCreationParams), /* persistent */
                true, new LowPoint(), Collections.singleton(kielBravo.getId()), /* buoyZoneRadiusInHullLengths */2.0,
                /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, RankingMetrics.ONE_DESIGN));
        server.apply(new AddColumnToSeries(pfingstbusch470.getRegattaIdentifier(), "Default", "R1"));
        server.apply(new AddColumnToSeries(pfingstbusch470.getRegattaIdentifier(), "Default", "R2"));
        server.apply(new AddColumnToSeries(pfingstbusch470.getRegattaIdentifier(), "Default", "R3"));
        RegattaLeaderboard pfingstbusch470Leaderboard = server.apply(new CreateRegattaLeaderboard(pfingstbusch470.getRegattaIdentifier(),
                /* leaderboardDisplayName */ null, /* discardThresholds */ new int[0]));
        UUID newGroupid = UUID.randomUUID();
        final LeaderboardGroup pfingstbuschLeaderboardGroup = server.apply(new CreateLeaderboardGroup(newGroupid,
                "Pfingstbusch", "Pfingstbusch", /* displayName */ null,
                /* displayGroupsInReverseOrder */ false, Arrays.asList(new String[] { RegattaImpl.getDefaultName("Pfingstbusch", "29er"), RegattaImpl.getDefaultName("Pfingstbusch", "470") }),
                new int[0], /* overallLeaderboardScoringSchemeType */ null));
        server.apply(new AddLeaderboardGroupToEvent(pfingstbusch.getId(), pfingstbuschLeaderboardGroup.getId()));
        cal.set(2014, 5, 7, 10, 00);
        final TimePoint aalStartDate = new MillisecondsTimePoint(cal.getTime());
        cal.set(2014, 5, 8, 18, 00);
        final TimePoint aalEndDate = new MillisecondsTimePoint(cal.getTime());
        aalEvent = server.apply(new CreateEvent("Aalregatta", /* eventDescription */ null, aalStartDate, aalEndDate,
                "Flensburg", /* isPublic */ true, UUID.randomUUID(),  /* officialWebsiteURLAsString */ null, /*baseURL*/null,
                /*sailorsInfoWebsiteURLAsString */ null, /* images */Collections.<ImageDescriptor> emptyList(), /* videos */Collections.<VideoDescriptor> emptyList(), /* leaderboardGroupIds */ Collections.<UUID> emptyList()));
        flensburg = aalEvent.getVenue();
        final CourseArea flensburgStandard = server.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Standard", /* centerPosition */ null, /* radius */ null);
        flensburg.addCourseArea(flensburgStandard);
        aalRegatta = server.apply(new AddSpecificRegatta(RegattaImpl.getDefaultName("Aalregatta", "ORC"), "ORC",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ UUID.randomUUID().toString(), /* startDate */ null, /* endDate */ null,
                UUID.randomUUID(),
                new RegattaCreationParametersDTO(seriesCreationParams), /* persistent */
                true, new LowPoint(), Collections.singleton(flensburgStandard.getId()), /* buoyZoneRadiusInHullLengths */2.0,
                /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, RankingMetrics.ONE_DESIGN));
        server.apply(new AddColumnToSeries(aalRegatta.getRegattaIdentifier(), "Default", "R1"));
        server.apply(new AddColumnToSeries(aalRegatta.getRegattaIdentifier(), "Default", "R2"));
        RegattaLeaderboard aalRegattaLeaderboard = server.apply(new CreateRegattaLeaderboard(aalRegatta.getRegattaIdentifier(),
                /* leaderboardDisplayName */ null, /* discardThresholds */ new int[0]));
        UUID newGroupid2 = UUID.randomUUID();
        final LeaderboardGroup aalLeaderboardGroup = server.apply(new CreateLeaderboardGroup(newGroupid2, "Aal Regatta",
                "Aal Regatta", /* displayName */ null,
                /* displayGroupsInReverseOrder */ false, Collections.singletonList(RegattaImpl.getDefaultName("Aalregatta", "ORC")),
                new int[0], /* overallLeaderboardScoringSchemeType */ null));
        server.apply(new AddLeaderboardGroupToEvent(aalEvent.getId(), aalLeaderboardGroup.getId()));
        hassoPlattner = AbstractTracTracLiveTest.createCompetitorWithBoat("Hasso Plattner");
        alexanderRies = AbstractTracTracLiveTest.createCompetitorWithBoat("Alexander Ries");
        antonKoch = AbstractTracTracLiveTest.createCompetitorWithBoat("Anton Koch");
        tobiasSchadewaldt = AbstractTracTracLiveTest.createCompetitorWithBoat("Tobias Schadewaldt");
        philippBuhl = AbstractTracTracLiveTest.createCompetitorWithBoat("Philipp Buhl");
        dennisGehrlein = AbstractTracTracLiveTest.createCompetitorWithBoat("Dennis Gehrlein");
        final RaceDefinitionImpl pfingstbusch29erR1 = new RaceDefinitionImpl("R1", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), pfingstbusch29er.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(alexanderRies, tobiasSchadewaldt));
        final RaceDefinitionImpl pfingstbusch29erR2 = new RaceDefinitionImpl("R2", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), pfingstbusch29er.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(alexanderRies, tobiasSchadewaldt));
        final RaceDefinitionImpl pfingstbusch29erR3 = new RaceDefinitionImpl("R3", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), pfingstbusch29er.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(alexanderRies, tobiasSchadewaldt));
        server.apply(new AddRaceDefinition(pfingstbusch29er.getRegattaIdentifier(), pfingstbusch29erR1));
        server.apply(new AddRaceDefinition(pfingstbusch29er.getRegattaIdentifier(), pfingstbusch29erR2));
        server.apply(new AddRaceDefinition(pfingstbusch29er.getRegattaIdentifier(), pfingstbusch29erR3));
        // track only R1 and R3
        pfingstbusch29erTrackedR1 = server.apply(new CreateTrackedRace(pfingstbusch29er.getRaceIdentifier(pfingstbusch29erR1), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        pfingstbusch29erTrackedR1.setStartOfTrackingReceived(pfingstbuschStartDate.plus(1));
        pfingstbusch29erLeaderboard.getRaceColumnByName("R1").setTrackedRace(pfingstbusch29erLeaderboard.getRaceColumnByName("R1").getFleetByName("Default"), pfingstbusch29erTrackedR1);
        pfingstbusch29erTrackedR3 = server.apply(new CreateTrackedRace(pfingstbusch29er.getRaceIdentifier(pfingstbusch29erR3), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        pfingstbusch29erLeaderboard.getRaceColumnByName("R3").setTrackedRace(pfingstbusch29erLeaderboard.getRaceColumnByName("R3").getFleetByName("Default"), pfingstbusch29erTrackedR3);
        final RaceDefinitionImpl pfingstbush470R1 = new RaceDefinitionImpl("R1", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), pfingstbusch470.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(philippBuhl, antonKoch));
        final RaceDefinitionImpl pfingstbush470R2 = new RaceDefinitionImpl("R2", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), pfingstbusch470.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(philippBuhl, antonKoch));
        server.apply(new AddRaceDefinition(pfingstbusch470.getRegattaIdentifier(), pfingstbush470R1));
        server.apply(new AddRaceDefinition(pfingstbusch470.getRegattaIdentifier(), pfingstbush470R2));
        // track only R1 and R2
        pfingstbusch470TrackedR1 = server.apply(new CreateTrackedRace(pfingstbusch470.getRaceIdentifier(pfingstbush470R1), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        pfingstbusch470TrackedR1.setStartOfTrackingReceived(pfingstbuschStartDate.plus(2)); // starts later than 29er
        pfingstbusch470Leaderboard.getRaceColumnByName("R1").setTrackedRace(pfingstbusch470Leaderboard.getRaceColumnByName("R1").getFleetByName("Default"), pfingstbusch470TrackedR1);
        pfingstbusch470TrackedR2 = server.apply(new CreateTrackedRace(pfingstbusch470.getRaceIdentifier(pfingstbush470R2), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        pfingstbusch470Leaderboard.getRaceColumnByName("R2").setTrackedRace(pfingstbusch470Leaderboard.getRaceColumnByName("R2").getFleetByName("Default"), pfingstbusch470TrackedR2);
        final RaceDefinitionImpl aalOrcR1 = new RaceDefinitionImpl("R1", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), aalRegatta.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(hassoPlattner, dennisGehrlein, philippBuhl));
        final RaceDefinitionImpl aalOrcR2 = new RaceDefinitionImpl("R2", new CourseImpl("up/down", Collections.<Waypoint>emptyList()), aalRegatta.getBoatClass(),
                AbstractTracTracLiveTest.createCompetitorAndBoatsMap(hassoPlattner, dennisGehrlein, philippBuhl));
        server.apply(new AddRaceDefinition(aalRegatta.getRegattaIdentifier(), aalOrcR1));
        server.apply(new AddRaceDefinition(aalRegatta.getRegattaIdentifier(), aalOrcR2));
        // track only R1 and R2
        aalOrcTrackedR1 = server.apply(new CreateTrackedRace(aalRegatta.getRaceIdentifier(aalOrcR1), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        aalOrcTrackedR1.setStartOfTrackingReceived(aalStartDate);
        aalRegattaLeaderboard.getRaceColumnByName("R1").setTrackedRace(aalRegattaLeaderboard.getRaceColumnByName("R1").getFleetByName("Default"), aalOrcTrackedR1);
        aalOrcTrackedR2 = server.apply(new CreateTrackedRace(aalRegatta.getRaceIdentifier(aalOrcR2), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 15000, null));
        aalRegattaLeaderboard.getRaceColumnByName("R2").setTrackedRace(aalRegattaLeaderboard.getRaceColumnByName("R2").getFleetByName("Default"), aalOrcTrackedR2);
    }

    @Test
    public void testSetup() {
        assertNotNull(pfingstbusch29er);
        assertNotNull(pfingstbusch29erTrackedR1);
        assertNotNull(pfingstbusch29erTrackedR3);
        assertNotNull(pfingstbusch470TrackedR1);
        assertNotNull(pfingstbusch470TrackedR2);
        assertNotNull(aalEvent);
        assertNotNull(aalRegatta);
        assertNotNull(aalOrcTrackedR1);
        assertNotNull(aalOrcTrackedR2);
    }

    @Test
    public void testSimpleSearchByCompetitorName() {
        Result<LeaderboardSearchResult> searchResults = server.search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Tobi" })));
        assertEquals(1, Util.size(searchResults.getHits()));
        Regatta foundRegatta = searchResults.getHits().iterator().next().getRegatta();
        assertSame(pfingstbusch29er, foundRegatta);
    }

    @Test
    public void testSimpleSearchByCompetitorName2() {
        Result<LeaderboardSearchResult> searchResults = server.search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Hasso" })));
        assertEquals(1, Util.size(searchResults.getHits()));
        Regatta foundRegatta = searchResults.getHits().iterator().next().getRegatta();
        assertSame(aalRegatta, foundRegatta);
    }

    @Test
    public void testSimpleSearchByVenueName() {
        Result<LeaderboardSearchResult> searchResults = server.search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Flensburg" })));
        assertEquals(1, Util.size(searchResults.getHits()));
        Regatta foundRegatta = searchResults.getHits().iterator().next().getRegatta();
        assertSame(aalRegatta, foundRegatta);
    }

    @Test
    public void testSimpleSearchByVenueName2() {
        Result<LeaderboardSearchResult> searchResults = server.search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Kiel" })));
        assertEquals(2, Util.size(searchResults.getHits()));
        final Iterator<LeaderboardSearchResult> iterator = searchResults.getHits().iterator();
        final LeaderboardSearchResult firstMatch = iterator.next();
        Regatta firstFoundRegatta = firstMatch.getRegatta();
        assertSame(pfingstbusch29er, firstFoundRegatta);
        assertSame(pfingstbusch, firstMatch.getEvents().iterator().next());
        final LeaderboardSearchResult secondMatch = iterator.next();
        Regatta secondFoundRegatta = secondMatch.getRegatta();
        assertSame(pfingstbusch470, secondFoundRegatta);
        assertSame(pfingstbusch, secondMatch.getEvents().iterator().next());
    }

    @Test
    public void testMultipleMatchesSortedCorrectly() {
        Result<LeaderboardSearchResult> searchResults = server.search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Buhl" })));
        assertEquals(2, Util.size(searchResults.getHits()));
        final Iterator<LeaderboardSearchResult> iter = searchResults.getHits().iterator();
        final LeaderboardSearchResult firstMatch = iter.next();
        Regatta earlierStartRegatta = firstMatch.getRegatta();
        assertSame(pfingstbusch470, earlierStartRegatta);
        assertSame(pfingstbusch, firstMatch.getEvents().iterator().next());
        final LeaderboardSearchResult secondMatch = iter.next();
        Regatta laterStartRegatta = secondMatch.getRegatta();
        assertSame(aalRegatta, laterStartRegatta);
        assertSame(aalEvent, secondMatch.getEvents().iterator().next());
    }

    @Test
    public void testSerializeAndDeserializeSearchResult() throws JsonDeserializationException {
        Result<LeaderboardSearchResult> searchResults = server
                .search(new KeywordQueryWithOptionalEventQualification(Arrays.asList(new String[] { "Buhl" })));
        LeaderboardGroupBaseJsonSerializer leaderboardGroupBaseJsonSerializer = new LeaderboardGroupBaseJsonSerializer();
        final CourseAreaJsonSerializer courseAreaSerializer = new CourseAreaJsonSerializer();
        LeaderboardSearchResultJsonSerializer serializer = new LeaderboardSearchResultJsonSerializer(
                new EventBaseJsonSerializer(new VenueJsonSerializer(courseAreaSerializer),
                        leaderboardGroupBaseJsonSerializer, new TrackingConnectorInfoJsonSerializer()),
                leaderboardGroupBaseJsonSerializer);
        LeaderboardGroupBaseJsonDeserializer leaderboardGroupBaseJsonDeserializer = new LeaderboardGroupBaseJsonDeserializer();
        final CourseAreaJsonDeserializer courseAreaJsonDeserializer = new CourseAreaJsonDeserializer(DomainFactory.INSTANCE);
        LeaderboardSearchResultBaseJsonDeserializer deserializer = new LeaderboardSearchResultBaseJsonDeserializer(
                new EventBaseJsonDeserializer(
                        new VenueJsonDeserializer(courseAreaJsonDeserializer),
                        leaderboardGroupBaseJsonDeserializer, new TrackingConnectorInfoJsonDeserializer()),
                leaderboardGroupBaseJsonDeserializer);
        final LeaderboardSearchResult expected = searchResults.getHits().iterator().next();
        LeaderboardSearchResultBase deserialized = deserializer.deserialize(serializer.serialize(expected));
        assertEquals("Pfingstbusch (470)", deserialized.getRegattaName());
        assertEquals(expected.getEvents().iterator().next().getName(), deserialized.getEvents().iterator().next().getName());
        assertEquals(expected.getEvents().iterator().next().getId(), deserialized.getEvents().iterator().next().getId());
        assertEquals(expected.getEvents().iterator().next().getStartDate(), deserialized.getEvents().iterator().next().getStartDate());
        assertEquals(expected.getEvents().iterator().next().getEndDate(), deserialized.getEvents().iterator().next().getEndDate());
        assertEquals(expected.getEvents().iterator().next().getVenue().getName(), deserialized.getEvents().iterator().next().getVenue().getName());
        Iterator<LeaderboardGroup> expectedLGs = expected.getLeaderboardGroups().iterator();
        Iterator<? extends LeaderboardGroupBase> deserializedLGs = deserialized.getLeaderboardGroups().iterator();
        while (expectedLGs.hasNext()) {
            assertTrue(deserializedLGs.hasNext());
            LeaderboardGroup expectedLG = expectedLGs.next();
            LeaderboardGroupBase deserializedLG = deserializedLGs.next();
            assertEquals(expectedLG.getId(), deserializedLG.getId());
            assertEquals(expectedLG.getName(), deserializedLG.getName());
            assertEquals(expectedLG.getDescription(), deserializedLG.getDescription());
        }
    }
}
