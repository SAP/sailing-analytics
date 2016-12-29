package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.impl.CompetitorWithID;
import com.sap.sailing.domain.swisstimingadapter.impl.CompetitorWithoutID;
import com.sap.sailing.domain.swisstimingadapter.impl.CrewMemberImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.StartListImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingAdapterFactoryImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sse.common.Util;

public class SwissTimingConnectivityParamsLoadAndStoreTest extends AbstractConnectivityParamsLoadAndStoreTest {
    public SwissTimingConnectivityParamsLoadAndStoreTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testStoreAndLoadSwissTimingLiveParamsWithCompetitorsWithIDAndCrew() throws MalformedURLException, URISyntaxException {
        final String hostname = "a.b.com";
        final int port = 1234;
        final String raceID = "Race ID 123";
        final String raceName = "The Race";
        final String raceDescription = null;
        final BoatClass boatClass = domainObjectFactory.getBaseDomainFactory().getOrCreateBoatClass("49er");
        final CrewMemberImpl c1c1 = new CrewMemberImpl("c1c1", "GER", "Helm");
        final CrewMemberImpl c1c2 = new CrewMemberImpl("c1c2", "POL", "Crew");
        final CrewMemberImpl c2c1 = new CrewMemberImpl("c2c1", "AUS", "Helm");
        final CrewMemberImpl c2c2 = new CrewMemberImpl("c2c2", "NZL", "Crew");
        final CompetitorWithID c1 = new CompetitorWithID("c1", "b1", "GER", "C1", Arrays.asList(c1c1, c1c2));
        final CompetitorWithID c2 = new CompetitorWithID("c2", "b2", "AUS", "C2", Arrays.asList(c2c1, c2c2));
        final StartList startList = new StartListImpl(raceID, Arrays.asList(c1, c2));
        final boolean useInternalMarkPassingAlgorithm = false;
        final long delayToLiveInMillis = 30000;
        final SwissTimingTrackingConnectivityParameters stParams = new SwissTimingTrackingConnectivityParameters(
                hostname, port, raceID, raceName, raceDescription, boatClass, startList, delayToLiveInMillis,
                SwissTimingFactory.INSTANCE, new SwissTimingAdapterFactoryImpl().getOrCreateSwissTimingAdapter(domainObjectFactory.getBaseDomainFactory()).getSwissTimingDomainFactory(),
                /* raceLogStore */ null, /* regattaLogStore */ null, useInternalMarkPassingAlgorithm);
        // store
        mongoObjectFactory.addConnectivityParametersForRaceToRestore(stParams);
        // load
        final Iterable<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore = domainObjectFactory.loadConnectivityParametersForRacesToRestore();
        // compare
        assertEquals(1, Util.size(connectivityParametersForRacesToRestore));
        final RaceTrackingConnectivityParameters paramsReadFromDB = connectivityParametersForRacesToRestore.iterator().next();
        assertTrue(paramsReadFromDB instanceof SwissTimingTrackingConnectivityParameters);
        SwissTimingTrackingConnectivityParameters stParamsReadFromDB = (SwissTimingTrackingConnectivityParameters) paramsReadFromDB;
        assertEquals(delayToLiveInMillis, stParamsReadFromDB.getDelayToLiveInMillis());
        assertEquals(hostname, stParamsReadFromDB.getHostname());
        assertEquals(boatClass, stParamsReadFromDB.getBoatClass());
        assertEquals(port, stParamsReadFromDB.getPort());
        assertEquals(raceDescription, stParamsReadFromDB.getRaceDescription());
        assertEquals(raceID, stParamsReadFromDB.getRaceID());
        assertEquals(raceName, stParamsReadFromDB.getRaceName());
        assertEquals(startList.getRaceID(), stParamsReadFromDB.getStartList().getRaceID());
        assertEquals(startList.getCompetitors(), stParamsReadFromDB.getStartList().getCompetitors());
        assertEquals(stParams.getTrackerID(), stParamsReadFromDB.getTrackerID());
    }
    
    @Test
    public void testStoreAndLoadSwissTimingLiveParamsWithCompetitorsWithoutID() throws MalformedURLException, URISyntaxException {
        final String hostname = "a.b.com";
        final int port = 1234;
        final String raceID = "Race ID 123";
        final String raceName = "The Race";
        final String raceDescription = null;
        final BoatClass boatClass = domainObjectFactory.getBaseDomainFactory().getOrCreateBoatClass("49er");
        final CompetitorWithoutID c1 = new CompetitorWithoutID("b1", "GER", "C1");
        final CompetitorWithoutID c2 = new CompetitorWithoutID("b2", "GER", "21");
        final StartList startList = new StartListImpl(raceID, Arrays.asList(c1, c2));
        final boolean useInternalMarkPassingAlgorithm = false;
        final long delayToLiveInMillis = 30000;
        final SwissTimingTrackingConnectivityParameters stParams = new SwissTimingTrackingConnectivityParameters(
                hostname, port, raceID, raceName, raceDescription, boatClass, startList, delayToLiveInMillis,
                SwissTimingFactory.INSTANCE, new SwissTimingAdapterFactoryImpl().getOrCreateSwissTimingAdapter(domainObjectFactory.getBaseDomainFactory()).getSwissTimingDomainFactory(),
                /* raceLogStore */ null, /* regattaLogStore */ null, useInternalMarkPassingAlgorithm);
        // store
        mongoObjectFactory.addConnectivityParametersForRaceToRestore(stParams);
        // load
        final Iterable<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore = domainObjectFactory.loadConnectivityParametersForRacesToRestore();
        // compare
        assertEquals(1, Util.size(connectivityParametersForRacesToRestore));
        final RaceTrackingConnectivityParameters paramsReadFromDB = connectivityParametersForRacesToRestore.iterator().next();
        assertTrue(paramsReadFromDB instanceof SwissTimingTrackingConnectivityParameters);
        SwissTimingTrackingConnectivityParameters stParamsReadFromDB = (SwissTimingTrackingConnectivityParameters) paramsReadFromDB;
        assertEquals(delayToLiveInMillis, stParamsReadFromDB.getDelayToLiveInMillis());
        assertEquals(hostname, stParamsReadFromDB.getHostname());
        assertEquals(boatClass, stParamsReadFromDB.getBoatClass());
        assertEquals(port, stParamsReadFromDB.getPort());
        assertEquals(raceDescription, stParamsReadFromDB.getRaceDescription());
        assertEquals(raceID, stParamsReadFromDB.getRaceID());
        assertEquals(raceName, stParamsReadFromDB.getRaceName());
        assertEquals(startList.getRaceID(), stParamsReadFromDB.getStartList().getRaceID());
        assertEquals(startList.getCompetitors(), stParamsReadFromDB.getStartList().getCompetitors());
        assertEquals(stParams.getTrackerID(), stParamsReadFromDB.getTrackerID());
    }
}
