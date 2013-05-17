package com.sap.sailing.simulator.test.analysis;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.simulator.impl.SimulatorUtils;

@SuppressWarnings("restriction")
public class TestRacingEventPersistence {

    RacingEventServiceImpl service;
    private RacesHandle raceHandle;
    String paramURLStr = "http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c";
    // proxy
    String liveURIStr = "tcp://10.18.22.156:1520";
    String storedURIStr = "tcp://10.18.22.156:1521";

    // non-proxy
    // String liveURIStr = "tcp://germanmaster.traclive.dk:4400";
    // String storedURIStr = "tcp://germanmaster.traclive.dk:4401";

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {

        System.setProperty("mongo.port", "10200");
        System.setProperty("http.proxyHost", "proxy.wdf.sap.corp");
        System.setProperty("http.proxyPort", "8080");

        service = new RacingEventServiceImpl();
        URL paramURL = new URL(paramURLStr);
        URI liveURI = new URI(liveURIStr);
        URI storedURI = new URI(storedURIStr);

        raceHandle = SimulatorUtils.loadRace(service, paramURL, liveURI, storedURI, null, EmptyWindStore.INSTANCE,
                60000);

        System.out.println("Done loading race.");

        String regatta = raceHandle.getRegatta().getName();
        Set<RaceDefinition> races = raceHandle.getRaces();

        List<TrackedRace> racesList = new ArrayList<TrackedRace>();

        for (RaceDefinition r : races) {
            RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta, r.getName());
            TrackedRace tr = service.getExistingTrackedRace(raceIdentifier);
            tr.waitUntilNotLoading();

            racesList.add(tr);
        }

        FileOutputStream f_os = new FileOutputStream(regatta + ".data");
        ObjectOutputStream os = new ObjectOutputStream(f_os);
        os.writeObject(racesList);
        os.close();

        List<TrackedRace> retrievedRacesList = null;
        FileInputStream f_in = new FileInputStream(regatta + ".data");
        ObjectInputStream obj_in = new ObjectInputStream(f_in);
        retrievedRacesList = (ArrayList<TrackedRace>) obj_in.readObject();
        obj_in.close();

        for (TrackedRace r : retrievedRacesList) {
            System.out.println(r.getRaceIdentifier().getRegattaName());
            Iterable<Competitor> competitors = r.getRace().getCompetitors();
            System.out.println("" + competitors);
            Iterator<Competitor> comIter = competitors.iterator();
            while (comIter.hasNext()) {
                Competitor com = comIter.next();
                GPSFixTrack<Competitor, GPSFixMoving> track = null;
                Iterable<GPSFixMoving> fixes = null;
                track = r.getTrack(com);
                track.lockForRead();
                fixes = track.getFixes();
                GPSFixMoving fix = fixes.iterator().next();
                System.out.println("" + com.getName() + ", First GPS-Fix: " + fix.getPosition().getLatDeg() + ", "
                        + fix.getPosition().getLngDeg());
                track.unlockAfterRead();
            }

        }
    }
}
