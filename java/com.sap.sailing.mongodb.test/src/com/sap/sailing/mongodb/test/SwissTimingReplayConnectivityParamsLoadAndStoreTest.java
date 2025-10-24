package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingAdapter;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingAdapterFactoryImpl;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayConnectivityParameters;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayServiceFactoryImpl;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sse.common.Util;

public class SwissTimingReplayConnectivityParamsLoadAndStoreTest extends AbstractConnectivityParamsLoadAndStoreTest {
    public SwissTimingReplayConnectivityParamsLoadAndStoreTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testStoreAndLoadSwissTimingReplayParams() throws MalformedURLException, URISyntaxException, InterruptedException, ExecutionException {
        final String link = "a.b.com?race=1234&_start=0";
        final String raceID = "Race ID 123";
        final String raceName = "The Race";
        final String boatClassName = "49er";
        final String swissTimingUrl = "https://static.sapsailing.com/OSG2016/OSG2016_static.json";
        final boolean useInternalMarkPassingAlgorithm = false;
        final SwissTimingAdapter swissTimingAdapter = new SwissTimingAdapterFactoryImpl().getOrCreateSwissTimingAdapter(domainObjectFactory.getBaseDomainFactory());
        final SwissTimingReplayConnectivityParameters stParams = new SwissTimingReplayConnectivityParameters(
                link, swissTimingUrl, raceName, raceID, boatClassName, useInternalMarkPassingAlgorithm,
                swissTimingAdapter.getSwissTimingDomainFactory(),
                new SwissTimingReplayServiceFactoryImpl().createSwissTimingReplayService(swissTimingAdapter.getSwissTimingDomainFactory(), racingEventService),
                /* raceLogStore */ null, /* regattaLogStore */ null);
        // store
        mongoObjectFactory.addConnectivityParametersForRaceToRestore(stParams);
        // load
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params->connectivityParametersForRacesToRestore.add(params))
            .waitForCompletionOfCallbacksForAllParameters();
        // compare
        assertEquals(1, Util.size(connectivityParametersForRacesToRestore));
        final RaceTrackingConnectivityParameters paramsReadFromDB = connectivityParametersForRacesToRestore.iterator().next();
        assertTrue(paramsReadFromDB instanceof SwissTimingReplayConnectivityParameters);
        SwissTimingReplayConnectivityParameters stParamsReadFromDB = (SwissTimingReplayConnectivityParameters) paramsReadFromDB;
        assertEquals(link, stParamsReadFromDB.getLink());
        assertEquals(swissTimingUrl, stParams.getSwissTimingUrl());
        assertEquals(boatClassName, stParamsReadFromDB.getBoatClassName());
        assertEquals(raceID, stParamsReadFromDB.getRaceID());
        assertEquals(raceName, stParamsReadFromDB.getRaceName());
        assertEquals(stParams.getTrackerID(), stParamsReadFromDB.getTrackerID());
        assertEquals(useInternalMarkPassingAlgorithm, stParamsReadFromDB.isUseInternalMarkPassingAlgorithm());
        // remove again
        mongoObjectFactory.removeConnectivityParametersForRaceToRestore(stParams);
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore2 = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params->connectivityParametersForRacesToRestore2.add(params))
            .waitForCompletionOfCallbacksForAllParameters();
        assertTrue(connectivityParametersForRacesToRestore2.isEmpty());
    }
}
