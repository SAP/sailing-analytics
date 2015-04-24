package com.sap.sailing.domain.swisstimingadapter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.xrr.schema.RegattaResults;

public interface SwissTimingAdapter {
    List<com.sap.sailing.domain.swisstimingadapter.RaceRecord> getSwissTimingRaceRecords(String hostname, int port) throws InterruptedException, UnknownHostException, IOException, ParseException;

    /**
     * @param regattaToAddTo
     *            if <code>null</code>, an existing regatta by the name of the TracTrac event with the boat class name
     *            appended in parentheses will be looked up; if not found, a default regatta with that name will be
     *            created, with a single default series and a single default fleet. If a valid {@link RegattaIdentifier}
     *            is specified, a regatta lookup is performed with that identifier; if the regatta is found, it is used
     *            to add the races to. Otherwise, a default regatta as described above will be created and used.
     * @param useInternalMarkPassingAlgorithm use our own instead of the SwissTiming-provided mark passing / split times
     */
    RaceHandle addSwissTimingRace(TrackerManager trackerManager, RegattaIdentifier regattaToAddTo, String raceID,
            String raceName, String raceDescription, BoatClass boatClass, String hostname, int port,
            StartList startList, RaceLogStore logStore, RegattaLogStore regattaLogStore, long timeoutInMilliseconds, boolean useInternalMarkPassingAlgorithm)
            throws InterruptedException, UnknownHostException, IOException, ParseException, Exception;

    StartList readStartListForRace(String raceId, RegattaResults regattaResults);
    
    RegattaResults readRegattaEntryListFromXrrUrl(String xrrEntryListUrl) throws IOException, JAXBException;
    
    SwissTimingFactory getSwissTimingFactory();

    com.sap.sailing.domain.swisstimingadapter.DomainFactory getSwissTimingDomainFactory();

}
