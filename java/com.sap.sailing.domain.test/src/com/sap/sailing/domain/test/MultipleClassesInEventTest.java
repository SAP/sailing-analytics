package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;

public class MultipleClassesInEventTest {
    private static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    private static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private DomainFactory domainFactory;
    private TracTracRaceTracker kiwotest1;
    private TracTracRaceTracker kiwotest2;
    private TracTracRaceTracker kiwotest3;
    private TracTracRaceTracker weym470may112014_2;
    
    @Before
    public void setUp() {
        domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl());
    }
    
    @Test
    public void testLoadTwoRacesWithEqualEventNameButDifferentClasses() throws MalformedURLException, FileNotFoundException, URISyntaxException {
        String httpAndHost = "http://" + TracTracConnectionConstants.HOST_NAME;
        String liveURI = "tcp://" + TracTracConnectionConstants.HOST_NAME + ":4400";
        String storedURI = "tcp://" + TracTracConnectionConstants.HOST_NAME + ":4401";
        if (tractracTunnel) {
            liveURI   = "tcp://"+tractracTunnelHost+":4412";
            storedURI = "tcp://"+tractracTunnelHost+":4413";
        }
        kiwotest1 = domainFactory
                .createRaceTracker(
                        new URL(
                                httpAndHost+"/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race=cce678c8-97e6-11e0-9aed-406186cbf87c"),
                        new URI(liveURI), new URI(storedURI),
                        EmptyWindStore.INSTANCE, new DummyTrackedEventRegistry());
        kiwotest2 = domainFactory
                .createRaceTracker(
                        new URL(
                                httpAndHost+"/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race=11290bd6-97e7-11e0-9aed-406186cbf87c"),
                        new URI(liveURI), new URI(storedURI),
                        EmptyWindStore.INSTANCE, new DummyTrackedEventRegistry());
        kiwotest3 = domainFactory
                .createRaceTracker(
                        new URL(
                                httpAndHost+"/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race=39635b24-97e7-11e0-9aed-406186cbf87c"),
                        new URI(liveURI), new URI(storedURI),
                        EmptyWindStore.INSTANCE, new DummyTrackedEventRegistry());
        weym470may112014_2 = domainFactory
                .createRaceTracker(
                        new URL(
                                httpAndHost+"/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race=04498426-7dfd-11e0-8236-406186cbf87c"),
                        new URI(liveURI), new URI(storedURI),
                        EmptyWindStore.INSTANCE, new DummyTrackedEventRegistry());
        
        assertEquals("STG", kiwotest1.getEvent().getBoatClass().getName());
        assertEquals("505", kiwotest2.getEvent().getBoatClass().getName());
        assertEquals("49er", kiwotest3.getEvent().getBoatClass().getName());
        assertEquals("STG", weym470may112014_2.getEvent().getBoatClass().getName());
        assertSame(weym470may112014_2.getEvent(), kiwotest1.getEvent());
        assertNotSame(kiwotest1.getEvent(), kiwotest2.getEvent());
        assertNotSame(kiwotest1.getEvent(), kiwotest3.getEvent());
        assertNotSame(kiwotest2.getEvent(), kiwotest3.getEvent());
    }
    
    @After
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        kiwotest1.stop();
        kiwotest2.stop();
        kiwotest3.stop();
        weym470may112014_2.stop();
    }
}
