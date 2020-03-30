package com.sap.sailing.domain.tracking;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RegattaIdentifier;

public interface TrackerManager {

    /**
     * Creates a {@link RaceTracker} based on the {@code params}. If the {@code params} request
     * {@link RaceTrackingConnectivityParameters#isTrackWind() wind tracking}, a callback is
     * {@link RaceTracker#add(com.sap.sailing.domain.tracking.RaceTracker.RaceCreationListener) registered} with the
     * {@link RaceTracker} that, when the race has been created by the tracker, will start wind tracking.
     * 
     * @param regattaToAddTo
     *            if <code>null</code> or no regatta by that identifier is found, the regatta into which the race has
     *            previously been loaded will be looked up; if found, the race will be loaded into that regatta;
     *            otherwise, an existing default regatta will be looked up; if not found, a default regatta with that
     *            name will be created, with a single default series and a single default fleet. If a valid
     *            {@link RegattaIdentifier} is specified, a regatta lookup is performed with that identifier; if the
     *            regatta is found, it is used to add the races to, and
     *            {@link #setRegattaForRace(Regatta, RaceDefinition)} is called to remember the association
     *            persistently. Otherwise, a default regatta as described above will be created and used.
     * @param params
     *            describe the race to be loaded/added and how it is to be loaded
     * @param timeoutInMilliseconds
     *            if -1 then loading doesn't time out; otherwise, if the {@link RaceDefinition} hasn't been received
     *            after so many milliseconds then the tracker will be {@link RaceTracker#stop(boolean) stopped}.
     * @param raceTrackingHandler
     *            a mandatory service (must not be {@code null}) that is used to create objects during loading/adding
     *            the race, such as the {@link RaceDefinition}, {@link Boat} and {@link Competitor} objects, and finally
     *            the {@link TrackedRace} object. There is a default implementation available at
     *            {@link RaceTrackingHandler.DefaultRaceTrackingHandler} that simply creates the objects; a more
     *            specialized implementation exists that can handle the security-related aspects. See
     *            {@code PermissionAwareRaceTrackingHandler}.
     */
    RaceHandle addRace(RegattaIdentifier regattaToAddTo, RaceTrackingConnectivityParameters params, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler)
            throws MalformedURLException, FileNotFoundException, URISyntaxException, Exception;

    /**
     * Same as {@link #addRace(RegattaIdentifier, RaceTrackingConnectivityParameters, long, RaceTrackingHandler)} only that
     * the implementation can choose the {@link RaceTrackingHandler} according to its preferences.
     */
    RaceHandle addRace(RegattaIdentifier regattaToAddTo, RaceTrackingConnectivityParameters params, long timeoutInMilliseconds)
            throws MalformedURLException, FileNotFoundException, URISyntaxException, Exception;

}
