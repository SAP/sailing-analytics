package com.sap.sailing.domain.tracking;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;

public interface TrackerManager {

    /**
     * @param regattaToAddTo
     *            if <code>null</code> or no regatta by that identifier is found, an existing regatta by the name of the
     *            TracTrac event with the boat class name appended in parentheses will be looked up; if not found, a
     *            default regatta with that name will be created, with a single default series and a single default
     *            fleet. If a valid {@link RegattaIdentifier} is specified, a regatta lookup is performed with that
     *            identifier; if the regatta is found, it is used to add the races to, and
     *            {@link #setRegattaForRace(Regatta, RaceDefinition)} is called to remember the association
     *            persistently. Otherwise, a default regatta as described above will be created and used.
     * @param windStore
     *            must not be <code>null</code>, but can, e.g., be an {@link EmptyWindStore}
     */
    RacesHandle addRace(RegattaIdentifier regattaToAddTo, RaceTrackingConnectivityParameters params, WindStore windStore, long timeoutInMilliseconds)
            throws MalformedURLException, FileNotFoundException, URISyntaxException, Exception;

}
