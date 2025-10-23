package com.sap.sailing.domain.swisstimingadapter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingFactoryImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingRaceTrackerImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
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
     * {@link RaceSpecificMessageLoader} is provided, the connector will fetch the {@link #getRace() race}
     * from that loader. Additionally, the connector will use the loader upon each
     * {@link SailMasterConnector#trackRace(String)} to load all messages recorded by the loader for the race requested
     * so far.
     * <p>
     * 
     * Generally, the connector needs to be instructed for which races it shall handle events using calls to the
     * {@link SailMasterConnector#trackRace} and {@link SailMasterConnector#stopTrackingRace} operations.
     * {@link MessageType#isRaceSpecific() Race-specific messages} for other races are ignored and not forwarded to any
     * listener.<p>
     * 
     * When the {@code hostname} is {@code null}, this is assumed to request a URL-based connection, and {@code raceDataUrl} has
     * to provide a valid URL leading to a downloadable log file.
     */
    SailMasterConnector getOrCreateSailMasterConnector(String hostname, int port, String raceId, URL raceDataUrl,
            String raceName, String raceDescription, BoatClass boatClass, SwissTimingRaceTrackerImpl swissTimingRaceTracker)
            throws InterruptedException, ParseException;

    SailMasterConnector getOrCreateSailMasterLiveSimulatorConnector(String host, int port, String raceId, String raceName,
            String raceDescription, BoatClass boatClass, SwissTimingRaceTrackerImpl swissTimingRaceTracker) throws InterruptedException, ParseException;

    SailMasterTransceiver createSailMasterTransceiver();

    SwissTimingConfiguration createSwissTimingConfiguration(String name, String jsonURL, String hostname, Integer port,
            String updateURL, String apiToken, String creatorName);
    
    SwissTimingRaceTracker createRaceTracker(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, WindStore windStore,
            DomainFactory domainFactory, TrackedRegattaRegistry trackedRegattaRegistry, RaceLogAndTrackedRaceResolver raceLogResolver, SwissTimingTrackingConnectivityParameters connectivityParams,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws InterruptedException, UnknownHostException, IOException, ParseException, URISyntaxException;

    RaceTracker createRaceTracker(Regatta regatta, WindStore windStore, DomainFactory domainFactory, TrackedRegattaRegistry trackedRegattaRegistry,
            RaceLogAndTrackedRaceResolver raceLogResolver, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, SwissTimingTrackingConnectivityParameters connectivityParams,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws UnknownHostException, InterruptedException, IOException, ParseException, URISyntaxException;

    Race createRace(String raceId, String raceName, String description, BoatClass boatClass);

    SailMasterMessage createMessage(String message);

    SwissTimingArchiveConfiguration createSwissTimingArchiveConfiguration(String jsonUrl, String creatorName);

}
