package com.sap.sailing.simulator.test.util;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.impl.TracTracAdapterFactoryImpl;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.simulator.impl.SimulatorUtils;

@SuppressWarnings("restriction")
public class TracTracWriter {

    private String[] sources;

    // Map<String, String> systemProperties;

    public TracTracWriter(String[] sources) {

        this.sources = sources;

    }

    public void write() throws Exception {
        RacingEventServiceImpl service = new RacingEventServiceImpl();
        final TracTracAdapterFactory tracTracAdapterFactory = new TracTracAdapterFactoryImpl();
        System.setProperty("mongo.port", "10200");
        System.setProperty("http.proxyHost", "proxy.wdf.sap.corp");
        System.setProperty("http.proxyPort", "8080");
        URI liveURI = new URI("tcp://10.18.22.156:1520");
        URI storedURI = new URI("tcp://10.18.22.156:1521");

        for (String paramURLStr : sources) {
            URL paramURL = new URL(paramURLStr);
            RacesHandle raceHandle = SimulatorUtils.loadRace(service, tracTracAdapterFactory, paramURL, liveURI, storedURI,
                    null, EmptyWindStore.INSTANCE, 60000);
            String regatta = raceHandle.getRegatta().getName();
            Set<RaceDefinition> races = raceHandle.getRaces();

            List<TrackedRace> racesList = new ArrayList<TrackedRace>();

            for (RaceDefinition r : races) {
                RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta, r.getName());
                TrackedRace tr = service.getExistingTrackedRace(raceIdentifier);
                tr.waitUntilNotLoading();

                racesList.add(tr);
            }
            System.out.println("start writing");
            // TODO: naming convention
            FileOutputStream f_os = new FileOutputStream(regatta + "race name" + ".data");
            ObjectOutputStream os = new ObjectOutputStream(f_os);
            os.writeObject(racesList);
            os.close();
            System.out.println("done writing");

        }

    }

}
