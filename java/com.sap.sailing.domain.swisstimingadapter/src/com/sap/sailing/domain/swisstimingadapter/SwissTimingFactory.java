package com.sap.sailing.domain.swisstimingadapter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingFactoryImpl;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;

public interface SwissTimingFactory {
    SwissTimingFactory INSTANCE = new SwissTimingFactoryImpl();
    
    SwissTimingMessageParser createMessageParser();
    
    /**
     * Obtains a connector to one or more SwissTiming Sail Master system(s). Such a connector uses a host name and port
     * number to establish the connecting via TCP. The connector offers a number of explicit service request methods.
     * Additionally, the connector can receive "spontaneous" events sent by the sail master system. Clients can register
     * for those spontaneous events (see {@link SailMasterConnector#addSailMasterListener}).
     * <p>
     * 
     * When the connector is used with SailMaster instances hidden behind a "bridge" / firewall, no explicit requests
     * are possible, and the connector has to rely solely on the events it receives. It may, though, load recorded
     * race-specific messages through a {@link RaceSpecificMessageLoader} object. If a non-<code>null</code>
     * {@link RaceSpecificMessageLoader} is provided, the connector will fetch the {@link #getRaces() list of races}
     * from that loader. Additionally, the connector will use the loader upon each
     * {@link SailMasterConnector#trackRace(String)} to load all messages recorded by the loader for the race requested
     * so far.
     * <p>
     * 
     * Generally, the connector needs to be instructed for which races it shall handle events using calls to the
     * {@link SailMasterConnector#trackRace} and {@link SailMasterConnector#stopTrackingRace} operations.
     * {@link MessageType#isRaceSpecific() Race-specific messages} for other races are ignored and not forwarded to any
     * listener.
     * 
     * @param canSendRequests
     *            tells whether the hostname/port combination is able to receive and process SailMaster request messages;
     *            if <code>false</code>, only events are sent by the host to which the connector connects.
     */
    SailMasterConnector getOrCreateSailMasterConnector(String hostname, int port,
            RaceSpecificMessageLoader messageLoader, boolean canSendRequests) throws InterruptedException;

    SailMasterConnector getOrCreateSailMasterLiveSimulatorConnector(String host, int port, 
            RaceSpecificMessageLoader messageLoader, boolean canSendRequests) throws InterruptedException;

    SailMasterTransceiver createSailMasterTransceiver();

    SwissTimingConfiguration createSwissTimingConfiguration(String name, String hostname, int port, boolean canSendRequests);

    SwissTimingRaceTracker createRaceTracker(String raceID, String hostname, int port, boolean canSendRequests, long delayToLiveInMillis, WindStore windStore, RaceSpecificMessageLoader messageLoader, DomainFactory domainFactory, TrackedRegattaRegistry trackedRegattaRegistry, RaceLogStore raceLogStore) throws InterruptedException, UnknownHostException, IOException, ParseException;

    RaceTracker createRaceTracker(Regatta regatta, String raceID, String hostname, int port, boolean canSendRequests, long delayToLiveInMillis,
            WindStore windStore, RaceSpecificMessageLoader messageLoader, DomainFactory domainFactory,
            TrackedRegattaRegistry trackedRegattaRegistry, RaceLogStore raceLogStore) throws UnknownHostException, InterruptedException,
            IOException, ParseException;

    Race createRace(String raceId, String description, TimePoint startTime);

    SailMasterMessage createMessage(String message, Long sequenceNumber);

    SwissTimingArchiveConfiguration createSwissTimingArchiveConfiguration(String string);

}
