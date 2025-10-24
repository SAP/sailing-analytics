package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.tractrac.model.lib.api.ModelLocator;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.ISubscriberFactory;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;
import com.tractrac.subscription.lib.api.SubscriptionLocator;
import com.tractrac.util.lib.api.exceptions.TimeOutException;

/**
 * Subclassing tests have to call {@link #addListenersForStoredDataAndStartController(Iterable)} to kick off
 * the data receiving process. If subclasses use this class's default constructor, a connection to the
 * STG default account with a few test races is established. Subclasses may also choose to configure other
 * races / events using the {@link #AbstractTracTracLiveTest(URL, URI, URI)} constructor variant.
 * 
 * @author Axel Uhl (D043530)
 *
 */
@Timeout(value=5, unit=TimeUnit.MINUTES)
public abstract class AbstractTracTracLiveTest extends StoredTrackBasedTest {
    private static final Logger logger = Logger.getLogger(AbstractTracTracLiveTest.class.getName());
    protected static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    protected static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private IRace race;
    private IEventSubscriber eventSubscriber;
    private IRaceSubscriber raceSubscriber;
    private final Collection<Receiver> receivers;

    protected AbstractTracTracLiveTest() throws URISyntaxException, MalformedURLException {
        receivers = new HashSet<Receiver>();
    }

    /**
     * Default set-up for an STG training session in Weymouth, 2011
     */
    @BeforeEach
    public void setUp() throws Exception {
        final String eventID = "event_20110505_SailingTea";
        final String raceID = "bd8c778e-7c65-11e0-8236-406186cbf87c";
        setUp(getParamURL(eventID, raceID), /* liveURI */ null, /* storedURI */ null);
    }

    public static URI getStoredURI() throws URISyntaxException {
        return tractracTunnel ? new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_STORED) : new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_STORED);
    }

    public static URI getLiveURI() throws URISyntaxException {
        return tractracTunnel ? new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_LIVE) : new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_LIVE);
    }

    public static URL getParamURL(final String eventID, final String raceID) throws MalformedURLException {
        return new URL("http://" + TracTracConnectionConstants.HOST_NAME + "/events/"+eventID+"/"+raceID+".txt");
    }
    
    protected IEventSubscriber getEventSubscriber() {
        return eventSubscriber;
    }

    protected IRaceSubscriber getRaceSubscriber() {
        return raceSubscriber;
    }

    protected void setUp(URL paramUrl, URI liveUri, URI storedUri) throws FileNotFoundException, MalformedURLException, URISyntaxException, SubscriberInitializationException, CreateModelException {
        // Read event data from configuration file
        try {
            final IRace race = ModelLocator.getEventFactory().createRace(new URI(paramUrl.toString()), (int) /* timeout in milliseconds */ Duration.ONE_MINUTE.asMillis());
            this.race = race;
            logger.info("Using race "+race.getName()+" with ID "+race.getId()+" for this test");
            ISubscriberFactory subscriberFactory = SubscriptionLocator.getSusbcriberFactory();
            if (storedUri == null) {
                eventSubscriber = subscriberFactory.createEventSubscriber(race.getEvent());
                raceSubscriber = subscriberFactory.createRaceSubscriber(race);
            } else {
                eventSubscriber = DomainFactory.INSTANCE.getOrCreateEventSubscriber(race.getEvent(), liveUri, storedUri);
                raceSubscriber = subscriberFactory.createRaceSubscriber(race, liveUri, storedUri);
            }
            assertNotNull(race);
            // Initialize data controller using live and stored data sources
            if (storedUri != null && storedUri.toString().startsWith("file:")) {
                try {
                    URI oldStoredUri = storedUri;
                    storedUri = new URI(storedUri.toString().replaceFirst("file:/([^/])", "file:////$1"));
                    logger.info("Replaced storedUri "+oldStoredUri+" by "+storedUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (TimeOutException te) {
            throw new RuntimeException(te);
        }
        // test cases need to start the thread calling addListenersForStoredDataAndStartController
        // after adding their listeners
    }

    protected String getExpectedEventName() {
        return "Sailing Team Germany";
    }
    
    protected void addListenersForStoredDataAndStartController(Iterable<Receiver> receivers) {
        for (Receiver receiver : receivers) {
            receiver.subscribe();
        }
        getEventSubscriber().start();
        getRaceSubscriber().start();
    }
    
    @AfterEach
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        logger.info("entering "+getClass().getName()+".tearDown()");
        for (Receiver receiver : receivers) {
            receiver.stopPreemptively();
            logger.info("successfully stopped receiver "+receiver);
        }
        getEventSubscriber().stop();
        getRaceSubscriber().stop();
        logger.info("leaving "+getClass().getName()+".tearDown()");
    }
    
    protected IEvent getTracTracEvent() {
        return race.getEvent();
    }
    
    protected IRace getTracTracRace() {
        return race;
    }

    public static Iterable<Pair<IMapItem, PassingInstruction>> getTracTracControlPointsWithPassingInstructions(Iterable<IMapItem> controlPoints) {
        List<Pair<IMapItem, PassingInstruction>> ttControlPoints = new ArrayList<>();
        for (IMapItem cp : controlPoints) {
            ttControlPoints.add(new Pair<IMapItem, PassingInstruction>(cp, PassingInstruction.None));
        }
        return ttControlPoints;
    }
    
    public static URI getCourseDesignUpdateURI() throws URISyntaxException {
        return new URI("http://" + TracTracConnectionConstants.HOST_NAME + "/update_course");
    }

    public static String getTracTracUsername() {
        return "tracTest";
    }

    public static String getTracTracPassword() {
        return "tracTest";
    }
    
    protected void addReceiverToStopDuringTearDown(Receiver receiver) {
        receivers.add(receiver);
    }
}
