package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.SailMasterTransceiver;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessageParser;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingRaceTracker;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sse.common.Util;

public class SwissTimingFactoryImpl implements SwissTimingFactory {
    private static final Logger logger = Logger.getLogger(SwissTimingFactoryImpl.class.getName());
    
    private final Map<Util.Triple<String, Integer, String>, SailMasterConnector> connectors;

    public SwissTimingFactoryImpl() {
        connectors = new HashMap<>();
    }

    @Override
    public SwissTimingMessageParser createMessageParser() {
        return new SwissTimingMessageParserImpl();
    }

    @Override
    public SailMasterConnector getOrCreateSailMasterConnector(String host, int port, String raceId, URL raceDataUrl,
            String raceName, String raceDescription, BoatClass boatClass, SwissTimingRaceTrackerImpl swissTimingRaceTracker) throws InterruptedException, ParseException {
        if (Boolean.valueOf(System.getProperty("simulateLiveMode", "false"))) {
            return getOrCreateSailMasterLiveSimulatorConnector(host, port, raceId, raceName, raceDescription, boatClass, swissTimingRaceTracker);
        } else {
            Util.Triple<String, Integer, String> key = new Util.Triple<>(host, port, raceId);
            SailMasterConnector result = connectors.get(key);
            if (result == null || result.isStopped()) {
                if (result == null) {
                    logger.info("Creating a new connector for "+key+" because none found");
                } else {
                    logger.info("Creating a new connector for "+key+" because the old one was stopped");
                }
                if (host == null) {
                    if (raceDataUrl == null) {
                        throw new IllegalArgumentException("raceDataUrl must be provided if no host/port is provided");
                    }
                    result = new SailMasterConnectorForUrlDownload(raceId, raceDataUrl, raceName, raceDescription, boatClass, swissTimingRaceTracker);
                } else {
                    result = new SailMasterConnectorForSocket(host, port, raceId, raceName, raceDescription, boatClass, swissTimingRaceTracker);
                }
                connectors.put(key, result);
                // TODO how do connectors get stopped, terminated and removed from the connectors map again?
            } else {
                logger.info("Re-using connector for "+key+" because it wasn't stopped");
            }
            return result;
        }
    }

    @Override
    public SailMasterConnector getOrCreateSailMasterLiveSimulatorConnector(String host, int port, String raceId, String raceName,
            String raceDescription, BoatClass boatClass, SwissTimingRaceTrackerImpl swissTimingRaceTracker) throws InterruptedException, ParseException {
        Util.Triple<String, Integer, String> key = new Util.Triple<>(host, port, raceId);
        SailMasterConnector result = connectors.get(key);
        if (result == null || result.isStopped()) {
            result = new SailMasterLiveSimulatorConnectorImpl(host, port, raceId, raceName, raceDescription, boatClass, swissTimingRaceTracker);
            connectors.put(key, result);
            // TODO how do connectors get stopped, terminated and removed from the connectors map again?
        }
        return result;
    }

    @Override
    public SwissTimingConfiguration createSwissTimingConfiguration(String name, String jsonURL, String hostname,
            Integer port, String updateURL, String apiToken, String creatorName) {
        return new SwissTimingConfigurationImpl(name, jsonURL, hostname, port, updateURL, apiToken,
                creatorName);
    }

    @Override
    public SwissTimingRaceTracker createRaceTracker(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            WindStore windStore, DomainFactory domainFactory, TrackedRegattaRegistry trackedRegattaRegistry,
            RaceLogAndTrackedRaceResolver raceLogResolver, SwissTimingTrackingConnectivityParameters connectivityParams,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws InterruptedException, UnknownHostException, IOException, ParseException, URISyntaxException {
        return new SwissTimingRaceTrackerImpl(raceLogStore, regattaLogStore, windStore, domainFactory, this,
                trackedRegattaRegistry, raceLogResolver, connectivityParams, raceTrackingHandler, markPassingRaceFingerprintRegistry);
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, WindStore windStore, DomainFactory domainFactory,
            TrackedRegattaRegistry trackedRegattaRegistry, RaceLogAndTrackedRaceResolver raceLogResolver, RaceLogStore raceLogStore,
            RegattaLogStore regattaLogStore, SwissTimingTrackingConnectivityParameters connectivityParams,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws UnknownHostException, InterruptedException, IOException, ParseException, URISyntaxException {
        return new SwissTimingRaceTrackerImpl(regatta, windStore, domainFactory, this, trackedRegattaRegistry,
                raceLogStore, regattaLogStore, raceLogResolver, connectivityParams, raceTrackingHandler, markPassingRaceFingerprintRegistry);
    }

    @Override
    public SailMasterTransceiver createSailMasterTransceiver() {
        return new SailMasterTransceiverImpl();
    }

    @Override
    public SailMasterMessage createMessage(String message) {
        return new SailMasterMessageImpl(message);
    }

    @Override
    public Race createRace(String raceId, String raceName, String description, BoatClass boatClass) {
        return new RaceImpl(raceId, raceName, description, boatClass);
    }

    @Override
    public SwissTimingArchiveConfiguration createSwissTimingArchiveConfiguration(String jsonUrl, String creatorName) {
        return new SwissTimingArchiveConfigurationImpl(jsonUrl, creatorName);
    }
}
