package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.Before;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;
import com.tractrac.clientmodule.Race;

/**
 * Connects to TracTrac data. Subclasses should implement a @Before method which calls
 * {@link #setUp(String, String, ReceiverType[])} with a useful set of receiver types and the race they want to observe
 * / load, or they should call {@link #setUp(String, String, ReceiverType[])} at the beginning of each respective test
 * in case they want to select/load different races for different tests. When all stored data has been received, the
 * {@link #getSemaphor() semaphor} is notified. Therefore, a typical pattern for subclasses should be to invoke
 * {@link #setUp(ReceiverType[])}, then wait on the semaphor before starting with test processing.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class OnlineTracTracBasedTest extends AbstractTracTracLiveTest {
    private final Logger logger = Logger.getLogger(OnlineTracTracBasedTest.class.getName());
    private DomainFactoryImpl domainFactory;
    private Regatta domainEvent;
    private DynamicTrackedRegatta trackedRegatta;
    private RaceDefinition race;
    private DynamicTrackedRace trackedRace;

    private final Object semaphor = new Object();
    
    /**
     * When the {@link #semaphor} is notified, this flag indicates whether {@link #storedDataEnd()} has already
     * been called.
     */
    private boolean storedDataLoaded;

    protected OnlineTracTracBasedTest() throws MalformedURLException, URISyntaxException {
        super();
    }
    
    
    @Before
    public void setUp() throws MalformedURLException, IOException, InterruptedException, URISyntaxException {
        domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl());
        // keep superclass implementation from automatically setting up for a Weymouth event and force subclasses
        // to select a race
    }

    protected void setUp(String regattaName, String raceId, ReceiverType... receiverTypes) throws MalformedURLException,
            IOException, InterruptedException, URISyntaxException {
        setUpWithoutLaunchingController(regattaName, raceId);
        finishSetUp(receiverTypes);
    }


    private void finishSetUp(ReceiverType... receiverTypes) throws InterruptedException {
        assertEquals(getExpectedEventName(), getTracTracEvent().getName());
        completeSetupLaunchingControllerAndWaitForRaceDefinition(receiverTypes);
    }

    protected void setUp(URL paramUrl, URI liveUri, URI storedUri, ReceiverType... receiverTypes)
            throws MalformedURLException, IOException, InterruptedException, URISyntaxException {
        setUpWithoutLaunchingController(paramUrl, liveUri, storedUri);
        finishSetUp(receiverTypes);
    }

    protected void completeSetupLaunchingControllerAndWaitForRaceDefinition(ReceiverType... receiverTypes)
            throws InterruptedException {
        setStoredDataLoaded(false);
        ArrayList<Receiver> receivers = new ArrayList<Receiver>();
        for (Receiver r : domainFactory.getUpdateReceivers(trackedRegatta, getTracTracEvent(), EmptyWindStore.INSTANCE,
        /* startOfTracking */null, /* endOfTracking */null, /* delayToLiveInMillis */0l, /* simulator */ null,
                new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                    }
                }, /* trackedRegattaRegistry */ null, /*courseDesignUpdateURI*/ null, /*tracTracUsername*/ null, /*tracTracPassword*/ null, receiverTypes)) {
            receivers.add(r);
        }
        addListenersForStoredDataAndStartController(receivers);
        Race tractracRace = getTracTracEvent().getRaceList().iterator().next();
        // we used to expect here that there is no RaceDefinition for the TracTrac race yet; however,
        // loading the race from an .mtb file stored locally, things work so fast that the race arrives through
        // a background thread (actually the RaceCourseReceiver) that it's initialized before we can check it here.
        race = getDomainFactory().getAndWaitForRaceDefinition(tractracRace.getId());
        assertNotNull(race);
        logger.info("Waiting for stored data to be loaded for " + race.getName());
        synchronized (getSemaphor()) {
            while (!isStoredDataLoaded()) {
                getSemaphor().wait();
            }
        }
        logger.info("Stored data has been loaded for " + race.getName());
        for (Receiver receiver : receivers) {
            logger.info("Stopping receiver "+receiver);
            receiver.stopAfterNotReceivingEventsForSomeTime(/* timeoutInMilliseconds */ 5000l);
            logger.info("Stopped receiver "+receiver);
        }
        for (Receiver receiver : receivers) {
            logger.info("Joining receiver "+receiver);
            receiver.join();
            logger.info("Joined receiver "+receiver);
        }
        trackedRace = getTrackedRegatta().getTrackedRace(race);
    }


    private void setStoredDataLoaded(boolean storedDataLoaded) {
        this.storedDataLoaded = storedDataLoaded;
    }


    protected void setUpWithoutLaunchingController(String regattaName, String raceId) throws FileNotFoundException, MalformedURLException,
            URISyntaxException {
        final URL paramUrl = new URL("http://" + TracTracConnectionConstants.HOST_NAME + "/events/"+regattaName+"/clientparams.php?event="+regattaName+"&race="+raceId);
        final URI liveUri = tractracTunnel ? new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_LIVE) : new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_LIVE);
        final URI storedUri = tractracTunnel ? new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_STORED) : new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_STORED);
        setUpWithoutLaunchingController(paramUrl, liveUri, storedUri);
    }


    protected void setUpWithoutLaunchingController(final URL paramUrl, final URI liveUri, final URI storedUri)
            throws FileNotFoundException, MalformedURLException {
        super.setUp(paramUrl, liveUri, storedUri);
        if (domainFactory == null) {
            domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl());
        }
        domainEvent = domainFactory.getOrCreateDefaultRegatta(EmptyRaceLogStore.INSTANCE, getTracTracEvent(), /* trackedRegattaRegistry */ null);
        trackedRegatta = new DynamicTrackedRegattaImpl(domainEvent);
    }
    
    protected Competitor getCompetitorByName(String nameRegexp) {
        Pattern p = Pattern.compile(nameRegexp);
        for (Competitor c : getTrackedRace().getRace().getCompetitors()) {
            if (p.matcher(c.getName()).matches()) {
                return c;
            }
        }
        return null;
    }

    /**
     * If a leg's type needs to be determined, some wind data is required to decide on upwind,
     * downwind or reaching leg. Wind information is queried by {@link TrackedLegImpl} based on
     * the marks' positions. Therefore, approximate mark positions are set here for all marks
     * of {@link #getTrackedRace()}'s courses for the time span starting at the epoch up to now.
     * @param timePointForFixes TODO
     */
    public static void fixApproximateMarkPositionsForWindReadOut(DynamicTrackedRace race, TimePoint timePointForFixes) {
        TimePoint epoch = new MillisecondsTimePoint(0l);
        TimePoint now = MillisecondsTimePoint.now();
        Map<String, Position> markPositions = new HashMap<String, Position>();
        markPositions.put("K Start (1)", new DegreePosition(54.497439439999994, 10.205943000000001));
        markPositions.put("K Start (2)", new DegreePosition(54.500209999999996, 10.20206472));
        markPositions.put("K Mark4 (2)", new DegreePosition(54.499422999999986, 10.200381692));
        markPositions.put("K Mark4 (1)", new DegreePosition(54.498954999999995, 10.200982));
        markPositions.put("K Mark1", new DegreePosition(54.489738990000006, 10.17079423000015));
        markPositions.put("K Finish (1)", new DegreePosition(54.48918199999999, 10.17003714));
        markPositions.put("K Finish (2)", new DegreePosition(54.48891756, 10.170632146666675));
        for (Waypoint w : race.getRace().getCourse().getWaypoints()) {
            for (Mark mark : w.getMarks()) {
                race.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), epoch));
                race.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()),
                        timePointForFixes));
                race.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), now));
            }
        }
    }

    protected RaceDefinition getRace() {
        return race;
    }
    
    protected void setRace(RaceDefinition race) {
        this.race = race;
    }

    protected DynamicTrackedRace getTrackedRace() {
        return trackedRace;
    }
    
    protected void setTrackedRace(DynamicTrackedRace race) {
        this.trackedRace = race;
    }

    /**
     * When all stored data has been transmitted, notify the semaphor for tests to start processing
     */
    @Override
    public void storedDataEnd() {
        super.storedDataEnd();
        synchronized (semaphor) {
            storedDataLoaded = true;
            semaphor.notifyAll();
        }
    }

    protected String getExpectedEventName() {
        return "Kieler Woche";
    }

    protected DomainFactoryImpl getDomainFactory() {
        return domainFactory;
    }

    protected Regatta getDomainEvent() {
        return domainEvent;
    }

    protected DynamicTrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    protected Object getSemaphor() {
        return semaphor;
    }

    protected boolean isStoredDataLoaded() {
        return storedDataLoaded;
    }
    
}
