package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RaceColumnInSeriesImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.VenueImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.leaderboard.EventResolver;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.ranking.TimeOnTimeAndDistanceRankingMetric;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.UpdateEliminatedCompetitorsInLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;

public class TestStoringAndLoadingEventsAndRegattas extends AbstractMongoDBTest {
    private static final Logger logger = Logger.getLogger(TestStoringAndLoadingEventsAndRegattas.class.getName());

    private final TimePoint eventStartDate;
    private final TimePoint eventEndDate;
    private final TimePoint regattaStartDate;
    private final TimePoint regattaEndDate;

    public TestStoringAndLoadingEventsAndRegattas() throws UnknownHostException, MongoException {
        super();

        Calendar cal = Calendar.getInstance();

        cal.set(2012, 12, 1);
        eventStartDate = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2012, 12, 5);
        eventEndDate = new MillisecondsTimePoint(cal.getTimeInMillis());

        cal.set(2012, 12, 2);
        regattaStartDate = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2012, 12, 3);
        regattaEndDate = new MillisecondsTimePoint(cal.getTimeInMillis());
    }

    private LeaderboardGroup createLeaderboardGroup(String name) {
        return new LeaderboardGroupImpl(name, "Description for "+name, /* displayName */ null, /* displayInReverseOrder */ false, Collections.<Leaderboard>emptyList());
    }

    @Test
    public void testLoadStoreSimpleEventWithLinkToLeaderboardGroups() throws MalformedURLException {
        final String eventName = "Event Name";
        final String eventDescription = "Event Description";
        final String venueName = "Venue Name";
        final String[] courseAreaNames = new String[] { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrott" };
        final URL officialWebsiteURL = new URL("http://official.website.com");
        final URL baseURL = new URL("http://base.url.com");
        final URL sailorsInfoWebsiteURL = new URL("http://sailorsinfo.website.com");
        final URL sailorsInfoWebsiteURLDE = new URL("http://sailorsinfo-de.website.com");
        final Venue venue = new VenueImpl(venueName);

        for (String courseAreaName : courseAreaNames) {
            CourseArea courseArea = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), courseAreaName, /* centerPosition */ null, /* radius */ null);
            venue.addCourseArea(courseArea);
        }
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        final Event event = new EventImpl(eventName, eventStartDate, eventEndDate, venue, /*isPublic*/ true, UUID.randomUUID());
        final LeaderboardGroup lg1 = createLeaderboardGroup("lg1");
        final LeaderboardGroup lg2 = createLeaderboardGroup("lg2");
        event.addLeaderboardGroup(lg1);
        event.addLeaderboardGroup(lg2);
        event.setDescription(eventDescription);
        event.setOfficialWebsiteURL(officialWebsiteURL);
        event.setBaseURL(baseURL);
        event.setSailorsInfoWebsiteURL(null, sailorsInfoWebsiteURL);
        event.setSailorsInfoWebsiteURL(Locale.GERMAN, sailorsInfoWebsiteURLDE);
        mof.storeEvent(event);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        final Event loadedEvent = dof.loadEvent(eventName);
        dof.loadLeaderboardGroupLinksForEvents(new EventResolver() {
            @Override
            public Event getEvent(Serializable id) {
                return id.equals(loadedEvent.getId()) ? loadedEvent : null;
            }
        }, new LeaderboardGroupResolver() {
            @Override
            public LeaderboardGroup getLeaderboardGroupByName(String leaderboardGroupName) {
                return leaderboardGroupName.equals(lg1.getName()) ? lg1 : leaderboardGroupName.equals(lg2.getName()) ? lg2 : null;
            }

            @Override
            public LeaderboardGroup getLeaderboardGroupByID(UUID leaderboardGroupID) {
                return leaderboardGroupID.equals(lg1.getId()) ? lg1 : leaderboardGroupID.equals(lg2.getId()) ? lg2 : null;
            }

            @Override
            public LeaderboardGroup resolveLeaderboardGroupByRegattaName(String regattaName) {
                return null;
            }
        });
        assertNotNull(loadedEvent);
        assertEquals(eventName, loadedEvent.getName());
        assertEquals(eventDescription, loadedEvent.getDescription());
        assertEquals(event.getOfficialWebsiteURL(), loadedEvent.getOfficialWebsiteURL());
        assertEquals(event.getBaseURL(), loadedEvent.getBaseURL());
        assertEquals(sailorsInfoWebsiteURL, loadedEvent.getSailorsInfoWebsiteURL(null));
        assertEquals(sailorsInfoWebsiteURLDE, loadedEvent.getSailorsInfoWebsiteURL(Locale.GERMAN));
        assertEquals(2, loadedEvent.getSailorsInfoWebsiteURLs().size());
        assertEquals(2, Util.size(loadedEvent.getLeaderboardGroups()));
        Iterator<LeaderboardGroup> lgIter = loadedEvent.getLeaderboardGroups().iterator();
        assertSame(lg1, lgIter.next());
        assertSame(lg2, lgIter.next());
        final Venue loadedVenue = loadedEvent.getVenue();
        assertNotNull(loadedVenue);
        assertEquals(venueName, loadedVenue.getName());
        assertEquals(courseAreaNames.length, Util.size(loadedVenue.getCourseAreas()));
        int i=0;
        for (CourseArea loadedCourseArea : loadedVenue.getCourseAreas()) {
            assertEquals(courseAreaNames[i++], loadedCourseArea.getName());
        }
    }

    @Test
    public void testLoadStoreSimpleEventAndRegattaWithCourseArea() {
        final String eventName = "Event Name";
        final String venueName = "Venue Name";
        final Venue venue = new VenueImpl(venueName);
        final String courseAreaAlphaName = "Alpha";
        final DegreePosition alphaCenter = new DegreePosition(49, 8);
        final NauticalMileDistance alphaRadius = new NauticalMileDistance(2.5);
        final CourseArea courseAreaAlpha = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), courseAreaAlphaName, alphaCenter, alphaRadius);
        venue.addCourseArea(courseAreaAlpha);
        final String courseAreaBravoName = "Bravo";
        final CourseArea courseAreaBravo = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), courseAreaBravoName, /* centerPosition */ null, /* radius */ null);
        venue.addCourseArea(courseAreaBravo);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        Event event = new EventImpl(eventName, eventStartDate, eventEndDate, venue, /*isPublic*/ true, UUID.randomUUID());
        mof.storeEvent(event);
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegatta(RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, regattaStartDate, regattaEndDate, /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), courseAreaAlpha, OneDesignRankingMetric::new);
        mof.storeRegatta(regatta);
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), new DomainFactoryImpl(/* raceLogResolver */ null));
        final Event loadedEvent = dof.loadEvent(eventName); // load event first to establish course areas in DomainFactory
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertNotNull(loadedRegatta);
        assertEquals(regatta.getName(), loadedRegatta.getName());
        assertEquals(Util.size(regatta.getSeries()), Util.size(loadedRegatta.getSeries()));
        assertFalse(Util.isEmpty(loadedRegatta.getCourseAreas()));
        assertTrue(Util.contains(Util.map(loadedRegatta.getCourseAreas(), CourseArea::getId), courseAreaAlpha.getId()));
        assertTrue(Util.contains(Util.map(loadedRegatta.getCourseAreas(), CourseArea::getName), courseAreaAlpha.getName()));
        assertEquals(alphaCenter, Util.first(Util.filter(loadedRegatta.getCourseAreas(), courseArea->courseArea.getName().equals(courseAreaAlphaName))).getCenterPosition());
        assertEquals(alphaRadius, Util.first(Util.filter(loadedRegatta.getCourseAreas(), courseArea->courseArea.getName().equals(courseAreaAlphaName))).getRadius());
        assertNull(Util.first(Util.filter(loadedEvent.getVenue().getCourseAreas(), courseArea->courseArea.getName().equals(courseAreaBravoName))).getCenterPosition());
        assertNull(Util.first(Util.filter(loadedEvent.getVenue().getCourseAreas(), courseArea->courseArea.getName().equals(courseAreaBravoName))).getRadius());
        assertEquals(loadedRegatta.getStartDate(), regattaStartDate);
        assertEquals(loadedRegatta.getEndDate(), regattaEndDate);
        assertTrue(loadedRegatta.getCompetitorRegistrationType() == CompetitorRegistrationType.CLOSED);
    }

    @Test
    public void testLoadStoreSimpleEventWithImages() throws MalformedURLException {
        final URL imageURL = new URL("http://some.host/with/some/file2.jpg");
        final String copyright = "copyright by Alex";
        final String imageTitle = "My image title";
        final String imageSubtitle = "My image subtitle";
        final Integer imageWidth = 500;
        final Integer imageHeight = 300;
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final String eventName = "Event Name";
        final String venueName = "Venue Name";
        final String courseAreaName = "Alpha";
        final Venue venue = new VenueImpl(venueName);
        CourseArea courseArea = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), courseAreaName, /* centerPosition */ null, /* radius */ null);
        venue.addCourseArea(courseArea);

        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        Event event = new EventImpl(eventName, eventStartDate, eventEndDate, venue, /*isPublic*/ true, UUID.randomUUID());

        ImageDescriptor image1 = new ImageDescriptorImpl(imageURL, createdAt);
        image1.setCopyright(copyright);
        image1.setSize(imageWidth, imageHeight);
        image1.setTitle(imageTitle);
        image1.setSubtitle(imageSubtitle);
        image1.addTag("Tag1");
        image1.addTag("Tag2");
        image1.addTag("Tag3");
        event.addImage(image1);

        ImageDescriptor image2 = new ImageDescriptorImpl(new URL("http://some.host/with/some/file2.jpg"), MillisecondsTimePoint.now());
        image2.setCopyright("copyright");
        event.addImage(image2);

        mof.storeEvent(event);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        final Event loadedEvent = dof.loadEvent(eventName);
        assertEquals(2, Util.size(loadedEvent.getImages()));
        ImageDescriptor loadedImage1 = loadedEvent.getImages().iterator().next();
        assertNotNull(loadedImage1);
        assertEquals(imageURL, loadedImage1.getURL());
        assertEquals(copyright, loadedImage1.getCopyright());
        assertEquals(copyright, loadedImage1.getCopyright());
        assertEquals(imageTitle, loadedImage1.getTitle());
        assertEquals(imageSubtitle, loadedImage1.getSubtitle());
        assertEquals(createdAt, loadedImage1.getCreatedAtDate());
        assertEquals(imageWidth, loadedImage1.getWidthInPx());
        assertEquals(imageHeight, loadedImage1.getHeightInPx());
        assertEquals(3, Util.size(loadedImage1.getTags()));
    }

    @Test
    public void testLoadStoreSimpleEventWithVideos() throws MalformedURLException {
        final URL videoURL = new URL("http://some.host/with/some/video.mpg");
        final URL videoThumbnailURL = new URL("http://some.host/with/some/video_thumbnail.jpg");
        final Locale locale = Locale.GERMAN;
        final Integer videoLengthInSeconds = 2  * 60 * 60 * 1000; // 2h
        final MimeType mimeType = MimeType.mp4;
        final String copyright = "copyright by Don";
        final String videoTitle = "My video title";
        final String videoSubtitle = "My video subtitle";
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final String eventName = "Event Name";
        final String venueName = "Venue Name";
        final String courseAreaName = "Alpha";
        final Venue venue = new VenueImpl(venueName);
        CourseArea courseArea = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), courseAreaName, /* centerPosition */ null, /* radius */ null);
        venue.addCourseArea(courseArea);

        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        Event event = new EventImpl(eventName, eventStartDate, eventEndDate, venue, /*isPublic*/ true, UUID.randomUUID());

        VideoDescriptor video1 = new VideoDescriptorImpl(videoURL, mimeType, createdAt);
        video1.setCopyright(copyright);
        video1.setTitle(videoTitle);
        video1.setLocale(locale);
        video1.setSubtitle(videoSubtitle);
        video1.setThumbnailURL(videoThumbnailURL);
        video1.setLengthInSeconds(videoLengthInSeconds);
        video1.addTag("Tag1");
        video1.addTag("Tag2");
        video1.addTag("Tag3");
        event.addVideo(video1);

        VideoDescriptor video2 = new VideoDescriptorImpl(new URL("http://some.host/with/some/file2.ogg"), MimeType.ogg, MillisecondsTimePoint.now());
        video2.setCopyright("copyright");
        event.addVideo(video2);

        mof.storeEvent(event);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        final Event loadedEvent = dof.loadEvent(eventName);
        assertEquals(2, Util.size(loadedEvent.getVideos()));
        VideoDescriptor loadedVideo1 = loadedEvent.getVideos().iterator().next();
        assertNotNull(loadedVideo1);
        assertEquals(videoURL, loadedVideo1.getURL());
        assertEquals(videoThumbnailURL, loadedVideo1.getThumbnailURL());
        assertEquals(videoLengthInSeconds, loadedVideo1.getLengthInSeconds());
        assertEquals(copyright, loadedVideo1.getCopyright());
        assertEquals(locale, loadedVideo1.getLocale());
        assertEquals(videoTitle, loadedVideo1.getTitle());
        assertEquals(videoSubtitle, loadedVideo1.getSubtitle());
        assertEquals(createdAt, loadedVideo1.getCreatedAtDate());
        assertEquals(3, Util.size(loadedVideo1.getTags()));
    }

    @Test
    /**
     * We expected that the migration code creates also an image URL for each image we create.
     * Images with the 'Sponsor' tag should create a corresponding sponsor image URL
     * Videos should create a video URL.
     */
    public void testLoadStoreSimpleEventWithImageAndVideoURLMigration() throws MalformedURLException {
        final URL imageURL = new URL("http://some.host/with/some/bla.jpg");
        final URL sponsorImageURL = new URL("http://some.host/with/some/sponsor.jpg");
        final URL videoURL = new URL("http://some.host/with/some/video.mpg");
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final String eventName = "Event Name";
        final Venue venue = new VenueImpl("My Venue");
        CourseArea courseArea = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), "Alfa", /* centerPosition */ null, /* radius */ null);
        venue.addCourseArea(courseArea);

        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        Event event = new EventImpl(eventName, eventStartDate, eventEndDate, venue, /*isPublic*/ true, UUID.randomUUID());

        ImageDescriptor image1 = new ImageDescriptorImpl(imageURL, createdAt);
        image1.addTag(MediaTagConstants.GALLERY.getName());
        event.addImage(image1);

        ImageDescriptor image2 = new ImageDescriptorImpl(sponsorImageURL, createdAt);
        event.addImage(image2);
        image2.addTag(MediaTagConstants.SPONSOR.getName());

        VideoDescriptor video1 = new VideoDescriptorImpl(videoURL, MimeType.mp4, createdAt);
        event.addVideo(video1);

        mof.storeEvent(event);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        final Event loadedEvent = dof.loadEvent(eventName);
        assertEquals(2, Util.size(loadedEvent.getImages()));
        assertEquals(1, Util.size(loadedEvent.getVideos()));
        assertEquals(1, Util.size(loadedEvent.findImagesWithTag(MediaTagConstants.GALLERY.getName())));
        assertEquals(1, Util.size(loadedEvent.findImagesWithTag(MediaTagConstants.SPONSOR.getName())));
        assertEquals(1, Util.size(loadedEvent.getVideos()));
    }


    @Test
    public void testLoadStoreRegattaConfiguration() {
        RegattaConfigurationImpl configuration = new RegattaConfigurationImpl();
        configuration.setDefaultRacingProcedureType(RacingProcedureType.BASIC);
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("ESS40", false);
        Regatta regatta = createRegattaAndAddRaceColumns(1, 1, RegattaImpl.getDefaultName("RR", boatClass.getName()), boatClass,
                regattaStartDate, regattaEndDate, false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT), OneDesignRankingMetric::new);
        regatta.setRegattaConfiguration(configuration);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), null);
        assertNotNull(loadedRegatta.getRegattaConfiguration());
        assertEquals(RacingProcedureType.BASIC, loadedRegatta.getRegattaConfiguration().getDefaultRacingProcedureType());
    }

    @Test
    public void testLoadStoreSimpleRegattaLeaderboard() {
        RacingEventService res = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService()), MediaDBFactory.INSTANCE.getMediaDB(getMongoService()), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false);
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regattaProxy = createRegatta(RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ false, CompetitorRegistrationType.CLOSED, regattaStartDate, regattaEndDate,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);
        final String regattaName = regattaProxy.getName();
        Regatta regatta = res.createRegatta(regattaName, regattaProxy.getBoatClass().getName(),
                /* canBoatsOfCompetitorsChangePerRace */ false, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, regattaStartDate, regattaEndDate, "123", regattaProxy.getSeries(),
                regattaProxy.isPersistent(), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* courseAreaIds */ Collections.emptySet(), /* buoyZoneRadiusInHullLengths */ 2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        DynamicCompetitorWithBoat competitorWithBoat1 = AbstractLeaderboardTest.createCompetitorWithBoat("Humba1");
        DynamicCompetitorWithBoat competitorWithBoat2 = AbstractLeaderboardTest.createCompetitorWithBoat("Humba2");
        DynamicBoat boat1= AbstractLeaderboardTest.createBoat("Humba1 Boot");
        DynamicBoat boat2 = AbstractLeaderboardTest.createBoat("Humba2 Boot");
        res.getCompetitorAndBoatStore().addNewCompetitors(Arrays.asList(competitorWithBoat1, competitorWithBoat2));
        res.getCompetitorAndBoatStore().addNewBoats(Arrays.asList(boat1, boat2));
        regatta.getRegattaLog().add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Axel", 0), competitorWithBoat1));
        regatta.getRegattaLog().add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Axel", 0), competitorWithBoat2));
        assertTrue(Util.contains(regatta.getAllCompetitors(), competitorWithBoat1));
        assertTrue(Util.contains(regatta.getAllCompetitors(), competitorWithBoat2));
        Iterable<Boat> allBoats = regatta.getAllBoats();
        assertTrue(Util.size(allBoats) == 2);
        assertTrue(Util.contains(regatta.getAllBoats(), competitorWithBoat1.getBoat()));
        assertTrue(Util.contains(regatta.getAllBoats(), competitorWithBoat2.getBoat()));
        addRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regatta);
        RegattaLeaderboard fullLeaderboard = res.addRegattaLeaderboard(regatta.getRegattaIdentifier(), null, new int[] { 3, 5 });
        // use the set-up and add a regatta leaderboard with eliminations that wraps the regatta leaderboard
        RegattaLeaderboardWithEliminations withEliminations = res.addRegattaLeaderboardWithEliminations("U16", /* leaderboardDisplayName */ "Display Name", fullLeaderboard);
        res.apply(new UpdateEliminatedCompetitorsInLeaderboard(withEliminations.getName(), Collections.singleton(competitorWithBoat1)));
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertNotNull(loadedRegatta);
        assertEquals(regatta.getName(), loadedRegatta.getName());
        assertEquals(Util.size(regatta.getSeries()), Util.size(loadedRegatta.getSeries()));
        Leaderboard loadedLeaderboard = dof.loadLeaderboard(regatta.getName(), res, /* leaderboardRegistry */ null);
        assertNotNull(loadedLeaderboard);
        assertTrue(loadedLeaderboard instanceof RegattaLeaderboard);
        RegattaLeaderboard loadedRegattaLeaderboard = (RegattaLeaderboard) loadedLeaderboard;
        assertSame(regatta, loadedRegattaLeaderboard.getRegatta());
        RacingEventService res2 = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService()), MediaDBFactory.INSTANCE.getMediaDB(getMongoService()), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false);
        // now load the thing again from the store:
        Leaderboard loadedLeaderboardWithEliminations = res2.getLeaderboardByName(withEliminations.getName());
        assertTrue(withEliminations != loadedLeaderboardWithEliminations);
        assertTrue(loadedLeaderboardWithEliminations instanceof RegattaLeaderboardWithEliminations);
        assertEquals("Display Name", loadedLeaderboardWithEliminations.getDisplayName());
        assertEquals(withEliminations.getAllCompetitors(), loadedLeaderboardWithEliminations.getAllCompetitors());
        assertTrue(((RegattaLeaderboardWithEliminations) loadedLeaderboardWithEliminations).isEliminated(competitorWithBoat1));
        assertFalse(((RegattaLeaderboardWithEliminations) loadedLeaderboardWithEliminations).isEliminated(competitorWithBoat2));
    }

    @Test
    public void testLoadStoreRegattaLeaderboardWithScoreCorrections() {
        // for some reason the dropping of collections doesn't work reliably on Linux... explicitly drop those collections that we depend on
        getMongoService().getDB().getCollection(CollectionNames.LEADERBOARDS.name()).drop();
        getMongoService().getDB().getCollection(CollectionNames.REGATTAS.name()).drop();
        CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Dr. Hasso Plattner");
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        final DynamicTrackedRegatta[] trackedRegatta = new DynamicTrackedRegatta[1];
        final DynamicTrackedRace q2YellowTrackedRace = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 1, /* started */ false, boatClass) {
            private static final long serialVersionUID = 1234L;
            @Override
            public RegattaAndRaceIdentifier getRaceIdentifier() {
                return new RegattaNameAndRaceName("Kieler Woche (29ERXX)", "Yellow Race 2");
            }
            @Override
            public DynamicTrackedRegatta getTrackedRegatta() {
                return trackedRegatta[0];
            }
        };
        RacingEventService res = createRacingEventServiceWithOneMockedTrackedRace(q2YellowTrackedRace);
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        Regatta regattaProxy = createRegatta(RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true,  CompetitorRegistrationType.OPEN_UNMODERATED, regattaStartDate, regattaEndDate,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);
        Regatta regatta = res.createRegatta(regattaProxy.getName(), regattaProxy.getBoatClass().getName(),
                regattaProxy.canBoatsOfCompetitorsChangePerRace(), regattaProxy.getCompetitorRegistrationType(),
                /* registrationLinkSecret */ null, regattaProxy.getStartDate(), regattaProxy.getEndDate(), "123",
                regattaProxy.getSeries(), regattaProxy.isPersistent(),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* courseAreaIds */ Collections.emptySet(),
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        trackedRegatta[0] = new DynamicTrackedRegattaImpl(regatta);
        addRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regatta);
        logColumnsInRegatta(regatta);
        RegattaLeaderboard regattaLeaderboard = res.addRegattaLeaderboard(regatta.getRegattaIdentifier(), null, new int[] { 3, 5 });
        assertSame(regatta, regattaLeaderboard.getRegatta());
        final RaceColumnInSeries q2 = regatta.getSeriesByName("Qualifying").getRaceColumnByName("Q2");
        final Fleet yellow = q2.getFleetByName("Yellow");
        logColumnsInRegatta(regatta);
        logColumnsInRegattaLeaderboard(regattaLeaderboard);
        assertNotNull(regattaLeaderboard.getRaceColumnByName(q2.getName()));
        res.apply(new ConnectTrackedRaceToLeaderboardColumn(regattaLeaderboard.getName(), q2.getName(), yellow
                .getName(), q2YellowTrackedRace.getRaceIdentifier()));
        res.apply(new UpdateLeaderboardMaxPointsReason(regattaLeaderboard.getName(), q2.getName(), hasso.getId().toString(),
                MaxPointsReason.DNF, MillisecondsTimePoint.now()));
        // load new RacingEventService including regatta and leaderboard
        RacingEventService resForLoading = createRacingEventServiceWithOneMockedTrackedRace(q2YellowTrackedRace);
        Regatta loadedRegatta = resForLoading.getRegattaByName("Kieler Woche (29ERXX)");
        assertNotNull(loadedRegatta);
        assertEquals(regatta.getName(), loadedRegatta.getName());
        assertEquals(Util.size(regatta.getSeries()), Util.size(loadedRegatta.getSeries()));
        Leaderboard loadedLeaderboard = resForLoading.getLeaderboardByName(loadedRegatta.getName());
        assertNotNull(loadedLeaderboard);
        assertEquals(((ThresholdBasedResultDiscardingRule) regattaLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces().length,
                ((ThresholdBasedResultDiscardingRule) loadedLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces().length);
        assertTrue(Arrays.equals(((ThresholdBasedResultDiscardingRule) regattaLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces(),
                ((ThresholdBasedResultDiscardingRule) loadedLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces()));
        assertTrue(loadedLeaderboard instanceof RegattaLeaderboard);
        RegattaLeaderboard loadedRegattaLeaderboard = (RegattaLeaderboard) loadedLeaderboard;
        assertSame(loadedRegatta, loadedRegattaLeaderboard.getRegatta());
        // now re-associate the tracked race to let score correction "snap" to competitor:
        final RaceColumnInSeries loadedQ2 = loadedRegatta.getSeriesByName("Qualifying").getRaceColumnByName("Q2");
        final Fleet loadedYellow = loadedQ2.getFleetByName("Yellow");
        // adjust tracked regatta for tracked race:
        trackedRegatta[0] = new DynamicTrackedRegattaImpl(loadedRegatta);
        resForLoading.apply(new ConnectTrackedRaceToLeaderboardColumn(loadedLeaderboard.getName(), loadedQ2.getName(), loadedYellow
                .getName(), q2YellowTrackedRace.getRaceIdentifier()));
        MaxPointsReason hassosLoadedMaxPointsReason = loadedLeaderboard.getScoreCorrection().getMaxPointsReason(hasso, loadedQ2, MillisecondsTimePoint.now());
        assertEquals(MaxPointsReason.DNF, hassosLoadedMaxPointsReason);
        assertEquals(regatta.getCompetitorRegistrationType(), loadedRegatta.getCompetitorRegistrationType());
    }

    private void logColumnsInRegattaLeaderboard(RegattaLeaderboard regattaLeaderboard) {
        StringBuilder rlbrcNames = new StringBuilder();
        for (RaceColumn rlbrc : regattaLeaderboard.getRaceColumns()) {
            rlbrcNames.append("; ");
            rlbrcNames.append(rlbrc.getName());
        }
        logger.info("columns in regatta leaderboard for regatta "+regattaLeaderboard.getRegatta().getName()+" ("+
                regattaLeaderboard.getRegatta().hashCode()+"): "+rlbrcNames);
        logColumnsInRegatta(regattaLeaderboard.getRegatta());
    }

    private void logColumnsInRegatta(Regatta regatta) {
        StringBuilder rrcNames = new StringBuilder();
        for (Series series : regatta.getSeries()) {
            for (RaceColumn raceColumn : series.getRaceColumns()) {
                rrcNames.append("; ");
                rrcNames.append(raceColumn.getName());
            }
        }
        logger.info("columns in regatta "+regatta.getName()+" ("+regatta.hashCode()+") : "+rrcNames);
    }

    private RacingEventServiceImpl createRacingEventServiceWithOneMockedTrackedRace(final DynamicTrackedRace q2YellowTrackedRace) {
        return new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService()), MediaDBFactory.INSTANCE.getMediaDB(getMongoService()), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false) {
            @Override
            public DynamicTrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier raceIdentifier) {
                return q2YellowTrackedRace;
            }
        };
    }

    @Test
    public void testLoadStoreSimpleRegatta() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regattaName,
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertSame(LowPoint.class, loadedRegatta.getScoringScheme().getClass());
        assertEquals(regattaName, loadedRegatta.getName());
        Iterator<? extends Series> seriesIter = loadedRegatta.getSeries().iterator();
        Series loadedQualifyingSeries = seriesIter.next();
        assertEquals(numberOfQualifyingRaces, Util.size(loadedQualifyingSeries.getRaceColumns()));
        assertEquals(0, loadedQualifyingSeries.getFleetByName("Yellow").compareTo(loadedQualifyingSeries.getFleetByName("Blue")));
        Series loadedFinalSeries = seriesIter.next();
        assertEquals(numberOfFinalRaces, Util.size(loadedFinalSeries.getRaceColumns()));
        assertTrue(loadedFinalSeries.getFleetByName("Silver").compareTo(loadedFinalSeries.getFleetByName("Gold")) > 0);
        Series loadedMedalSeries = seriesIter.next();
        assertEquals(1, Util.size(loadedMedalSeries.getRaceColumns()));
        assertEquals(loadedRegatta.getStartDate(), regattaStartDate);
        assertEquals(loadedRegatta.getEndDate(), regattaEndDate);
        assertEquals(RankingMetrics.ONE_DESIGN, loadedRegatta.getRankingMetricType());
    }

    @Test
    public void testLoadStoreRegattaWithHandicapRanking() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regattaName,
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), TimeOnTimeAndDistanceRankingMetric::new);
        assertEquals(RankingMetrics.TIME_ON_TIME_AND_DISTANCE, regatta.getRankingMetricType());
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertSame(LowPoint.class, loadedRegatta.getScoringScheme().getClass());
        assertEquals(regattaName, loadedRegatta.getName());
        Iterator<? extends Series> seriesIter = loadedRegatta.getSeries().iterator();
        Series loadedQualifyingSeries = seriesIter.next();
        assertEquals(numberOfQualifyingRaces, Util.size(loadedQualifyingSeries.getRaceColumns()));
        assertEquals(0, loadedQualifyingSeries.getFleetByName("Yellow").compareTo(loadedQualifyingSeries.getFleetByName("Blue")));
        Series loadedFinalSeries = seriesIter.next();
        assertEquals(numberOfFinalRaces, Util.size(loadedFinalSeries.getRaceColumns()));
        assertTrue(loadedFinalSeries.getFleetByName("Silver").compareTo(loadedFinalSeries.getFleetByName("Gold")) > 0);
        Series loadedMedalSeries = seriesIter.next();
        assertEquals(1, Util.size(loadedMedalSeries.getRaceColumns()));
        assertEquals(loadedRegatta.getStartDate(), regattaStartDate);
        assertEquals(loadedRegatta.getEndDate(), regattaEndDate);
        assertEquals(RankingMetrics.TIME_ON_TIME_AND_DISTANCE, loadedRegatta.getRankingMetricType());
    }

    @Test
    public void testLoadStoreSimpleRegattaWithEmptyStartAndEndDate() {
        final int numberOfQualifyingRaces = 1;
        final int numberOfFinalRaces = 1;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regattaName,
                boatClass, null, null, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertEquals(regattaName, loadedRegatta.getName());
        assertEquals(loadedRegatta.getStartDate(), null);
        assertEquals(loadedRegatta.getEndDate(), null);
    }

    @Test
    public void testLoadStoreSimpleRegattaWithSeriesScoringScheme() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()),
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        regatta.getSeriesByName("Qualifying").setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 1, 2, 3 }));
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertTrue(Arrays.equals(new int[] { 1, 2, 3 },
                loadedRegatta.getSeriesByName("Qualifying").getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces()));
    }

    @Test
    public void testLoadStoreSimpleRegattaWithScoreForMedalStartingWithZero() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()),
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        regatta.getSeriesByName("Medal").setStartsWithZeroScore(true);
        regatta.getSeriesByName("Medal").setOneAlwaysStaysOne(true);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertFalse(loadedRegatta.getSeriesByName("Qualifying").isStartsWithZeroScore());
        assertTrue(loadedRegatta.getSeriesByName("Medal").isStartsWithZeroScore());
        assertTrue(loadedRegatta.getSeriesByName("Medal").isOneAlwaysStaysOne());
    }

    @Test
    public void testLoadStoreRegattaWithSeriesWhereFleetsCanRunInParallel() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()),
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        regatta.getSeriesByName("Qualifying").setIsFleetsCanRunInParallel(true);
        regatta.getSeriesByName("Final").setIsFleetsCanRunInParallel(false);
        regatta.getSeriesByName("Medal").setIsFleetsCanRunInParallel(false);

        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertTrue(loadedRegatta.getSeriesByName("Qualifying").isFleetsCanRunInParallel());
        assertFalse(loadedRegatta.getSeriesByName("Final").isFleetsCanRunInParallel());
        assertFalse(loadedRegatta.getSeriesByName("Medal").isFleetsCanRunInParallel());
    }

    @Test
    public void testLoadStoreRegattaWithCompetitorsWhichCanChangeBoats() {
        final boolean canBoatsOfCompetitorsChangePerRace = true;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegatta(regattaName, boatClass, canBoatsOfCompetitorsChangePerRace,  CompetitorRegistrationType.CLOSED,
                null, null, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertEquals(regattaName, loadedRegatta.getName());
        assertEquals(loadedRegatta.canBoatsOfCompetitorsChangePerRace(), canBoatsOfCompetitorsChangePerRace);
    }

    @Test
    public void testLoadStoreRegattaWithCompetitorsWhichCanNotChangeBoats() {
        final boolean canBoatsOfCompetitorsChangePerRace = false;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegatta(regattaName, boatClass, canBoatsOfCompetitorsChangePerRace,  CompetitorRegistrationType.CLOSED,
                null, null, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertEquals(regattaName, loadedRegatta.getName());
        assertEquals(loadedRegatta.canBoatsOfCompetitorsChangePerRace(), canBoatsOfCompetitorsChangePerRace);
    }

    @Test
    public void testLoadStoreSimpleRegattaWithHighPointScoringScheme() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "ESS40 Cardiff 2012";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("ESS40", /* typicallyStartsUpwind */ false);
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()),
                boatClass, regattaStartDate, regattaEndDate, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT), OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertSame(HighPoint.class, loadedRegatta.getScoringScheme().getClass());
    }

    @Test
    public void testLoadStoreRegattaWithFleetsEnsuringIdenticalFleetsInSeriesAndRaceColumns() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces,
                regattaName, boatClass, regattaStartDate, regattaEndDate,
                /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertEquals(regattaName, loadedRegatta.getName());
        Iterator<? extends Series> seriesIter = loadedRegatta.getSeries().iterator();
        Series loadedQualifyingSeries = seriesIter.next();
        int i=1;
        for (RaceColumn raceColumn : loadedQualifyingSeries.getRaceColumns()) {
            assertTrue(raceColumn instanceof RaceColumnInSeriesImpl);
            assertEquals("Q"+i, raceColumn.getName());
            assertTrue(Util.equals(loadedQualifyingSeries.getFleets(), raceColumn.getFleets()));
            i++;
        }
        Series loadedFinalSeries = seriesIter.next();
        i=1;
        for (RaceColumn raceColumn : loadedFinalSeries.getRaceColumns()) {
            assertTrue(raceColumn instanceof RaceColumnInSeriesImpl);
            assertEquals("F"+i, raceColumn.getName());
            assertTrue(Util.equals(loadedFinalSeries.getFleets(), raceColumn.getFleets()));
            i++;
        }
        Series loadedMedalSeries = seriesIter.next();
        for (RaceColumn raceColumn : loadedMedalSeries.getRaceColumns()) {
            assertTrue(raceColumn instanceof RaceColumnInSeriesImpl);
            assertEquals("M", raceColumn.getName());
            assertTrue(Util.equals(loadedMedalSeries.getFleets(), raceColumn.getFleets()));
        }
    }

    @Test
    public void testLoadStoreRegattaWithFleetsEnsuringFleetOrdering() {
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        final String regattaName = RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName());
        Regatta regatta = createRegatta(regattaName, boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, regattaStartDate, regattaEndDate,
                /* persistent */ false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        assertEquals(regattaName, loadedRegatta.getName());

        Iterator<? extends Series> seriesIter = loadedRegatta.getSeries().iterator();
        Series loadedQualifyingSeries = seriesIter.next();

        Iterator<? extends Fleet> qualiFleetIt = loadedQualifyingSeries.getFleets().iterator();
        Fleet qualiFleet1 = qualiFleetIt.next();
        assertEquals(qualiFleet1.getName(), "Yellow");
        Fleet qualiFleet2 = qualiFleetIt.next();
        assertEquals(qualiFleet2.getName(), "Blue");

        Series loadedFinalSeries = seriesIter.next();
        Iterator<? extends Fleet> finalFleetIt = loadedFinalSeries.getFleets().iterator();
        Fleet finalFleet1 = finalFleetIt.next();
        assertEquals(finalFleet1.getName(), "Gold");
        Fleet finalFleet2 = finalFleetIt.next();
        assertEquals(finalFleet2.getName(), "Silver");
    }

    @Test
    public void testStorageOfRaceIdentifiersOnRaceColumnInSeries() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegattaAndAddRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces,
                RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass, regattaStartDate, regattaEndDate,
                /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        Series qualifyingSeries = regatta.getSeries().iterator().next();
        RaceColumn q2 = qualifyingSeries.getRaceColumnByName("Q2");
        final RegattaNameAndRaceName q2TrackedRaceIdentifier = new RegattaNameAndRaceName(regatta.getName(), "Q2 TracTrac");
        q2.setRaceIdentifier(qualifyingSeries.getFleetByName("Yellow"), q2TrackedRaceIdentifier);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        Series loadedQualifyingSeries = loadedRegatta.getSeries().iterator().next();
        RaceColumn loadedQ2 = loadedQualifyingSeries.getRaceColumnByName("Q2");
        RaceIdentifier loadedQ2TrackedRaceIdentifier = loadedQ2.getRaceIdentifier(loadedQualifyingSeries.getFleetByName("Yellow"));
        assertEquals(q2TrackedRaceIdentifier, loadedQ2TrackedRaceIdentifier);
        assertNotSame(q2TrackedRaceIdentifier, loadedQ2TrackedRaceIdentifier);
        assertNull(loadedQualifyingSeries.getRaceColumnByName("Q1").getRaceIdentifier(loadedQualifyingSeries.getFleetByName("Yellow")));
        assertNull(loadedQualifyingSeries.getRaceColumnByName("Q2").getRaceIdentifier(loadedQualifyingSeries.getFleetByName("Blue")));
    }

    @Test
    public void testStorageOfRaceIdentifiersOnRaceColumnInSeriesWithSpecialCharactersInFleetName() {
        final int numberOfQualifyingRaces = 5;
        final int numberOfFinalRaces = 7;
        final String regattaBaseName = "Kieler Woche";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegattaAndAddRaceColumns(new String[] {"1.Start", "2.Start"},
                new String[] {"Gold", "Silver"}, numberOfQualifyingRaces, numberOfFinalRaces,
                RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass, regattaStartDate, regattaEndDate,
                /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), OneDesignRankingMetric::new);
        Series qualifyingSeries = regatta.getSeries().iterator().next();
        RaceColumn q2 = qualifyingSeries.getRaceColumnByName("Q2");
        final RegattaNameAndRaceName q2TrackedRaceIdentifier = new RegattaNameAndRaceName(regatta.getName(), "Q2 TracTrac");
        q2.setRaceIdentifier(qualifyingSeries.getFleetByName("1.Start"), q2TrackedRaceIdentifier);
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);

        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Regatta loadedRegatta = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        Series loadedQualifyingSeries = loadedRegatta.getSeries().iterator().next();
        RaceColumn loadedQ2 = loadedQualifyingSeries.getRaceColumnByName("Q2");
        RaceIdentifier loadedQ2TrackedRaceIdentifier = loadedQ2.getRaceIdentifier(loadedQualifyingSeries.getFleetByName("1.Start"));
        assertEquals(q2TrackedRaceIdentifier, loadedQ2TrackedRaceIdentifier);
        assertNotSame(q2TrackedRaceIdentifier, loadedQ2TrackedRaceIdentifier);
        assertNull(loadedQualifyingSeries.getRaceColumnByName("Q1").getRaceIdentifier(loadedQualifyingSeries.getFleetByName("1.Start")));
        assertNull(loadedQualifyingSeries.getRaceColumnByName("Q2").getRaceIdentifier(loadedQualifyingSeries.getFleetByName("2.Start")));
    }

    private Regatta createRegattaAndAddRaceColumns(final int numberOfQualifyingRaces, final int numberOfFinalRaces,
            final String regattaName, BoatClass boatClass, TimePoint startDate, TimePoint endDate, boolean persistent,
            ScoringScheme scoringScheme, RankingMetricConstructor rankingMetricConstructor) {
        return createRegattaAndAddRaceColumns(new String[] {"Yellow", "Blue"},
                new String[] {"Gold", "Silver"}, numberOfQualifyingRaces, numberOfFinalRaces, regattaName,
                boatClass, startDate, endDate, persistent, scoringScheme, rankingMetricConstructor);
    }

    private Regatta createRegattaAndAddRaceColumns(String[] qualifyingSeriesFleetNames, String[] finalSeriesFleetNames,
            final int numberOfQualifyingRaces, final int numberOfFinalRaces,
            final String regattaName, BoatClass boatClass, TimePoint startDate, TimePoint endDate, boolean persistent,
            ScoringScheme scoringScheme, RankingMetricConstructor rankingMetricConstructor) {
        Regatta regatta = createRegatta(qualifyingSeriesFleetNames, finalSeriesFleetNames,
                regattaName, boatClass, /* canBoatsOfCompetitorsChangePerRace */ true,
                CompetitorRegistrationType.CLOSED, startDate, endDate, persistent, scoringScheme, null, rankingMetricConstructor);
        addRaceColumns(numberOfQualifyingRaces, numberOfFinalRaces, regatta);
        return regatta;
    }

    private void addRaceColumns(final int numberOfQualifyingRaces, final int numberOfFinalRaces, Regatta regatta) {
        List<String> finalRaceColumnNames = new ArrayList<String>();
        for (int i=1; i<=numberOfFinalRaces; i++) {
            finalRaceColumnNames.add("F"+i);
        }
        List<String> qualifyingRaceColumnNames = new ArrayList<String>();
        for (int i=1; i<=numberOfQualifyingRaces; i++) {
            qualifyingRaceColumnNames.add("Q"+i);
        }
        List<String> medalRaceColumnNames = new ArrayList<String>();
        medalRaceColumnNames.add("M");
        addRaceColumnsToSeries(qualifyingRaceColumnNames, regatta.getSeriesByName("Qualifying"));
        addRaceColumnsToSeries(finalRaceColumnNames, regatta.getSeriesByName("Final"));
        addRaceColumnsToSeries(medalRaceColumnNames, regatta.getSeriesByName("Medal"));
    }

    private Regatta createRegatta(final String regattaName, BoatClass boatClass, boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType, TimePoint startDate,
            TimePoint endDate, boolean persistent, ScoringScheme scoringScheme, CourseArea courseArea,
            RankingMetricConstructor rankingMetricConstructor) {
        return createRegatta(new String[] {"Yellow", "Blue"},
                new String[] {"Gold", "Silver"}, regattaName, boatClass, canBoatsOfCompetitorsChangePerRace,
                competitorRegistrationType, startDate, endDate, persistent, scoringScheme, courseArea, rankingMetricConstructor);
    }

    private Regatta createRegatta(String[] qualifyingSeriesFleetNames, String[] finalSeriesFleetNames,
            final String regattaName, BoatClass boatClass, boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType, TimePoint startDate,
            TimePoint endDate, boolean persistent, ScoringScheme scoringScheme, CourseArea courseArea,
            RankingMetricConstructor rankingMetricConstructor) {
        List<String> emptyRaceColumnNames = Collections.emptyList();
        List<Series> series = new ArrayList<Series>();

        // -------- qualifying series ------------
        List<Fleet> qualifyingFleets = new ArrayList<Fleet>();
        for (final String qualifyingSeriesFleetName : qualifyingSeriesFleetNames) {
            qualifyingFleets.add(new FleetImpl(qualifyingSeriesFleetName));
        }
        Series qualifyingSeries = new SeriesImpl("Qualifying", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualifyingFleets,
                emptyRaceColumnNames, /* trackedRegattaRegistry */ null);
        series.add(qualifyingSeries);

        // -------- final series ------------
        List<Fleet> finalFleets = new ArrayList<Fleet>();
        int rank=1;
        for (final String finalSeriesFleetName : finalSeriesFleetNames) {
            finalFleets.add(new FleetImpl(finalSeriesFleetName, rank++));
        }
        Series finalSeries = new SeriesImpl("Final", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, finalFleets, emptyRaceColumnNames, /* trackedRegattaRegistry */ null);
        series.add(finalSeries);

        // ------------ medal --------------
        List<Fleet> medalFleets = new ArrayList<Fleet>();
        medalFleets.add(new FleetImpl("Medal"));
        Series medalSeries = new SeriesImpl("Medal", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, medalFleets, emptyRaceColumnNames, /* trackedRegattaRegistry */ null);
        series.add(medalSeries);
        Regatta regatta = new RegattaImpl(regattaName, boatClass, canBoatsOfCompetitorsChangePerRace,  competitorRegistrationType, startDate, endDate,
                series, persistent, scoringScheme, "123", courseArea, rankingMetricConstructor,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private void addRaceColumnsToSeries(List<String> finalRaceColumnNames, Series finalSeries) {
        for (String raceColumnName : finalRaceColumnNames) {
            finalSeries.addRaceColumn(raceColumnName, /* trackedRegattaRegistry */ null);
        }
    }

    @Test
    public void testRegattaRaceAssociationStore() throws Exception {
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("112er", /* typicallyStartsUpwind */ true);
        Regatta regatta = createRegatta(RegattaImpl.getDefaultName("Cologne Masters", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true,  CompetitorRegistrationType.CLOSED, regattaStartDate, regattaEndDate, /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), null, OneDesignRankingMetric::new);

        Map<Competitor, Boat> competitorsAndBoats = new LinkedHashMap<>();
        Competitor c = new CompetitorImpl("Axel", "Axel Uhl", "KYC", Color.RED, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Boat b = new BoatImpl("123", "boat", boatClass, "GER 124");
        competitorsAndBoats.put(c, b);
        Iterable<Waypoint> waypoints = Collections.emptyList();
        Course course = new CourseImpl("Course", waypoints);

        RaceDefinition racedef = new RaceDefinitionImpl("M1", course, boatClass, competitorsAndBoats);
        regatta.addRace(racedef);

        RacingEventServiceImpl evs = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService()), MediaDBFactory.INSTANCE.getMediaDB(getMongoService()), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false);
        assertNull(evs.getRememberedRegattaForRace(racedef.getId()));
        evs.raceAdded(regatta, racedef);
        assertNotNull(evs.getRememberedRegattaForRace(racedef.getId()));
        evs.removeRegatta(regatta);
        assertNull(evs.getRememberedRegattaForRace(racedef.getId()));
    }

    @Test
    public void testStoringAnConfiguredSeries() throws Exception {
        final String seriesName = "TestSeries";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        Fleet fleet = new FleetImpl("TestFleet");
        Set<Fleet> fleets = new HashSet<>();
        fleets.add(fleet);
        Series series = new SeriesImpl(seriesName, false, true, fleets, new HashSet<String>(), null);
        // set to the opposite of the default
        series.setCrossFleetMergedRanking(true);
        series.setSplitFleetContiguousScoring(true);
        series.setFirstColumnIsNonDiscardableCarryForward(true);
        series.setIsMedal(true);
        series.setIsFleetsCanRunInParallel(false);
        Set<Series> seriesSet = new HashSet<>();
        seriesSet.add(series);
        String raceColumnName = "TestRace";
        series.addRaceColumn(raceColumnName, null);
        ScoringScheme scoringScheme = new LowPoint();
        Regatta regatta = new RegattaImpl("TestRegatta", boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */ null, seriesSet, false, scoringScheme, UUID.randomUUID(),
                null, null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        mof.storeRegatta(regatta);
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        Series loadedSeries = dof.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null).getSeriesByName(seriesName);

        assertEquals(series.hasCrossFleetMergedRanking(), loadedSeries.hasCrossFleetMergedRanking());
        assertEquals(series.hasSplitFleetContiguousScoring(), loadedSeries.hasSplitFleetContiguousScoring());
        assertEquals(series.isFirstColumnNonDiscardableCarryForward(), loadedSeries.isFirstColumnNonDiscardableCarryForward());
        assertEquals(series.isMedal(), loadedSeries.isMedal());
        assertEquals(series.isFleetsCanRunInParallel(), loadedSeries.isFleetsCanRunInParallel());
    }

}
