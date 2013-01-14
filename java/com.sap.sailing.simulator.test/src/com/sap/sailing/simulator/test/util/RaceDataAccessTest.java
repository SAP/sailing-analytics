package com.sap.sailing.simulator.test.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;

public class RaceDataAccessTest {

    private static final Logger logger = Logger.getLogger(RaceDataAccessTest.class.getName());
    protected static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    protected static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private static URL paramUrl;
    private static URI liveUri;
    private static URI storedUri;
    private static RacingEventServiceImpl service;
    private static RacesHandle raceHandle;
    public boolean storedend = false;

    public static void main(String[] args) throws Exception, InterruptedException, MalformedURLException, URISyntaxException {

        // JSON URL: http://germanmaster.traclive.dk/events/event_20110929_Internatio/jsonservice.php

        // 49er Race2
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=5b7a34f6-ec44-11e0-a523-406186cbf87c");

        // 49er Race3
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=be8a79a8-ec52-11e0-a523-406186cbf87c");

        // 49er Race4
        // paramUrl = new URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c");

        // 49er Race5
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=eb06795a-ec52-11e0-a523-406186cbf87c");

        // Star Race4
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=6bb0829e-ec44-11e0-a523-406186cbf87c");

        // 49er Race6
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=fceb3390-ec52-11e0-a523-406186cbf87c");

        // Star Race5
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=16fd961a-ec53-11e0-a523-406186cbf87c");

        // Laser Medal
        // paramUrl = new
        // URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=93ef1a40-ed25-11e0-a523-406186cbf87c");

        //
        // Kieler Woche 2012, JSON: http://germanmaster.traclive.dk/events/event_20120615_KielerWoch/jsonservice.php
        //
        // 49er 
        paramUrl = new URL("http://germanmaster.traclive.dk/events/event_20120615_KielerWoch/clientparams.php?event=event_20120615_KielerWoch&race=0b5969cc-b789-11e1-a845-406186cbf87c");
        
        liveUri = new URI("tcp://10.18.206.73:1520");
        storedUri = new URI("tcp://10.18.206.73:1521");

        System.setProperty("mongo.port", "10200");
        System.setProperty("http.proxyHost", "proxy.wdf.sap.corp");
        System.setProperty("http.proxyPort", "8080");
       // paramUrl = new URL("http://germanmaster.traclive.dk/events/event_20120615_KielerWoch/clientparams.php?event=event_20120615_KielerWoch&race=0b5969cc-b789-11e1-a845-406186cbf87c");
        paramUrl = new URL("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c");
        //old
        //liveUri = new URI("tcp://10.18.206.73:1520"); 
        //proxy
        liveUri = new URI("tcp://10.18.22.156:1520");
        //non-proxy
        //liveUri =  new URI("tcp://germanmaster.traclive.dk:4400");
        
        //old
        //storedUri = new URI("tcp://10.18.206.73:1521");
        //proxy
        storedUri = new URI("tcp://10.18.22.156:1521");
        //non-proxy
        //storedUri = new URI("tcp://germanmaster.traclive.dk:4401");
        
        service = new RacingEventServiceImpl();
        logger.info("Calling service.addTracTracRace");
        Object obj = new Object();
        raceHandle = service.addTracTracRace(paramUrl, liveUri, storedUri, EmptyWindStore.INSTANCE, /* timeoutInMilliseconds */60000, obj);
        logger.info("Calling raceHandle.getRaces(): " + raceHandle);

        synchronized (obj) {
            obj.wait();
        }

        Set<RaceDefinition> races = raceHandle.getRaces(); // wait for RaceDefinition to be completely wired in Regatta
        logger.info("Obtained races: " + races.size());

        String regatta = raceHandle.getRegatta().getName();

        Iterator<RaceDefinition> racIter = races.iterator();
        while (racIter.hasNext()) {
            RaceDefinition race = racIter.next();
            System.out.println("Race: \"" + race.getName() + "\", \"" + regatta + "\"");

            RaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta, race.getName());
            TrackedRace tr = service.getExistingTrackedRace(raceIdentifier);

            System.out.println("Competitors:");
            Iterable<Competitor> competitors = tr.getRace().getCompetitors();
            System.out.println("" + competitors);
            Iterator<Competitor> comIter = competitors.iterator();
            while (comIter.hasNext()) {
                Competitor com = comIter.next();
                GPSFixTrack<Competitor, GPSFixMoving> track = null;
                Iterable<GPSFixMoving> fixes = null;
                track = tr.getTrack(com);

                track.lockForRead();
                fixes = track.getFixes();
                GPSFixMoving fix = fixes.iterator().next();
                System.out.println("" + com.getName() + ", First GPS-Fix: " + fix.getPosition().getLatDeg() + ", " + fix.getPosition().getLngDeg());
                track.unlockAfterRead();

            }

        }

    }

}
