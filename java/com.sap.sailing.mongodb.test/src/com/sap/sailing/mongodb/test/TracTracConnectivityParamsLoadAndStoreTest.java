package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.impl.RaceTrackingConnectivityParametersImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TracTracConnectivityParamsLoadAndStoreTest extends AbstractConnectivityParamsLoadAndStoreTest {
    public TracTracConnectivityParamsLoadAndStoreTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testStoreAndLoadSimpleTracTracParams() throws Exception {
        // set up
        final boolean trackWind = true;
        final boolean correctWindDirectionByMagneticDeclination = true;
        final URL paramURL = new URL("http://event.tractrac.com/events/event_20160604_JuniorenSe/clientparams.php?event=event_20160604_JuniorenSe&race=4b9f0190-0b0d-0134-5b24-60a44ce903c3");
        final URI storedURI = new URI("http://event.tractrac.com/events/event_20160604_JuniorenSe/datafiles/4b9f0190-0b0d-0134-5b24-60a44ce903c3.mtb");
        final URI courseDesignUpdateURI = new URI("https://event2.tractrac.com/reverse/update");
        final TimePoint startOfTracking = MillisecondsTimePoint.now();
        final TimePoint endOfTracking = startOfTracking.plus(1000);
        final long delayToLiveInMillis = 3000;
        final Duration offsetToStartTimeOfSimulatedRace = Duration.ONE_MINUTE;
        final boolean useInternalMarkPassingAlgorithm = false;
        final String tracTracUsername = "user";
        final String tracTracPassword = "pass";
        final String raceStatus = (String) TracTracConnectionConstants.REPLAY_STATUS;
        final String raceVisibility = (String) TracTracConnectionConstants.REPLAY_VISIBILITY;
        final RaceTrackingConnectivityParameters tracTracParams = new RaceTrackingConnectivityParametersImpl(
                paramURL, /* live URI */ null, storedURI, courseDesignUpdateURI, startOfTracking, endOfTracking,
                delayToLiveInMillis, offsetToStartTimeOfSimulatedRace, useInternalMarkPassingAlgorithm,
                /* raceLogStore */ null, /* regattaLogStore */ null, DomainFactory.INSTANCE, tracTracUsername, tracTracPassword,
                raceStatus, raceVisibility, trackWind, correctWindDirectionByMagneticDeclination, /* preferReplayIfAvailable */ false,
                /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, /* useOfficialEventsToUpdateRaceLog */ true,
                /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null);
        // store
        mongoObjectFactory.addConnectivityParametersForRaceToRestore(tracTracParams);
        // load
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params->connectivityParametersForRacesToRestore.add(params))
            .waitForCompletionOfCallbacksForAllParameters();
        // compare
        assertEquals(1, Util.size(connectivityParametersForRacesToRestore));
        final RaceTrackingConnectivityParameters paramsReadFromDB = connectivityParametersForRacesToRestore.iterator().next();
        assertTrue(paramsReadFromDB instanceof RaceTrackingConnectivityParametersImpl);
        RaceTrackingConnectivityParametersImpl tracTracParamsReadFromDB = (RaceTrackingConnectivityParametersImpl) paramsReadFromDB;
        assertEquals(delayToLiveInMillis, tracTracParamsReadFromDB.getDelayToLiveInMillis());
        assertEquals(paramURL, tracTracParamsReadFromDB.getParamURL());
        assertEquals(storedURI, tracTracParamsReadFromDB.getStoredURI());
        assertEquals(courseDesignUpdateURI, tracTracParamsReadFromDB.getUpdateURI());
        assertEquals(startOfTracking, tracTracParamsReadFromDB.getStartOfTracking());
        assertEquals(endOfTracking, tracTracParamsReadFromDB.getEndOfTracking());
        assertEquals(offsetToStartTimeOfSimulatedRace, tracTracParamsReadFromDB.getOffsetToStartTimeOfSimulatedRace());
        assertEquals(useInternalMarkPassingAlgorithm, tracTracParamsReadFromDB.isUseInternalMarkPassingAlgorithm());
        assertEquals(tracTracUsername, tracTracParamsReadFromDB.getTracTracUsername());
        assertEquals(tracTracPassword, tracTracParamsReadFromDB.getTracTracPassword());
        assertEquals(raceStatus, tracTracParamsReadFromDB.getRaceStatus());
        assertEquals(raceVisibility, tracTracParamsReadFromDB.getRaceVisibility());
        assertEquals(tracTracParams.getTrackerID(), tracTracParamsReadFromDB.getTrackerID());
        assertEquals(tracTracParams.isTrackWind(), tracTracParamsReadFromDB.isTrackWind());
        assertEquals(tracTracParams.isCorrectWindDirectionByMagneticDeclination(), tracTracParamsReadFromDB.isCorrectWindDirectionByMagneticDeclination());
        assertEquals(((RaceTrackingConnectivityParametersImpl) tracTracParams).isUseOfficialEventsToUpdateRaceLog(), tracTracParamsReadFromDB.isUseOfficialEventsToUpdateRaceLog());
        // remove again
        mongoObjectFactory.removeConnectivityParametersForRaceToRestore(tracTracParams);
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore2 = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params->connectivityParametersForRacesToRestore2.add(params))
            .waitForCompletionOfCallbacksForAllParameters();
        assertTrue(connectivityParametersForRacesToRestore2.isEmpty());
    }
}
