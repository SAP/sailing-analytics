package com.sap.sailing.domain.swisstimingadapter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Util;
import com.sap.sailing.domain.swisstimingadapter.MessageType;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterTransceiver;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.StoreAndForward;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sailing.domain.swisstimingadapter.persistence.impl.CollectionNames;
import com.sap.sailing.domain.swisstimingadapter.persistence.impl.FieldNames;
import com.sap.sailing.domain.tracking.DynamicTrackedEvent;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;

public class EndToEndListeningStoreAndFowardTest {
    private static final Logger logger = Logger.getLogger(EndToEndListeningStoreAndFowardTest.class.getName());

    private static final int RECEIVE_PORT = 6543;
    private static final int CLIENT_PORT = 6544;

    private DB db;
    private StoreAndForward storeAndForward;
    private Socket sendingSocket;
    private OutputStream sendingStream;
    private SailMasterTransceiver transceiver;
    private SailMasterConnector connector;
    private SwissTimingAdapterPersistence swissTimingAdapterPersistence;
    private SwissTimingFactory swissTimingFactory;

    private EmptyWindStore emptyWindStore;
    private RacingEventService racingEventService;

    private List<RacesHandle> raceHandles;

    @Before
    public void setUp() throws UnknownHostException, IOException, InterruptedException {
        logger.info("EndToEndListeningStoreAndFowardTest.setUp");
        MongoDBService mongoDBService = MongoDBService.INSTANCE;
        db = mongoDBService.getDB();
        swissTimingAdapterPersistence = SwissTimingAdapterPersistence.INSTANCE;
        swissTimingAdapterPersistence.dropAllMessageData();
        swissTimingAdapterPersistence.dropAllRaceMasterData();
        storeAndForward = new StoreAndForward(RECEIVE_PORT, CLIENT_PORT, SwissTimingFactory.INSTANCE,
                swissTimingAdapterPersistence, mongoDBService);
        sendingSocket = new Socket("localhost", RECEIVE_PORT);
        sendingStream = sendingSocket.getOutputStream();
        swissTimingFactory = SwissTimingFactory.INSTANCE;
        emptyWindStore = EmptyWindStore.INSTANCE;
        transceiver = swissTimingFactory.createSailMasterTransceiver();
        DBCollection lastMessageCountCollection = db.getCollection(CollectionNames.LAST_MESSAGE_COUNT.name());
        lastMessageCountCollection.update(new BasicDBObject(),
                new BasicDBObject().append(FieldNames.LAST_MESSAGE_COUNT.name(), 0l),
                /* upsert */true, /* multi */false);
        racingEventService = new RacingEventServiceImpl();
        raceHandles = new ArrayList<RacesHandle>();
    }

    @After
    public void tearDown() throws InterruptedException, IOException {
        logger.entering(getClass().getName(), "tearDown");
        for (RacesHandle raceHandle : raceHandles) {
            racingEventService.stopTracking(raceHandle.getEvent());
        }
        logger.info("Calling StoreAndForward.stop() in tearDown");
        storeAndForward.stop();
        logger.exiting(getClass().getName(), "tearDown");
    }

     @Test
    public void testEndToEndScenarioWithInitMessages() throws IOException, InterruptedException, ParseException {
        String[] racesToTrack = new String[] { "4711", "4712" };
        String scriptName = "/InitMessagesScript.txt";
        setUpUsingScript(racesToTrack, scriptName);

        Set<TrackedRace> allTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allEvents = racingEventService.getAllEvents();
        for (Event event : allEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allTrackedRaces.add(trackedRace);
            }
        }
        assertEquals(2, Util.size(allTrackedRaces));
        Set<String> raceIDs = new HashSet<String>();
        for (TrackedRace trackedRace : allTrackedRaces) {
            RaceDefinition race = trackedRace.getRace();
            raceIDs.add(race.getName());
        }
        Set<String> expectedRaceIDs = new HashSet<String>();
        for (String raceIDToTrack : new String[] { "4711", "4712" }) {
            expectedRaceIDs.add(raceIDToTrack);
        }
        assertEquals(expectedRaceIDs, raceIDs);
    }

    @Test
    public void testLongRaceLog() throws IOException, InterruptedException, ParseException {
        String[] racesToTrack = new String[] { "W4702" };
        String scriptName1 = "/SailMasterDataInterfaceRACandSTL.txt";
        String scriptName2 = "/SailMasterDataInterface-ExampleAsText.txt";
        setUpUsingScript(racesToTrack, scriptName1, scriptName2);

        Set<TrackedRace> allTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allEvents = racingEventService.getAllEvents();
        for (Event event : allEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allTrackedRaces.add(trackedRace);
            }
        }
        assertEquals(1, Util.size(allTrackedRaces));
        Set<RaceDefinition> races = raceHandles.iterator().next().getRaceTracker().getRaces();
        assertEquals(1, races.size());
        RaceDefinition raceFromTracker = races.iterator().next();
        assertNotNull(raceFromTracker);
        Set<String> raceIDs = new HashSet<String>();
        for (TrackedRace trackedRace : allTrackedRaces) {
            RaceDefinition race = trackedRace.getRace();
            raceIDs.add(race.getName());
            assertEquals(46, Util.size(race.getCompetitors()));
            assertEquals(6, Util.size(race.getCourse().getWaypoints()));
            assertEquals(5, Util.size(race.getCourse().getLegs()));
            for (Competitor competitor : race.getCompetitors()) {
                if (!competitor.getName().equals("Competitor 35") && !competitor.getName().equals("Competitor 20")) {
                    assertTrue("Track of competitor " + competitor + " empty",
                            !Util.isEmpty(trackedRace.getTrack(competitor).getRawFixes()));
                }
            }
            Set<Buoy> buoys = new HashSet<Buoy>();
            for (Waypoint waypoint : race.getCourse().getWaypoints()) {
                for (Buoy buoy : waypoint.getBuoys()) {
                    buoys.add(buoy);
                }
            }
            for (Buoy buoy : buoys) {
                assertTrue("Track of buoy " + buoy + " empty",
                        !Util.isEmpty(trackedRace.getOrCreateTrack(buoy).getRawFixes()));
            }
        }
        Set<String> expectedRaceIDs = new HashSet<String>();
        for (String raceIDToTrack : new String[] { "W4702" }) {
            expectedRaceIDs.add(raceIDToTrack);
        }
        assertEquals(expectedRaceIDs, raceIDs);
    }
    
    @Test
    public void testLongLogRaceNewConfig() throws UnknownHostException, InterruptedException, IOException, ParseException {
        String[] racesToTrack = new String[] { "W4702" };
        String scriptName1 = "/SailMasterDataInterfaceRACandSTL.txt";
        String scriptName2 = "/SailMasterDataInterface-ExampleAsText.txt";
         String scriptNewCourseConfig = "/SailMasterDataInterfaceNewCourseConfig.txt";
        setUpUsingScript(racesToTrack, scriptName1, scriptName2, scriptNewCourseConfig);
        Set<TrackedRace> allNewTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allNewEvents = racingEventService.getAllEvents();
        for (Event event : allNewEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allNewTrackedRaces.add(trackedRace);
            }
        }
        assertEquals(1, Util.size(allNewTrackedRaces));
        Set<RaceDefinition> races = raceHandles.iterator().next().getRaceTracker().getRaces();
        assertEquals(1, races.size());
        RaceDefinition raceFromTracker = races.iterator().next();
        assertNotNull(raceFromTracker);
        Set<String> raceIDs = new HashSet<String>();
        for (TrackedRace trackedRace : allNewTrackedRaces) {
            RaceDefinition race = trackedRace.getRace();
            raceIDs.add(race.getName());
            assertEquals(46, Util.size(race.getCompetitors()));
            assertEquals(3, Util.size(race.getCourse().getWaypoints()));
            assertEquals(2, Util.size(race.getCourse().getLegs()));
            for (Competitor competitor : race.getCompetitors()) {
                if (!competitor.getName().equals("Competitor 35") && !competitor.getName().equals("Competitor 20")) {
                    assertTrue("Track of competitor " + competitor + " empty",
                            !Util.isEmpty(trackedRace.getTrack(competitor).getRawFixes()));
                }
            }
            Set<Buoy> buoys = new HashSet<Buoy>();
            for (Waypoint waypoint : race.getCourse().getWaypoints()) {
                for (Buoy buoy : waypoint.getBuoys()) {
                    buoys.add(buoy);
                }
            }
            for (Buoy buoy : buoys) {
                assertTrue("Track of buoy " + buoy + " empty",
                        !Util.isEmpty(trackedRace.getOrCreateTrack(buoy).getRawFixes()));
            }
        }
        Set<String> expectedRaceIDs = new HashSet<String>();
        for (String raceIDToTrack : new String[] { "W4702" }) {
            expectedRaceIDs.add(raceIDToTrack);
        }
        assertEquals(expectedRaceIDs, raceIDs);
    }

    @Test
    public void testDuplicateCCGMessageAndWaypointUniqueness() throws IOException, InterruptedException, ParseException {
        String[] racesToTrack = new String[] { "W4702" };
        setUpUsingScript(racesToTrack, "/DuplicateCCG.txt");

        Set<TrackedRace> allTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allEvents = racingEventService.getAllEvents();
        for (Event event : allEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allTrackedRaces.add(trackedRace);
            }
        }
        assertEquals(1, allTrackedRaces.size());
        TrackedRace trackedRace = allTrackedRaces.iterator().next();
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        for (Waypoint waypoint : trackedRace.getRace().getCourse().getWaypoints()) {
            waypoints.add(waypoint);
        }
        assertEquals(7, Util.size(waypoints));
    }
    
    @Test
    public void testRongRaceLogRACZero() throws UnknownHostException, InterruptedException, IOException, ParseException{
        String[] racesToTrack = new String[] { "W4702" };
        String scriptName2 = "/SailMasterDataInterfaceRACZero.txt";
        setUpUsingScript(racesToTrack, scriptName2);
        Set<TrackedRace> allNewTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allNewEvents = racingEventService.getAllEvents();
        for (Event event : allNewEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allNewTrackedRaces.add(trackedRace);
            }
        }
        assertEquals(0, Util.size(allNewTrackedRaces));
    }

    @Test
    public void testEndToEndWithSwissTimingData() throws IOException, InterruptedException, ParseException {
        String[] racesToTrack = new String[] { "W4702" };
        String scriptName1 = "/SailMasterDataInterfaceRACandSTL.txt";
        String scriptName2 = "/SailMasterDataInterface-ExampleAsText.txt";
        setUpUsingScript(racesToTrack, scriptName1, scriptName2);

        Set<TrackedRace> allTrackedRaces = new HashSet<TrackedRace>();
        Iterable<Event> allEvents = racingEventService.getAllEvents();
        for (Event event : allEvents) {
            DynamicTrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
            Iterable<TrackedRace> trackedRaces = trackedEvent.getTrackedRaces();
            for (TrackedRace trackedRace : trackedRaces) {
                allTrackedRaces.add(trackedRace);
            }
        }
    }

    private void setUpUsingScript(String[] racesToTrack, String... scriptNames) throws InterruptedException,
            UnknownHostException, IOException, ParseException {
        for (String raceToTrack : racesToTrack) {
            RacesHandle raceHandle = racingEventService.addSwissTimingRace(raceToTrack, "localhost", CLIENT_PORT, /* canSendRequests */
                    false, emptyWindStore, -1);
            raceHandles.add(raceHandle);
            if (connector == null) {
                connector = racingEventService.getSwissTimingFactory().getOrCreateSailMasterConnector("localhost",
                        CLIENT_PORT, swissTimingAdapterPersistence, /* canSendRequests */false);
            }
        }
        ScriptedMessagesReader scriptedMessagesReader = new ScriptedMessagesReader();
        for (String scriptName : scriptNames) {
            InputStream is = getClass().getResourceAsStream(scriptName);
            scriptedMessagesReader.addMessagesFromTextFile(is);
        }
        for (String msg : scriptedMessagesReader.getMessages()) {
            transceiver.sendMessage(msg, sendingStream);
        }
        transceiver.sendMessage(swissTimingFactory.createMessage(MessageType._STOPSERVER.name(), null), sendingStream);
        synchronized (connector) {
            while (!connector.isStopped()) {
                connector.wait();
            }
        }
    }

}
