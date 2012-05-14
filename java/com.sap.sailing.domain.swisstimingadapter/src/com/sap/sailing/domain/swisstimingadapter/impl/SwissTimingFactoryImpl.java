package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceSpecificMessageLoader;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.SailMasterTransceiver;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessageParser;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingRaceTracker;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;

public class SwissTimingFactoryImpl implements SwissTimingFactory {
    private final Map<Triple<String, Integer, RaceSpecificMessageLoader>, SailMasterConnector> connectors;
    
    public SwissTimingFactoryImpl() {
        connectors = new HashMap<Triple<String, Integer, RaceSpecificMessageLoader>, SailMasterConnector>();
    }

    @Override
    public SwissTimingMessageParser createMessageParser() {
        return new SwissTimingMessageParserImpl();
    }

    @Override
    public SailMasterConnector getOrCreateSailMasterConnector(String host, int port, RaceSpecificMessageLoader messageLoader, 
            boolean canSendRequests) throws InterruptedException {
        if (Boolean.valueOf(System.getProperty("simulateLiveMode", "false"))) {
            return getOrCreateSailMasterLiveSimulatorConnector(host, port, messageLoader, canSendRequests);
        } else {
            Triple<String, Integer, RaceSpecificMessageLoader> key = new Triple<String, Integer, RaceSpecificMessageLoader>(
                    host, port, messageLoader);
            SailMasterConnector result = connectors.get(key);
            if (result == null) {
                result = new SailMasterConnectorImpl(host, port, messageLoader, canSendRequests);
                connectors.put(key, result);
                // TODO how do connectors get stopped, terminated and removed from the connectors map again?
            } else if (result.isStopped()) {
                result = new SailMasterConnectorImpl(host, port, messageLoader, canSendRequests);
                connectors.put(key, result);
            }
            return result;
        }
    }

    @Override
    public SailMasterConnector getOrCreateSailMasterLiveSimulatorConnector(String host, int port, RaceSpecificMessageLoader messageLoader, boolean canSendRequests) throws InterruptedException {
        Triple<String, Integer, RaceSpecificMessageLoader> key = new Triple<String, Integer, RaceSpecificMessageLoader>(host, port, messageLoader);
        SailMasterConnector result = connectors.get(key);
        if (result == null) {
            result = new SailMasterLiveSimulatorConnectorImpl(host, port, messageLoader, canSendRequests);
            connectors.put(key, result);
            // TODO how do connectors get stopped, terminated and removed from the connectors map again?
        } else if (result.isStopped()) {
            result = new SailMasterLiveSimulatorConnectorImpl(host, port, messageLoader, canSendRequests);
            connectors.put(key, result);
        }
        return result;
    }

    @Override
    public SwissTimingConfiguration createSwissTimingConfiguration(String name, String hostname, int port, boolean canSendRequests) {
        return new SwissTimingConfigurationImpl(name, hostname, port, canSendRequests);
    }

    @Override
    public SwissTimingRaceTracker createRaceTracker(String raceID, String hostname, int port, boolean canSendRequests,
            WindStore windStore, RaceSpecificMessageLoader messageLoader, DomainFactory domainFactory,
            TrackedRegattaRegistry trackedEventRegistry) throws InterruptedException, UnknownHostException, IOException,
            ParseException {
        return new SwissTimingRaceTrackerImpl(raceID, hostname, port, windStore, domainFactory, this, messageLoader,
                trackedEventRegistry, canSendRequests);
    }

    @Override
    public SailMasterTransceiver createSailMasterTransceiver() {
        return new SailMasterTransceiverImpl();
    }

    @Override
    public SailMasterMessage createMessage(String message, Long sequenceNumber) {
        return new SailMasterMessageImpl(message, sequenceNumber);
    }
    
    @Override
    public Race createRace(String raceId, String description, TimePoint startTime) {
    	return new RaceImpl(raceId, description, startTime);
    }

    
}
